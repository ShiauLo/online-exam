package com.maghert.examscore.service.impl;

import cn.hutool.core.lang.Snowflake;
import com.maghert.examcommon.auth.RoleMappings;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;
import com.maghert.examcommon.resource.LocalResourceFileStore;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examscore.context.RequestContext;
import com.maghert.examscore.entity.ClassSnapshotEntity;
import com.maghert.examscore.entity.ExamSnapshotEntity;
import com.maghert.examscore.entity.PaperSnapshotEntity;
import com.maghert.examscore.entity.ScoreAppealEntity;
import com.maghert.examscore.entity.ScoreChangeLogEntity;
import com.maghert.examscore.entity.ScoreDetailEntity;
import com.maghert.examscore.entity.ScoreRecordEntity;
import com.maghert.examscore.model.dto.ScoreApplyRecheckRequest;
import com.maghert.examscore.model.dto.ScoreAutoScoreRequest;
import com.maghert.examscore.model.dto.ScoreAnalyzeRequest;
import com.maghert.examscore.model.dto.ScoreDetailRequest;
import com.maghert.examscore.model.dto.ScoreHandleAppealRequest;
import com.maghert.examscore.model.dto.ScoreManualScoreRequest;
import com.maghert.examscore.model.dto.ScorePublishRequest;
import com.maghert.examscore.model.dto.ScoreQueryRequest;
import com.maghert.examscore.model.dto.ScoreUpdateRequest;
import com.maghert.examscore.model.vo.ScoreAnalyzeView;
import com.maghert.examscore.model.vo.ScoreApplyRecheckView;
import com.maghert.examscore.model.vo.ScoreAutoScoreView;
import com.maghert.examscore.model.vo.ScoreDetailView;
import com.maghert.examscore.model.vo.ScoreExportView;
import com.maghert.examscore.model.vo.ScoreHandleAppealView;
import com.maghert.examscore.model.vo.ScoreManualScoreView;
import com.maghert.examscore.model.vo.ScorePublishView;
import com.maghert.examscore.model.vo.ScoreQueryView;
import com.maghert.examscore.model.vo.ScoreQuestionDetailView;
import com.maghert.examscore.model.vo.ScoreUpdateView;
import com.maghert.examscore.repository.ScoreDomainRepository;
import com.maghert.examscore.service.ExamScoreService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
public class ExamScoreServiceImpl implements ExamScoreService {

    private static final Integer SUPER_ADMIN_ROLE_ID = 1;
    private static final String DIMENSION_OVERVIEW = "overview";
    private static final String DIMENSION_STATUS = "status";
    private static final String DIMENSION_SCORE_RANGE = "scoreRange";
    private static final String DIMENSION_CLASS = "class";
    private static final List<String> ANALYZE_DIMENSION_ORDER = List.of(
            DIMENSION_OVERVIEW,
            DIMENSION_STATUS,
            DIMENSION_SCORE_RANGE,
            DIMENSION_CLASS);
    private static final List<String> STATUS_ORDER = List.of("PENDING", "SCORING", "SCORED", "PUBLISHED");
    private static final List<String> SCORE_RANGE_LABELS = List.of("0-59", "60-69", "70-79", "80-89", "90-100");
    private static final String APPEAL_STATUS_PENDING = "PENDING";
    private static final String APPEAL_STATUS_APPROVED = "APPROVED";
    private static final String APPEAL_STATUS_REJECTED = "REJECTED";

    private final ScoreDomainRepository repository;
    private final Snowflake snowflake;
    private final LocalResourceFileStore resourceFileStore;

    public ExamScoreServiceImpl(ScoreDomainRepository repository,
                                Snowflake snowflake,
                                @Value("${exam.resource.local-storage-root:./tmp_resource}") String localStorageRoot) {
        this.repository = repository;
        this.snowflake = snowflake;
        this.resourceFileStore = new LocalResourceFileStore(localStorageRoot);
    }

