package com.maghert.examcore.service;

import cn.hutool.core.lang.Snowflake;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examcore.context.RequestContext;
import com.maghert.examcore.entity.ClassMemberSnapshotEntity;
import com.maghert.examcore.entity.ClassSnapshotEntity;
import com.maghert.examcore.entity.ExamClassRelationEntity;
import com.maghert.examcore.entity.ExamEntity;
import com.maghert.examcore.entity.ExamRetestApplyEntity;
import com.maghert.examcore.entity.ExamStatusLogEntity;
import com.maghert.examcore.entity.ExamStudentRelationEntity;
import com.maghert.examcore.entity.PaperQuestionSnapshotEntity;
import com.maghert.examcore.entity.PaperPublishClassSnapshotEntity;
import com.maghert.examcore.entity.PaperSnapshotEntity;
import com.maghert.examcore.entity.QuestionSnapshotEntity;
import com.maghert.examcore.entity.ScoreDetailEntity;
import com.maghert.examcore.entity.ScoreRecordEntity;
import com.maghert.examcore.entity.UserSnapshotEntity;
import com.maghert.examcore.model.dto.ExamApproveRetestRequest;
import com.maghert.examcore.model.dto.ExamApplyRetestRequest;
import com.maghert.examcore.model.dto.ExamCreateRequest;
import com.maghert.examcore.model.dto.ExamDistributeRequest;
import com.maghert.examcore.model.dto.ExamQueryRequest;
import com.maghert.examcore.model.dto.ExamSubmitRequest;
import com.maghert.examcore.model.dto.ExamToggleStatusRequest;
import com.maghert.examcore.model.dto.ExamUpdateParamsRequest;
import com.maghert.examcore.model.enums.ExamLifecycleStatus;
import com.maghert.examcore.model.vo.ExamDistributeView;
import com.maghert.examcore.model.vo.ExamView;
import com.maghert.examcore.repository.ExamDomainRepository;
import com.maghert.examcore.service.impl.ExamCoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamCoreServiceImplTests {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final RequestContext TEACHER_CONTEXT = new RequestContext(3001L, 3, "req-teacher");
    private static final RequestContext ADMIN_CONTEXT = new RequestContext(2001L, 2, "req-admin");
    private static final RequestContext AUDITOR_CONTEXT = new RequestContext(5001L, 5, "req-auditor");
    private static final RequestContext STUDENT_CONTEXT = new RequestContext(4001L, 4, "req-student");
    private static final RequestContext OPERATOR_CONTEXT = new RequestContext(6001L, 6, "req-ops");

    private InMemoryExamDomainRepository repository;
    private ExamCoreServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryExamDomainRepository();
        service = new ExamCoreServiceImpl(new Snowflake(1, 1), repository);
        seedBaseData();
    }

    @Test
    void createShouldPersistDraftExamForTeacherOwnClasses() throws Exception {
        ExamCreateRequest request = new ExamCreateRequest();
        request.setExamName("Java 期中考试");
        request.setPaperId(9101L);
        request.setClassIds(List.of(501L));
        request.setStartTime("2026-05-01 09:00:00");
        request.setDuration(120);

        ExamView view = (ExamView) service.create(request, TEACHER_CONTEXT).getData();

        assertEquals(ExamLifecycleStatus.DRAFT.name(), view.getStatus());
        assertEquals("Java 期中考试", view.getExamName());
        assertEquals(9101L, view.getPaperId());
        assertEquals(List.of(501L), view.getClassIds());
        assertEquals(1, repository.exams.size());
        assertEquals(1, repository.examClasses.get(view.getExamId()).size());
    }

    @Test
    void createShouldRejectTeacherForeignClass() {
        ExamCreateRequest request = new ExamCreateRequest();
        request.setExamName("越权班级考试");
        request.setPaperId(9101L);
        request.setClassIds(List.of(502L));
        request.setStartTime("2026-05-01 09:00:00");
        request.setDuration(120);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create(request, TEACHER_CONTEXT));
        assertEquals(403, exception.getCode());
    }

    @Test
    void createShouldRejectNonPublishedPaper() {
        repository.papers.get(9102L).setStatus("APPROVED");

        ExamCreateRequest request = new ExamCreateRequest();
        request.setExamName("未发布试卷考试");
        request.setPaperId(9102L);
        request.setClassIds(List.of(501L));
        request.setStartTime("2026-05-01 09:00:00");
        request.setDuration(120);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create(request, ADMIN_CONTEXT));
        assertEquals(409, exception.getCode());
    }

    @Test
    void createShouldRejectClassesOutsidePaperPublishedScope() {
        ExamCreateRequest request = new ExamCreateRequest();
        request.setExamName("超范围分发考试");
        request.setPaperId(9101L);
        request.setClassIds(List.of(503L));
        request.setStartTime("2026-05-01 09:00:00");
        request.setDuration(120);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create(request, ADMIN_CONTEXT));
        assertEquals(409, exception.getCode());
    }

    @Test
    void queryShouldReturnTeacherVisibleExamOnly() throws Exception {
        seedExam(8801L, "Java 周测", "DRAFT", 3001L, "2026-05-01T09:00:00", List.of(501L));
        seedExam(8802L, "Python 月考", "PUBLISHED", 2001L, "2026-05-03T09:00:00", List.of(502L));

        ExamQueryRequest request = new ExamQueryRequest();
        request.setPageNum(1L);
        request.setPageSize(10L);

        PageResult<ExamView> result = service.query(request, TEACHER_CONTEXT).getData();

        assertEquals(1, result.getTotal());
        assertEquals(8801L, result.getRecords().get(0).getExamId());
    }

    @Test
    void queryShouldSupportAdminFilterByStudentAndStatus() throws Exception {
        seedExam(8801L, "Java 周测", "DRAFT", 3001L, "2026-05-01T09:00:00", List.of(501L));
        seedExam(8802L, "Python 月考", "PUBLISHED", 2001L, "2026-05-03T09:00:00", List.of(502L));
        seedExam(8803L, "Java 期末考试", "PUBLISHED", 2001L, "2026-05-05T09:00:00", List.of(501L, 503L));

        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(1L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("APPROVED"));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(2L)
                .setClassId(502L)
                .setStudentId(4001L)
                .setStatus("PENDING"));
        seedDistributedStudent(8803L, 501L, 4001L);

        ExamQueryRequest request = new ExamQueryRequest();
        request.setStudentId(4001L);
        request.setStatus("PUBLISHED");
        request.setKeyword("期末");
        request.setPageNum(1L);
        request.setPageSize(10L);

        PageResult<ExamView> result = service.query(request, ADMIN_CONTEXT).getData();

        assertEquals(1, result.getTotal());
        assertEquals(8803L, result.getRecords().get(0).getExamId());
    }

    @Test
    void queryShouldOnlyAllowStudentQuerySelfDistributedExams() throws Exception {
        seedExam(8801L, "Java 周测", "DRAFT", 3001L, "2026-05-01T09:00:00", List.of(501L));
        seedExam(8802L, "Python 月考", "PUBLISHED", 2001L, "2026-05-03T09:00:00", List.of(502L));

        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(1L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("APPROVED"));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(2L)
                .setClassId(502L)
                .setStudentId(4001L)
                .setStatus("PENDING"));
        seedDistributedStudent(8801L, 501L, 4001L);

        ExamQueryRequest request = new ExamQueryRequest();
        request.setPageNum(1L);
        request.setPageSize(10L);

        PageResult<ExamView> result = service.query(request, STUDENT_CONTEXT).getData();

        assertEquals(1, result.getTotal());
        assertEquals(8801L, result.getRecords().get(0).getExamId());

        ExamQueryRequest forbiddenRequest = new ExamQueryRequest();
        forbiddenRequest.setStudentId(4002L);
        forbiddenRequest.setPageNum(1L);
        forbiddenRequest.setPageSize(10L);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.query(forbiddenRequest, STUDENT_CONTEXT));
        assertEquals(403, exception.getCode());
    }

    @Test
    void distributeShouldPersistApprovedStudentsForExam() throws Exception {
        seedExam(8801L, "Java 周测", "DRAFT", 3001L, LocalDateTime.now().plusDays(1), List.of(501L, 503L));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(1L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("APPROVED"));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(2L)
                .setClassId(503L)
                .setStudentId(4003L)
                .setStatus("APPROVED"));

        ExamDistributeRequest request = new ExamDistributeRequest();
        request.setExamId(8801L);
        request.setStudentIds(List.of(4001L, 4003L));

        ExamDistributeView result = service.distribute(request, TEACHER_CONTEXT).getData();

        assertEquals(2, result.getDistributedCount());
        assertEquals(List.of(4001L, 4003L), result.getStudentIds());
        assertEquals(2, repository.examStudents.get(8801L).size());
        assertEquals(2, repository.listScoresByExamId(8801L).size());
        assertEquals(4, repository.scoreDetails.stream()
                .filter(item -> item.getExamId().equals(8801L))
                .count());

        ScoreRecordEntity firstScore = repository.listScoresByExamId(8801L).stream()
                .filter(item -> item.getStudentId().equals(4001L))
                .findFirst()
                .orElseThrow();
        assertEquals("PENDING", firstScore.getStatus());
        assertEquals("张三", firstScore.getStudentName());
        assertEquals("Java 1班", firstScore.getClassName());

        ScoreDetailEntity firstDetail = repository.scoreDetails.stream()
                .filter(item -> item.getScoreId().equals(firstScore.getScoreId()))
                .filter(item -> item.getQuestionId().equals(7001L))
                .findFirst()
                .orElseThrow();
        assertEquals(1, firstDetail.getSortNo());
        assertEquals("single", firstDetail.getQuestionType());
        assertEquals("JDK 默认编码", firstDetail.getQuestionStem());
        assertEquals("A", firstDetail.getCorrectAnswer());
        assertEquals(5, firstDetail.getAssignedScore());
        assertEquals(0, firstDetail.getScore());
        assertEquals(null, firstDetail.getStudentAnswer());
    }

    @Test
    void distributeShouldRejectPendingOrOutOfScopeStudents() {
        seedExam(8801L, "Java 周测", "DRAFT", 3001L, LocalDateTime.now().plusDays(1), List.of(501L));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(1L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("PENDING"));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(2L)
                .setClassId(502L)
                .setStudentId(4002L)
                .setStatus("APPROVED"));

        ExamDistributeRequest pendingRequest = new ExamDistributeRequest();
        pendingRequest.setExamId(8801L);
        pendingRequest.setStudentIds(List.of(4001L));
        BusinessException pendingException = assertThrows(BusinessException.class,
                () -> service.distribute(pendingRequest, TEACHER_CONTEXT));
        assertEquals(409, pendingException.getCode());

        ExamDistributeRequest scopeRequest = new ExamDistributeRequest();
        scopeRequest.setExamId(8801L);
        scopeRequest.setStudentIds(List.of(4002L));
        BusinessException scopeException = assertThrows(BusinessException.class,
                () -> service.distribute(scopeRequest, TEACHER_CONTEXT));
        assertEquals(409, scopeException.getCode());
    }

    @Test
    void distributeShouldRejectRedistributeWhenScoreAlreadyStarted() {
        seedExam(8801L, "Java 周测", "DRAFT", 3001L, LocalDateTime.now().plusDays(1), List.of(501L));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(1L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("APPROVED"));
        repository.scoreRecords.add(new ScoreRecordEntity()
                .setScoreId(9101L)
                .setExamId(8801L)
                .setExamName("Java 周测")
                .setPaperId(9101L)
                .setPaperName("已发布试卷")
                .setClassId(501L)
                .setClassName("Java 1班")
                .setStudentId(4001L)
                .setStudentName("张三")
                .setTotalScore(60)
                .setObjectiveScore(50)
                .setSubjectiveScore(10)
                .setStatus("SCORING"));

        ExamDistributeRequest request = new ExamDistributeRequest();
        request.setExamId(8801L);
        request.setStudentIds(List.of(4001L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.distribute(request, TEACHER_CONTEXT));
        assertEquals(409, exception.getCode());
        assertEquals(1522, exception.getErrorCode());
    }

    @Test
    void submitShouldPersistAnswersAndSubmittedAtForStudent() throws Exception {
        seedExam(8801L, "Java 周测", "PUBLISHED", 3001L, LocalDateTime.now().minusMinutes(10), List.of(501L));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(1L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("APPROVED"));

        ExamDistributeRequest distributeRequest = new ExamDistributeRequest();
        distributeRequest.setExamId(8801L);
        distributeRequest.setStudentIds(List.of(4001L));
        service.distribute(distributeRequest, TEACHER_CONTEXT);

        ExamSubmitRequest submitRequest = new ExamSubmitRequest();
        submitRequest.setExamId(8801L);
        submitRequest.setStudentId(4001L);
        submitRequest.setAnswers(List.of(
                answerItem(7001L, " A "),
                answerItem(7002L, "JVM 包含堆、栈、方法区")
        ));

        var result = service.submit(submitRequest, STUDENT_CONTEXT).getData();

        assertEquals(8801L, result.getExamId());
        assertEquals(4001L, result.getStudentId());
        assertEquals("SCORING", result.getStatus());
        assertEquals(2, result.getAnsweredCount());
        ScoreRecordEntity score = repository.findScoreByExamIdAndStudentId(8801L, 4001L).orElseThrow();
        assertTrue(score.getSubmittedAt() != null);
        assertEquals("SCORING", score.getStatus());
        assertEquals(5, score.getObjectiveScore());
        assertEquals(0, score.getSubjectiveScore());
        assertEquals(5, score.getTotalScore());
        List<ScoreDetailEntity> details = repository.listScoreDetails(score.getScoreId());
        ScoreDetailEntity objectiveDetail = details.stream()
                .filter(item -> item.getQuestionId().equals(7001L))
                .findFirst()
                .orElseThrow();
        assertEquals(" A ", objectiveDetail.getStudentAnswer());
        assertEquals(true, objectiveDetail.getCorrect());
        assertEquals(5, objectiveDetail.getScore());
        assertEquals("JVM 包含堆、栈、方法区", details.stream()
                .filter(item -> item.getQuestionId().equals(7002L))
                .findFirst()
                .orElseThrow()
                .getStudentAnswer());
    }

    @Test
    void submitShouldRejectForbiddenConflictAndInvalidQuestion() throws Exception {
        seedExam(8801L, "Java 周测", "PUBLISHED", 3001L, LocalDateTime.now().minusMinutes(5), List.of(501L));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(1L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("APPROVED"));

        ExamDistributeRequest distributeRequest = new ExamDistributeRequest();
        distributeRequest.setExamId(8801L);
        distributeRequest.setStudentIds(List.of(4001L));
        service.distribute(distributeRequest, TEACHER_CONTEXT);

        ExamSubmitRequest forbiddenRequest = new ExamSubmitRequest();
        forbiddenRequest.setExamId(8801L);
        forbiddenRequest.setStudentId(4001L);
        BusinessException forbiddenException = assertThrows(BusinessException.class,
                () -> service.submit(forbiddenRequest, TEACHER_CONTEXT));
        assertEquals(403, forbiddenException.getCode());
        assertEquals(1523, forbiddenException.getErrorCode());

        ExamSubmitRequest invalidQuestionRequest = new ExamSubmitRequest();
        invalidQuestionRequest.setExamId(8801L);
        invalidQuestionRequest.setStudentId(4001L);
        invalidQuestionRequest.setAnswers(List.of(answerItem(9999L, "X")));
        BusinessException invalidQuestionException = assertThrows(BusinessException.class,
                () -> service.submit(invalidQuestionRequest, STUDENT_CONTEXT));
        assertEquals(409, invalidQuestionException.getCode());
        assertEquals(1525, invalidQuestionException.getErrorCode());

        ExamSubmitRequest submitRequest = new ExamSubmitRequest();
        submitRequest.setExamId(8801L);
        submitRequest.setStudentId(4001L);
        submitRequest.setAnswers(List.of(answerItem(7001L, "A")));
        service.submit(submitRequest, STUDENT_CONTEXT);

        BusinessException duplicateSubmitException = assertThrows(BusinessException.class,
                () -> service.submit(submitRequest, STUDENT_CONTEXT));
        assertEquals(409, duplicateSubmitException.getCode());
        assertEquals(1524, duplicateSubmitException.getErrorCode());
    }

    @Test
    void submitShouldRejectBeforeStartOrUnsupportedExamStatus() throws Exception {
        seedExam(8801L, "Java 周测", "PUBLISHED", 3001L, LocalDateTime.now().plusMinutes(20), List.of(501L));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(1L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("APPROVED"));
        ExamDistributeRequest distributeRequest = new ExamDistributeRequest();
        distributeRequest.setExamId(8801L);
        distributeRequest.setStudentIds(List.of(4001L));
        service.distribute(distributeRequest, TEACHER_CONTEXT);

        ExamSubmitRequest request = new ExamSubmitRequest();
        request.setExamId(8801L);
        request.setStudentId(4001L);
        BusinessException beforeStartException = assertThrows(BusinessException.class,
                () -> service.submit(request, STUDENT_CONTEXT));
        assertEquals(409, beforeStartException.getCode());
        assertEquals(1524, beforeStartException.getErrorCode());

        seedExam(8802L, "Java 期中", "PAUSED", 3001L, LocalDateTime.now().minusMinutes(20), List.of(501L));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(2L)
                .setClassId(501L)
                .setStudentId(4002L)
                .setStatus("APPROVED"));
        ExamDistributeRequest pausedDistributeRequest = new ExamDistributeRequest();
        pausedDistributeRequest.setExamId(8802L);
        pausedDistributeRequest.setStudentIds(List.of(4002L));
        service.distribute(pausedDistributeRequest, TEACHER_CONTEXT);

        ExamSubmitRequest pausedRequest = new ExamSubmitRequest();
        pausedRequest.setExamId(8802L);
        pausedRequest.setStudentId(4002L);
        BusinessException pausedException = assertThrows(BusinessException.class,
                () -> service.submit(pausedRequest, new RequestContext(4002L, 4, "req-student-2")));
        assertEquals(409, pausedException.getCode());
        assertEquals(1524, pausedException.getErrorCode());
    }

    @Test
    void submitShouldMarkScoreAsScoredWhenPaperContainsOnlyObjectiveQuestions() throws Exception {
        repository.paperQuestions.put(9101L, new ArrayList<>(List.of(
                new PaperQuestionSnapshotEntity()
                        .setBindingId(101L)
                        .setPaperId(9101L)
                        .setQuestionId(7001L)
                        .setSortNo(1)
                        .setAssignedScore(5)
                        .setQuestionType("single")
        )));
        seedExam(8801L, "Java 周测", "UNDERWAY", 3001L, LocalDateTime.now().minusMinutes(10), List.of(501L));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(1L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("APPROVED"));

        ExamDistributeRequest distributeRequest = new ExamDistributeRequest();
        distributeRequest.setExamId(8801L);
        distributeRequest.setStudentIds(List.of(4001L));
        service.distribute(distributeRequest, TEACHER_CONTEXT);

        ExamSubmitRequest submitRequest = new ExamSubmitRequest();
        submitRequest.setExamId(8801L);
        submitRequest.setStudentId(4001L);
        submitRequest.setAnswers(List.of(answerItem(7001L, "A")));

        var result = service.submit(submitRequest, STUDENT_CONTEXT).getData();

        assertEquals("SCORED", result.getStatus());
        ScoreRecordEntity score = repository.findScoreByExamIdAndStudentId(8801L, 4001L).orElseThrow();
        assertEquals("SCORED", score.getStatus());
        assertEquals(5, score.getTotalScore());
    }

    @Test
    void applyRetestShouldAllowStudentApplyAfterExamClosed() throws Exception {
        seedExam(8801L, "Java 周测", "UNDERWAY", 3001L, LocalDateTime.now().minusHours(3), List.of(501L));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(1L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("APPROVED"));

        ExamDistributeRequest distributeRequest = new ExamDistributeRequest();
        distributeRequest.setExamId(8801L);
        distributeRequest.setStudentIds(List.of(4001L));
        service.distribute(distributeRequest, TEACHER_CONTEXT);

        ExamApplyRetestRequest request = new ExamApplyRetestRequest();
        request.setExamId(8801L);
        request.setStudentId(4001L);
        request.setReason("考试当天发烧缺考");

        var result = service.applyRetest(request, STUDENT_CONTEXT).getData();

        assertEquals(8801L, result.getExamId());
        assertEquals(4001L, result.getStudentId());
        assertEquals("PENDING", result.getStatus());
        assertTrue(repository.findRetestApplyByExamIdAndStudentId(8801L, 4001L).isPresent());
    }

    @Test
    void applyRetestShouldRejectForbiddenConflictAndDuplicate() throws Exception {
        seedExam(8801L, "Java 周测", "UNDERWAY", 3001L, LocalDateTime.now().minusMinutes(30), List.of(501L));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(1L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("APPROVED"));
        ExamDistributeRequest distributeRequest = new ExamDistributeRequest();
        distributeRequest.setExamId(8801L);
        distributeRequest.setStudentIds(List.of(4001L));
        service.distribute(distributeRequest, TEACHER_CONTEXT);

        ExamApplyRetestRequest request = new ExamApplyRetestRequest();
        request.setExamId(8801L);
        request.setStudentId(4001L);
        request.setReason("申请补考");

        BusinessException beforeCloseException = assertThrows(BusinessException.class,
                () -> service.applyRetest(request, STUDENT_CONTEXT));
        assertEquals(409, beforeCloseException.getCode());
        assertEquals(1519, beforeCloseException.getErrorCode());

        seedExam(8802L, "Java 期中", "UNDERWAY", 3001L, LocalDateTime.now().minusHours(3), List.of(501L));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(2L)
                .setClassId(501L)
                .setStudentId(4002L)
                .setStatus("APPROVED"));
        ExamDistributeRequest secondDistributeRequest = new ExamDistributeRequest();
        secondDistributeRequest.setExamId(8802L);
        secondDistributeRequest.setStudentIds(List.of(4002L));
        service.distribute(secondDistributeRequest, TEACHER_CONTEXT);

        ExamApplyRetestRequest forbiddenRequest = new ExamApplyRetestRequest();
        forbiddenRequest.setExamId(8802L);
        forbiddenRequest.setStudentId(4002L);
        forbiddenRequest.setReason("申请补考");
        BusinessException forbiddenException = assertThrows(BusinessException.class,
                () -> service.applyRetest(forbiddenRequest, STUDENT_CONTEXT));
        assertEquals(403, forbiddenException.getCode());
        assertEquals(1526, forbiddenException.getErrorCode());

        ExamSubmitRequest submitRequest = new ExamSubmitRequest();
        submitRequest.setExamId(8802L);
        submitRequest.setStudentId(4002L);
        submitRequest.setAnswers(List.of(answerItem(7001L, "A")));
        service.submit(submitRequest, new RequestContext(4002L, 4, "req-student-2"));

        ExamApplyRetestRequest submittedRequest = new ExamApplyRetestRequest();
        submittedRequest.setExamId(8802L);
        submittedRequest.setStudentId(4002L);
        submittedRequest.setReason("已交卷后再申请");
        BusinessException submittedException = assertThrows(BusinessException.class,
                () -> service.applyRetest(submittedRequest, new RequestContext(4002L, 4, "req-student-2")));
        assertEquals(409, submittedException.getCode());
        assertEquals(1519, submittedException.getErrorCode());

        seedExam(8803L, "Java 期末", "UNDERWAY", 3001L, LocalDateTime.now().minusHours(4), List.of(501L));
        repository.classMembers.add(new ClassMemberSnapshotEntity()
                .setMemberId(3L)
                .setClassId(501L)
                .setStudentId(4003L)
                .setStatus("APPROVED"));
        ExamDistributeRequest thirdDistributeRequest = new ExamDistributeRequest();
        thirdDistributeRequest.setExamId(8803L);
        thirdDistributeRequest.setStudentIds(List.of(4003L));
        service.distribute(thirdDistributeRequest, TEACHER_CONTEXT);

        ExamApplyRetestRequest duplicateRequest = new ExamApplyRetestRequest();
        duplicateRequest.setExamId(8803L);
        duplicateRequest.setStudentId(4003L);
        duplicateRequest.setReason("第一次申请");
        service.applyRetest(duplicateRequest, new RequestContext(4003L, 4, "req-student-3"));

        BusinessException duplicateException = assertThrows(BusinessException.class,
                () -> service.applyRetest(duplicateRequest, new RequestContext(4003L, 4, "req-student-3")));
        assertEquals(409, duplicateException.getCode());
        assertEquals(1519, duplicateException.getErrorCode());
    }

    @Test
    void queryShouldRejectUnauthorizedRole() {
        ExamQueryRequest request = new ExamQueryRequest();
        request.setPageNum(1L);
        request.setPageSize(10L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.query(request, OPERATOR_CONTEXT));
        assertEquals(403, exception.getCode());
    }

    @Test
    void queryShouldReturnPagedNewestFirstForAuditor() throws Exception {
        seedExam(8801L, "Java 周测", "DRAFT", 3001L, "2026-05-01T09:00:00", List.of(501L));
        seedExam(8802L, "Python 月考", "PUBLISHED", 2001L, "2026-05-03T09:00:00", List.of(502L));
        seedExam(8803L, "Java 期末考试", "PUBLISHED", 2001L, "2026-05-05T09:00:00", List.of(501L, 503L));

        ExamQueryRequest request = new ExamQueryRequest();
        request.setPageNum(1L);
        request.setPageSize(2L);

        PageResult<ExamView> result = service.query(request, AUDITOR_CONTEXT).getData();

        assertEquals(3, result.getTotal());
        assertEquals(2, result.getRecords().size());
        assertEquals(List.of(8803L, 8802L),
                result.getRecords().stream().map(ExamView::getExamId).toList());
        assertTrue(result.getRecords().get(0).getClassIds().contains(501L));
    }

    @Test
    void updateParamsShouldAllowCreatorUpdateBeforeStart() throws Exception {
        Long examId = 8801L;
        repository.exams.put(examId, new ExamEntity()
                .setExamId(examId)
                .setExamName("Java 周测")
                .setPaperId(9101L)
                .setPaperName("已发布试卷")
                .setStatus(ExamLifecycleStatus.DRAFT.name())
                .setCreatorId(3001L)
                .setCreatorRoleId(3)
                .setDuration(120)
                .setStartTime(LocalDateTime.now().plusDays(1)));
        repository.examClasses.put(examId, List.of(
                new ExamClassRelationEntity().setRelationId(1L).setExamId(examId).setClassId(501L)
        ));

        ExamUpdateParamsRequest request = new ExamUpdateParamsRequest();
        request.setExamId(examId);
        request.setDuration(150);
        request.setStartTime(LocalDateTime.now().plusDays(2).withSecond(0).withNano(0).format(DATE_TIME_FORMATTER));

        ExamView result = service.updateParams(request, TEACHER_CONTEXT).getData();

        assertEquals(150, result.getDuration());
        assertEquals(501L, result.getClassIds().get(0));
        assertEquals(150, repository.exams.get(examId).getDuration());
    }

    @Test
    void updateParamsShouldRejectTeacherUpdatingForeignExam() {
        seedExam(8801L, "Java 周测", "DRAFT", 3002L, LocalDateTime.now().plusDays(1), List.of(501L));

        ExamUpdateParamsRequest request = new ExamUpdateParamsRequest();
        request.setExamId(8801L);
        request.setDuration(150);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateParams(request, TEACHER_CONTEXT));
        assertEquals(403, exception.getCode());
    }

    @Test
    void updateParamsShouldAllowOnlyDurationExtensionAfterStart() throws Exception {
        seedExam(8801L, "Java 周测", "UNDERWAY", 3001L, LocalDateTime.now().minusMinutes(10), List.of(501L));
        repository.exams.get(8801L).setDuration(120);

        ExamUpdateParamsRequest request = new ExamUpdateParamsRequest();
        request.setExamId(8801L);
        request.setDuration(150);

        ExamView result = service.updateParams(request, TEACHER_CONTEXT).getData();

        assertEquals(150, result.getDuration());

        ExamUpdateParamsRequest startTimeRequest = new ExamUpdateParamsRequest();
        startTimeRequest.setExamId(8801L);
        startTimeRequest.setStartTime(LocalDateTime.now().plusHours(1).withSecond(0).withNano(0).format(DATE_TIME_FORMATTER));
        BusinessException startTimeException = assertThrows(BusinessException.class,
                () -> service.updateParams(startTimeRequest, TEACHER_CONTEXT));
        assertEquals(409, startTimeException.getCode());

        ExamUpdateParamsRequest shrinkRequest = new ExamUpdateParamsRequest();
        shrinkRequest.setExamId(8801L);
        shrinkRequest.setDuration(120);
        BusinessException shrinkException = assertThrows(BusinessException.class,
                () -> service.updateParams(shrinkRequest, TEACHER_CONTEXT));
        assertEquals(409, shrinkException.getCode());
    }

    @Test
    void updateParamsShouldRejectEndedExamAndEmptyPayload() {
        seedExam(8801L, "Java 周测", "ENDED", 3001L, LocalDateTime.now().minusHours(3), List.of(501L));

        ExamUpdateParamsRequest emptyRequest = new ExamUpdateParamsRequest();
        emptyRequest.setExamId(8801L);
        BusinessException emptyException = assertThrows(BusinessException.class,
                () -> service.updateParams(emptyRequest, TEACHER_CONTEXT));
        assertEquals(400, emptyException.getCode());

        ExamUpdateParamsRequest endedRequest = new ExamUpdateParamsRequest();
        endedRequest.setExamId(8801L);
        endedRequest.setDuration(180);
        BusinessException endedException = assertThrows(BusinessException.class,
                () -> service.updateParams(endedRequest, TEACHER_CONTEXT));
        assertEquals(409, endedException.getCode());
    }

    @Test
    void toggleStatusShouldPausePublishedExamAndWriteLog() throws Exception {
        seedExam(8801L, "Java 周测", "PUBLISHED", 3001L, LocalDateTime.now().plusHours(2), List.of(501L));

        ExamToggleStatusRequest request = new ExamToggleStatusRequest();
        request.setExamId(8801L);
        request.setIsPaused(true);
        request.setReason("监考老师临时处理中");

        ExamView result = service.toggleStatus(request, TEACHER_CONTEXT).getData();

        assertEquals(ExamLifecycleStatus.PAUSED.name(), result.getStatus());
        assertEquals(ExamLifecycleStatus.PAUSED.name(), repository.exams.get(8801L).getStatus());
        assertEquals(1, repository.statusLogs.size());
        assertEquals("exam.pause", repository.statusLogs.get(0).getActionType());
    }

    @Test
    void toggleStatusShouldResumePausedExamByTimeWindow() throws Exception {
        seedExam(8801L, "Java 周测", "PAUSED", 3001L, LocalDateTime.now().plusHours(2), List.of(501L));

        ExamToggleStatusRequest preStartRequest = new ExamToggleStatusRequest();
        preStartRequest.setExamId(8801L);
        preStartRequest.setIsPaused(false);
        preStartRequest.setReason("故障处理完成");
        ExamView preStartResult = service.toggleStatus(preStartRequest, TEACHER_CONTEXT).getData();
        assertEquals(ExamLifecycleStatus.PUBLISHED.name(), preStartResult.getStatus());

        seedExam(8802L, "Java 期中", "PAUSED", 3001L, LocalDateTime.now().minusMinutes(30), List.of(501L));
        ExamToggleStatusRequest underwayRequest = new ExamToggleStatusRequest();
        underwayRequest.setExamId(8802L);
        underwayRequest.setIsPaused(false);
        underwayRequest.setReason("网络恢复");
        ExamView underwayResult = service.toggleStatus(underwayRequest, TEACHER_CONTEXT).getData();
        assertEquals(ExamLifecycleStatus.UNDERWAY.name(), underwayResult.getStatus());
    }

    @Test
    void toggleStatusShouldRejectInvalidTransitionAndBlankReason() {
        seedExam(8801L, "Java 周测", "DRAFT", 3001L, LocalDateTime.now().plusHours(2), List.of(501L));

        ExamToggleStatusRequest blankReasonRequest = new ExamToggleStatusRequest();
        blankReasonRequest.setExamId(8801L);
        blankReasonRequest.setIsPaused(true);
        blankReasonRequest.setReason("   ");
        BusinessException blankReasonException = assertThrows(BusinessException.class,
                () -> service.toggleStatus(blankReasonRequest, TEACHER_CONTEXT));
        assertEquals(400, blankReasonException.getCode());

        ExamToggleStatusRequest invalidTransitionRequest = new ExamToggleStatusRequest();
        invalidTransitionRequest.setExamId(8801L);
        invalidTransitionRequest.setIsPaused(true);
        invalidTransitionRequest.setReason("尝试暂停草稿考试");
        BusinessException invalidTransitionException = assertThrows(BusinessException.class,
                () -> service.toggleStatus(invalidTransitionRequest, TEACHER_CONTEXT));
        assertEquals(409, invalidTransitionException.getCode());
    }

    @Test
    void approveRetestShouldAllowTeacherApproveOwnClassPendingApply() throws Exception {
        seedExam(8801L, "Java 周测", "PUBLISHED", 3001L, LocalDateTime.now().plusHours(2), List.of(501L));
        repository.retestApplies.put(9901L, new ExamRetestApplyEntity()
                .setRetestApplyId(9901L)
                .setExamId(8801L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("PENDING")
                .setApplyReason("请假缺考"));

        ExamApproveRetestRequest request = new ExamApproveRetestRequest();
        request.setRetestApplyId(9901L);
        request.setApproveResult("approved");

        String result = service.approveRetest(request, TEACHER_CONTEXT).getData();

        assertEquals("ok", result);
        assertEquals("APPROVED", repository.retestApplies.get(9901L).getStatus());
        assertEquals(3001L, repository.retestApplies.get(9901L).getReviewedBy());
    }

    @Test
    void approveRetestShouldRejectForeignTeacherAndNonPendingApply() {
        seedExam(8801L, "Java 周测", "PUBLISHED", 3001L, LocalDateTime.now().plusHours(2), List.of(501L));
        repository.retestApplies.put(9901L, new ExamRetestApplyEntity()
                .setRetestApplyId(9901L)
                .setExamId(8801L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("PENDING")
                .setApplyReason("请假缺考"));

        ExamApproveRetestRequest foreignTeacherRequest = new ExamApproveRetestRequest();
        foreignTeacherRequest.setRetestApplyId(9901L);
        foreignTeacherRequest.setApproveResult("approved");
        BusinessException foreignTeacherException = assertThrows(BusinessException.class,
                () -> service.approveRetest(foreignTeacherRequest, new RequestContext(3002L, 3, "req-teacher-2")));
        assertEquals(403, foreignTeacherException.getCode());

        repository.retestApplies.get(9901L).setStatus("APPROVED");
        ExamApproveRetestRequest nonPendingRequest = new ExamApproveRetestRequest();
        nonPendingRequest.setRetestApplyId(9901L);
        nonPendingRequest.setApproveResult("approved");
        BusinessException nonPendingException = assertThrows(BusinessException.class,
                () -> service.approveRetest(nonPendingRequest, ADMIN_CONTEXT));
        assertEquals(409, nonPendingException.getCode());
    }

    @Test
    void approveRetestShouldRequireReasonWhenRejected() throws Exception {
        seedExam(8801L, "Java 周测", "PUBLISHED", 3001L, LocalDateTime.now().plusHours(2), List.of(501L));
        repository.retestApplies.put(9901L, new ExamRetestApplyEntity()
                .setRetestApplyId(9901L)
                .setExamId(8801L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setStatus("PENDING")
                .setApplyReason("请假缺考"));

        ExamApproveRetestRequest blankReasonRequest = new ExamApproveRetestRequest();
        blankReasonRequest.setRetestApplyId(9901L);
        blankReasonRequest.setApproveResult("rejected");
        blankReasonRequest.setReason("  ");
        BusinessException blankReasonException = assertThrows(BusinessException.class,
                () -> service.approveRetest(blankReasonRequest, ADMIN_CONTEXT));
        assertEquals(400, blankReasonException.getCode());

        ExamApproveRetestRequest rejectRequest = new ExamApproveRetestRequest();
        rejectRequest.setRetestApplyId(9901L);
        rejectRequest.setApproveResult("rejected");
        rejectRequest.setReason("不符合补考条件");
        String result = service.approveRetest(rejectRequest, ADMIN_CONTEXT).getData();
        assertEquals("ok", result);
        assertEquals("REJECTED", repository.retestApplies.get(9901L).getStatus());
    }

    private void seedBaseData() {
        repository.papers.put(9101L, new PaperSnapshotEntity()
                .setPaperId(9101L)
                .setPaperName("已发布试卷")
                .setCreatorId(3001L)
                .setStatus("PUBLISHED"));
        repository.papers.put(9102L, new PaperSnapshotEntity()
                .setPaperId(9102L)
                .setPaperName("已审核未发布试卷")
                .setCreatorId(3001L)
                .setStatus("APPROVED"));

        repository.paperPublishClasses.put(9101L, new ArrayList<>(List.of(
                new PaperPublishClassSnapshotEntity().setRelationId(1L).setPaperId(9101L).setClassId(501L),
                new PaperPublishClassSnapshotEntity().setRelationId(2L).setPaperId(9101L).setClassId(502L)
        )));
        repository.paperPublishClasses.put(9102L, new ArrayList<>(List.of(
                new PaperPublishClassSnapshotEntity().setRelationId(3L).setPaperId(9102L).setClassId(501L)
        )));

        repository.paperQuestions.put(9101L, new ArrayList<>(List.of(
                new PaperQuestionSnapshotEntity()
                        .setBindingId(101L)
                        .setPaperId(9101L)
                        .setQuestionId(7001L)
                        .setSortNo(1)
                        .setAssignedScore(5)
                        .setQuestionType("single"),
                new PaperQuestionSnapshotEntity()
                        .setBindingId(102L)
                        .setPaperId(9101L)
                        .setQuestionId(7002L)
                        .setSortNo(2)
                        .setAssignedScore(10)
                        .setQuestionType("subjective")
        )));
        repository.questions.put(7001L, new QuestionSnapshotEntity()
                .setQuestionId(7001L)
                .setContent("JDK 默认编码")
                .setQuestionType("single")
                .setAnswer("A"));
        repository.questions.put(7002L, new QuestionSnapshotEntity()
                .setQuestionId(7002L)
                .setContent("说明 JVM 内存结构")
                .setQuestionType("subjective")
                .setAnswer("略"));

        repository.users.put(4001L, new UserSnapshotEntity().setUserId(4001L).setRealName("张三"));
        repository.users.put(4002L, new UserSnapshotEntity().setUserId(4002L).setRealName("李四"));
        repository.users.put(4003L, new UserSnapshotEntity().setUserId(4003L).setRealName("王五"));
        repository.users.put(4004L, new UserSnapshotEntity().setUserId(4004L).setRealName("赵六"));

        repository.classes.put(501L, new ClassSnapshotEntity()
                .setClassId(501L)
                .setClassName("Java 1班")
                .setTeacherId(3001L)
                .setCreatedBy(3001L)
                .setStatus("active"));
        repository.classes.put(502L, new ClassSnapshotEntity()
                .setClassId(502L)
                .setClassName("Python 1班")
                .setTeacherId(3002L)
                .setCreatedBy(3002L)
                .setStatus("active"));
        repository.classes.put(503L, new ClassSnapshotEntity()
                .setClassId(503L)
                .setClassName("测试 3班")
                .setTeacherId(3001L)
                .setCreatedBy(3001L)
                .setStatus("active"));
    }

    private void seedExam(Long examId,
                          String examName,
                          String status,
                          Long creatorId,
                          String startTime,
                          List<Long> classIds) {
        repository.exams.put(examId, new ExamEntity()
                .setExamId(examId)
                .setExamName(examName)
                .setPaperId(9101L)
                .setPaperName("已发布试卷")
                .setStatus(status)
                .setCreatorId(creatorId)
                .setCreatorRoleId(2)
                .setDuration(120)
                .setStartTime(LocalDateTime.parse(startTime))
                .setRequestId("seed-" + examId)
                .setCreateTime(LocalDateTime.parse(startTime))
                .setUpdateTime(LocalDateTime.parse(startTime)));
        repository.examClasses.put(examId, classIds.stream()
                .map(classId -> new ExamClassRelationEntity()
                        .setRelationId(examId * 10 + classId)
                        .setExamId(examId)
                        .setClassId(classId))
                .toList());
    }

    private void seedDistributedStudent(Long examId, Long classId, Long studentId) {
        repository.examStudents.computeIfAbsent(examId, key -> new ArrayList<>())
                .add(new ExamStudentRelationEntity()
                        .setRelationId(examId * 100 + studentId)
                        .setExamId(examId)
                        .setClassId(classId)
                        .setStudentId(studentId));
    }

    private void seedExam(Long examId,
                          String examName,
                          String status,
                          Long creatorId,
                          LocalDateTime startTime,
                          List<Long> classIds) {
        repository.exams.put(examId, new ExamEntity()
                .setExamId(examId)
                .setExamName(examName)
                .setPaperId(9101L)
                .setPaperName("已发布试卷")
                .setStatus(status)
                .setCreatorId(creatorId)
                .setCreatorRoleId(2)
                .setDuration(120)
                .setStartTime(startTime)
                .setRequestId("seed-" + examId)
                .setCreateTime(startTime)
                .setUpdateTime(startTime));
        repository.examClasses.put(examId, classIds.stream()
                .map(classId -> new ExamClassRelationEntity()
                        .setRelationId(examId * 10 + classId)
                        .setExamId(examId)
                        .setClassId(classId))
                .toList());
    }

    private ExamSubmitRequest.AnswerItem answerItem(Long questionId, String answer) {
        ExamSubmitRequest.AnswerItem item = new ExamSubmitRequest.AnswerItem();
        item.setQuestionId(questionId);
        item.setAnswer(answer);
        return item;
    }

    private static final class InMemoryExamDomainRepository implements ExamDomainRepository {

        private final Map<Long, ExamEntity> exams = new LinkedHashMap<>();
        private final Map<Long, List<ExamClassRelationEntity>> examClasses = new LinkedHashMap<>();
        private final Map<Long, PaperSnapshotEntity> papers = new LinkedHashMap<>();
        private final Map<Long, List<PaperQuestionSnapshotEntity>> paperQuestions = new LinkedHashMap<>();
        private final Map<Long, List<PaperPublishClassSnapshotEntity>> paperPublishClasses = new LinkedHashMap<>();
        private final Map<Long, QuestionSnapshotEntity> questions = new LinkedHashMap<>();
        private final Map<Long, UserSnapshotEntity> users = new LinkedHashMap<>();
        private final Map<Long, ClassSnapshotEntity> classes = new LinkedHashMap<>();
        private final List<ClassMemberSnapshotEntity> classMembers = new ArrayList<>();
        private final Map<Long, List<ExamStudentRelationEntity>> examStudents = new LinkedHashMap<>();
        private final List<ScoreRecordEntity> scoreRecords = new ArrayList<>();
        private final List<ScoreDetailEntity> scoreDetails = new ArrayList<>();
        private final List<ExamStatusLogEntity> statusLogs = new ArrayList<>();
        private final Map<Long, ExamRetestApplyEntity> retestApplies = new LinkedHashMap<>();

        @Override
        public ExamEntity saveExam(ExamEntity exam) {
            exams.put(exam.getExamId(), exam);
            return exam;
        }

        @Override
        public void replaceExamClasses(Long examId, List<ExamClassRelationEntity> relations) {
            examClasses.put(examId, new ArrayList<>(relations));
        }

        @Override
        public void replaceExamStudents(Long examId, List<ExamStudentRelationEntity> relations) {
            examStudents.put(examId, new ArrayList<>(relations));
        }

        @Override
        public ExamStatusLogEntity saveExamStatusLog(ExamStatusLogEntity log) {
            statusLogs.add(log);
            return log;
        }

        @Override
        public Optional<PaperSnapshotEntity> findPaperById(Long paperId) {
            return Optional.ofNullable(papers.get(paperId));
        }

        @Override
        public Optional<ExamEntity> findExamById(Long examId) {
            return Optional.ofNullable(exams.get(examId));
        }

        @Override
        public Optional<ExamRetestApplyEntity> findRetestApplyById(Long retestApplyId) {
            return Optional.ofNullable(retestApplies.get(retestApplyId));
        }

        @Override
        public Optional<ExamRetestApplyEntity> findRetestApplyByExamIdAndStudentId(Long examId, Long studentId) {
            return retestApplies.values().stream()
                    .filter(item -> java.util.Objects.equals(item.getExamId(), examId))
                    .filter(item -> java.util.Objects.equals(item.getStudentId(), studentId))
                    .findFirst();
        }

        @Override
        public List<PaperPublishClassSnapshotEntity> listPaperPublishClasses(Long paperId) {
            return new ArrayList<>(paperPublishClasses.getOrDefault(paperId, List.of()));
        }

        @Override
        public List<ClassSnapshotEntity> findClassesByIds(java.util.Collection<Long> classIds) {
            return classIds.stream()
                    .map(classes::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public ExamEntity updateExam(ExamEntity exam) {
            exams.put(exam.getExamId(), exam);
            return exam;
        }

        @Override
        public ExamRetestApplyEntity saveRetestApply(ExamRetestApplyEntity apply) {
            retestApplies.put(apply.getRetestApplyId(), apply);
            return apply;
        }

        @Override
        public ExamRetestApplyEntity updateRetestApply(ExamRetestApplyEntity apply) {
            retestApplies.put(apply.getRetestApplyId(), apply);
            return apply;
        }

        @Override
        public List<ExamEntity> listExams() {
            return new ArrayList<>(exams.values());
        }

        @Override
        public List<ExamClassRelationEntity> listExamClasses(java.util.Collection<Long> examIds) {
            return examIds.stream()
                    .flatMap(examId -> examClasses.getOrDefault(examId, List.of()).stream())
                    .toList();
        }

        @Override
        public List<ClassMemberSnapshotEntity> listClassMembersByStudentId(Long studentId) {
            return classMembers.stream()
                    .filter(item -> java.util.Objects.equals(item.getStudentId(), studentId))
                    .toList();
        }

        @Override
        public List<ClassMemberSnapshotEntity> listClassMembersByStudentIds(java.util.Collection<Long> studentIds) {
            return classMembers.stream()
                    .filter(item -> studentIds.contains(item.getStudentId()))
                    .toList();
        }

        @Override
        public List<ExamStudentRelationEntity> listExamStudents(java.util.Collection<Long> examIds) {
            return examIds.stream()
                    .flatMap(examId -> examStudents.getOrDefault(examId, List.of()).stream())
                    .toList();
        }

        @Override
        public List<PaperQuestionSnapshotEntity> listPaperQuestions(Long paperId) {
            return new ArrayList<>(paperQuestions.getOrDefault(paperId, List.of()));
        }

        @Override
        public List<QuestionSnapshotEntity> findQuestionsByIds(java.util.Collection<Long> questionIds) {
            return questionIds.stream()
                    .map(questions::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public List<UserSnapshotEntity> findUsersByIds(java.util.Collection<Long> userIds) {
            return userIds.stream()
                    .map(users::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public List<ScoreRecordEntity> listScoresByExamId(Long examId) {
            return scoreRecords.stream()
                    .filter(item -> java.util.Objects.equals(item.getExamId(), examId))
                    .toList();
        }

        @Override
        public Optional<ScoreRecordEntity> findScoreByExamIdAndStudentId(Long examId, Long studentId) {
            return scoreRecords.stream()
                    .filter(item -> java.util.Objects.equals(item.getExamId(), examId))
                    .filter(item -> java.util.Objects.equals(item.getStudentId(), studentId))
                    .findFirst();
        }

        @Override
        public List<ScoreDetailEntity> listScoreDetails(Long scoreId) {
            return scoreDetails.stream()
                    .filter(item -> java.util.Objects.equals(item.getScoreId(), scoreId))
                    .toList();
        }

        @Override
        public ScoreRecordEntity updateScore(ScoreRecordEntity score) {
            scoreRecords.removeIf(item -> java.util.Objects.equals(item.getScoreId(), score.getScoreId()));
            scoreRecords.add(score);
            return score;
        }

        @Override
        public ScoreDetailEntity updateScoreDetail(ScoreDetailEntity detail) {
            scoreDetails.removeIf(item -> java.util.Objects.equals(item.getDetailId(), detail.getDetailId()));
            scoreDetails.add(detail);
            return detail;
        }

        @Override
        public void replaceExamScores(Long examId, List<ScoreRecordEntity> scores, List<ScoreDetailEntity> details) {
            java.util.Set<Long> removedScoreIds = scoreRecords.stream()
                    .filter(item -> java.util.Objects.equals(item.getExamId(), examId))
                    .map(ScoreRecordEntity::getScoreId)
                    .collect(java.util.stream.Collectors.toSet());
            scoreRecords.removeIf(item -> java.util.Objects.equals(item.getExamId(), examId));
            scoreDetails.removeIf(item -> removedScoreIds.contains(item.getScoreId())
                    || java.util.Objects.equals(item.getExamId(), examId));
            scoreRecords.addAll(scores);
            scoreDetails.addAll(details);
        }
    }
}
