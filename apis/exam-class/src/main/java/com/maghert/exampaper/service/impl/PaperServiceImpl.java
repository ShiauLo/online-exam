package com.maghert.exampaper.service.impl;

import cn.hutool.core.lang.Snowflake;
import com.maghert.examcommon.audit.AuditTrail;
import com.maghert.examcommon.auth.RoleMappings;
import com.maghert.examcommon.enums.QuestionAuditStatus;
import com.maghert.examcommon.enums.QuestionType;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;
import com.maghert.examcommon.resource.LocalResourceFileStore;
import com.maghert.examcommon.web.ApiResponse;
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
import com.maghert.exampaper.model.dto.PaperDeleteRequest;
import com.maghert.exampaper.model.dto.PaperManualCreateRequest;
import com.maghert.exampaper.model.dto.PaperPublishRequest;
import com.maghert.exampaper.model.dto.PaperQueryRequest;
import com.maghert.exampaper.model.dto.PaperRecycleRequest;
import com.maghert.exampaper.model.dto.PaperTerminateRequest;
import com.maghert.exampaper.model.dto.PaperUpdateRequest;
import com.maghert.exampaper.model.enums.PaperLifecycleStatus;
import com.maghert.exampaper.model.enums.PaperSourceType;
import com.maghert.exampaper.model.vo.PaperExportView;
import com.maghert.exampaper.model.vo.PaperView;
import com.maghert.exampaper.repository.PaperDomainRepository;
import com.maghert.exampaper.service.PaperService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaperServiceImpl implements PaperService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_AUTO_EXAM_TIME = 90;

    private final Snowflake snowflake;
    private final PaperDomainRepository repository;
    private final LocalResourceFileStore resourceFileStore;

    public PaperServiceImpl(Snowflake snowflake,
                            PaperDomainRepository repository,
                            @Value("${exam.resource.local-storage-root:./tmp_resource}") String localStorageRoot) {
        this.snowflake = snowflake;
        this.repository = repository;
        this.resourceFileStore = new LocalResourceFileStore(localStorageRoot);
    }

    @Override
    public ApiResponse<PageResult<PaperView>> query(PaperQueryRequest request, RequestContext context)
            throws BusinessException {
        ensureTeacherAdminOrAuditor(context);
        List<PaperView> records = repository.listPapers().stream()
                .filter(paper -> !RoleMappings.isTeacher(context.roleId())
                        || Objects.equals(paper.getCreatorId(), context.userId()))
                .filter(paper -> request.getPaperId() == null || Objects.equals(paper.getPaperId(), request.getPaperId()))
                .filter(paper -> request.getCreatorId() == null || Objects.equals(paper.getCreatorId(), request.getCreatorId()))
                .filter(paper -> request.getStatus() == null || paper.getStatus().equalsIgnoreCase(request.getStatus()))
                .filter(paper -> request.getSourceType() == null || paper.getSourceType().equalsIgnoreCase(request.getSourceType()))
                .filter(paper -> request.getKeyword() == null || containsIgnoreCase(paper.getPaperName(), request.getKeyword()))
                .filter(paper -> request.getClassId() == null || currentClassIds(paper.getPaperId()).contains(request.getClassId()))
                .sorted(Comparator.comparing(PaperEntity::getUpdateTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PaperEntity::getPaperId, Comparator.reverseOrder()))
                .map(paper -> toPaperView(paper, currentQuestionIds(paper.getPaperId()), currentClassIds(paper.getPaperId())))
                .toList();
        return ApiResponse.ok(page(records, Math.toIntExact(request.getPageNum()), Math.toIntExact(request.getPageSize())))
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<PaperView> createManual(PaperManualCreateRequest request, RequestContext context)
            throws BusinessException {
        ensureTeacherOrAdmin(context);
        List<Long> questionIds = normalizeIds(request.getQuestionIds());
        if (questionIds.isEmpty()) {
            throw new BusinessException(DomainErrorCode.PAPER_QUESTION_REQUIRED);
        }
        int examTime = validateExamTime(request.getExamTime());
        List<QuestionSnapshotEntity> selectedQuestions = resolveQuestionsForPaper(questionIds, context);
        int totalScore = selectedQuestions.size();
        int passScore = validatePassScore(request.getPassScore(), totalScore);
        PaperEntity paper = buildPaper(request.getPaperName(), PaperSourceType.MANUAL, context, examTime, passScore, totalScore);
        repository.savePaper(paper);
        repository.replacePaperQuestions(paper.getPaperId(), buildBindings(paper.getPaperId(), selectedQuestions));
        changeQuestionReferenceCount(questionIds, 1);
        return ok(toPaperView(paper, questionIds, List.of()), context);
    }

    @Override
    public ApiResponse<PaperView> createAuto(PaperAutoCreateRequest request, RequestContext context)
            throws BusinessException {
        ensureTeacherOrAdmin(context);
        int totalScore = request.getTotalScore();
        List<QuestionSnapshotEntity> pool = accessibleQuestionPool(context);
        Map<String, Integer> typeTargets = allocateTargets(
                normalizeTypeRatio(request.getTypeRatio()), totalScore);
        Map<Integer, Integer> difficultyTargets = allocateTargets(
                normalizeDifficultyRatio(request.getDifficultyRatio()), totalScore);
        Map<Long, Integer> knowledgeTargets = allocateTargets(
                normalizeKnowledgeRatio(request.getKnowledgeRatio()), totalScore);
        List<QuestionSnapshotEntity> selectedQuestions = autoAssemble(pool, typeTargets, difficultyTargets, knowledgeTargets);
        int examTime = request.getExamTime() == null
                ? DEFAULT_AUTO_EXAM_TIME
                : validateExamTime(request.getExamTime());
        int passScore = request.getPassScore() == null
                ? defaultPassScore(totalScore)
                : validatePassScore(request.getPassScore(), totalScore);
        PaperEntity paper = buildPaper(request.getPaperName(), PaperSourceType.AUTO, context, examTime, passScore, totalScore);
        repository.savePaper(paper);
        repository.replacePaperQuestions(paper.getPaperId(), buildBindings(paper.getPaperId(), selectedQuestions));
        List<Long> questionIds = selectedQuestions.stream()
                .map(QuestionSnapshotEntity::getQuestionId)
                .toList();
        changeQuestionReferenceCount(questionIds, 1);
        return ok(toPaperView(paper, questionIds, List.of()), context);
    }

    @Override
    public ApiResponse<PaperView> update(PaperUpdateRequest request, RequestContext context) throws BusinessException {
        ensureTeacherOrAdmin(context);
        PaperEntity paper = requirePaper(request.getPaperId());
        ensurePaperOperator(context, paper);
        PaperLifecycleStatus status = PaperLifecycleStatus.valueOf(paper.getStatus());
        if (!status.editable()) {
            throw new BusinessException(DomainErrorCode.PAPER_STATUS_CONFLICT);
        }

        List<Long> currentQuestionIds = currentQuestionIds(paper.getPaperId());
        List<Long> nextQuestionIds = currentQuestionIds;
        int nextTotalScore = paper.getTotalScore();
        if (request.getQuestionIds() != null) {
            nextQuestionIds = normalizeIds(request.getQuestionIds());
            if (nextQuestionIds.isEmpty()) {
                throw new BusinessException(DomainErrorCode.PAPER_QUESTION_REQUIRED);
            }
            nextTotalScore = nextQuestionIds.size();
        }
        int nextExamTime = request.getExamTime() == null ? paper.getExamTime() : validateExamTime(request.getExamTime());
        int nextPassScore = request.getPassScore() == null ? paper.getPassScore() : request.getPassScore();
        validatePassScore(nextPassScore, nextTotalScore);

        if (request.getPaperName() != null) {
            paper.setPaperName(normalizePaperName(request.getPaperName()));
        }
        paper.setExamTime(nextExamTime)
                .setPassScore(nextPassScore)
                .setTotalScore(nextTotalScore)
                .setRequestId(context.requestId())
                .setUpdateTime(LocalDateTime.now());
        if (status == PaperLifecycleStatus.REJECTED) {
            paper.setStatus(PaperLifecycleStatus.DRAFT.name());
        }

        if (request.getQuestionIds() != null) {
            List<QuestionSnapshotEntity> selectedQuestions = resolveQuestionsForPaper(nextQuestionIds, context);
            Set<Long> currentSet = new LinkedHashSet<>(currentQuestionIds);
            Set<Long> nextSet = new LinkedHashSet<>(nextQuestionIds);
            changeQuestionReferenceCount(currentSet.stream().filter(item -> !nextSet.contains(item)).toList(), -1);
            changeQuestionReferenceCount(nextSet.stream().filter(item -> !currentSet.contains(item)).toList(), 1);
            repository.replacePaperQuestions(paper.getPaperId(), buildBindings(paper.getPaperId(), selectedQuestions));
        }

        repository.updatePaper(paper);
        return ok(toPaperView(paper, nextQuestionIds, currentClassIds(paper.getPaperId())), context);
    }

    @Override
    public ApiResponse<String> delete(PaperDeleteRequest request, RequestContext context) throws BusinessException {
        ensureAdmin(context);
        PaperEntity paper = requirePaper(request.getPaperId());
        PaperLifecycleStatus status = PaperLifecycleStatus.valueOf(paper.getStatus());
        if (status == PaperLifecycleStatus.PUBLISHED
                || status == PaperLifecycleStatus.TERMINATED
                || status == PaperLifecycleStatus.RECYCLED) {
            throw new BusinessException(DomainErrorCode.PAPER_STATUS_CONFLICT);
        }
        changeQuestionReferenceCount(currentQuestionIds(paper.getPaperId()), -1);
        repository.deletePaperPublishClasses(paper.getPaperId());
        repository.deletePaperQuestions(paper.getPaperId());
        repository.deletePaper(paper.getPaperId());
        return ApiResponse.ok("ok").withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<PaperView> audit(PaperAuditRequest request, RequestContext context) throws BusinessException {
        ensureAdmin(context);
        PaperEntity paper = requirePaper(request.getPaperId());
        PaperLifecycleStatus status = PaperLifecycleStatus.valueOf(paper.getStatus());
        if (status == PaperLifecycleStatus.PUBLISHED
                || status == PaperLifecycleStatus.TERMINATED
                || status == PaperLifecycleStatus.RECYCLED) {
            throw new BusinessException(DomainErrorCode.PAPER_STATUS_CONFLICT);
        }
        PaperLifecycleStatus nextStatus = resolveAuditResult(request.getAuditResult(), request.getReason());
        LocalDateTime now = LocalDateTime.now();
        paper.setStatus(nextStatus.name())
                .setRequestId(context.requestId())
                .setUpdateTime(now);
        repository.updatePaper(paper);
        AuditTrail trail = AuditTrail.builder()
                .actionType("paper.audit")
                .operatorId(context.userId())
                .targetType("paper")
                .targetId(String.valueOf(paper.getPaperId()))
                .requestId(context.requestId())
                .detail(nextStatus.name())
                .occurredAt(now)
                .build();
        repository.saveAuditLog(new PaperAuditLogEntity()
                .setAuditLogId(snowflake.nextId())
                .setPaperId(paper.getPaperId())
                .setAuditorId(context.userId())
                .setAuditResult(nextStatus.name())
                .setReason(request.getReason())
                .setRequestId(trail.getRequestId())
                .setAuditTime(trail.getOccurredAt())
                .setCreateTime(trail.getOccurredAt())
                .setUpdateTime(trail.getOccurredAt()));
        return ok(toPaperView(paper, currentQuestionIds(paper.getPaperId()), currentClassIds(paper.getPaperId())), context);
    }

    @Override
    public ApiResponse<PaperView> publish(PaperPublishRequest request, RequestContext context)
            throws BusinessException {
        ensureTeacherOrAdmin(context);
        PaperEntity paper = requirePaper(request.getPaperId());
        ensurePaperOperator(context, paper);
        if (!PaperLifecycleStatus.valueOf(paper.getStatus()).publishable()) {
            throw new BusinessException(DomainErrorCode.PAPER_STATUS_CONFLICT);
        }
        List<Long> classIds = normalizeIds(request.getClassIds());
        if (classIds.isEmpty()) {
            throw new BusinessException(DomainErrorCode.PAPER_PUBLISH_CLASS_REQUIRED);
        }
        List<ClassSnapshotEntity> classes = repository.findClassesByIds(classIds);
        if (classes.size() != classIds.size()) {
            throw new BusinessException(DomainErrorCode.CLASS_NOT_FOUND);
        }
        for (ClassSnapshotEntity clazz : classes) {
            if (!"active".equalsIgnoreCase(clazz.getStatus())) {
                throw new BusinessException(DomainErrorCode.PAPER_CLASS_STATUS_INVALID);
            }
            if (RoleMappings.isTeacher(context.roleId()) && !Objects.equals(context.userId(), clazz.getCreatedBy())) {
                throw new BusinessException(DomainErrorCode.PAPER_CLASS_ACCESS_FORBIDDEN);
            }
        }
        LocalDateTime scheduledTime = parseDateTime(request.getExamTime());
        LocalDateTime now = LocalDateTime.now();
        paper.setStatus(PaperLifecycleStatus.PUBLISHED.name())
                .setScheduledExamTime(scheduledTime)
                .setPublishedAt(now)
                .setRequestId(context.requestId())
                .setUpdateTime(now);
        repository.updatePaper(paper);
        repository.replacePaperPublishClasses(paper.getPaperId(), classIds.stream()
                .map(classId -> new PaperPublishClassEntity()
                        .setRelationId(snowflake.nextId())
                        .setPaperId(paper.getPaperId())
                        .setClassId(classId)
                        .setCreateTime(now)
                        .setUpdateTime(now))
                .toList());
        return ok(toPaperView(paper, currentQuestionIds(paper.getPaperId()), classIds), context);
    }

    @Override
    public ApiResponse<PaperView> terminate(PaperTerminateRequest request, RequestContext context)
            throws BusinessException {
        ensureTeacherOrAdmin(context);
        PaperEntity paper = requirePaper(request.getPaperId());
        ensurePaperOperator(context, paper);
        if (PaperLifecycleStatus.valueOf(paper.getStatus()) != PaperLifecycleStatus.PUBLISHED) {
            throw new BusinessException(DomainErrorCode.PAPER_STATUS_CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now();
        paper.setStatus(PaperLifecycleStatus.TERMINATED.name())
                .setRequestId(context.requestId())
                .setUpdateTime(now);
        repository.updatePaper(paper);
        repository.saveAuditLog(new PaperAuditLogEntity()
                .setAuditLogId(snowflake.nextId())
                .setPaperId(paper.getPaperId())
                .setAuditorId(context.userId())
                .setAuditResult(PaperLifecycleStatus.TERMINATED.name())
                .setReason(request.getReason().trim())
                .setRequestId(context.requestId())
                .setAuditTime(now)
                .setCreateTime(now)
                .setUpdateTime(now));
        return ok(toPaperView(paper, currentQuestionIds(paper.getPaperId()), currentClassIds(paper.getPaperId())), context);
    }

    @Override
    public ApiResponse<PaperView> recycle(PaperRecycleRequest request, RequestContext context)
            throws BusinessException {
        ensureTeacherOrAdmin(context);
        PaperEntity paper = requirePaper(request.getPaperId());
        ensurePaperOperator(context, paper);
        if (PaperLifecycleStatus.valueOf(paper.getStatus()) != PaperLifecycleStatus.PUBLISHED) {
            throw new BusinessException(DomainErrorCode.PAPER_STATUS_CONFLICT);
        }
        if (paper.getScheduledExamTime() == null || paper.getExamTime() == null) {
            throw new BusinessException(DomainErrorCode.PAPER_RECYCLE_TIME_CONFLICT);
        }
        if (paper.getScheduledExamTime().plusMinutes(paper.getExamTime()).isAfter(LocalDateTime.now())) {
            throw new BusinessException(DomainErrorCode.PAPER_RECYCLE_TIME_CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now();
        paper.setStatus(PaperLifecycleStatus.RECYCLED.name())
                .setRecycledAt(now)
                .setRequestId(context.requestId())
                .setUpdateTime(now);
        repository.updatePaper(paper);
        return ok(toPaperView(paper, currentQuestionIds(paper.getPaperId()), currentClassIds(paper.getPaperId())), context);
    }

    @Override
    public ApiResponse<PaperExportView> export(Long paperId, String approverId, RequestContext context)
            throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isTeacher(context.roleId())
                && !RoleMappings.isAdmin(context.roleId())
                && !RoleMappings.isAuditor(context.roleId())) {
            throw new BusinessException(DomainErrorCode.PAPER_EXPORT_FORBIDDEN);
        }
        if (RoleMappings.isAuditor(context.roleId()) && (approverId == null || approverId.isBlank())) {
            throw new BusinessException(DomainErrorCode.PAPER_EXPORT_APPROVAL_REQUIRED);
        }
        List<PaperEntity> papers = repository.listPapers().stream()
                .filter(paper -> paperId == null || Objects.equals(paper.getPaperId(), paperId))
                .filter(paper -> !RoleMappings.isTeacher(context.roleId())
                        || Objects.equals(paper.getCreatorId(), context.userId()))
                .sorted(Comparator.comparing(PaperEntity::getPaperId))
                .toList();
        String watermark = "exportedBy=%s#%s".formatted(RoleMappings.toExternalCode(context.roleId()), context.userId());
        List<String> lines = new ArrayList<>();
        lines.add("paperId,paperName,status,sourceType,creatorId,examTime,passScore,totalScore,questionIds,classIds,scheduledExamTime,publishedAt,recycledAt,approverId,watermark");
        papers.forEach(paper -> lines.add("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s".formatted(
                paper.getPaperId(),
                sanitize(paper.getPaperName()),
                sanitize(paper.getStatus()),
                sanitize(paper.getSourceType()),
                paper.getCreatorId(),
                paper.getExamTime(),
                paper.getPassScore(),
                paper.getTotalScore(),
                sanitize(joinIds(currentQuestionIds(paper.getPaperId()))),
                sanitize(joinIds(currentClassIds(paper.getPaperId()))),
                paper.getScheduledExamTime(),
                paper.getPublishedAt(),
                paper.getRecycledAt(),
                sanitize(approverId),
                sanitize(watermark))));
        LocalDateTime now = LocalDateTime.now();
        String fileKey = "paper-export-" + snowflake.nextIdStr();
        String fileName = "paper-export-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        resourceFileStore.writeCsv(fileKey, String.join(System.lineSeparator(), lines));
        return ApiResponse.ok(PaperExportView.builder()
                        .fileKey(fileKey)
                        .fileName(fileName)
                        .recordCount(papers.size())
                        .generatedAt(now)
                        .build())
                .withRequestId(context.requestId());
    }

    private List<QuestionSnapshotEntity> autoAssemble(List<QuestionSnapshotEntity> pool,
                                                      Map<String, Integer> typeTargets,
                                                      Map<Integer, Integer> difficultyTargets,
                                                      Map<Long, Integer> knowledgeTargets) throws BusinessException {
        ensurePoolSatisfiesTargets(pool, typeTargets, QuestionSnapshotEntity::getQuestionType);
        ensurePoolSatisfiesTargets(pool, difficultyTargets, QuestionSnapshotEntity::getDifficulty);
        if (!knowledgeTargets.isEmpty()) {
            ensurePoolSatisfiesTargets(pool, knowledgeTargets, QuestionSnapshotEntity::getCategoryId);
        }
        Map<String, Integer> remainingTypes = new LinkedHashMap<>(typeTargets);
        Map<Integer, Integer> remainingDifficulty = new LinkedHashMap<>(difficultyTargets);
        Map<Long, Integer> remainingKnowledge = new LinkedHashMap<>(knowledgeTargets);
        List<QuestionSnapshotEntity> selected = new ArrayList<>();
        Set<Long> selectedIds = new LinkedHashSet<>();
        int expectedCount = typeTargets.values().stream().mapToInt(Integer::intValue).sum();
        for (int index = 0; index < expectedCount; index++) {
            QuestionSnapshotEntity candidate = pool.stream()
                    .filter(item -> !selectedIds.contains(item.getQuestionId()))
                    .max(Comparator.comparingInt((QuestionSnapshotEntity item) ->
                                    scoreCandidate(item, remainingTypes, remainingDifficulty, remainingKnowledge))
                            .thenComparing(QuestionSnapshotEntity::getQuestionId, Comparator.reverseOrder()))
                    .orElseThrow(() -> new BusinessException(DomainErrorCode.PAPER_AUTO_ASSEMBLE_FAILED));
            selected.add(candidate);
            selectedIds.add(candidate.getQuestionId());
            consumeQuota(remainingTypes, candidate.getQuestionType());
            consumeQuota(remainingDifficulty, candidate.getDifficulty());
            consumeQuota(remainingKnowledge, candidate.getCategoryId());
        }
        if (hasUnmetQuota(remainingTypes) || hasUnmetQuota(remainingDifficulty) || hasUnmetQuota(remainingKnowledge)) {
            throw new BusinessException(DomainErrorCode.PAPER_AUTO_ASSEMBLE_FAILED);
        }
        return selected;
    }

    private int scoreCandidate(QuestionSnapshotEntity question,
                               Map<String, Integer> remainingTypes,
                               Map<Integer, Integer> remainingDifficulty,
                               Map<Long, Integer> remainingKnowledge) {
        int score = 1;
        score += positiveQuota(remainingTypes, question.getQuestionType()) * 100;
        score += positiveQuota(remainingDifficulty, question.getDifficulty()) * 10;
        score += positiveQuota(remainingKnowledge, question.getCategoryId()) * 50;
        return score;
    }

    private <K> int positiveQuota(Map<K, Integer> quotas, K key) {
        Integer value = quotas.get(key);
        return value != null && value > 0 ? value : 0;
    }

    private <K> void ensurePoolSatisfiesTargets(List<QuestionSnapshotEntity> pool,
                                                Map<K, Integer> targets,
                                                Function<QuestionSnapshotEntity, K> keyResolver)
            throws BusinessException {
        for (Map.Entry<K, Integer> entry : targets.entrySet()) {
            long matched = pool.stream().filter(item -> Objects.equals(keyResolver.apply(item), entry.getKey())).count();
            if (matched < entry.getValue()) {
                throw new BusinessException(DomainErrorCode.PAPER_AUTO_ASSEMBLE_FAILED);
            }
        }
    }

    private <K> void consumeQuota(Map<K, Integer> quotas, K key) {
        Integer current = quotas.get(key);
        if (current != null && current > 0) {
            quotas.put(key, current - 1);
        }
    }

    private <K> boolean hasUnmetQuota(Map<K, Integer> quotas) {
        return quotas.values().stream().anyMatch(value -> value != null && value > 0);
    }

    private PaperLifecycleStatus resolveAuditResult(String auditResult, String reason) throws BusinessException {
        String normalized = auditResult == null ? "" : auditResult.trim().toLowerCase(Locale.ROOT);
        if ("approved".equals(normalized)) {
            return PaperLifecycleStatus.APPROVED;
        }
        if ("rejected".equals(normalized)) {
            if (reason == null || reason.isBlank()) {
                throw new BusinessException(DomainErrorCode.PAPER_REJECT_REASON_REQUIRED);
            }
            return PaperLifecycleStatus.REJECTED;
        }
        throw new BusinessException(DomainErrorCode.PAPER_AUDIT_RESULT_INVALID);
    }

    private PaperEntity buildPaper(String paperName,
                                   PaperSourceType sourceType,
                                   RequestContext context,
                                   int examTime,
                                   int passScore,
                                   int totalScore) throws BusinessException {
        LocalDateTime now = LocalDateTime.now();
        return new PaperEntity()
                .setPaperId(snowflake.nextId())
                .setPaperName(normalizePaperName(paperName))
                .setCreatorId(context.userId())
                .setCreatorRoleId(context.roleId())
                .setStatus(PaperLifecycleStatus.DRAFT.name())
                .setSourceType(sourceType.name())
                .setExamTime(examTime)
                .setPassScore(passScore)
                .setTotalScore(totalScore)
                .setRequestId(context.requestId())
                .setCreateTime(now)
                .setUpdateTime(now);
    }

    private List<PaperQuestionBindingEntity> buildBindings(Long paperId, List<QuestionSnapshotEntity> questions) {
        LocalDateTime now = LocalDateTime.now();
        List<PaperQuestionBindingEntity> bindings = new ArrayList<>();
        for (int index = 0; index < questions.size(); index++) {
            QuestionSnapshotEntity question = questions.get(index);
            bindings.add(new PaperQuestionBindingEntity()
                    .setBindingId(snowflake.nextId())
                    .setPaperId(paperId)
                    .setQuestionId(question.getQuestionId())
                    .setSortNo(index + 1)
                    .setAssignedScore(1)
                    .setQuestionType(question.getQuestionType())
                    .setDifficulty(question.getDifficulty())
                    .setCreateTime(now)
                    .setUpdateTime(now));
        }
        return bindings;
    }

    private List<QuestionSnapshotEntity> resolveQuestionsForPaper(List<Long> questionIds, RequestContext context)
            throws BusinessException {
        List<QuestionSnapshotEntity> questions = repository.findQuestionsByIds(questionIds);
        if (questions.size() != questionIds.size()) {
            throw new BusinessException(DomainErrorCode.QUESTION_NOT_FOUND);
        }
        Map<Long, QuestionSnapshotEntity> questionById = questions.stream()
                .collect(Collectors.toMap(QuestionSnapshotEntity::getQuestionId, Function.identity()));
        Map<Long, QuestionCategorySnapshotEntity> categoriesById = loadCategories(questions);
        List<QuestionSnapshotEntity> selected = new ArrayList<>();
        for (Long questionId : questionIds) {
            QuestionSnapshotEntity question = questionById.get(questionId);
            if (!canUseQuestion(question, categoriesById.get(question.getCategoryId()), context)) {
                throw new BusinessException(DomainErrorCode.PAPER_QUESTION_ACCESS_FORBIDDEN);
            }
            selected.add(question);
        }
        return selected;
    }

    private List<QuestionSnapshotEntity> accessibleQuestionPool(RequestContext context) {
        List<QuestionSnapshotEntity> allQuestions = repository.listQuestions().stream()
                .filter(item -> !Boolean.TRUE.equals(item.getDisabled()))
                .sorted(Comparator.comparing(QuestionSnapshotEntity::getQuestionId))
                .toList();
        Map<Long, QuestionCategorySnapshotEntity> categoriesById = loadCategories(allQuestions);
        return allQuestions.stream()
                .filter(question -> canUseQuestion(question, categoriesById.get(question.getCategoryId()), context))
                .toList();
    }

    private Map<Long, QuestionCategorySnapshotEntity> loadCategories(Collection<QuestionSnapshotEntity> questions) {
        Set<Long> categoryIds = questions.stream()
                .map(QuestionSnapshotEntity::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return repository.findQuestionCategoriesByIds(categoryIds).stream()
                .collect(Collectors.toMap(QuestionCategorySnapshotEntity::getCategoryId, Function.identity()));
    }

    private boolean canUseQuestion(QuestionSnapshotEntity question,
                                   QuestionCategorySnapshotEntity category,
                                   RequestContext context) {
        if (question == null || category == null || Boolean.TRUE.equals(question.getDisabled())) {
            return false;
        }
        if (category.getStatus() != null && category.getStatus() == 0) {
            return false;
        }
        if (Objects.equals(question.getCreatorId(), context.userId())) {
            return true;
        }
        if (Boolean.TRUE.equals(category.getPersonal())) {
            return false;
        }
        return QuestionAuditStatus.APPROVED.getCode().equalsIgnoreCase(question.getAuditStatus());
    }

    private void ensureTeacherOrAdmin(RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isTeacher(context.roleId()) && !RoleMappings.isAdmin(context.roleId())) {
            throw new BusinessException(DomainErrorCode.PAPER_MANAGE_FORBIDDEN);
        }
    }

    private void ensureTeacherAdminOrAuditor(RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isTeacher(context.roleId())
                && !RoleMappings.isAdmin(context.roleId())
                && !RoleMappings.isAuditor(context.roleId())) {
            throw new BusinessException(DomainErrorCode.PAPER_MANAGE_FORBIDDEN);
        }
    }

    private void ensureAdmin(RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isAdmin(context.roleId())) {
            throw new BusinessException(DomainErrorCode.PAPER_MANAGE_FORBIDDEN);
        }
    }

    private void ensureAuthenticated(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
    }

    private void ensurePaperOperator(RequestContext context, PaperEntity paper) throws BusinessException {
        if (RoleMappings.isAdmin(context.roleId())) {
            return;
        }
        if (RoleMappings.isTeacher(context.roleId()) && Objects.equals(context.userId(), paper.getCreatorId())) {
            return;
        }
        throw new BusinessException(DomainErrorCode.PAPER_MANAGE_FORBIDDEN);
    }

    private PaperEntity requirePaper(Long paperId) throws BusinessException {
        return repository.findPaperById(paperId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PAPER_NOT_FOUND));
    }

    private int validateExamTime(Integer examTime) throws BusinessException {
        if (examTime == null || examTime <= 0) {
            throw new BusinessException(DomainErrorCode.PAPER_EXAM_TIME_INVALID);
        }
        return examTime;
    }

    private int validatePassScore(Integer passScore, int totalScore) throws BusinessException {
        if (passScore == null || passScore <= 0 || passScore > totalScore) {
            throw new BusinessException(DomainErrorCode.PAPER_PASS_SCORE_INVALID);
        }
        return passScore;
    }

    private int defaultPassScore(int totalScore) {
        return Math.max(1, (int) Math.ceil(totalScore * 0.6D));
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && keyword != null && source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String joinIds(List<Long> ids) {
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("|"));
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String normalizePaperName(String paperName) throws BusinessException {
        if (paperName == null || paperName.isBlank()) {
            throw new BusinessException(400, "paperName 不能为空");
        }
        return paperName.trim();
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf));
    }

    private Map<String, Integer> normalizeTypeRatio(Map<String, Integer> raw) throws BusinessException {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : raw.entrySet()) {
            validateRatioEntry(entry);
            normalized.put(QuestionType.fromCode(entry.getKey().trim()).getCode(), entry.getValue());
        }
        return normalized;
    }

    private Map<Integer, Integer> normalizeDifficultyRatio(Map<String, Integer> raw) throws BusinessException {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<Integer, Integer> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : raw.entrySet()) {
            validateRatioEntry(entry);
            normalized.put(parseDifficulty(entry.getKey().trim()), entry.getValue());
        }
        return normalized;
    }

    private Map<Long, Integer> normalizeKnowledgeRatio(Map<String, Integer> raw) throws BusinessException {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : raw.entrySet()) {
            validateRatioEntry(entry);
            normalized.put(parseKnowledgeKey(entry.getKey().trim()), entry.getValue());
        }
        return normalized;
    }

    private void validateRatioEntry(Map.Entry<String, Integer> entry) throws BusinessException {
        if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() <= 0) {
            throw new BusinessException(DomainErrorCode.PAPER_RATIO_INVALID);
        }
    }

    private int parseDifficulty(String raw) throws BusinessException {
        try {
            int difficulty = Integer.parseInt(raw);
            if (difficulty < 1 || difficulty > 5) {
                throw new BusinessException(DomainErrorCode.PAPER_RATIO_INVALID);
            }
            return difficulty;
        } catch (NumberFormatException exception) {
            throw new BusinessException(DomainErrorCode.PAPER_RATIO_INVALID);
        }
    }

    private long parseKnowledgeKey(String raw) throws BusinessException {
        try {
            long categoryId = Long.parseLong(raw);
            if (categoryId <= 0) {
                throw new BusinessException(DomainErrorCode.PAPER_RATIO_INVALID);
            }
            return categoryId;
        } catch (NumberFormatException exception) {
            throw new BusinessException(DomainErrorCode.PAPER_RATIO_INVALID);
        }
    }

    private <K> Map<K, Integer> allocateTargets(Map<K, Integer> ratios, int total) throws BusinessException {
        if (ratios == null || ratios.isEmpty()) {
            return Map.of();
        }
        int sum = ratios.values().stream().mapToInt(Integer::intValue).sum();
        if (sum <= 0) {
            throw new BusinessException(DomainErrorCode.PAPER_RATIO_INVALID);
        }
        Map<K, Integer> targets = new LinkedHashMap<>();
        List<QuotaAllocation<K>> allocations = new ArrayList<>();
        int assigned = 0;
        for (Map.Entry<K, Integer> entry : ratios.entrySet()) {
            double rawShare = (double) total * entry.getValue() / sum;
            int base = (int) Math.floor(rawShare);
            targets.put(entry.getKey(), base);
            allocations.add(new QuotaAllocation<>(entry.getKey(), rawShare - base));
            assigned += base;
        }
        allocations.sort(Comparator.comparingDouble(QuotaAllocation<K>::fraction).reversed()
                .thenComparing(item -> String.valueOf(item.key())));
        for (int index = 0; index < total - assigned; index++) {
            QuotaAllocation<K> allocation = allocations.get(index % allocations.size());
            targets.put(allocation.key(), targets.get(allocation.key()) + 1);
        }
        return targets;
    }

    private LocalDateTime parseDateTime(String raw) throws BusinessException {
        try {
            return LocalDateTime.parse(raw, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(DomainErrorCode.PAPER_SCHEDULED_TIME_REQUIRED);
        }
    }

    private void changeQuestionReferenceCount(List<Long> questionIds, int delta) {
        questionIds.forEach(questionId -> repository.updateQuestionReferenceCount(questionId, delta));
    }

    private List<Long> currentQuestionIds(Long paperId) {
        return repository.listPaperQuestions(paperId).stream()
                .map(PaperQuestionBindingEntity::getQuestionId)
                .toList();
    }

    private List<Long> currentClassIds(Long paperId) {
        return repository.listPaperPublishClasses(paperId).stream()
                .map(PaperPublishClassEntity::getClassId)
                .toList();
    }

    private PaperView toPaperView(PaperEntity paper, List<Long> questionIds, List<Long> classIds) {
        return PaperView.builder()
                .paperId(paper.getPaperId())
                .paperName(paper.getPaperName())
                .status(paper.getStatus())
                .sourceType(paper.getSourceType())
                .creatorId(paper.getCreatorId())
                .examTime(paper.getExamTime())
                .passScore(paper.getPassScore())
                .totalScore(paper.getTotalScore())
                .questionIds(questionIds)
                .classIds(classIds)
                .scheduledExamTime(paper.getScheduledExamTime())
                .publishedAt(paper.getPublishedAt())
                .recycledAt(paper.getRecycledAt())
                .build();
    }

    private <T> PageResult<T> page(List<T> source, int pageNum, int pageSize) {
        int fromIndex = Math.min((pageNum - 1) * pageSize, source.size());
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        return new PageResult<>(source.subList(fromIndex, toIndex), source.size(), pageNum, pageSize);
    }

    private ApiResponse<PaperView> ok(PaperView data, RequestContext context) {
        return ApiResponse.ok(data).withRequestId(context.requestId());
    }

    private record QuotaAllocation<K>(K key, double fraction) {
    }
}