    @Override
    public ApiResponse<ScoreAnalyzeView> analyze(ScoreAnalyzeRequest request, RequestContext context)
            throws BusinessException {
        ensureAnalyzeOperator(context);
        ExamSnapshotEntity exam = repository.findExamById(request.getExamId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.EXAM_NOT_FOUND));
        PaperSnapshotEntity paper = repository.findPaperById(exam.getPaperId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PAPER_NOT_FOUND));
        Set<String> dimensions = normalizeAnalyzeDimensions(request.getDimensions());

        List<ScoreRecordEntity> examScores = repository.listScoresByExamId(request.getExamId());
        Set<Long> classIds = examScores.stream()
                .map(ScoreRecordEntity::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (request.getClassId() != null) {
            classIds.add(request.getClassId());
        }
        Map<Long, ClassSnapshotEntity> classById = repository.findClassesByIds(classIds).stream()
                .collect(Collectors.toMap(ClassSnapshotEntity::getClassId, Function.identity()));
        Set<Long> visibleClassIds = resolveVisibleClassIds(context, classById);

        ClassSnapshotEntity requestedClass = null;
        if (request.getClassId() != null) {
            requestedClass = classById.get(request.getClassId());
            if (requestedClass == null) {
                throw new BusinessException(DomainErrorCode.CLASS_NOT_FOUND);
            }
            if (RoleMappings.isTeacher(context.roleId())
                    && !Objects.equals(requestedClass.getCreatedBy(), context.userId())) {
                throw new BusinessException(DomainErrorCode.SCORE_ANALYZE_FORBIDDEN);
            }
        }

        List<ScoreRecordEntity> visibleScores = examScores.stream()
                .filter(score -> request.getClassId() == null || Objects.equals(score.getClassId(), request.getClassId()))
                .filter(score -> canAnalyzeScore(score, context, visibleClassIds))
                .toList();
        if (RoleMappings.isTeacher(context.roleId())
                && request.getClassId() == null
                && visibleScores.isEmpty()
                && !Objects.equals(exam.getCreatorId(), context.userId())) {
            throw new BusinessException(DomainErrorCode.SCORE_ANALYZE_FORBIDDEN);
        }

        List<ScoreRecordEntity> finishedScores = visibleScores.stream()
                .filter(this::isFinishedScore)
                .toList();
        ScoreAnalyzeView response = ScoreAnalyzeView.builder()
                .examId(exam.getExamId())
                .examName(exam.getExamName())
                .classId(request.getClassId())
                .passScore(safeScore(paper.getPassScore()))
                .totalScore(safeScore(paper.getTotalScore()))
                .totalParticipants(visibleScores.size())
                .finishedParticipants(finishedScores.size())
                .generatedAt(LocalDateTime.now())
                .overview(dimensions.contains(DIMENSION_OVERVIEW)
                        ? buildOverview(finishedScores, safeScore(paper.getPassScore()))
                        : null)
                .statusDistribution(dimensions.contains(DIMENSION_STATUS)
                        ? buildStatusDistribution(visibleScores)
                        : null)
                .scoreRangeDistribution(dimensions.contains(DIMENSION_SCORE_RANGE)
                        ? buildScoreRangeDistribution(finishedScores, safeScore(paper.getTotalScore()))
                        : null)
                .classDistribution(dimensions.contains(DIMENSION_CLASS)
                        ? buildClassDistribution(visibleScores, classById, request.getClassId(),
                        requestedClass, safeScore(paper.getPassScore()))
                        : null)
                .build();
        return ApiResponse.ok(response).withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<ScoreApplyRecheckView> applyRecheck(ScoreApplyRecheckRequest request, RequestContext context)
            throws BusinessException {
        ensureApplyRecheckOperator(context);
        if (!Objects.equals(request.getStudentId(), context.userId())) {
            throw new BusinessException(DomainErrorCode.SCORE_RECHECK_FORBIDDEN);
        }
        ScoreRecordEntity score = repository.findScoreByExamIdAndStudentId(request.getExamId(), request.getStudentId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.SCORE_NOT_FOUND));
        if (!"PUBLISHED".equalsIgnoreCase(score.getStatus())
                || score.getPublishedAt() == null
                || score.getPublishedAt().isBefore(LocalDateTime.now().minusDays(7))) {
            throw new BusinessException(DomainErrorCode.SCORE_RECHECK_STATUS_CONFLICT);
        }

        List<ScoreDetailEntity> details = repository.listScoreDetails(score.getScoreId());
        ScoreDetailEntity targetDetail = details.stream()
                .filter(item -> Objects.equals(item.getQuestionId(), request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(DomainErrorCode.SCORE_DETAIL_NOT_FOUND));
        if (!isSubjectiveQuestion(targetDetail.getQuestionType())) {
            throw new BusinessException(DomainErrorCode.SCORE_RECHECK_STATUS_CONFLICT);
        }

        boolean hasPendingAppeal = repository.listAppealsByScoreIds(List.of(score.getScoreId())).stream()
                .anyMatch(item -> Objects.equals(item.getQuestionId(), request.getQuestionId())
                        && APPEAL_STATUS_PENDING.equalsIgnoreCase(item.getStatus()));
        if (hasPendingAppeal) {
            throw new BusinessException(DomainErrorCode.SCORE_RECHECK_STATUS_CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now();
        ScoreAppealEntity appeal = new ScoreAppealEntity()
                .setAppealId(snowflake.nextId())
                .setScoreId(score.getScoreId())
                .setExamId(score.getExamId())
                .setClassId(score.getClassId())
                .setStudentId(score.getStudentId())
                .setQuestionId(request.getQuestionId())
                .setAppealReason(request.getReason().trim())
                .setStatus(APPEAL_STATUS_PENDING)
                .setRequestId(context.requestId())
                .setCreateTime(now)
                .setUpdateTime(now);
        repository.saveAppeal(appeal);

        return ApiResponse.ok(ScoreApplyRecheckView.builder()
                        .appealId(appeal.getAppealId())
                        .scoreId(score.getScoreId())
                        .questionId(appeal.getQuestionId())
                        .status(appeal.getStatus())
                        .recheckStatus(toRecheckStatus(appeal))
                        .createdAt(appeal.getCreateTime())
                        .build())
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<ScoreAutoScoreView> autoScore(ScoreAutoScoreRequest request, RequestContext context)
            throws BusinessException {
        ensureAutoScoreOperator(context);
        if (RoleMappings.isStudent(context.roleId()) && !Objects.equals(context.userId(), request.getStudentId())) {
            throw new BusinessException(DomainErrorCode.SCORE_AUTO_FORBIDDEN);
        }
        ScoreRecordEntity score = repository.findScoreByExamIdAndStudentId(request.getExamId(), request.getStudentId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.SCORE_NOT_FOUND));
        ClassSnapshotEntity clazz = repository.findClassesByIds(List.of(score.getClassId())).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(DomainErrorCode.CLASS_NOT_FOUND));
        if (RoleMappings.isTeacher(context.roleId()) && !Objects.equals(clazz.getCreatedBy(), context.userId())) {
            throw new BusinessException(DomainErrorCode.SCORE_AUTO_FORBIDDEN);
        }
        if ("PUBLISHED".equalsIgnoreCase(score.getStatus())) {
            throw new BusinessException(DomainErrorCode.SCORE_AUTO_STATUS_CONFLICT);
        }

        List<ScoreDetailEntity> details = repository.listScoreDetails(score.getScoreId());
        LocalDateTime now = LocalDateTime.now();
        int scoredQuestionCount = 0;
        for (ScoreDetailEntity detail : details) {
            if (!isObjectiveQuestion(detail.getQuestionType())) {
                continue;
            }
            boolean correct = isObjectiveAnswerCorrect(detail);
            detail.setCorrect(correct)
                    .setScore(correct ? safeScore(detail.getAssignedScore()) : 0)
                    .setUpdateTime(now);
            repository.updateScoreDetail(detail);
            scoredQuestionCount++;
        }

        int objectiveScore = details.stream()
                .filter(item -> isObjectiveQuestion(item.getQuestionType()))
                .mapToInt(item -> safeScore(item.getScore()))
                .sum();
        int subjectiveScore = details.stream()
                .filter(item -> isSubjectiveQuestion(item.getQuestionType()))
                .mapToInt(item -> safeScore(item.getScore()))
                .sum();
        boolean allSubjectiveReviewed = details.stream()
                .filter(item -> isSubjectiveQuestion(item.getQuestionType()))
                .allMatch(item -> item.getReviewComment() != null);
        score.setObjectiveScore(objectiveScore)
                .setSubjectiveScore(subjectiveScore)
                .setTotalScore(objectiveScore + subjectiveScore)
                .setStatus(allSubjectiveReviewed ? "SCORED" : "SCORING")
                .setRequestId(context.requestId())
                .setUpdateTime(now);
        repository.updateScore(score);

        return ApiResponse.ok(ScoreAutoScoreView.builder()
                        .examId(score.getExamId())
                        .studentId(score.getStudentId())
                        .objectiveScore(score.getObjectiveScore())
                        .totalScore(score.getTotalScore())
                        .status(score.getStatus())
                        .scoredQuestionCount(scoredQuestionCount)
                .build())
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<PageResult<ScoreQueryView>> query(ScoreQueryRequest request, RequestContext context)
            throws BusinessException {
        ensureQueryOperator(context);
        Long targetStudentId = resolveQueryStudentId(request, context);
        List<ScoreRecordEntity> scores = repository.listScores();
        if (scores.isEmpty()) {
            return ApiResponse.ok(page(List.<ScoreQueryView>of(),
                            Math.toIntExact(request.getPageNum()),
                            Math.toIntExact(request.getPageSize())))
                    .withRequestId(context.requestId());
        }

        Set<Long> relatedClassIds = scores.stream()
                .map(ScoreRecordEntity::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ClassSnapshotEntity> classById = repository.findClassesByIds(relatedClassIds).stream()
                .collect(Collectors.toMap(ClassSnapshotEntity::getClassId, Function.identity()));
        Set<Long> visibleClassIds = resolveVisibleClassIds(context, classById);
        Map<Long, ScoreAppealEntity> latestAppealByScoreId = buildLatestAppealByScoreId(scores.stream()
                .map(ScoreRecordEntity::getScoreId)
                .toList());

        List<ScoreQueryView> records = scores.stream()
                .filter(score -> request.getExamId() == null || Objects.equals(score.getExamId(), request.getExamId()))
                .filter(score -> request.getClassId() == null || Objects.equals(score.getClassId(), request.getClassId()))
                .filter(score -> targetStudentId == null || Objects.equals(score.getStudentId(), targetStudentId))
                .filter(score -> !StringUtils.hasText(request.getStatus())
                        || request.getStatus().equalsIgnoreCase(score.getStatus()))
                .filter(score -> !StringUtils.hasText(request.getKeyword()) || matchesKeyword(score, request.getKeyword()))
                .filter(score -> canViewScore(score, context, visibleClassIds))
                .sorted(Comparator.comparing(ScoreRecordEntity::getPublishedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ScoreRecordEntity::getSubmittedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ScoreRecordEntity::getScoreId, Comparator.reverseOrder()))
                .map(score -> toQueryView(score, latestAppealByScoreId.get(score.getScoreId())))
                .toList();
        return ApiResponse.ok(page(records, Math.toIntExact(request.getPageNum()), Math.toIntExact(request.getPageSize())))
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<ScoreDetailView> detail(ScoreDetailRequest request, RequestContext context) throws BusinessException {
        ensureQueryOperator(context);
        if (RoleMappings.isStudent(context.roleId()) && !Objects.equals(context.userId(), request.getStudentId())) {
            throw new BusinessException(DomainErrorCode.SCORE_DETAIL_FORBIDDEN);
        }
        ScoreRecordEntity score = repository.findScoreByExamIdAndStudentId(request.getExamId(), request.getStudentId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.SCORE_NOT_FOUND));
        Map<Long, ClassSnapshotEntity> classById = repository.findClassesByIds(List.of(score.getClassId())).stream()
                .collect(Collectors.toMap(ClassSnapshotEntity::getClassId, Function.identity()));
        if (!canViewScore(score, context, resolveVisibleClassIds(context, classById))) {
            throw new BusinessException(DomainErrorCode.SCORE_DETAIL_FORBIDDEN);
        }

        List<ScoreQuestionDetailView> questionDetails = repository.listScoreDetails(score.getScoreId()).stream()
                .sorted(Comparator.comparing(ScoreDetailEntity::getSortNo, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ScoreDetailEntity::getDetailId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toQuestionDetailView)
                .toList();
        return ApiResponse.ok(ScoreDetailView.builder()
                        .scoreId(score.getScoreId())
                        .examId(score.getExamId())
                        .examName(score.getExamName())
                        .paperId(score.getPaperId())
                        .paperName(score.getPaperName())
                        .classId(score.getClassId())
                        .className(score.getClassName())
                        .studentId(score.getStudentId())
                        .studentName(score.getStudentName())
                        .totalScore(score.getTotalScore())
                        .objectiveScore(score.getObjectiveScore())
                        .subjectiveScore(score.getSubjectiveScore())
                        .status(score.getStatus())
                        .submittedAt(score.getSubmittedAt())
                        .publishedAt(score.getPublishedAt())
                        .questionDetails(questionDetails)
                .build())
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<ScoreManualScoreView> manualScore(ScoreManualScoreRequest request, RequestContext context)
            throws BusinessException {
        ensureManualScoreOperator(context);
        ScoreRecordEntity score = repository.findScoreByExamIdAndStudentId(request.getExamId(), request.getStudentId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.SCORE_NOT_FOUND));
        ClassSnapshotEntity clazz = repository.findClassesByIds(List.of(score.getClassId())).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(DomainErrorCode.CLASS_NOT_FOUND));
        if (RoleMappings.isTeacher(context.roleId()) && !Objects.equals(clazz.getCreatedBy(), context.userId())) {
            throw new BusinessException(DomainErrorCode.SCORE_MANUAL_FORBIDDEN);
        }
        if ("PUBLISHED".equalsIgnoreCase(score.getStatus())) {
            throw new BusinessException(DomainErrorCode.SCORE_MANUAL_STATUS_CONFLICT);
        }

        List<ScoreDetailEntity> details = repository.listScoreDetails(score.getScoreId());
        ScoreDetailEntity targetDetail = details.stream()
                .filter(item -> Objects.equals(item.getQuestionId(), request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(DomainErrorCode.SCORE_DETAIL_NOT_FOUND));
        if (!"subjective".equalsIgnoreCase(targetDetail.getQuestionType())) {
            throw new BusinessException(DomainErrorCode.SCORE_MANUAL_QUESTION_TYPE_CONFLICT);
        }
        validateManualScore(request.getScore(), targetDetail.getAssignedScore());

        LocalDateTime now = LocalDateTime.now();
        String normalizedComment = normalizeManualComment(request.getComment());
        targetDetail.setScore(request.getScore())
                .setCorrect(Objects.equals(request.getScore(), targetDetail.getAssignedScore()))
                .setReviewComment(normalizedComment)
                .setUpdateTime(now);
        repository.updateScoreDetail(targetDetail);

        int subjectiveScore = details.stream()
                .filter(item -> "subjective".equalsIgnoreCase(item.getQuestionType()))
                .mapToInt(item -> Objects.equals(item.getQuestionId(), targetDetail.getQuestionId())
                        ? request.getScore()
                        : safeScore(item.getScore()))
                .sum();
        boolean allSubjectiveReviewed = details.stream()
                .filter(item -> "subjective".equalsIgnoreCase(item.getQuestionType()))
                .allMatch(item -> Objects.equals(item.getQuestionId(), targetDetail.getQuestionId())
                        ? normalizedComment != null
                        : item.getReviewComment() != null);
        score.setSubjectiveScore(subjectiveScore)
                .setTotalScore(safeScore(score.getObjectiveScore()) + subjectiveScore)
                .setStatus(allSubjectiveReviewed ? "SCORED" : "SCORING")
                .setRequestId(context.requestId())
                .setUpdateTime(now);
        repository.updateScore(score);

        return ApiResponse.ok(ScoreManualScoreView.builder()
                        .examId(score.getExamId())
                        .studentId(score.getStudentId())
                        .questionId(targetDetail.getQuestionId())
                        .score(request.getScore())
                        .assignedScore(targetDetail.getAssignedScore())
                        .reviewComment(normalizedComment)
                        .subjectiveScore(score.getSubjectiveScore())
                        .totalScore(score.getTotalScore())
                        .status(score.getStatus())
                .build())
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<ScorePublishView> publish(ScorePublishRequest request, RequestContext context) throws BusinessException {
        ensurePublishOperator(context);
        if (request.getExamId() == null || request.getClassId() == null) {
            throw new BusinessException(DomainErrorCode.SCORE_PUBLISH_TARGET_REQUIRED);
        }
        ClassSnapshotEntity clazz = repository.findClassesByIds(List.of(request.getClassId())).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(DomainErrorCode.CLASS_NOT_FOUND));
        if (RoleMappings.isTeacher(context.roleId()) && !Objects.equals(clazz.getCreatedBy(), context.userId())) {
            throw new BusinessException(DomainErrorCode.SCORE_PUBLISH_FORBIDDEN);
        }

        List<ScoreRecordEntity> scores = repository.listScoresByExamIdAndClassId(request.getExamId(), request.getClassId());
        if (scores.isEmpty()) {
            throw new BusinessException(DomainErrorCode.SCORE_NOT_FOUND);
        }
        boolean hasUnscored = scores.stream()
                .anyMatch(item -> !"SCORED".equalsIgnoreCase(item.getStatus())
                        && !"PUBLISHED".equalsIgnoreCase(item.getStatus()));
        if (hasUnscored) {
            throw new BusinessException(DomainErrorCode.SCORE_PUBLISH_STATUS_CONFLICT);
        }

        LocalDateTime publishedAt = LocalDateTime.now();
        int publishedCount = 0;
        for (ScoreRecordEntity score : scores) {
            if ("SCORED".equalsIgnoreCase(score.getStatus())) {
                score.setStatus("PUBLISHED")
                        .setPublishedAt(publishedAt)
                        .setRequestId(context.requestId())
                        .setUpdateTime(publishedAt);
                repository.updateScore(score);
                publishedCount++;
            }
        }
        LocalDateTime effectivePublishedAt = scores.stream()
                .map(ScoreRecordEntity::getPublishedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(publishedAt);
        return ApiResponse.ok(ScorePublishView.builder()
                        .examId(request.getExamId())
                        .classId(request.getClassId())
                        .publishedCount(publishedCount)
                        .status("PUBLISHED")
                        .publishedAt(effectivePublishedAt)
                .build())
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<ScoreHandleAppealView> handleAppeal(ScoreHandleAppealRequest request, RequestContext context)
            throws BusinessException {
        ensureHandleAppealOperator(context);
        String handleResult = normalizeAppealHandleResult(request.getHandleResult());
        ScoreAppealEntity appeal = repository.findAppealById(request.getAppealId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.SCORE_APPEAL_NOT_FOUND));
        if (!APPEAL_STATUS_PENDING.equalsIgnoreCase(appeal.getStatus())) {
            throw new BusinessException(DomainErrorCode.SCORE_APPEAL_HANDLE_STATUS_CONFLICT);
        }

        ScoreRecordEntity score = repository.findScoreById(appeal.getScoreId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.SCORE_NOT_FOUND));
        ClassSnapshotEntity clazz = repository.findClassesByIds(List.of(score.getClassId())).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(DomainErrorCode.CLASS_NOT_FOUND));
        if (RoleMappings.isTeacher(context.roleId()) && !Objects.equals(clazz.getCreatedBy(), context.userId())) {
            throw new BusinessException(DomainErrorCode.SCORE_APPEAL_HANDLE_FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now();
        appeal.setStatus(handleResult)
                .setHandlerId(context.userId())
                .setHandleResult(handleResult)
                .setHandleReason(request.getReason().trim())
                .setHandledAt(now)
                .setRequestId(context.requestId())
                .setUpdateTime(now);
        repository.updateAppeal(appeal);

        if (APPEAL_STATUS_APPROVED.equals(handleResult) && "PUBLISHED".equalsIgnoreCase(score.getStatus())) {
            score.setStatus("SCORING")
                    .setPublishedAt(null)
                    .setRequestId(context.requestId())
                    .setUpdateTime(now);
            repository.updateScore(score);
        }

        return ApiResponse.ok(ScoreHandleAppealView.builder()
                        .appealId(appeal.getAppealId())
                        .scoreId(appeal.getScoreId())
                        .questionId(appeal.getQuestionId())
                        .status(appeal.getStatus())
                        .recheckStatus(toRecheckStatus(appeal))
                        .handleResult(handleResult)
                        .scoreStatus(score.getStatus())
                        .handledAt(appeal.getHandledAt())
                        .build())
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<ScoreExportView> export(Long examId, Long classId, Boolean includeAnalysis, RequestContext context)
            throws BusinessException {
        ensureExportOperator(context);
        if (examId != null) {
            repository.findExamById(examId)
                    .orElseThrow(() -> new BusinessException(DomainErrorCode.EXAM_NOT_FOUND));
        }
        ClassSnapshotEntity requestedClass = null;
        if (classId != null) {
            requestedClass = repository.findClassesByIds(List.of(classId)).stream()
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(DomainErrorCode.CLASS_NOT_FOUND));
            if (RoleMappings.isTeacher(context.roleId()) && !Objects.equals(requestedClass.getCreatedBy(), context.userId())) {
                throw new BusinessException(DomainErrorCode.SCORE_EXPORT_FORBIDDEN);
            }
        }

        List<ScoreRecordEntity> scores = repository.listScores();
        Set<Long> relatedClassIds = scores.stream()
                .map(ScoreRecordEntity::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (classId != null) {
            relatedClassIds.add(classId);
        }
        Map<Long, ClassSnapshotEntity> classById = repository.findClassesByIds(relatedClassIds).stream()
                .collect(Collectors.toMap(ClassSnapshotEntity::getClassId, Function.identity()));
        if (requestedClass != null) {
            classById.put(requestedClass.getClassId(), requestedClass);
        }
        Set<Long> visibleClassIds = resolveVisibleClassIds(context, classById);

        List<ScoreRecordEntity> filteredScores = scores.stream()
                .filter(score -> examId == null || Objects.equals(score.getExamId(), examId))
                .filter(score -> classId == null || Objects.equals(score.getClassId(), classId))
                .filter(score -> canExportScore(score, context, visibleClassIds))
                .sorted(Comparator.comparing(ScoreRecordEntity::getExamId, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ScoreRecordEntity::getClassId, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ScoreRecordEntity::getStudentId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        LocalDateTime now = LocalDateTime.now();
        String fileKey = "score-export-" + snowflake.nextIdStr();
        String fileName = buildExportFileName(examId, classId, now);
        resourceFileStore.writeCsv(fileKey, buildCsvContent(filteredScores, Boolean.TRUE.equals(includeAnalysis)));
        return ApiResponse.ok(ScoreExportView.builder()
                        .fileKey(fileKey)
                        .fileName(fileName)
                        .recordCount(filteredScores.size())
                        .includeAnalysis(Boolean.TRUE.equals(includeAnalysis))
                        .generatedAt(now)
                        .build())
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<ScoreUpdateView> update(ScoreUpdateRequest request, RequestContext context) throws BusinessException {
        ensureUpdateOperator(context);
        if (request.getApproverId() == null || Objects.equals(request.getApproverId(), context.userId())) {
            throw new BusinessException(DomainErrorCode.SCORE_UPDATE_APPROVER_INVALID);
        }
        ScoreRecordEntity score = repository.findScoreById(request.getScoreId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.SCORE_NOT_FOUND));
        if (!"SCORED".equalsIgnoreCase(score.getStatus()) && !"PUBLISHED".equalsIgnoreCase(score.getStatus())) {
            throw new BusinessException(DomainErrorCode.SCORE_UPDATE_STATUS_CONFLICT);
        }

        ExamSnapshotEntity exam = repository.findExamById(score.getExamId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.EXAM_NOT_FOUND));
        PaperSnapshotEntity paper = repository.findPaperById(exam.getPaperId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PAPER_NOT_FOUND));
        if (request.getNewScore() == null
                || request.getNewScore() < safeScore(score.getObjectiveScore())
                || request.getNewScore() > safeScore(paper.getTotalScore())) {
            throw new BusinessException(DomainErrorCode.SCORE_UPDATE_SCORE_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        int previousTotalScore = safeScore(score.getTotalScore());
        int newSubjectiveScore = request.getNewScore() - safeScore(score.getObjectiveScore());
        score.setTotalScore(request.getNewScore())
                .setSubjectiveScore(newSubjectiveScore)
                .setRequestId(context.requestId())
                .setUpdateTime(now);
        repository.updateScore(score);

        repository.saveScoreChangeLog(new ScoreChangeLogEntity()
                .setChangeLogId(snowflake.nextId())
                .setScoreId(score.getScoreId())
                .setExamId(score.getExamId())
                .setClassId(score.getClassId())
                .setStudentId(score.getStudentId())
                .setOperatorId(context.userId())
                .setApproverId(request.getApproverId())
                .setPreviousTotalScore(previousTotalScore)
                .setNewTotalScore(request.getNewScore())
                .setReason(request.getReason().trim())
                .setRequestId(context.requestId())
                .setCreateTime(now)
                .setUpdateTime(now));

        return ApiResponse.ok(ScoreUpdateView.builder()
                        .scoreId(score.getScoreId())
                        .previousTotalScore(previousTotalScore)
                        .totalScore(score.getTotalScore())
                        .objectiveScore(score.getObjectiveScore())
                        .subjectiveScore(score.getSubjectiveScore())
                        .approverId(request.getApproverId())
                        .updatedAt(now)
                        .build())
                .withRequestId(context.requestId());
    }

    private void ensureQueryOperator(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!RoleMappings.isTeacher(context.roleId())
                && !RoleMappings.isAdmin(context.roleId())
                && !RoleMappings.isAuditor(context.roleId())
                && !RoleMappings.isStudent(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SCORE_QUERY_FORBIDDEN);
        }
    }

    private void ensureAnalyzeOperator(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!RoleMappings.isTeacher(context.roleId())
                && !RoleMappings.isAdmin(context.roleId())
                && !RoleMappings.isAuditor(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SCORE_ANALYZE_FORBIDDEN);
        }
    }

    private void ensureApplyRecheckOperator(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!RoleMappings.isStudent(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SCORE_RECHECK_FORBIDDEN);
        }
    }

    private void ensureAutoScoreOperator(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!RoleMappings.isTeacher(context.roleId())
                && !RoleMappings.isAdmin(context.roleId())
                && !RoleMappings.isStudent(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SCORE_AUTO_FORBIDDEN);
        }
    }

    private void ensureManualScoreOperator(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!RoleMappings.isTeacher(context.roleId()) && !RoleMappings.isAdmin(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SCORE_MANUAL_FORBIDDEN);
        }
    }

    private void ensurePublishOperator(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!RoleMappings.isTeacher(context.roleId()) && !RoleMappings.isAdmin(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SCORE_PUBLISH_FORBIDDEN);
        }
    }

    private void ensureHandleAppealOperator(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!RoleMappings.isTeacher(context.roleId()) && !RoleMappings.isAdmin(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SCORE_APPEAL_HANDLE_FORBIDDEN);
        }
    }

    private void ensureExportOperator(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!RoleMappings.isTeacher(context.roleId())
                && !RoleMappings.isAdmin(context.roleId())
                && !RoleMappings.isAuditor(context.roleId())) {
            throw new BusinessException(DomainErrorCode.SCORE_EXPORT_FORBIDDEN);
        }
    }

    private void ensureUpdateOperator(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!Objects.equals(SUPER_ADMIN_ROLE_ID, context.roleId())) {
            throw new BusinessException(DomainErrorCode.SCORE_UPDATE_FORBIDDEN);
        }
    }

    private void validateManualScore(Integer score, Integer assignedScore) throws BusinessException {
        if (score == null || assignedScore == null || score < 0 || score > assignedScore) {
            throw new BusinessException(DomainErrorCode.SCORE_MANUAL_SCORE_INVALID);
        }
    }

    private String normalizeManualComment(String comment) {
        if (comment == null) {
            return "";
        }
        return comment.trim();
    }

    private int safeScore(Integer score) {
        return score == null ? 0 : score;
    }

    private Set<String> normalizeAnalyzeDimensions(List<String> dimensions) throws BusinessException {
        if (dimensions == null || dimensions.isEmpty()) {
            return new LinkedHashSet<>(ANALYZE_DIMENSION_ORDER);
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String dimension : dimensions) {
            if (!StringUtils.hasText(dimension)) {
                throw new BusinessException(DomainErrorCode.SCORE_ANALYZE_DIMENSION_INVALID);
            }
            String value = switch (dimension.trim().toLowerCase(Locale.ROOT)) {
                case "overview" -> DIMENSION_OVERVIEW;
                case "status" -> DIMENSION_STATUS;
                case "scorerange" -> DIMENSION_SCORE_RANGE;
                case "class" -> DIMENSION_CLASS;
                default -> throw new BusinessException(DomainErrorCode.SCORE_ANALYZE_DIMENSION_INVALID);
            };
            normalized.add(value);
        }
        return normalized;
    }

    private boolean isObjectiveQuestion(String questionType) {
        return "single".equalsIgnoreCase(questionType) || "multi".equalsIgnoreCase(questionType);
    }

    private boolean isSubjectiveQuestion(String questionType) {
        return "subjective".equalsIgnoreCase(questionType);
    }

    private boolean isObjectiveAnswerCorrect(ScoreDetailEntity detail) {
        if ("multi".equalsIgnoreCase(detail.getQuestionType())) {
            return Objects.equals(normalizeMultiChoiceAnswer(detail.getStudentAnswer()),
                    normalizeMultiChoiceAnswer(detail.getCorrectAnswer()));
        }
        return Objects.equals(normalizePlainAnswer(detail.getStudentAnswer()),
                normalizePlainAnswer(detail.getCorrectAnswer()));
    }

    private String normalizePlainAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        return answer.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeMultiChoiceAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        return Arrays.stream(answer.trim().toUpperCase(Locale.ROOT).split("[,，;；、\\s]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }

    private Long resolveQueryStudentId(ScoreQueryRequest request, RequestContext context) throws BusinessException {
        if (RoleMappings.isStudent(context.roleId())) {
            if (request.getStudentId() != null && !Objects.equals(request.getStudentId(), context.userId())) {
                throw new BusinessException(DomainErrorCode.SCORE_QUERY_FORBIDDEN);
            }
            return context.userId();
        }
        return request.getStudentId();
    }

    private String normalizeAppealHandleResult(String handleResult) throws BusinessException {
        if (!StringUtils.hasText(handleResult)) {
            throw new BusinessException(DomainErrorCode.SCORE_APPEAL_RESULT_INVALID);
        }
        String normalized = handleResult.trim().toUpperCase(Locale.ROOT);
        if (!APPEAL_STATUS_APPROVED.equals(normalized) && !APPEAL_STATUS_REJECTED.equals(normalized)) {
            throw new BusinessException(DomainErrorCode.SCORE_APPEAL_RESULT_INVALID);
        }
        return normalized;
    }

    private Set<Long> resolveVisibleClassIds(RequestContext context, Map<Long, ClassSnapshotEntity> classById) {
        if (RoleMappings.isAdmin(context.roleId()) || RoleMappings.isAuditor(context.roleId())) {
            return classById.keySet();
        }
        if (RoleMappings.isTeacher(context.roleId())) {
            return classById.values().stream()
                    .filter(item -> Objects.equals(item.getCreatedBy(), context.userId()))
                    .map(ClassSnapshotEntity::getClassId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Set.of();
    }

    private boolean canAnalyzeScore(ScoreRecordEntity score, RequestContext context, Set<Long> visibleClassIds) {
        if (RoleMappings.isAdmin(context.roleId()) || RoleMappings.isAuditor(context.roleId())) {
            return true;
        }
        return visibleClassIds.contains(score.getClassId());
    }

    private boolean canViewScore(ScoreRecordEntity score, RequestContext context, Set<Long> visibleClassIds) {
        if (RoleMappings.isStudent(context.roleId())) {
            return Objects.equals(score.getStudentId(), context.userId());
        }
        if (RoleMappings.isAdmin(context.roleId()) || RoleMappings.isAuditor(context.roleId())) {
            return true;
        }
        return visibleClassIds.contains(score.getClassId());
    }

    private boolean canExportScore(ScoreRecordEntity score, RequestContext context, Set<Long> visibleClassIds) {
        if (RoleMappings.isAdmin(context.roleId()) || RoleMappings.isAuditor(context.roleId())) {
            return true;
        }
        return visibleClassIds.contains(score.getClassId());
    }

    private boolean matchesKeyword(ScoreRecordEntity score, String keyword) {
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(score.getExamName(), normalized)
                || containsIgnoreCase(score.getClassName(), normalized)
                || containsIgnoreCase(score.getStudentName(), normalized)
                || containsIgnoreCase(score.getPaperName(), normalized);
    }

    private boolean containsIgnoreCase(String source, String normalizedKeyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private Map<Long, ScoreAppealEntity> buildLatestAppealByScoreId(Collection<Long> scoreIds) {
        return repository.listAppealsByScoreIds(scoreIds).stream()
                .collect(Collectors.toMap(
                        ScoreAppealEntity::getScoreId,
                        Function.identity(),
                        this::pickLaterAppeal,
                        LinkedHashMap::new));
    }

    private ScoreAppealEntity pickLaterAppeal(ScoreAppealEntity left, ScoreAppealEntity right) {
        Comparator<ScoreAppealEntity> comparator = Comparator
                .comparing(ScoreAppealEntity::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ScoreAppealEntity::getAppealId, Comparator.nullsLast(Comparator.naturalOrder()));
        return comparator.compare(left, right) >= 0 ? left : right;
    }

    private ScoreQueryView toQueryView(ScoreRecordEntity entity, ScoreAppealEntity latestAppeal) {
        return ScoreQueryView.builder()
                .scoreId(entity.getScoreId())
                .examId(entity.getExamId())
                .examName(entity.getExamName())
                .classId(entity.getClassId())
                .className(entity.getClassName())
                .studentId(entity.getStudentId())
                .studentName(entity.getStudentName())
                .totalScore(entity.getTotalScore())
                .objectiveScore(entity.getObjectiveScore())
                .subjectiveScore(entity.getSubjectiveScore())
                .status(entity.getStatus())
                .publishStatus(resolvePublishStatus(entity))
                .appealId(latestAppeal == null ? null : latestAppeal.getAppealId())
                .recheckStatus(toRecheckStatus(latestAppeal))
                .submittedAt(entity.getSubmittedAt())
                .publishedAt(entity.getPublishedAt())
                .build();
    }

    private String resolvePublishStatus(ScoreRecordEntity entity) {
        return "PUBLISHED".equalsIgnoreCase(entity.getStatus()) ? "published" : "unpublished";
    }

    private String toRecheckStatus(ScoreAppealEntity appeal) {
        if (appeal == null) {
            return "none";
        }
        return APPEAL_STATUS_PENDING.equalsIgnoreCase(appeal.getStatus()) ? "pending" : "processed";
    }

    private ScoreQuestionDetailView toQuestionDetailView(ScoreDetailEntity entity) {
        return ScoreQuestionDetailView.builder()
                .questionId(entity.getQuestionId())
                .sortNo(entity.getSortNo())
                .questionType(entity.getQuestionType())
                .questionStem(entity.getQuestionStem())
                .studentAnswer(entity.getStudentAnswer())
                .correctAnswer(entity.getCorrectAnswer())
                .assignedScore(entity.getAssignedScore())
                .score(entity.getScore())
                .correct(entity.getCorrect())
                .reviewComment(entity.getReviewComment())
                .build();
    }

    private boolean isFinishedScore(ScoreRecordEntity score) {
        return "SCORED".equalsIgnoreCase(score.getStatus()) || "PUBLISHED".equalsIgnoreCase(score.getStatus());
    }

    private ScoreAnalyzeView.Overview buildOverview(List<ScoreRecordEntity> finishedScores, int passScore) {
        if (finishedScores.isEmpty()) {
            return ScoreAnalyzeView.Overview.builder()
                    .averageScore(zeroDecimal())
                    .highestScore(0)
                    .lowestScore(0)
                    .passCount(0)
                    .passRate(zeroDecimal())
                    .build();
        }
        int scoreSum = finishedScores.stream()
                .mapToInt(item -> safeScore(item.getTotalScore()))
                .sum();
        int highest = finishedScores.stream()
                .map(ScoreRecordEntity::getTotalScore)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        int lowest = finishedScores.stream()
                .map(ScoreRecordEntity::getTotalScore)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(0);
        int passCount = (int) finishedScores.stream()
                .filter(item -> safeScore(item.getTotalScore()) >= passScore)
                .count();
        return ScoreAnalyzeView.Overview.builder()
                .averageScore(divide(scoreSum, finishedScores.size()))
                .highestScore(highest)
                .lowestScore(lowest)
                .passCount(passCount)
                .passRate(rate(passCount, finishedScores.size()))
                .build();
    }

    private List<ScoreAnalyzeView.StatusDistributionItem> buildStatusDistribution(List<ScoreRecordEntity> visibleScores) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        STATUS_ORDER.forEach(status -> counts.put(status, 0));
        for (ScoreRecordEntity score : visibleScores) {
            String status = score.getStatus() == null ? "" : score.getStatus().toUpperCase(Locale.ROOT);
            if (counts.containsKey(status)) {
                counts.put(status, counts.get(status) + 1);
            }
        }
        return STATUS_ORDER.stream()
                .map(status -> ScoreAnalyzeView.StatusDistributionItem.builder()
                        .status(status)
                        .count(counts.get(status))
                        .build())
                .toList();
    }

    private List<ScoreAnalyzeView.ScoreRangeDistributionItem> buildScoreRangeDistribution(List<ScoreRecordEntity> finishedScores,
                                                                                          int totalScore) {
        int[] counts = new int[SCORE_RANGE_LABELS.size()];
        for (ScoreRecordEntity score : finishedScores) {
            counts[resolveScoreRangeIndex(safeScore(score.getTotalScore()), totalScore)]++;
        }
        List<ScoreAnalyzeView.ScoreRangeDistributionItem> items = new java.util.ArrayList<>();
        for (int index = 0; index < SCORE_RANGE_LABELS.size(); index++) {
            items.add(ScoreAnalyzeView.ScoreRangeDistributionItem.builder()
                    .rangeLabel(SCORE_RANGE_LABELS.get(index))
                    .count(counts[index])
                    .build());
        }
        return items;
    }

    private List<ScoreAnalyzeView.ClassDistributionItem> buildClassDistribution(List<ScoreRecordEntity> visibleScores,
                                                                                Map<Long, ClassSnapshotEntity> classById,
                                                                                Long requestedClassId,
                                                                                ClassSnapshotEntity requestedClass,
                                                                                int passScore) {
        if (requestedClassId != null && visibleScores.isEmpty() && requestedClass != null) {
            return List.of(buildZeroClassDistribution(requestedClass));
        }
        Map<Long, List<ScoreRecordEntity>> grouped = visibleScores.stream()
                .collect(Collectors.groupingBy(ScoreRecordEntity::getClassId, LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> buildClassDistributionItem(entry.getKey(), entry.getValue(), classById, passScore))
                .toList();
    }

    private ScoreAnalyzeView.ClassDistributionItem buildZeroClassDistribution(ClassSnapshotEntity clazz) {
        return ScoreAnalyzeView.ClassDistributionItem.builder()
                .classId(clazz.getClassId())
                .className(clazz.getClassName())
                .studentCount(0)
                .finishedCount(0)
                .averageScore(zeroDecimal())
                .passRate(zeroDecimal())
                .build();
    }

    private ScoreAnalyzeView.ClassDistributionItem buildClassDistributionItem(Long classId,
                                                                              List<ScoreRecordEntity> classScores,
                                                                              Map<Long, ClassSnapshotEntity> classById,
                                                                              int passScore) {
        List<ScoreRecordEntity> finishedScores = classScores.stream()
                .filter(this::isFinishedScore)
                .toList();
        int passCount = (int) finishedScores.stream()
                .filter(item -> safeScore(item.getTotalScore()) >= passScore)
                .count();
        int scoreSum = finishedScores.stream()
                .mapToInt(item -> safeScore(item.getTotalScore()))
                .sum();
        String className = classScores.stream()
                .map(ScoreRecordEntity::getClassName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElseGet(() -> classById.containsKey(classId) ? classById.get(classId).getClassName() : null);
        return ScoreAnalyzeView.ClassDistributionItem.builder()
                .classId(classId)
                .className(className)
                .studentCount(classScores.size())
                .finishedCount(finishedScores.size())
                .averageScore(finishedScores.isEmpty() ? zeroDecimal() : divide(scoreSum, finishedScores.size()))
                .passRate(rate(passCount, finishedScores.size()))
                .build();
    }

    private int resolveScoreRangeIndex(int score, int totalScore) {
        if (totalScore <= 0) {
            return 0;
        }
        BigDecimal percent = BigDecimal.valueOf(score)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalScore), 2, RoundingMode.HALF_UP);
        if (percent.compareTo(BigDecimal.valueOf(60)) < 0) {
            return 0;
        }
        if (percent.compareTo(BigDecimal.valueOf(70)) < 0) {
            return 1;
        }
        if (percent.compareTo(BigDecimal.valueOf(80)) < 0) {
            return 2;
        }
        if (percent.compareTo(BigDecimal.valueOf(90)) < 0) {
            return 3;
        }
        return 4;
    }

    private BigDecimal divide(int numerator, int denominator) {
        if (denominator <= 0) {
            return zeroDecimal();
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return zeroDecimal();
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroDecimal() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildExportFileName(Long examId, Long classId, LocalDateTime generatedAt) {
        List<String> parts = new ArrayList<>();
        parts.add("score-export");
        if (examId != null) {
            parts.add("exam-" + examId);
        }
        if (classId != null) {
            parts.add("class-" + classId);
        }
        parts.add(generatedAt.toLocalDate().toString());
        return String.join("-", parts) + ".csv";
    }

    private String buildCsvContent(List<ScoreRecordEntity> scores, boolean includeAnalysis) {
        StringBuilder builder = new StringBuilder("scoreId,examId,classId,studentId,totalScore,status\n");
        for (ScoreRecordEntity score : scores) {
            builder.append(score.getScoreId()).append(',')
                    .append(score.getExamId()).append(',')
                    .append(score.getClassId()).append(',')
                    .append(score.getStudentId()).append(',')
                    .append(safeScore(score.getTotalScore())).append(',')
                    .append(score.getStatus()).append('\n');
        }
        if (includeAnalysis) {
            builder.append("analysisIncluded,true\n");
        }
        return builder.toString();
    }

    private <T> PageResult<T> page(List<T> source, int pageNum, int pageSize) {
        int fromIndex = Math.min((pageNum - 1) * pageSize, source.size());
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        return new PageResult<>(source.subList(fromIndex, toIndex), source.size(), pageNum, pageSize);
    }
}
