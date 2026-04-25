package com.maghert.exampaper.service;

import cn.hutool.core.lang.Snowflake;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.PageResult;
import com.maghert.exampaper.context.RequestContext;
import com.maghert.exampaper.entity.ClassSnapshotEntity;
import com.maghert.exampaper.entity.PaperAuditLogEntity;
import com.maghert.exampaper.entity.PaperEntity;
import com.maghert.exampaper.entity.PaperPublishClassEntity;
import com.maghert.exampaper.entity.PaperQuestionBindingEntity;
import com.maghert.exampaper.entity.QuestionCategorySnapshotEntity;
import com.maghert.exampaper.entity.QuestionSnapshotEntity;
import com.maghert.exampaper.model.dto.PaperAuditRequest;
import com.maghert.exampaper.model.dto.PaperAutoCreateRequest;
import com.maghert.exampaper.model.dto.PaperManualCreateRequest;
import com.maghert.exampaper.model.dto.PaperPublishRequest;
import com.maghert.exampaper.model.dto.PaperQueryRequest;
import com.maghert.exampaper.model.dto.PaperRecycleRequest;
import com.maghert.exampaper.model.dto.PaperTerminateRequest;
import com.maghert.exampaper.model.dto.PaperUpdateRequest;
import com.maghert.exampaper.model.enums.PaperLifecycleStatus;
import com.maghert.exampaper.model.vo.PaperExportView;
import com.maghert.exampaper.model.vo.PaperView;
import com.maghert.exampaper.repository.PaperDomainRepository;
import com.maghert.exampaper.service.impl.PaperServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperServiceImplTests {

    private static final RequestContext TEACHER_CONTEXT = new RequestContext(3001L, 3, "req-teacher");
    private static final RequestContext ADMIN_CONTEXT = new RequestContext(2001L, 2, "req-admin");

    private InMemoryPaperDomainRepository repository;
    private PaperServiceImpl service;
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        repository = new InMemoryPaperDomainRepository();
        tempDir = Files.createDirectories(Path.of("target", "paper-service-tests", Long.toString(System.nanoTime())));
        service = new PaperServiceImpl(new Snowflake(1, 1), repository, tempDir.toString());
        seedBaseData();
    }

    @Test
    void manualCreateShouldIncreaseReferenceCountAndReturnDraft() throws Exception {
        PaperManualCreateRequest request = new PaperManualCreateRequest();
        request.setPaperName("Java 基础卷");
        request.setQuestionIds(List.of(101L, 201L));
        request.setExamTime(90);
        request.setPassScore(2);

        PaperView view = (PaperView) service.createManual(request, TEACHER_CONTEXT).getData();

        assertEquals(PaperLifecycleStatus.DRAFT.name(), view.getStatus());
        assertEquals(List.of(101L, 201L), view.getQuestionIds());
        assertEquals(1, repository.questions.get(101L).getReferenceCount());
        assertEquals(1, repository.questions.get(201L).getReferenceCount());
    }

    @Test
    void queryShouldOnlyReturnCurrentTeacherPapers() throws Exception {
        seedQueryPaper(9201L, "我的试卷", 3001L, PaperLifecycleStatus.DRAFT, "MANUAL", List.of(501L), 101L);
        seedQueryPaper(9202L, "别人的试卷", 3002L, PaperLifecycleStatus.PUBLISHED, "AUTO", List.of(502L), 301L);

        PaperQueryRequest request = new PaperQueryRequest();
        request.setPageNum(1L);
        request.setPageSize(10L);

        PageResult<PaperView> page = (PageResult<PaperView>) service.query(request, TEACHER_CONTEXT).getData();

        assertEquals(1, page.getTotal());
        assertEquals(1, page.getRecords().size());
        assertEquals(9201L, page.getRecords().get(0).getPaperId());
    }

    @Test
    void queryShouldFilterByClassStatusAndSourceTypeForAuditor() throws Exception {
        seedQueryPaper(9301L, "已发布自动卷", 3001L, PaperLifecycleStatus.PUBLISHED, "AUTO", List.of(501L), 101L);
        seedQueryPaper(9302L, "草稿手动卷", 3001L, PaperLifecycleStatus.DRAFT, "MANUAL", List.of(), 201L);

        PaperQueryRequest request = new PaperQueryRequest();
        request.setClassId(501L);
        request.setStatus(PaperLifecycleStatus.PUBLISHED.name());
        request.setSourceType("AUTO");
        request.setPageNum(1L);
        request.setPageSize(10L);

        PageResult<PaperView> page = (PageResult<PaperView>) service.query(request, new RequestContext(5001L, 5, "req-auditor")).getData();

        assertEquals(1, page.getTotal());
        assertEquals(9301L, page.getRecords().get(0).getPaperId());
        assertEquals(List.of(501L), page.getRecords().get(0).getClassIds());
    }

    @Test
    void manualCreateShouldRejectForeignPersonalQuestion() {
        PaperManualCreateRequest request = new PaperManualCreateRequest();
        request.setPaperName("越权试卷");
        request.setQuestionIds(List.of(301L));
        request.setExamTime(90);
        request.setPassScore(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createManual(request, TEACHER_CONTEXT));
        assertEquals(403, exception.getCode());
    }

    @Test
    void updateShouldResetRejectedPaperToDraftAndAdjustReferenceCount() throws Exception {
        PaperEntity paper = repository.savePaper(new PaperEntity()
                .setPaperId(9001L)
                .setPaperName("待修改试卷")
                .setCreatorId(3001L)
                .setCreatorRoleId(3)
                .setStatus(PaperLifecycleStatus.REJECTED.name())
                .setSourceType("MANUAL")
                .setExamTime(90)
                .setPassScore(1)
                .setTotalScore(1)
                .setRequestId("seed"));
        repository.replacePaperQuestions(paper.getPaperId(), List.of(new PaperQuestionBindingEntity()
                .setBindingId(1L)
                .setPaperId(paper.getPaperId())
                .setQuestionId(101L)
                .setSortNo(1)
                .setAssignedScore(1)
                .setQuestionType("single")
                .setDifficulty(2)));
        repository.updateQuestionReferenceCount(101L, 1);

        PaperUpdateRequest request = new PaperUpdateRequest();
        request.setPaperId(paper.getPaperId());
        request.setQuestionIds(List.of(201L));
        request.setPassScore(1);

        PaperView view = (PaperView) service.update(request, TEACHER_CONTEXT).getData();

        assertEquals(PaperLifecycleStatus.DRAFT.name(), view.getStatus());
        assertEquals(List.of(201L), view.getQuestionIds());
        assertEquals(0, repository.questions.get(101L).getReferenceCount());
        assertEquals(1, repository.questions.get(201L).getReferenceCount());
    }

    @Test
    void auditShouldWriteAuditLog() throws Exception {
        Long paperId = seedDraftPaper();
        PaperAuditRequest request = new PaperAuditRequest();
        request.setPaperId(paperId);
        request.setAuditResult("approved");

        PaperView view = (PaperView) service.audit(request, ADMIN_CONTEXT).getData();

        assertEquals(PaperLifecycleStatus.APPROVED.name(), view.getStatus());
        assertEquals(1, repository.auditLogs.size());
        assertEquals(PaperLifecycleStatus.APPROVED.name(), repository.auditLogs.get(0).getAuditResult());
    }

    @Test
    void publishShouldOnlyAllowTeacherOwnClasses() throws Exception {
        Long paperId = seedDraftPaper();
        PaperPublishRequest request = new PaperPublishRequest();
        request.setPaperId(paperId);
        request.setExamTime("2026-04-20 10:00:00");
        request.setClassIds(List.of(502L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.publish(request, TEACHER_CONTEXT));
        assertEquals(403, exception.getCode());
    }

    @Test
    void publishShouldPersistClassRelations() throws Exception {
        Long paperId = seedDraftPaper();
        PaperPublishRequest request = new PaperPublishRequest();
        request.setPaperId(paperId);
        request.setExamTime("2026-04-10 10:00:00");
        request.setClassIds(List.of(501L));

        PaperView view = (PaperView) service.publish(request, TEACHER_CONTEXT).getData();

        assertEquals(PaperLifecycleStatus.PUBLISHED.name(), view.getStatus());
        assertEquals(List.of(501L), view.getClassIds());
        assertEquals(1, repository.publishClasses.get(paperId).size());
    }

    @Test
    void terminateShouldAllowPublishedPaperAndWriteReasonLog() throws Exception {
        Long paperId = seedDraftPaper();
        repository.papers.get(paperId)
                .setStatus(PaperLifecycleStatus.PUBLISHED.name())
                .setScheduledExamTime(LocalDateTime.now().plusHours(1));

        PaperTerminateRequest request = new PaperTerminateRequest();
        request.setPaperId(paperId);
        request.setReason("监考安排调整，终止本场考试");

        PaperView view = (PaperView) service.terminate(request, TEACHER_CONTEXT).getData();

        assertEquals(PaperLifecycleStatus.TERMINATED.name(), view.getStatus());
        assertEquals(PaperLifecycleStatus.TERMINATED.name(), repository.papers.get(paperId).getStatus());
        assertEquals(1, repository.auditLogs.size());
        assertEquals(PaperLifecycleStatus.TERMINATED.name(), repository.auditLogs.get(0).getAuditResult());
        assertEquals("监考安排调整，终止本场考试", repository.auditLogs.get(0).getReason());
    }

    @Test
    void terminateShouldRejectNonPublishedPaper() {
        Long paperId = seedDraftPaper();

        PaperTerminateRequest request = new PaperTerminateRequest();
        request.setPaperId(paperId);
        request.setReason("草稿不应允许终止");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.terminate(request, TEACHER_CONTEXT));
        assertEquals(409, exception.getCode());
    }

    @Test
    void exportShouldOnlyReturnTeacherOwnPapersWithWatermark() throws Exception {
        seedQueryPaper(9401L, "我的导出试卷", 3001L, PaperLifecycleStatus.PUBLISHED, "MANUAL", List.of(501L), 101L);
        seedQueryPaper(9402L, "别人的导出试卷", 3002L, PaperLifecycleStatus.PUBLISHED, "AUTO", List.of(502L), 301L);

        PaperExportView exportView = service.export(null, null, TEACHER_CONTEXT).getData();
        String csv = Files.readString(tempDir.resolve("exports").resolve(exportView.getFileKey() + ".csv"));

        assertTrue(csv.contains("paperId,paperName,status"));
        assertTrue(csv.contains("9401"));
        assertFalse(csv.contains("9402"));
        assertTrue(csv.contains("\"exportedBy=teacher#3001\""));
        assertEquals(1, exportView.getRecordCount());
    }

    @Test
    void exportShouldRequireApproverForAuditor() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.export(null, null, new RequestContext(5001L, 5, "req-auditor")));
        assertEquals(403, exception.getCode());
    }

    @Test
    void recycleShouldRejectPaperBeforeExamEnded() throws Exception {
        Long paperId = seedDraftPaper();
        repository.papers.get(paperId)
                .setStatus(PaperLifecycleStatus.PUBLISHED.name())
                .setScheduledExamTime(LocalDateTime.now().minusMinutes(10))
                .setExamTime(90);

        PaperRecycleRequest request = new PaperRecycleRequest();
        request.setPaperId(paperId);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.recycle(request, TEACHER_CONTEXT));
        assertEquals(409, exception.getCode());
    }

    @Test
    void autoCreateShouldSelectQuestionsByRatio() throws Exception {
        PaperAutoCreateRequest request = new PaperAutoCreateRequest();
        request.setPaperName("自动组卷");
        request.setTotalScore(2);
        request.setTypeRatio(Map.of("single", 1, "multi", 1));
        request.setDifficultyRatio(Map.of("2", 1, "3", 1));
        request.setKnowledgeRatio(Map.of("1001", 1, "2001", 1));

        PaperView view = (PaperView) service.createAuto(request, TEACHER_CONTEXT).getData();

        assertEquals(2, view.getQuestionIds().size());
        assertTrue(view.getQuestionIds().contains(101L));
        assertTrue(view.getQuestionIds().contains(201L));
        assertEquals(1, repository.questions.get(101L).getReferenceCount());
        assertEquals(1, repository.questions.get(201L).getReferenceCount());
    }

    private Long seedDraftPaper() {
        PaperEntity paper = repository.savePaper(new PaperEntity()
                .setPaperId(9101L)
                .setPaperName("草稿试卷")
                .setCreatorId(3001L)
                .setCreatorRoleId(3)
                .setStatus(PaperLifecycleStatus.DRAFT.name())
                .setSourceType("MANUAL")
                .setExamTime(90)
                .setPassScore(1)
                .setTotalScore(1)
                .setRequestId("seed"));
        repository.replacePaperQuestions(paper.getPaperId(), List.of(new PaperQuestionBindingEntity()
                .setBindingId(2L)
                .setPaperId(paper.getPaperId())
                .setQuestionId(101L)
                .setSortNo(1)
                .setAssignedScore(1)
                .setQuestionType("single")
                .setDifficulty(2)));
        repository.updateQuestionReferenceCount(101L, 1);
        return paper.getPaperId();
    }

    private void seedQueryPaper(Long paperId,
                                String paperName,
                                Long creatorId,
                                PaperLifecycleStatus status,
                                String sourceType,
                                List<Long> classIds,
                                Long questionId) {
        PaperEntity paper = repository.savePaper(new PaperEntity()
                .setPaperId(paperId)
                .setPaperName(paperName)
                .setCreatorId(creatorId)
                .setCreatorRoleId(3)
                .setStatus(status.name())
                .setSourceType(sourceType)
                .setExamTime(90)
                .setPassScore(1)
                .setTotalScore(1)
                .setRequestId("seed-query")
                .setCreateTime(LocalDateTime.now().minusDays(1))
                .setUpdateTime(LocalDateTime.now()));
        repository.replacePaperQuestions(paper.getPaperId(), List.of(new PaperQuestionBindingEntity()
                .setBindingId(paperId + 10000)
                .setPaperId(paper.getPaperId())
                .setQuestionId(questionId)
                .setSortNo(1)
                .setAssignedScore(1)
                .setQuestionType(repository.questions.get(questionId).getQuestionType())
                .setDifficulty(repository.questions.get(questionId).getDifficulty())));
        if (!classIds.isEmpty()) {
            repository.replacePaperPublishClasses(paper.getPaperId(), classIds.stream()
                    .map(classId -> new PaperPublishClassEntity()
                            .setRelationId(paperId + classId)
                            .setPaperId(paper.getPaperId())
                            .setClassId(classId))
                    .toList());
        }
    }

    private void seedBaseData() {
        repository.categories.put(1001L, new QuestionCategorySnapshotEntity()
                .setCategoryId(1001L)
                .setPersonal(true)
                .setOwnerId(3001L)
                .setStatus(1));
        repository.categories.put(1002L, new QuestionCategorySnapshotEntity()
                .setCategoryId(1002L)
                .setPersonal(true)
                .setOwnerId(3002L)
                .setStatus(1));
        repository.categories.put(2001L, new QuestionCategorySnapshotEntity()
                .setCategoryId(2001L)
                .setPersonal(false)
                .setOwnerId(null)
                .setStatus(1));

        repository.questions.put(101L, new QuestionSnapshotEntity()
                .setQuestionId(101L)
                .setCategoryId(1001L)
                .setCreatorId(3001L)
                .setCreatorRoleId(3)
                .setQuestionType("single")
                .setDifficulty(2)
                .setAuditStatus("approved")
                .setDisabled(false)
                .setReferenceCount(0));
        repository.questions.put(201L, new QuestionSnapshotEntity()
                .setQuestionId(201L)
                .setCategoryId(2001L)
                .setCreatorId(2001L)
                .setCreatorRoleId(2)
                .setQuestionType("multi")
                .setDifficulty(3)
                .setAuditStatus("approved")
                .setDisabled(false)
                .setReferenceCount(0));
        repository.questions.put(301L, new QuestionSnapshotEntity()
                .setQuestionId(301L)
                .setCategoryId(1002L)
                .setCreatorId(3002L)
                .setCreatorRoleId(3)
                .setQuestionType("single")
                .setDifficulty(2)
                .setAuditStatus("approved")
                .setDisabled(false)
                .setReferenceCount(0));

        repository.classes.put(501L, new ClassSnapshotEntity()
                .setClassId(501L)
                .setClassName("Java 1班")
                .setCreatedBy(3001L)
                .setTeacherId(3001L)
                .setStatus("active"));
        repository.classes.put(502L, new ClassSnapshotEntity()
                .setClassId(502L)
                .setClassName("Python 1班")
                .setCreatedBy(3002L)
                .setTeacherId(3002L)
                .setStatus("active"));
    }

    private static final class InMemoryPaperDomainRepository implements PaperDomainRepository {

        private final Map<Long, PaperEntity> papers = new LinkedHashMap<>();
        private final Map<Long, List<PaperQuestionBindingEntity>> paperQuestions = new LinkedHashMap<>();
        private final Map<Long, List<PaperPublishClassEntity>> publishClasses = new LinkedHashMap<>();
        private final List<PaperAuditLogEntity> auditLogs = new ArrayList<>();
        private final Map<Long, QuestionSnapshotEntity> questions = new LinkedHashMap<>();
        private final Map<Long, QuestionCategorySnapshotEntity> categories = new LinkedHashMap<>();
        private final Map<Long, ClassSnapshotEntity> classes = new LinkedHashMap<>();

        @Override
        public PaperEntity savePaper(PaperEntity paper) {
            papers.put(paper.getPaperId(), paper);
            return paper;
        }

        @Override
        public void updatePaper(PaperEntity paper) {
            papers.put(paper.getPaperId(), paper);
        }

        @Override
        public Optional<PaperEntity> findPaperById(Long paperId) {
            return Optional.ofNullable(papers.get(paperId));
        }

        @Override
        public List<PaperEntity> listPapers() {
            return new ArrayList<>(papers.values());
        }

        @Override
        public void deletePaper(Long paperId) {
            papers.remove(paperId);
        }

        @Override
        public void replacePaperQuestions(Long paperId, List<PaperQuestionBindingEntity> bindings) {
            paperQuestions.put(paperId, new ArrayList<>(bindings));
        }

        @Override
        public List<PaperQuestionBindingEntity> listPaperQuestions(Long paperId) {
            return new ArrayList<>(paperQuestions.getOrDefault(paperId, List.of()));
        }

        @Override
        public void deletePaperQuestions(Long paperId) {
            paperQuestions.remove(paperId);
        }

        @Override
        public void replacePaperPublishClasses(Long paperId, List<PaperPublishClassEntity> relations) {
            publishClasses.put(paperId, new ArrayList<>(relations));
        }

        @Override
        public List<PaperPublishClassEntity> listPaperPublishClasses(Long paperId) {
            return new ArrayList<>(publishClasses.getOrDefault(paperId, List.of()));
        }

        @Override
        public void deletePaperPublishClasses(Long paperId) {
            publishClasses.remove(paperId);
        }

        @Override
        public void saveAuditLog(PaperAuditLogEntity auditLog) {
            auditLogs.add(auditLog);
        }

        @Override
        public List<QuestionSnapshotEntity> findQuestionsByIds(java.util.Collection<Long> questionIds) {
            return questionIds.stream()
                    .map(questions::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public List<QuestionSnapshotEntity> listQuestions() {
            return new ArrayList<>(questions.values());
        }

        @Override
        public List<QuestionCategorySnapshotEntity> findQuestionCategoriesByIds(java.util.Collection<Long> categoryIds) {
            return categoryIds.stream()
                    .map(categories::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public List<ClassSnapshotEntity> findClassesByIds(java.util.Collection<Long> classIds) {
            return classIds.stream()
                    .map(classes::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public void updateQuestionReferenceCount(Long questionId, int delta) {
            QuestionSnapshotEntity question = questions.get(questionId);
            if (question == null) {
                return;
            }
            int current = question.getReferenceCount() == null ? 0 : question.getReferenceCount();
            question.setReferenceCount(Math.max(0, current + delta));
        }
    }
}
