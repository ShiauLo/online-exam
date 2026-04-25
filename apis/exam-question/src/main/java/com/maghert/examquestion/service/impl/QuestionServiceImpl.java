package com.maghert.examquestion.service.impl;

import cn.hutool.core.lang.Snowflake;
import com.maghert.examcommon.audit.AuditTrail;
import com.maghert.examcommon.resource.LocalResourceFileStore;
import com.maghert.examcommon.enums.QuestionAuditStatus;
import com.maghert.examcommon.enums.QuestionType;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examquestion.constants.QuestionRoleConstants;
import com.maghert.examquestion.context.QuestionAccessContext;
import com.maghert.examquestion.dto.QuestionAuditRequest;
import com.maghert.examquestion.dto.QuestionCategoryQueryRequest;
import com.maghert.examquestion.dto.QuestionCategoryUpsertRequest;
import com.maghert.examquestion.dto.QuestionCreateRequest;
import com.maghert.examquestion.dto.QuestionDeleteRequest;
import com.maghert.examquestion.dto.QuestionQueryRequest;
import com.maghert.examquestion.dto.QuestionToggleStatusRequest;
import com.maghert.examquestion.dto.QuestionUpdateRequest;
import com.maghert.examquestion.entity.QuestionAuditLog;
import com.maghert.examquestion.entity.QuestionCategory;
import com.maghert.examquestion.entity.QuestionItem;
import com.maghert.examquestion.repository.QuestionDomainRepository;
import com.maghert.examquestion.service.QuestionService;
import com.maghert.examquestion.utils.QuestionCsvCodec;
import com.maghert.examquestion.vo.QuestionCategoryView;
import com.maghert.examquestion.vo.QuestionExportView;
import com.maghert.examquestion.vo.QuestionImportResult;
import com.maghert.examquestion.vo.QuestionView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class QuestionServiceImpl implements QuestionService {

    private static final int IMPORT_LIMIT = 1000;

    private final Snowflake snowflake;
    private final QuestionDomainRepository repository;
    private final QuestionAccessContext accessContext;
    private final QuestionCsvCodec csvCodec;
    private final LocalResourceFileStore resourceFileStore;

    public QuestionServiceImpl(Snowflake snowflake,
                               QuestionDomainRepository repository,
                               QuestionAccessContext accessContext,
                               QuestionCsvCodec csvCodec,
                               @Value("${exam.resource.local-storage-root:./tmp_resource}") String localStorageRoot) {
        this.snowflake = snowflake;
        this.repository = repository;
        this.accessContext = accessContext;
        this.csvCodec = csvCodec;
        this.resourceFileStore = new LocalResourceFileStore(localStorageRoot);
    }

    @Override
    public ApiResponse<QuestionView> create(QuestionCreateRequest request) throws BusinessException {
        Long userId = accessContext.requireUserId();
        Integer roleId = accessContext.requireRoleId();
        ensureTeacherOrAdmin(roleId);
        QuestionCategory category = requireCategory(request.getCategoryId());
        ensureCategoryAvailableForQuestionCreation(category, roleId, userId);
        validateQuestionPayload(request.getType(), request.getOptions(), request.getAnswer(), request.getDifficulty());

        LocalDateTime now = LocalDateTime.now();
        QuestionItem question = new QuestionItem()
                .setId(snowflake.nextId())
                .setCategoryId(category.getId())
                .setCreatorId(userId)
                .setCreatorRoleId(roleId)
                .setContent(request.getContent())
                .setQuestionType(normalizeQuestionType(request.getType()))
                .setOptions(copyOptions(request.getOptions()))
                .setAnswer(request.getAnswer())
                .setAnalysis(request.getAnalysis())
                .setDifficulty(request.getDifficulty())
                .setAuditStatus(defaultAuditStatus(roleId, category))
                .setDisabled(false)
                .setReferenceCount(0)
                .setCreateTime(now)
                .setUpdateTime(now);
        repository.saveQuestion(question);
        return ok(toQuestionView(question, !QuestionRoleConstants.isAuditor(roleId)));
    }

    @Override
    public ApiResponse<QuestionView> update(QuestionUpdateRequest request) throws BusinessException {
        Long userId = accessContext.requireUserId();
        Integer roleId = accessContext.requireRoleId();
        QuestionItem question = requireQuestion(request.getQuestionId());
        ensureQuestionWriteAccess(question, roleId, userId);
        if (question.getReferenceCount() != null && question.getReferenceCount() > 0) {
            if (request.getContent() != null && !Objects.equals(request.getContent(), question.getContent())) {
                throw new BusinessException(DomainErrorCode.QUESTION_REFERENCE_UPDATE_FORBIDDEN);
            }
            if (request.getOptions() != null && !Objects.equals(request.getOptions(), question.getOptions())) {
                throw new BusinessException(DomainErrorCode.QUESTION_REFERENCE_UPDATE_FORBIDDEN);
            }
            if (request.getAnswer() != null && !Objects.equals(request.getAnswer(), question.getAnswer())) {
                throw new BusinessException(DomainErrorCode.QUESTION_REFERENCE_UPDATE_FORBIDDEN);
            }
            if (request.getDifficulty() != null && !Objects.equals(request.getDifficulty(), question.getDifficulty())) {
                throw new BusinessException(DomainErrorCode.QUESTION_REFERENCE_UPDATE_FORBIDDEN);
            }
        }
        if (request.getDifficulty() != null && (request.getDifficulty() < 1 || request.getDifficulty() > 5)) {
            throw new BusinessException(DomainErrorCode.QUESTION_DIFFICULTY_INVALID);
        }
        if (request.getContent() != null) {
            question.setContent(request.getContent());
        }
        if (request.getOptions() != null) {
            question.setOptions(copyOptions(request.getOptions()));
        }
        if (request.getAnswer() != null) {
            question.setAnswer(request.getAnswer());
        }
        if (request.getAnalysis() != null) {
            question.setAnalysis(request.getAnalysis());
        }
        if (request.getDifficulty() != null) {
            question.setDifficulty(request.getDifficulty());
        }
        question.setUpdateTime(LocalDateTime.now());
        repository.updateQuestion(question);
        return ok(toQuestionView(question, !QuestionRoleConstants.isAuditor(roleId)));
    }

    @Override
    public ApiResponse<String> delete(QuestionDeleteRequest request) throws BusinessException {
        Long userId = accessContext.requireUserId();
        Integer roleId = accessContext.requireRoleId();
        QuestionItem question = requireQuestion(request.getQuestionId());
        ensureQuestionWriteAccess(question, roleId, userId);
        if (question.getReferenceCount() != null && question.getReferenceCount() > 0) {
            question.setDisabled(true);
            question.setUpdateTime(LocalDateTime.now());
            repository.updateQuestion(question);
            return okMessage("question disabled because it is referenced");
        }
        repository.deleteQuestion(question.getId());
        return okMessage("question deleted");
    }

    @Override
    public ApiResponse<QuestionView> toggleStatus(QuestionToggleStatusRequest request) throws BusinessException {
        Long userId = accessContext.requireUserId();
        Integer roleId = accessContext.requireRoleId();
        QuestionItem question = requireQuestion(request.getQuestionId());
        ensureQuestionWriteAccess(question, roleId, userId);
        question.setDisabled(request.getIsDisabled());
        question.setUpdateTime(LocalDateTime.now());
        repository.updateQuestion(question);
        return ok(toQuestionView(question, !QuestionRoleConstants.isAuditor(roleId)));
    }

    @Override
    public ApiResponse<QuestionCategoryView> createCategory(QuestionCategoryUpsertRequest request) throws BusinessException {
        Long userId = accessContext.requireUserId();
        Integer roleId = accessContext.requireRoleId();
        QuestionCategory category = buildNewCategory(request, roleId, userId);
        if (request.getParentId() != null) {
            QuestionCategory parent = requireCategory(request.getParentId());
            ensureParentScopeCompatible(parent, request.getIsPersonal(), userId, roleId);
        }
        repository.saveCategory(category);
        return ok(toCategoryView(category));
    }

    @Override
    public ApiResponse<QuestionCategoryView> updateCategory(QuestionCategoryUpsertRequest request) throws BusinessException {
        Long userId = accessContext.requireUserId();
        Integer roleId = accessContext.requireRoleId();
        if (request.getCategoryId() == null) {
            throw new BusinessException(DomainErrorCode.QUESTION_CATEGORY_ID_REQUIRED);
        }
        QuestionCategory category = requireCategory(request.getCategoryId());
        if (!Objects.equals(category.getPersonal(), request.getIsPersonal())) {
            throw new BusinessException(DomainErrorCode.QUESTION_CATEGORY_SCOPE_CONFLICT);
        }
        ensureNoCategoryCycle(category.getId(), request.getParentId());
        ensureCategoryWriteAccess(category, roleId, userId);
        if (request.getParentId() != null) {
            QuestionCategory parent = requireCategory(request.getParentId());
            ensureParentScopeCompatible(parent, request.getIsPersonal(), userId, roleId);
        }
        category.setName(request.getName())
                .setParentId(request.getParentId())
                .setStatus(resolveCategoryStatus(request.getIsDisabled(), category))
                .setUpdateTime(LocalDateTime.now());
        repository.updateCategory(category);
        return ok(toCategoryView(category));
    }

    @Override
    public ApiResponse<QuestionImportResult> importQuestions(MultipartFile file, Long categoryId) throws BusinessException {
        Long userId = accessContext.requireUserId();
        Integer roleId = accessContext.requireRoleId();
        ensureTeacherOrAdmin(roleId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(DomainErrorCode.QUESTION_IMPORT_FILE_REQUIRED);
        }
        QuestionCategory category = requireCategory(categoryId);
        ensureCategoryAvailableForQuestionCreation(category, roleId, userId);

        List<QuestionCsvCodec.QuestionImportRow> rows;
        try {
            rows = csvCodec.read(file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException(DomainErrorCode.QUESTION_IMPORT_READ_FAILED);
        }
        if (rows.size() > IMPORT_LIMIT) {
            throw new BusinessException(DomainErrorCode.QUESTION_IMPORT_LIMIT_EXCEEDED);
        }

        int importedCount = 0;
        List<String> errors = new ArrayList<>();
        for (QuestionCsvCodec.QuestionImportRow row : rows) {
            if (row.parseError() != null) {
                errors.add("line " + row.lineNumber() + ": " + row.parseError());
                continue;
            }
            try {
                validateQuestionPayload(row.type(), row.options(), row.answer(), row.difficulty());
                QuestionItem question = new QuestionItem()
                        .setId(snowflake.nextId())
                        .setCategoryId(categoryId)
                        .setCreatorId(userId)
                        .setCreatorRoleId(roleId)
                        .setContent(row.content())
                        .setQuestionType(normalizeQuestionType(row.type()))
                        .setOptions(copyOptions(row.options()))
                        .setAnswer(row.answer())
                        .setAnalysis(row.analysis())
                        .setDifficulty(row.difficulty())
                        .setAuditStatus(defaultAuditStatus(roleId, category))
                        .setDisabled(false)
                        .setReferenceCount(0)
                        .setCreateTime(LocalDateTime.now())
                        .setUpdateTime(LocalDateTime.now());
                repository.saveQuestion(question);
                importedCount++;
            } catch (BusinessException ex) {
                errors.add("line " + row.lineNumber() + ": " + ex.getMessage());
            }
        }
        return ok(new QuestionImportResult(importedCount, errors.size(), errors));
    }

    @Override
    public ApiResponse<QuestionExportView> exportQuestions(Long categoryId, Long creatorId) throws BusinessException {
        Integer roleId = accessContext.requireRoleId();
        Long userId = accessContext.requireUserId();
        ensureTeacherAdminOrAuditor(roleId);
        List<QuestionItem> questions = visibleQuestions(roleId, userId).stream()
                .filter(question -> categoryId == null || Objects.equals(question.getCategoryId(), categoryId))
                .filter(question -> creatorId == null || Objects.equals(question.getCreatorId(), creatorId))
                .sorted(Comparator.comparing(QuestionItem::getId))
                .toList();
        boolean masked = QuestionRoleConstants.isAuditor(roleId);
        byte[] content = csvCodec.write(questions, masked);
        LocalDateTime now = LocalDateTime.now();
        String fileKey = "question-export-" + snowflake.nextIdStr();
        String fileName = "question-export-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        resourceFileStore.write(fileKey, ".csv", content);
        return ApiResponse.ok(QuestionExportView.builder()
                        .fileKey(fileKey)
                        .fileName(fileName)
                        .recordCount(questions.size())
                        .masked(masked)
                        .generatedAt(now)
                        .build())
                .withRequestId(accessContext.currentRequestId());
    }

    @Override
    public ApiResponse<QuestionView> audit(QuestionAuditRequest request) throws BusinessException {
        Long userId = accessContext.requireUserId();
        Integer roleId = accessContext.requireRoleId();
        ensureAdmin(roleId);
        QuestionItem question = requireQuestion(request.getQuestionId());
        QuestionAuditStatus status = QuestionAuditStatus.fromCode(request.getAuditResult());
        if (!QuestionAuditStatus.PENDING.getCode().equals(question.getAuditStatus())) {
            throw new BusinessException(DomainErrorCode.QUESTION_AUDIT_STATUS_CONFLICT);
        }
        if (status == QuestionAuditStatus.REJECTED && (request.getReason() == null || request.getReason().isBlank())) {
            throw new BusinessException(DomainErrorCode.QUESTION_REJECT_REASON_REQUIRED);
        }
        question.setAuditStatus(status.getCode());
        question.setUpdateTime(LocalDateTime.now());
        repository.updateQuestion(question);
        AuditTrail trail = AuditTrail.builder()
                .actionType("question.audit")
                .operatorId(userId)
                .targetType("question")
                .targetId(String.valueOf(question.getId()))
                .requestId(accessContext.currentRequestId())
                .detail(status.getCode())
                .occurredAt(LocalDateTime.now())
                .build();
        repository.saveAuditLog(new QuestionAuditLog()
                .setId(snowflake.nextId())
                .setQuestionId(question.getId())
                .setAuditorId(trail.getOperatorId())
                .setAuditResult(status.getCode())
                .setReason(request.getReason())
                .setRequestId(trail.getRequestId())
                .setAuditTime(trail.getOccurredAt())
                .setCreateTime(trail.getOccurredAt())
                .setUpdateTime(trail.getOccurredAt()));
        return ok(toQuestionView(question, true));
    }

    @Override
    public ApiResponse<PageResult<QuestionView>> query(QuestionQueryRequest request) throws BusinessException {
        Integer roleId = accessContext.requireRoleId();
        Long userId = accessContext.requireUserId();
        ensureTeacherAdminOrAuditor(roleId);
        List<QuestionView> records = visibleQuestions(roleId, userId).stream()
                .filter(question -> request.getQuestionId() == null || Objects.equals(question.getId(), request.getQuestionId()))
                .filter(question -> request.getCategoryId() == null || Objects.equals(question.getCategoryId(), request.getCategoryId()))
                .filter(question -> request.getCreatorId() == null || Objects.equals(question.getCreatorId(), request.getCreatorId()))
                .filter(question -> request.getKeyword() == null || containsIgnoreCase(question.getContent(), request.getKeyword()))
                .filter(question -> request.getType() == null || question.getQuestionType().equalsIgnoreCase(request.getType()))
                .filter(question -> request.getDifficulty() == null || Objects.equals(question.getDifficulty(), request.getDifficulty()))
                .filter(question -> request.getAuditStatus() == null || question.getAuditStatus().equalsIgnoreCase(request.getAuditStatus()))
                .filter(question -> request.getIsDisabled() == null || Objects.equals(question.getDisabled(), request.getIsDisabled()))
                .filter(question -> request.getIsReferenced() == null
                        || Objects.equals(isQuestionReferenced(question), request.getIsReferenced()))
                .sorted(Comparator.comparing(QuestionItem::getId).reversed())
                .map(question -> toQuestionView(question, !QuestionRoleConstants.isAuditor(roleId)))
                .toList();
        return ok(page(records, request.getPageNum(), request.getPageSize()));
    }

    @Override
    public ApiResponse<PageResult<QuestionCategoryView>> queryCategories(QuestionCategoryQueryRequest request) throws BusinessException {
        Integer roleId = accessContext.requireRoleId();
        Long userId = accessContext.requireUserId();
        ensureTeacherAdminOrAuditor(roleId);
        List<QuestionCategoryView> records = visibleCategories(roleId, userId).stream()
                .filter(category -> request.getCategoryId() == null || Objects.equals(category.getId(), request.getCategoryId()))
                .filter(category -> request.getParentId() == null || Objects.equals(category.getParentId(), request.getParentId()))
                .filter(category -> request.getCreatorId() == null || Objects.equals(category.getOwnerId(), request.getCreatorId()))
                .filter(category -> request.getIsPersonal() == null || Objects.equals(category.getPersonal(), request.getIsPersonal()))
                .filter(category -> request.getIsDisabled() == null || Objects.equals(isCategoryDisabled(category), request.getIsDisabled()))
                .filter(category -> request.getKeyword() == null || containsIgnoreCase(category.getName(), request.getKeyword()))
                .sorted(Comparator.comparing(QuestionCategory::getId).reversed())
                .map(this::toCategoryView)
                .toList();
        return ok(page(records, request.getPageNum(), request.getPageSize()));
    }

    private QuestionCategory buildNewCategory(QuestionCategoryUpsertRequest request,
                                              Integer roleId,
                                              Long userId) throws BusinessException {
        LocalDateTime now = LocalDateTime.now();
        QuestionCategory category = new QuestionCategory()
                .setId(snowflake.nextId())
                .setName(request.getName())
                .setParentId(request.getParentId())
                .setPersonal(request.getIsPersonal())
                .setStatus(resolveCategoryStatus(request.getIsDisabled(), null))
                .setCreateTime(now)
                .setUpdateTime(now);
        if (QuestionRoleConstants.isTeacher(roleId)) {
            if (!Boolean.TRUE.equals(request.getIsPersonal())) {
                throw new BusinessException(DomainErrorCode.QUESTION_CATEGORY_SCOPE_FORBIDDEN);
            }
            category.setOwnerId(userId);
            return category;
        }
        if (QuestionRoleConstants.isAdmin(roleId)) {
            if (Boolean.TRUE.equals(request.getIsPersonal())) {
                throw new BusinessException(DomainErrorCode.QUESTION_CATEGORY_SCOPE_FORBIDDEN);
            }
            return category;
        }
        throw new BusinessException(DomainErrorCode.QUESTION_CATEGORY_MANAGE_FORBIDDEN);
    }

    private void ensureParentScopeCompatible(QuestionCategory parent,
                                             Boolean personal,
                                             Long userId,
                                             Integer roleId) throws BusinessException {
        ensureCategoryEnabled(parent);
        if (Boolean.TRUE.equals(personal)) {
            if (!Boolean.TRUE.equals(parent.getPersonal()) || !Objects.equals(parent.getOwnerId(), userId)) {
                throw new BusinessException(DomainErrorCode.QUESTION_CATEGORY_SCOPE_FORBIDDEN);
            }
            return;
        }
        if (!QuestionRoleConstants.isAdmin(roleId) || Boolean.TRUE.equals(parent.getPersonal())) {
            throw new BusinessException(DomainErrorCode.QUESTION_CATEGORY_SCOPE_FORBIDDEN);
        }
    }

    private void ensureCategoryAvailableForQuestionCreation(QuestionCategory category,
                                                            Integer roleId,
                                                            Long userId) throws BusinessException {
        ensureCategoryEnabled(category);
        if (Boolean.TRUE.equals(category.getPersonal())) {
            if (!QuestionRoleConstants.isTeacher(roleId) || !Objects.equals(category.getOwnerId(), userId)) {
                throw new BusinessException(DomainErrorCode.QUESTION_CATEGORY_SCOPE_FORBIDDEN);
            }
            return;
        }
        ensureTeacherOrAdmin(roleId);
    }

    private void ensureCategoryWriteAccess(QuestionCategory category,
                                           Integer roleId,
                                           Long userId) throws BusinessException {
        if (Boolean.TRUE.equals(category.getPersonal())) {
            if (!QuestionRoleConstants.isTeacher(roleId) || !Objects.equals(category.getOwnerId(), userId)) {
                throw new BusinessException(DomainErrorCode.QUESTION_CATEGORY_SCOPE_FORBIDDEN);
            }
            return;
        }
        ensureAdmin(roleId);
    }

    private void ensureQuestionWriteAccess(QuestionItem question,
                                           Integer roleId,
                                           Long userId) throws BusinessException {
        if (QuestionRoleConstants.isAdmin(roleId)) {
            return;
        }
        if (QuestionRoleConstants.isTeacher(roleId) && Objects.equals(question.getCreatorId(), userId)) {
            return;
        }
        throw new BusinessException(DomainErrorCode.QUESTION_ACCESS_FORBIDDEN);
    }

    private void ensureTeacherOrAdmin(Integer roleId) throws BusinessException {
        if (QuestionRoleConstants.isTeacher(roleId) || QuestionRoleConstants.isAdmin(roleId)) {
            return;
        }
        throw new BusinessException(DomainErrorCode.QUESTION_MANAGE_FORBIDDEN);
    }

    private void ensureTeacherAdminOrAuditor(Integer roleId) throws BusinessException {
        if (QuestionRoleConstants.isTeacher(roleId)
                || QuestionRoleConstants.isAdmin(roleId)
                || QuestionRoleConstants.isAuditor(roleId)) {
            return;
        }
        throw new BusinessException(DomainErrorCode.QUESTION_QUERY_FORBIDDEN);
    }

    private void ensureAdmin(Integer roleId) throws BusinessException {
        if (!QuestionRoleConstants.isAdmin(roleId)) {
            throw new BusinessException(DomainErrorCode.QUESTION_AUDIT_FORBIDDEN);
        }
    }

    private QuestionCategory requireCategory(Long categoryId) throws BusinessException {
        return repository.findCategoryById(categoryId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.QUESTION_CATEGORY_NOT_FOUND));
    }

    private QuestionItem requireQuestion(Long questionId) throws BusinessException {
        return repository.findQuestionById(questionId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.QUESTION_NOT_FOUND));
    }

    private void validateQuestionPayload(String type,
                                         List<String> options,
                                         String answer,
                                         Integer difficulty) throws BusinessException {
        QuestionType questionType = QuestionType.fromCode(type);
        if (difficulty == null || difficulty < 1 || difficulty > 5) {
            throw new BusinessException(DomainErrorCode.QUESTION_DIFFICULTY_INVALID);
        }
        if (answer == null || answer.isBlank()) {
            throw new BusinessException(DomainErrorCode.QUESTION_ANSWER_REQUIRED);
        }
        if (questionType == QuestionType.SUBJECTIVE) {
            return;
        }
        if (options == null || options.isEmpty()) {
            throw new BusinessException(DomainErrorCode.QUESTION_OPTIONS_REQUIRED);
        }
    }

    private void ensureNoCategoryCycle(Long categoryId, Long parentId) throws BusinessException {
        if (parentId == null) {
            return;
        }
        if (Objects.equals(categoryId, parentId)) {
            throw new BusinessException(DomainErrorCode.QUESTION_CATEGORY_PARENT_INVALID);
        }
        Long cursor = parentId;
        while (cursor != null) {
            if (Objects.equals(cursor, categoryId)) {
                throw new BusinessException(DomainErrorCode.QUESTION_CATEGORY_PARENT_INVALID);
            }
            QuestionCategory parent = repository.findCategoryById(cursor).orElse(null);
            cursor = parent == null ? null : parent.getParentId();
        }
    }

    private void ensureCategoryEnabled(QuestionCategory category) throws BusinessException {
        if (isCategoryDisabled(category)) {
            throw new BusinessException(DomainErrorCode.QUESTION_CATEGORY_DISABLED);
        }
    }

    private Integer resolveCategoryStatus(Boolean isDisabled, QuestionCategory existingCategory) {
        if (isDisabled != null) {
            return Boolean.TRUE.equals(isDisabled) ? 0 : 1;
        }
        if (existingCategory != null && existingCategory.getStatus() != null) {
            return existingCategory.getStatus();
        }
        return 1;
    }

    private boolean isCategoryDisabled(QuestionCategory category) {
        return category.getStatus() != null && category.getStatus() == 0;
    }

    private String normalizeQuestionType(String type) throws BusinessException {
        return QuestionType.fromCode(type).getCode();
    }

    private String defaultAuditStatus(Integer roleId, QuestionCategory category) {
        if (QuestionRoleConstants.isAdmin(roleId)) {
            return QuestionAuditStatus.APPROVED.getCode();
        }
        if (Boolean.TRUE.equals(category.getPersonal())) {
            return QuestionAuditStatus.APPROVED.getCode();
        }
        return QuestionAuditStatus.PENDING.getCode();
    }

    private List<String> copyOptions(List<String> options) {
        return options == null ? new ArrayList<>() : new ArrayList<>(options);
    }

    private List<QuestionItem> visibleQuestions(Integer roleId, Long userId) {
        List<QuestionItem> allQuestions = repository.listQuestions();
        if (QuestionRoleConstants.isAdmin(roleId) || QuestionRoleConstants.isAuditor(roleId)) {
            return allQuestions;
        }
        return allQuestions.stream()
                .filter(question -> Objects.equals(question.getCreatorId(), userId))
                .toList();
    }

    private List<QuestionCategory> visibleCategories(Integer roleId, Long userId) {
        List<QuestionCategory> allCategories = repository.listCategories();
        if (QuestionRoleConstants.isAdmin(roleId) || QuestionRoleConstants.isAuditor(roleId)) {
            return allCategories;
        }
        return allCategories.stream()
                .filter(category -> !Boolean.TRUE.equals(category.getPersonal())
                        || Objects.equals(category.getOwnerId(), userId))
                .toList();
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private <T> PageResult<T> page(List<T> source, int pageNum, int pageSize) {
        int fromIndex = Math.min((pageNum - 1) * pageSize, source.size());
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        return new PageResult<>(source.subList(fromIndex, toIndex), source.size(), pageNum, pageSize);
    }

    private QuestionView toQuestionView(QuestionItem question, boolean includeSensitive) {
        return QuestionView.builder()
                .questionId(question.getId())
                .categoryId(question.getCategoryId())
                .creatorId(question.getCreatorId())
                .content(question.getContent())
                .type(question.getQuestionType())
                .options(copyOptions(question.getOptions()))
                .answer(includeSensitive ? question.getAnswer() : null)
                .analysis(includeSensitive ? question.getAnalysis() : null)
                .difficulty(question.getDifficulty())
                .auditStatus(question.getAuditStatus())
                .isDisabled(question.getDisabled())
                .referenceCount(question.getReferenceCount())
                .referenceLocked(isQuestionReferenced(question))
                .build();
    }

    private boolean isQuestionReferenced(QuestionItem question) {
        return question.getReferenceCount() != null && question.getReferenceCount() > 0;
    }

    private QuestionCategoryView toCategoryView(QuestionCategory category) {
        return QuestionCategoryView.builder()
                .categoryId(category.getId())
                .name(category.getName())
                .parentId(category.getParentId())
                .isPersonal(category.getPersonal())
                .isDisabled(isCategoryDisabled(category))
                .ownerId(category.getOwnerId())
                .build();
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.ok(data).withRequestId(accessContext.currentRequestId());
    }

    private ApiResponse<String> okMessage(String message) {
        return ApiResponse.ok(message).withRequestId(accessContext.currentRequestId());
    }
}
