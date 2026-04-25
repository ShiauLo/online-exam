package com.maghert.examquestion.service;

import cn.hutool.core.lang.Snowflake;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examquestion.context.QuestionAccessContext;
import com.maghert.examquestion.dto.QuestionAuditRequest;
import com.maghert.examquestion.dto.QuestionCategoryQueryRequest;
import com.maghert.examquestion.dto.QuestionCategoryUpsertRequest;
import com.maghert.examquestion.dto.QuestionCreateRequest;
import com.maghert.examquestion.dto.QuestionQueryRequest;
import com.maghert.examquestion.dto.QuestionToggleStatusRequest;
import com.maghert.examquestion.dto.QuestionUpdateRequest;
import com.maghert.examquestion.entity.QuestionAuditLog;
import com.maghert.examquestion.entity.QuestionCategory;
import com.maghert.examquestion.entity.QuestionItem;
import com.maghert.examquestion.repository.QuestionDomainRepository;
import com.maghert.examquestion.service.impl.QuestionServiceImpl;
import com.maghert.examquestion.utils.QuestionCsvCodec;
import com.maghert.examquestion.vo.QuestionCategoryView;
import com.maghert.examquestion.vo.QuestionImportResult;
import com.maghert.examquestion.vo.QuestionView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionServiceImplTests {

    private InMemoryQuestionDomainRepository repository;
    private MutableAccessContext accessContext;
    private QuestionServiceImpl service;
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        repository = new InMemoryQuestionDomainRepository();
        accessContext = new MutableAccessContext();
        tempDir = Files.createDirectories(Path.of("target", "question-service-tests", Long.toString(System.nanoTime())));
        service = new QuestionServiceImpl(
                new Snowflake(1, 1),
                repository,
                accessContext,
                new QuestionCsvCodec(),
                tempDir.toString());
    }

    @Test
    void teacherShouldOnlyOperateOwnQuestions() throws Exception {
        accessContext.set(3001L, 3, "req-1");
        Long categoryId = service.createCategory(personalCategory("teacher-category")).getData().getCategoryId();
        Long questionId = service.create(questionRequest(categoryId, "my question", "A", "analysis")).getData().getQuestionId();

        accessContext.set(3002L, 3, "req-2");
        QuestionUpdateRequest updateRequest = new QuestionUpdateRequest();
        updateRequest.setQuestionId(questionId);
        updateRequest.setContent("changed");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.update(updateRequest));
        assertEquals(403, exception.getCode());
    }

    @Test
    void adminShouldAuditTeacherGlobalQuestion() throws Exception {
        accessContext.set(2001L, 2, "req-admin");
        Long categoryId = service.createCategory(globalCategory("global-category")).getData().getCategoryId();

        accessContext.set(3001L, 3, "req-teacher");
        QuestionView question = service.create(questionRequest(categoryId, "pending question", "SECRET_ANSWER",
                "SECRET_ANALYSIS")).getData();
        assertEquals("pending", question.getAuditStatus());

        accessContext.set(2001L, 2, "req-admin-2");
        QuestionAuditRequest auditRequest = new QuestionAuditRequest();
        auditRequest.setQuestionId(question.getQuestionId());
        auditRequest.setAuditResult("approved");
        QuestionView audited = service.audit(auditRequest).getData();

        assertEquals("approved", audited.getAuditStatus());
        assertEquals(1, repository.auditLogs.size());
    }

    @Test
    void auditorExportShouldMaskSensitiveFields() throws Exception {
        accessContext.set(2001L, 2, "req-admin");
        Long categoryId = service.createCategory(globalCategory("global-category")).getData().getCategoryId();
        service.create(questionRequest(categoryId, "question text", "SECRET_ANSWER", "SECRET_ANALYSIS"));

        accessContext.set(5001L, 5, "req-auditor");
        var exportView = service.exportQuestions(categoryId, null).getData();
        String content = Files.readString(tempDir.resolve("exports").resolve(exportView.getFileKey() + ".csv"));

        assertFalse(content.contains("SECRET_ANSWER"));
        assertFalse(content.contains("SECRET_ANALYSIS"));
        assertTrue(Boolean.TRUE.equals(exportView.getMasked()));
        assertEquals(1, exportView.getRecordCount());
    }

    @Test
    void shouldRejectOptionChangeWhenQuestionIsReferenced() throws Exception {
        accessContext.set(3001L, 3, "req-1");
        Long categoryId = service.createCategory(personalCategory("teacher-category")).getData().getCategoryId();
        QuestionView question = service.create(questionRequest(categoryId, "referenced question", "A", "analysis"))
                .getData();
        repository.findQuestionById(question.getQuestionId()).orElseThrow().setReferenceCount(1);

        QuestionUpdateRequest updateRequest = new QuestionUpdateRequest();
        updateRequest.setQuestionId(question.getQuestionId());
        updateRequest.setOptions(List.of("A", "B", "C"));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.update(updateRequest));
        assertEquals(409, exception.getCode());
    }

    @Test
    void shouldRejectContentChangeWhenQuestionIsReferenced() throws Exception {
        accessContext.set(3001L, 3, "req-1");
        Long categoryId = service.createCategory(personalCategory("teacher-category")).getData().getCategoryId();
        QuestionView question = service.create(questionRequest(categoryId, "referenced question", "A", "analysis"))
                .getData();
        repository.findQuestionById(question.getQuestionId()).orElseThrow().setReferenceCount(1);

        QuestionUpdateRequest updateRequest = new QuestionUpdateRequest();
        updateRequest.setQuestionId(question.getQuestionId());
        updateRequest.setContent("changed content");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.update(updateRequest));
        assertEquals(409, exception.getCode());
    }

    @Test
    void queryShouldSupportMinimalFilters() throws Exception {
        accessContext.set(3001L, 3, "req-1");
        Long categoryId = service.createCategory(personalCategory("teacher-category")).getData().getCategoryId();
        Long activeQuestionId = service.create(questionRequest(categoryId, "java basics", "A", "analysis"))
                .getData().getQuestionId();
        Long disabledQuestionId = service.create(questionRequest(categoryId, "spring advanced", "B", "analysis"))
                .getData().getQuestionId();

        QuestionToggleStatusRequest toggleStatusRequest = new QuestionToggleStatusRequest();
        toggleStatusRequest.setQuestionId(disabledQuestionId);
        toggleStatusRequest.setIsDisabled(true);
        service.toggleStatus(toggleStatusRequest);

        QuestionQueryRequest queryRequest = new QuestionQueryRequest();
        queryRequest.setKeyword("spring");
        queryRequest.setIsDisabled(true);
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);

        var result = service.query(queryRequest).getData();
        assertEquals(1, result.getRecords().size());
        assertEquals(disabledQuestionId, result.getRecords().get(0).getQuestionId());
        assertTrue(result.getRecords().stream().noneMatch(item -> item.getQuestionId().equals(activeQuestionId)));
    }

    @Test
    void queryShouldSupportReferencedFilterAndExposeReferenceLocked() throws Exception {
        accessContext.set(3001L, 3, "req-1");
        Long categoryId = service.createCategory(personalCategory("teacher-category")).getData().getCategoryId();
        Long referencedQuestionId = service.create(questionRequest(categoryId, "referenced question", "A", "analysis"))
                .getData().getQuestionId();
        service.create(questionRequest(categoryId, "plain question", "B", "analysis"));
        repository.findQuestionById(referencedQuestionId).orElseThrow().setReferenceCount(2);

        QuestionQueryRequest queryRequest = new QuestionQueryRequest();
        queryRequest.setIsReferenced(true);
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);

        var result = service.query(queryRequest).getData();
        assertEquals(1, result.getRecords().size());
        assertEquals(referencedQuestionId, result.getRecords().get(0).getQuestionId());
        assertTrue(Boolean.TRUE.equals(result.getRecords().get(0).getReferenceLocked()));
        assertEquals(2, result.getRecords().get(0).getReferenceCount());
    }

    @Test
    void teacherCategoryQueryShouldIncludeGlobalAndOwnPersonalCategories() throws Exception {
        accessContext.set(2001L, 2, "req-admin");
        service.createCategory(globalCategory("global-category"));
        accessContext.set(3001L, 3, "req-teacher");
        service.createCategory(personalCategory("personal-category"));

        QuestionCategoryQueryRequest request = new QuestionCategoryQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        var result = service.queryCategories(request).getData();
        assertEquals(2, result.getRecords().size());
        assertTrue(result.getRecords().stream().map(QuestionCategoryView::getName)
                .anyMatch("global-category"::equals));
        assertTrue(result.getRecords().stream().map(QuestionCategoryView::getName)
                .anyMatch("personal-category"::equals));
    }

    @Test
    void categoryQueryShouldSupportCreatorFilter() throws Exception {
        accessContext.set(3001L, 3, "req-teacher-1");
        service.createCategory(personalCategory("teacher-1-category"));
        accessContext.set(3002L, 3, "req-teacher-2");
        service.createCategory(personalCategory("teacher-2-category"));

        accessContext.set(2001L, 2, "req-admin");
        QuestionCategoryQueryRequest request = new QuestionCategoryQueryRequest();
        request.setCreatorId(3001L);
        request.setPageNum(1);
        request.setPageSize(10);

        var result = service.queryCategories(request).getData();
        assertEquals(1, result.getRecords().size());
        assertEquals("teacher-1-category", result.getRecords().get(0).getName());
    }

    @Test
    void updateCategoryShouldRejectCycleParenting() throws Exception {
        accessContext.set(2001L, 2, "req-admin");
        Long rootId = service.createCategory(globalCategory("root")).getData().getCategoryId();
        QuestionCategoryUpsertRequest childRequest = globalCategory("child");
        childRequest.setParentId(rootId);
        Long childId = service.createCategory(childRequest).getData().getCategoryId();

        QuestionCategoryUpsertRequest updateRequest = globalCategory("root");
        updateRequest.setCategoryId(rootId);
        updateRequest.setParentId(childId);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.updateCategory(updateRequest));
        assertEquals(400, exception.getCode());
    }

    @Test
    void disabledCategoryShouldRejectQuestionCreation() throws Exception {
        accessContext.set(3001L, 3, "req-teacher");
        Long categoryId = service.createCategory(personalCategory("teacher-category")).getData().getCategoryId();

        QuestionCategoryUpsertRequest disableRequest = personalCategory("teacher-category");
        disableRequest.setCategoryId(categoryId);
        disableRequest.setIsDisabled(true);
        service.updateCategory(disableRequest);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create(questionRequest(categoryId, "blocked question", "A", "analysis")));
        assertEquals(409, exception.getCode());
    }

    @Test
    void categoryQueryShouldSupportDisabledFilter() throws Exception {
        accessContext.set(3001L, 3, "req-teacher");
        Long activeId = service.createCategory(personalCategory("active-category")).getData().getCategoryId();
        Long disabledId = service.createCategory(personalCategory("disabled-category")).getData().getCategoryId();

        QuestionCategoryUpsertRequest disableRequest = personalCategory("disabled-category");
        disableRequest.setCategoryId(disabledId);
        disableRequest.setIsDisabled(true);
        service.updateCategory(disableRequest);

        QuestionCategoryQueryRequest request = new QuestionCategoryQueryRequest();
        request.setIsDisabled(true);
        request.setPageNum(1);
        request.setPageSize(10);

        var result = service.queryCategories(request).getData();
        assertEquals(1, result.getRecords().size());
        assertEquals(disabledId, result.getRecords().get(0).getCategoryId());
        assertTrue(Boolean.TRUE.equals(result.getRecords().get(0).getIsDisabled()));
        assertTrue(result.getRecords().stream().noneMatch(item -> item.getCategoryId().equals(activeId)));
    }

    @Test
    void importResultShouldExposeDocumentedSummaryFields() throws Exception {
        accessContext.set(3001L, 3, "req-teacher");
        Long categoryId = service.createCategory(personalCategory("teacher-category")).getData().getCategoryId();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.csv",
                "text/csv",
                ("content,type,answer,analysis,difficulty,options\n" +
                        "question 1,single,A,analysis,3,A|B\n" +
                        "bad question,single,,analysis,3,A|B")
                        .getBytes());

        QuestionImportResult result = service.importQuestions(file, categoryId).getData();

        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
        assertEquals(1, result.getImportedCount());
        assertEquals(1, result.getRowErrors().size());
        assertEquals(result.getRowErrors(), result.getErrors());
    }

    @Test
    void importShouldAcceptExportHeaderTemplate() throws Exception {
        accessContext.set(3001L, 3, "req-teacher");
        Long categoryId = service.createCategory(personalCategory("teacher-category")).getData().getCategoryId();
        service.create(questionRequest(categoryId, "template question", "A", "analysis"));
        repository.listQuestions().get(0).setReferenceCount(3);

        var exportView = service.exportQuestions(categoryId, null).getData();
        byte[] exported = Files.readAllBytes(tempDir.resolve("exports").resolve(exportView.getFileKey() + ".csv"));
        assertTrue(new String(exported).contains("referenceCount"));
        MockMultipartFile file = new MockMultipartFile("file", "question-export.csv", "text/csv", exported);

        QuestionImportResult result = service.importQuestions(file, categoryId).getData();

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());
    }

    private QuestionCategoryUpsertRequest personalCategory(String name) {
        QuestionCategoryUpsertRequest request = new QuestionCategoryUpsertRequest();
        request.setName(name);
        request.setIsPersonal(true);
        return request;
    }

    private QuestionCategoryUpsertRequest globalCategory(String name) {
        QuestionCategoryUpsertRequest request = new QuestionCategoryUpsertRequest();
        request.setName(name);
        request.setIsPersonal(false);
        return request;
    }

    private QuestionCreateRequest questionRequest(Long categoryId, String content, String answer, String analysis) {
        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setCategoryId(categoryId);
        request.setContent(content);
        request.setType("single");
        request.setOptions(List.of("A", "B"));
        request.setAnswer(answer);
        request.setAnalysis(analysis);
        request.setDifficulty(3);
        return request;
    }

    private static final class MutableAccessContext implements QuestionAccessContext {

        private Long userId;
        private Integer roleId;
        private String requestId;

        void set(Long userId, Integer roleId, String requestId) {
            this.userId = userId;
            this.roleId = roleId;
            this.requestId = requestId;
        }

        @Override
        public Long requireUserId() {
            return userId;
        }

        @Override
        public Integer requireRoleId() {
            return roleId;
        }

        @Override
        public String currentRequestId() {
            return requestId;
        }
    }

    private static final class InMemoryQuestionDomainRepository implements QuestionDomainRepository {

        private final Map<Long, QuestionCategory> categories = new LinkedHashMap<>();
        private final Map<Long, QuestionItem> questions = new LinkedHashMap<>();
        private final Map<Long, QuestionAuditLog> auditLogs = new LinkedHashMap<>();

        @Override
        public QuestionCategory saveCategory(QuestionCategory category) {
            categories.put(category.getId(), category);
            return category;
        }

        @Override
        public void updateCategory(QuestionCategory category) {
            categories.put(category.getId(), category);
        }

        @Override
        public Optional<QuestionCategory> findCategoryById(Long categoryId) {
            return Optional.ofNullable(categories.get(categoryId));
        }

        @Override
        public List<QuestionCategory> listCategories() {
            return List.copyOf(categories.values());
        }

        @Override
        public QuestionItem saveQuestion(QuestionItem question) {
            questions.put(question.getId(), question);
            return question;
        }

        @Override
        public void updateQuestion(QuestionItem question) {
            questions.put(question.getId(), question);
        }

        @Override
        public void deleteQuestion(Long questionId) {
            questions.remove(questionId);
        }

        @Override
        public Optional<QuestionItem> findQuestionById(Long questionId) {
            return Optional.ofNullable(questions.get(questionId));
        }

        @Override
        public List<QuestionItem> listQuestions() {
            return List.copyOf(questions.values());
        }

        @Override
        public void saveAuditLog(QuestionAuditLog auditLog) {
            auditLogs.put(auditLog.getId(), auditLog);
        }
    }
}
