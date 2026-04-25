package com.maghert.examcore.service.impl;

import cn.hutool.core.lang.Snowflake;
import com.maghert.examcommon.auth.RoleMappings;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examcore.entity.ClassMemberSnapshotEntity;
import com.maghert.examcore.context.RequestContext;
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
import com.maghert.examcore.model.dto.ExamApplyRetestRequest;
import com.maghert.examcore.model.dto.ExamApproveRetestRequest;
import com.maghert.examcore.model.dto.ExamCreateRequest;
import com.maghert.examcore.model.dto.ExamDistributeRequest;
import com.maghert.examcore.model.dto.ExamQueryRequest;
import com.maghert.examcore.model.dto.ExamSubmitRequest;
import com.maghert.examcore.model.dto.ExamToggleStatusRequest;
import com.maghert.examcore.model.dto.ExamUpdateParamsRequest;
import com.maghert.examcore.model.enums.ExamLifecycleStatus;
import com.maghert.examcore.model.vo.ExamDistributeView;
import com.maghert.examcore.model.vo.ExamRetestApplyView;
import com.maghert.examcore.model.vo.ExamSubmitView;
import com.maghert.examcore.model.vo.ExamView;
import com.maghert.examcore.repository.ExamDomainRepository;
import com.maghert.examcore.service.ExamCoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Locale;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExamCoreServiceImpl implements ExamCoreService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Snowflake snowflake;
    private final ExamDomainRepository repository;

    public ExamCoreServiceImpl(Snowflake snowflake, ExamDomainRepository repository) {
        this.snowflake = snowflake;
        this.repository = repository;
    }

    @Override
    public ApiResponse<ExamView> create(ExamCreateRequest request, RequestContext context) throws BusinessException {
        ensureTeacherOrAdmin(context);
        List<Long> classIds = normalizeIds(request.getClassIds());
        if (classIds.isEmpty()) {
            throw new BusinessException(DomainErrorCode.EXAM_CLASS_REQUIRED);
        }
        int duration = validateDuration(request.getDuration());
        LocalDateTime startTime = parseDateTime(request.getStartTime());

        PaperSnapshotEntity paper = repository.findPaperById(request.getPaperId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PAPER_NOT_FOUND));
        if (!"PUBLISHED".equalsIgnoreCase(paper.getStatus())) {
            throw new BusinessException(DomainErrorCode.EXAM_PAPER_STATUS_INVALID);
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
                throw new BusinessException(DomainErrorCode.EXAM_CLASS_ACCESS_FORBIDDEN);
            }
        }

        Set<Long> publishedClassIds = repository.listPaperPublishClasses(paper.getPaperId()).stream()
                .map(PaperPublishClassSnapshotEntity::getClassId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!publishedClassIds.containsAll(classIds)) {
            throw new BusinessException(DomainErrorCode.EXAM_PAPER_CLASS_SCOPE_CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now();
        ExamEntity exam = new ExamEntity()
                .setExamId(snowflake.nextId())
                .setExamName(normalizeExamName(request.getExamName()))
                .setPaperId(paper.getPaperId())
                .setPaperName(paper.getPaperName())
                .setStatus(ExamLifecycleStatus.DRAFT.name())
                .setCreatorId(context.userId())
                .setCreatorRoleId(context.roleId())
                .setDuration(duration)
                .setStartTime(startTime)
                .setRequestId(context.requestId())
                .setCreateTime(now)
                .setUpdateTime(now);
        repository.saveExam(exam);
        repository.replaceExamClasses(exam.getExamId(), classIds.stream()
                .map(classId -> new ExamClassRelationEntity()
                        .setRelationId(snowflake.nextId())
                        .setExamId(exam.getExamId())
                        .setClassId(classId)
                        .setCreateTime(now)
                        .setUpdateTime(now))
                .toList());
        return ApiResponse.ok(ExamView.builder()
                        .examId(exam.getExamId())
                        .examName(exam.getExamName())
                        .paperId(exam.getPaperId())
                        .paperName(exam.getPaperName())
                        .status(exam.getStatus())
                        .creatorId(exam.getCreatorId())
                        .duration(exam.getDuration())
                        .startTime(exam.getStartTime())
                        .classIds(classIds)
                .build())
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<PageResult<ExamView>> query(ExamQueryRequest request, RequestContext context)
            throws BusinessException {
        ensureQueryOperator(context);
        Long targetStudentId = resolveQueryStudentId(request, context);
        List<ExamEntity> exams = repository.listExams();
        if (exams.isEmpty()) {
            return ApiResponse.ok(page(List.<ExamView>of(),
                            Math.toIntExact(request.getPageNum()),
                            Math.toIntExact(request.getPageSize())))
                    .withRequestId(context.requestId());
        }

        Map<Long, List<Long>> classIdsByExamId = repository.listExamClasses(exams.stream()
                        .map(ExamEntity::getExamId)
                        .toList())
                .stream()
                .collect(Collectors.groupingBy(ExamClassRelationEntity::getExamId,
                        LinkedHashMap::new,
                        Collectors.mapping(ExamClassRelationEntity::getClassId, Collectors.toList())));
        Set<Long> allClassIds = classIdsByExamId.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ClassSnapshotEntity> classById = repository.findClassesByIds(allClassIds).stream()
                .collect(Collectors.toMap(ClassSnapshotEntity::getClassId, Function.identity()));
        Map<Long, Set<Long>> distributedStudentIdsByExamId = repository.listExamStudents(exams.stream()
                        .map(ExamEntity::getExamId)
                        .toList())
                .stream()
                .collect(Collectors.groupingBy(ExamStudentRelationEntity::getExamId,
                        LinkedHashMap::new,
                        Collectors.mapping(ExamStudentRelationEntity::getStudentId,
                                Collectors.toCollection(LinkedHashSet::new))));

        Set<Long> visibleClassIds = resolveVisibleClassIds(context, classById.keySet(), classById.values());
        Set<Long> visibleExamIds = resolveVisibleExamIds(context, targetStudentId, exams, distributedStudentIdsByExamId);
        List<ExamView> records = exams.stream()
                .filter(exam -> request.getExamId() == null || Objects.equals(exam.getExamId(), request.getExamId()))
                .filter(exam -> request.getStatus() == null || exam.getStatus().equalsIgnoreCase(request.getStatus()))
                .filter(exam -> request.getKeyword() == null || containsIgnoreCase(exam.getExamName(), request.getKeyword()))
                .map(exam -> toExamView(exam, classIdsByExamId.getOrDefault(exam.getExamId(), List.of())))
                .filter(view -> request.getClassId() == null || view.getClassIds().contains(request.getClassId()))
                .filter(view -> canViewExam(view, visibleClassIds, visibleExamIds, context))
                .sorted(Comparator.comparing(ExamView::getStartTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ExamView::getExamId, Comparator.reverseOrder()))
                .toList();
        return ApiResponse.ok(page(records, Math.toIntExact(request.getPageNum()), Math.toIntExact(request.getPageSize())))
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<ExamView> updateParams(ExamUpdateParamsRequest request, RequestContext context)
            throws BusinessException {
        ensureTeacherOrAdmin(context);
        if (request.getDuration() == null && request.getStartTime() == null) {
            throw new BusinessException(DomainErrorCode.EXAM_UPDATE_PARAM_REQUIRED);
        }
        ExamEntity exam = repository.findExamById(request.getExamId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.EXAM_NOT_FOUND));
        ensureExamOperator(context, exam);
        ExamLifecycleStatus lifecycleStatus = ExamLifecycleStatus.valueOf(exam.getStatus());
        if (lifecycleStatus == ExamLifecycleStatus.ENDED || lifecycleStatus == ExamLifecycleStatus.TERMINATED) {
            throw new BusinessException(DomainErrorCode.EXAM_STATUS_CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean started = !now.isBefore(exam.getStartTime());
        if (started && request.getStartTime() != null) {
            throw new BusinessException(DomainErrorCode.EXAM_START_TIME_UPDATE_FORBIDDEN);
        }
        if (request.getStartTime() != null) {
            exam.setStartTime(parseDateTime(request.getStartTime()));
        }
        if (request.getDuration() != null) {
            int nextDuration = validateDuration(request.getDuration());
            if (started && nextDuration <= exam.getDuration()) {
                throw new BusinessException(DomainErrorCode.EXAM_DURATION_EXTENSION_REQUIRED);
            }
            exam.setDuration(nextDuration);
        }
        exam.setRequestId(context.requestId())
                .setUpdateTime(now);
        repository.updateExam(exam);
        return ApiResponse.ok(toExamView(exam, currentClassIds(exam.getExamId())))
                .withRequestId(context.requestId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<ExamDistributeView> distribute(ExamDistributeRequest request, RequestContext context)
            throws BusinessException {
        ensureTeacherOrAdmin(context);
        List<Long> studentIds = normalizeIds(request.getStudentIds());
        if (studentIds.isEmpty()) {
            throw new BusinessException(DomainErrorCode.EXAM_DISTRIBUTE_TARGET_REQUIRED);
        }
        ExamEntity exam = repository.findExamById(request.getExamId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.EXAM_NOT_FOUND));
        ensureExamOperator(context, exam);
        ExamLifecycleStatus lifecycleStatus = ExamLifecycleStatus.valueOf(exam.getStatus());
        if (lifecycleStatus == ExamLifecycleStatus.ENDED || lifecycleStatus == ExamLifecycleStatus.TERMINATED) {
            throw new BusinessException(DomainErrorCode.EXAM_DISTRIBUTE_STATUS_CONFLICT);
        }

        List<Long> classIds = currentClassIds(exam.getExamId());
        Map<Long, ClassSnapshotEntity> classById = repository.findClassesByIds(classIds).stream()
                .collect(Collectors.toMap(ClassSnapshotEntity::getClassId, Function.identity()));
        if (classById.size() != classIds.size()) {
            throw new BusinessException(DomainErrorCode.CLASS_NOT_FOUND);
        }
        if (RoleMappings.isTeacher(context.roleId())) {
            boolean hasForeignClass = classById.values().stream()
                    .anyMatch(item -> !Objects.equals(item.getCreatedBy(), context.userId()));
            if (hasForeignClass) {
                throw new BusinessException(DomainErrorCode.EXAM_MANAGE_FORBIDDEN);
            }
        }
        ensureScoreRedistributeAllowed(exam.getExamId());

        Map<Long, List<ClassMemberSnapshotEntity>> membershipsByStudentId = repository.listClassMembersByStudentIds(studentIds).stream()
                .filter(item -> classById.containsKey(item.getClassId()))
                .collect(Collectors.groupingBy(ClassMemberSnapshotEntity::getStudentId,
                        LinkedHashMap::new,
                        Collectors.toList()));
        List<ExamStudentRelationEntity> relations = new java.util.ArrayList<>();
        Set<Long> scopeMismatchStudentIds = new LinkedHashSet<>();
        Set<Long> statusMismatchStudentIds = new LinkedHashSet<>();
        LocalDateTime now = LocalDateTime.now();
        for (Long studentId : studentIds) {
            List<ClassMemberSnapshotEntity> memberships = membershipsByStudentId.getOrDefault(studentId, List.of());
            if (memberships.isEmpty()) {
                scopeMismatchStudentIds.add(studentId);
                continue;
            }
            ClassMemberSnapshotEntity approvedMembership = memberships.stream()
                    .filter(item -> "APPROVED".equalsIgnoreCase(item.getStatus()))
                    .findFirst()
                    .orElse(null);
            if (approvedMembership == null) {
                statusMismatchStudentIds.add(studentId);
                continue;
            }
            relations.add(new ExamStudentRelationEntity()
                    .setRelationId(snowflake.nextId())
                    .setExamId(exam.getExamId())
                    .setClassId(approvedMembership.getClassId())
                    .setStudentId(studentId)
                    .setCreateTime(now)
                    .setUpdateTime(now));
        }
        if (!scopeMismatchStudentIds.isEmpty()) {
            throw new BusinessException(DomainErrorCode.EXAM_STUDENT_SCOPE_CONFLICT);
        }
        if (!statusMismatchStudentIds.isEmpty()) {
            throw new BusinessException(DomainErrorCode.EXAM_STUDENT_STATUS_CONFLICT);
        }
        repository.replaceExamStudents(exam.getExamId(), relations);
        initializeScores(exam, relations, classById, context, now);
        exam.setRequestId(context.requestId())
                .setUpdateTime(now);
        repository.updateExam(exam);
        return ApiResponse.ok(ExamDistributeView.builder()
                        .examId(exam.getExamId())
                        .status(exam.getStatus())
                        .classIds(classIds)
                        .studentIds(studentIds)
                        .distributedCount(studentIds.size())
                .build())
                .withRequestId(context.requestId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<ExamSubmitView> submit(ExamSubmitRequest request, RequestContext context)
            throws BusinessException {
        ensureStudentSubmitOperator(context, request.getStudentId());
        ExamEntity exam = repository.findExamById(request.getExamId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.EXAM_NOT_FOUND));
        ensureExamSubmitAllowed(exam);

        ScoreRecordEntity score = repository.findScoreByExamIdAndStudentId(request.getExamId(), request.getStudentId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.SCORE_NOT_FOUND));
        if (score.getSubmittedAt() != null || !"PENDING".equalsIgnoreCase(score.getStatus())) {
            throw new BusinessException(DomainErrorCode.EXAM_SUBMIT_STATUS_CONFLICT);
        }

        List<ScoreDetailEntity> details = repository.listScoreDetails(score.getScoreId());
        Map<Long, ScoreDetailEntity> detailByQuestionId = details.stream()
                .collect(Collectors.toMap(ScoreDetailEntity::getQuestionId, Function.identity()));
        Map<Long, String> answerByQuestionId = normalizeAnswerMap(request.getAnswers(), detailByQuestionId.keySet());

        LocalDateTime now = LocalDateTime.now();
        int objectiveScore = 0;
        int subjectiveScore = 0;
        boolean hasSubjectiveQuestion = false;
        for (ScoreDetailEntity detail : details) {
            detail.setStudentAnswer(answerByQuestionId.get(detail.getQuestionId()))
                    .setUpdateTime(now);
            if (isObjectiveQuestion(detail.getQuestionType())) {
                boolean correct = isObjectiveAnswerCorrect(detail);
                int questionScore = correct ? safeScore(detail.getAssignedScore()) : 0;
                detail.setCorrect(correct)
                        .setScore(questionScore);
                objectiveScore += questionScore;
            } else if (isSubjectiveQuestion(detail.getQuestionType())) {
                hasSubjectiveQuestion = true;
                subjectiveScore += safeScore(detail.getScore());
            }
            repository.updateScoreDetail(detail);
        }
        score.setSubmittedAt(now)
                .setObjectiveScore(objectiveScore)
                .setSubjectiveScore(subjectiveScore)
                .setTotalScore(objectiveScore + subjectiveScore)
                .setStatus(hasSubjectiveQuestion ? "SCORING" : "SCORED")
                .setRequestId(context.requestId())
                .setUpdateTime(now);
        repository.updateScore(score);

        long answeredCount = answerByQuestionId.values().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .count();
        return ApiResponse.ok(ExamSubmitView.builder()
                        .examId(score.getExamId())
                        .studentId(score.getStudentId())
                        .status(score.getStatus())
                        .submittedAt(score.getSubmittedAt())
                        .answeredCount((int) answeredCount)
                .build())
                .withRequestId(context.requestId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<ExamRetestApplyView> applyRetest(ExamApplyRetestRequest request, RequestContext context)
            throws BusinessException {
        ensureStudentRetestOperator(context, request.getStudentId());
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException(DomainErrorCode.EXAM_RETEST_REASON_REQUIRED);
        }
        ExamEntity exam = repository.findExamById(request.getExamId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.EXAM_NOT_FOUND));
        ensureRetestApplyAllowed(exam);
        ScoreRecordEntity score = repository.findScoreByExamIdAndStudentId(request.getExamId(), request.getStudentId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.SCORE_NOT_FOUND));
        if (score.getSubmittedAt() != null
                || repository.findRetestApplyByExamIdAndStudentId(request.getExamId(), request.getStudentId()).isPresent()) {
            throw new BusinessException(DomainErrorCode.EXAM_RETEST_STATUS_CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now();
        ExamRetestApplyEntity apply = new ExamRetestApplyEntity()
                .setRetestApplyId(snowflake.nextId())
                .setExamId(request.getExamId())
                .setClassId(score.getClassId())
                .setStudentId(request.getStudentId())
                .setStatus("PENDING")
                .setApplyReason(request.getReason().trim())
                .setRequestId(context.requestId())
                .setCreateTime(now)
                .setUpdateTime(now);
        repository.saveRetestApply(apply);
        return ApiResponse.ok(ExamRetestApplyView.builder()
                        .retestApplyId(apply.getRetestApplyId())
                        .examId(apply.getExamId())
                        .studentId(apply.getStudentId())
                        .status(apply.getStatus())
                        .createdAt(apply.getCreateTime())
                .build())
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<ExamView> toggleStatus(ExamToggleStatusRequest request, RequestContext context)
            throws BusinessException {
        ensureTeacherOrAdmin(context);
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException(DomainErrorCode.EXAM_TOGGLE_REASON_REQUIRED);
        }
        ExamEntity exam = repository.findExamById(request.getExamId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.EXAM_NOT_FOUND));
        ensureExamOperator(context, exam);

        ExamLifecycleStatus currentStatus = ExamLifecycleStatus.valueOf(exam.getStatus());
        ExamLifecycleStatus nextStatus = resolveToggleTargetStatus(request.getIsPaused(), currentStatus, exam.getStartTime());
        LocalDateTime now = LocalDateTime.now();
        exam.setStatus(nextStatus.name())
                .setRequestId(context.requestId())
                .setUpdateTime(now);
        repository.updateExam(exam);
        repository.saveExamStatusLog(new ExamStatusLogEntity()
                .setLogId(snowflake.nextId())
                .setExamId(exam.getExamId())
                .setOperatorId(context.userId())
                .setOperatorRole(RoleMappings.toInternalCode(context.roleId()))
                .setActionType(Boolean.TRUE.equals(request.getIsPaused()) ? "exam.pause" : "exam.resume")
                .setFromStatus(currentStatus.name())
                .setToStatus(nextStatus.name())
                .setReason(request.getReason().trim())
                .setRequestId(context.requestId())
                .setOperateTime(now)
                .setCreateTime(now)
                .setUpdateTime(now));
        return ApiResponse.ok(toExamView(exam, currentClassIds(exam.getExamId())))
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<String> approveRetest(ExamApproveRetestRequest request, RequestContext context)
            throws BusinessException {
        ensureTeacherOrAdmin(context);
        ExamRetestApplyEntity apply = repository.findRetestApplyById(request.getRetestApplyId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.EXAM_RETEST_APPLY_NOT_FOUND));
        if (!"PENDING".equalsIgnoreCase(apply.getStatus())) {
            throw new BusinessException(DomainErrorCode.EXAM_RETEST_STATUS_CONFLICT);
        }
        ClassSnapshotEntity clazz = repository.findClassesByIds(List.of(apply.getClassId())).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(DomainErrorCode.CLASS_NOT_FOUND));
        if (RoleMappings.isTeacher(context.roleId()) && !Objects.equals(clazz.getCreatedBy(), context.userId())) {
            throw new BusinessException(DomainErrorCode.EXAM_MANAGE_FORBIDDEN);
        }
        List<Long> examClassIds = currentClassIds(apply.getExamId());
        if (!examClassIds.contains(apply.getClassId())) {
            throw new BusinessException(DomainErrorCode.EXAM_STATUS_CONFLICT);
        }

        String nextStatus = resolveRetestDecision(request.getApproveResult(), request.getReason());
        LocalDateTime now = LocalDateTime.now();
        apply.setStatus(nextStatus)
                .setDecisionReason(request.getReason() == null ? null : request.getReason().trim())
                .setReviewedBy(context.userId())
                .setRequestId(context.requestId())
                .setReviewTime(now)
                .setUpdateTime(now);
        repository.updateRetestApply(apply);
        return ApiResponse.ok("ok").withRequestId(context.requestId());
    }

    private void ensureTeacherOrAdmin(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!RoleMappings.isTeacher(context.roleId()) && !RoleMappings.isAdmin(context.roleId())) {
            throw new BusinessException(DomainErrorCode.EXAM_MANAGE_FORBIDDEN);
        }
    }

    private void ensureExamOperator(RequestContext context, ExamEntity exam) throws BusinessException {
        if (RoleMappings.isAdmin(context.roleId())) {
            return;
        }
        if (RoleMappings.isTeacher(context.roleId()) && Objects.equals(context.userId(), exam.getCreatorId())) {
            return;
        }
        throw new BusinessException(DomainErrorCode.EXAM_MANAGE_FORBIDDEN);
    }

    private void ensureQueryOperator(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!RoleMappings.isTeacher(context.roleId())
                && !RoleMappings.isAdmin(context.roleId())
                && !RoleMappings.isAuditor(context.roleId())
                && !RoleMappings.isStudent(context.roleId())) {
            throw new BusinessException(DomainErrorCode.EXAM_MANAGE_FORBIDDEN);
        }
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf));
    }

    private int validateDuration(Integer duration) throws BusinessException {
        if (duration == null || duration <= 0) {
            throw new BusinessException(DomainErrorCode.EXAM_DURATION_INVALID);
        }
        return duration;
    }

    private void ensureScoreRedistributeAllowed(Long examId) throws BusinessException {
        boolean startedScoring = repository.listScoresByExamId(examId).stream()
                .anyMatch(score -> !"PENDING".equalsIgnoreCase(score.getStatus()));
        if (startedScoring) {
            throw new BusinessException(DomainErrorCode.EXAM_DISTRIBUTE_SCORE_CONFLICT);
        }
    }

    private void ensureStudentSubmitOperator(RequestContext context, Long studentId) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!RoleMappings.isStudent(context.roleId()) || !Objects.equals(context.userId(), studentId)) {
            throw new BusinessException(DomainErrorCode.EXAM_SUBMIT_FORBIDDEN);
        }
    }

    private void ensureStudentRetestOperator(RequestContext context, Long studentId) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!RoleMappings.isStudent(context.roleId()) || !Objects.equals(context.userId(), studentId)) {
            throw new BusinessException(DomainErrorCode.EXAM_RETEST_APPLY_FORBIDDEN);
        }
    }

    private void ensureExamSubmitAllowed(ExamEntity exam) throws BusinessException {
        ExamLifecycleStatus lifecycleStatus = ExamLifecycleStatus.valueOf(exam.getStatus());
        if ((lifecycleStatus != ExamLifecycleStatus.PUBLISHED && lifecycleStatus != ExamLifecycleStatus.UNDERWAY)
                || LocalDateTime.now().isBefore(exam.getStartTime())) {
            throw new BusinessException(DomainErrorCode.EXAM_SUBMIT_STATUS_CONFLICT);
        }
    }

    private void ensureRetestApplyAllowed(ExamEntity exam) throws BusinessException {
        LocalDateTime planEndTime = exam.getStartTime().plusMinutes(exam.getDuration());
        boolean examClosed = !LocalDateTime.now().isBefore(planEndTime);
        ExamLifecycleStatus lifecycleStatus = ExamLifecycleStatus.valueOf(exam.getStatus());
        if (!examClosed && lifecycleStatus != ExamLifecycleStatus.ENDED && lifecycleStatus != ExamLifecycleStatus.TERMINATED) {
            throw new BusinessException(DomainErrorCode.EXAM_RETEST_STATUS_CONFLICT);
        }
    }

    private void initializeScores(ExamEntity exam,
                                  List<ExamStudentRelationEntity> relations,
                                  Map<Long, ClassSnapshotEntity> classById,
                                  RequestContext context,
                                  LocalDateTime now) throws BusinessException {
        List<PaperQuestionSnapshotEntity> paperQuestions = repository.listPaperQuestions(exam.getPaperId()).stream()
                .sorted(Comparator.comparing(PaperQuestionSnapshotEntity::getSortNo,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PaperQuestionSnapshotEntity::getBindingId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (paperQuestions.isEmpty()) {
            throw new BusinessException(DomainErrorCode.PAPER_QUESTION_REQUIRED);
        }

        Map<Long, QuestionSnapshotEntity> questionById = repository.findQuestionsByIds(paperQuestions.stream()
                        .map(PaperQuestionSnapshotEntity::getQuestionId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .stream()
                .collect(Collectors.toMap(QuestionSnapshotEntity::getQuestionId, Function.identity()));
        boolean hasMissingQuestion = paperQuestions.stream()
                .anyMatch(item -> !questionById.containsKey(item.getQuestionId()));
        if (hasMissingQuestion) {
            throw new BusinessException(DomainErrorCode.QUESTION_NOT_FOUND);
        }

        Map<Long, UserSnapshotEntity> userById = repository.findUsersByIds(relations.stream()
                        .map(ExamStudentRelationEntity::getStudentId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .stream()
                .collect(Collectors.toMap(UserSnapshotEntity::getUserId, Function.identity()));

        List<ScoreRecordEntity> scores = new java.util.ArrayList<>(relations.size());
        List<ScoreDetailEntity> details = new java.util.ArrayList<>(relations.size() * paperQuestions.size());
        for (ExamStudentRelationEntity relation : relations) {
            ClassSnapshotEntity clazz = classById.get(relation.getClassId());
            Long scoreId = snowflake.nextId();
            scores.add(new ScoreRecordEntity()
                    .setScoreId(scoreId)
                    .setExamId(exam.getExamId())
                    .setExamName(exam.getExamName())
                    .setPaperId(exam.getPaperId())
                    .setPaperName(exam.getPaperName())
                    .setClassId(relation.getClassId())
                    .setClassName(clazz == null ? null : clazz.getClassName())
                    .setStudentId(relation.getStudentId())
                    .setStudentName(resolveStudentName(relation.getStudentId(), userById))
                    .setTotalScore(0)
                    .setObjectiveScore(0)
                    .setSubjectiveScore(0)
                    .setStatus("PENDING")
                    .setRequestId(context.requestId())
                    .setCreateTime(now)
                    .setUpdateTime(now));
            for (PaperQuestionSnapshotEntity paperQuestion : paperQuestions) {
                QuestionSnapshotEntity question = questionById.get(paperQuestion.getQuestionId());
                details.add(new ScoreDetailEntity()
                        .setDetailId(snowflake.nextId())
                        .setScoreId(scoreId)
                        .setExamId(exam.getExamId())
                        .setStudentId(relation.getStudentId())
                        .setQuestionId(paperQuestion.getQuestionId())
                        .setSortNo(paperQuestion.getSortNo())
                        .setQuestionType(resolveQuestionType(paperQuestion, question))
                        .setQuestionStem(question.getContent())
                        .setCorrectAnswer(question.getAnswer())
                        .setAssignedScore(paperQuestion.getAssignedScore())
                        .setScore(0)
                        .setCorrect(null)
                        .setReviewComment(null)
                        .setCreateTime(now)
                        .setUpdateTime(now));
            }
        }
        repository.replaceExamScores(exam.getExamId(), scores, details);
    }

    private String resolveStudentName(Long studentId, Map<Long, UserSnapshotEntity> userById) {
        UserSnapshotEntity user = userById.get(studentId);
        if (user == null || user.getRealName() == null || user.getRealName().isBlank()) {
            return Long.toString(studentId);
        }
        return user.getRealName();
    }

    private String resolveQuestionType(PaperQuestionSnapshotEntity paperQuestion, QuestionSnapshotEntity question) {
        if (paperQuestion.getQuestionType() != null && !paperQuestion.getQuestionType().isBlank()) {
            return paperQuestion.getQuestionType();
        }
        return question.getQuestionType();
    }

    private Map<Long, String> normalizeAnswerMap(List<ExamSubmitRequest.AnswerItem> answers,
                                                 Set<Long> allowedQuestionIds) throws BusinessException {
        Map<Long, String> answerByQuestionId = new LinkedHashMap<>();
        if (answers == null || answers.isEmpty()) {
            return answerByQuestionId;
        }
        for (ExamSubmitRequest.AnswerItem answer : answers) {
            if (answer == null || answer.getQuestionId() == null) {
                throw new BusinessException(DomainErrorCode.EXAM_SUBMIT_QUESTION_CONFLICT);
            }
            Long questionId = answer.getQuestionId();
            if (!allowedQuestionIds.contains(questionId) || answerByQuestionId.containsKey(questionId)) {
                throw new BusinessException(DomainErrorCode.EXAM_SUBMIT_QUESTION_CONFLICT);
            }
            answerByQuestionId.put(questionId, answer.getAnswer());
        }
        return answerByQuestionId;
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
        if (answer == null || answer.isBlank()) {
            return "";
        }
        return answer.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeMultiChoiceAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return "";
        }
        return Arrays.stream(answer.trim().toUpperCase(Locale.ROOT).split("[,，;；、\\s]+"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }

    private int safeScore(Integer score) {
        return score == null ? 0 : score;
    }

    private Long resolveQueryStudentId(ExamQueryRequest request, RequestContext context) throws BusinessException {
        if (RoleMappings.isStudent(context.roleId())) {
            if (request.getStudentId() != null && !Objects.equals(request.getStudentId(), context.userId())) {
                throw new BusinessException(DomainErrorCode.EXAM_MANAGE_FORBIDDEN);
            }
            return context.userId();
        }
        return request.getStudentId();
    }

    private Set<Long> resolveVisibleClassIds(RequestContext context,
                                             Set<Long> relatedClassIds,
                                             java.util.Collection<ClassSnapshotEntity> relatedClasses) {
        if (RoleMappings.isAdmin(context.roleId()) || RoleMappings.isAuditor(context.roleId())) {
            return relatedClassIds;
        }
        if (RoleMappings.isTeacher(context.roleId())) {
            return relatedClasses.stream()
                    .filter(item -> Objects.equals(item.getCreatedBy(), context.userId()))
                    .map(ClassSnapshotEntity::getClassId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Set.of();
    }

    private Set<Long> resolveVisibleExamIds(RequestContext context,
                                            Long targetStudentId,
                                            List<ExamEntity> exams,
                                            Map<Long, Set<Long>> distributedStudentIdsByExamId) {
        if (RoleMappings.isStudent(context.roleId())) {
            Long studentId = targetStudentId == null ? context.userId() : targetStudentId;
            return distributedExamIdsOfStudent(studentId, distributedStudentIdsByExamId);
        }
        if (targetStudentId != null) {
            return distributedExamIdsOfStudent(targetStudentId, distributedStudentIdsByExamId);
        }
        return exams.stream()
                .map(ExamEntity::getExamId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> distributedExamIdsOfStudent(Long studentId,
                                                  Map<Long, Set<Long>> distributedStudentIdsByExamId) {
        if (studentId == null) {
            return Set.of();
        }
        Set<Long> examIds = new LinkedHashSet<>();
        for (Map.Entry<Long, Set<Long>> entry : distributedStudentIdsByExamId.entrySet()) {
            if (entry.getValue().contains(studentId)) {
                examIds.add(entry.getKey());
            }
        }
        return examIds;
    }

    private ExamLifecycleStatus resolveToggleTargetStatus(Boolean paused,
                                                          ExamLifecycleStatus currentStatus,
                                                          LocalDateTime startTime) throws BusinessException {
        boolean isPaused = Boolean.TRUE.equals(paused);
        if (isPaused) {
            if (currentStatus == ExamLifecycleStatus.PUBLISHED || currentStatus == ExamLifecycleStatus.UNDERWAY) {
                return ExamLifecycleStatus.PAUSED;
            }
            throw new BusinessException(DomainErrorCode.EXAM_STATUS_CONFLICT);
        }
        if (currentStatus != ExamLifecycleStatus.PAUSED) {
            throw new BusinessException(DomainErrorCode.EXAM_STATUS_CONFLICT);
        }
        return LocalDateTime.now().isBefore(startTime)
                ? ExamLifecycleStatus.PUBLISHED
                : ExamLifecycleStatus.UNDERWAY;
    }

    private String resolveRetestDecision(String approveResult, String reason) throws BusinessException {
        String normalized = approveResult == null ? "" : approveResult.trim().toLowerCase(Locale.ROOT);
        if ("approved".equals(normalized)) {
            return "APPROVED";
        }
        if ("rejected".equals(normalized)) {
            if (reason == null || reason.isBlank()) {
                throw new BusinessException(DomainErrorCode.EXAM_RETEST_REASON_REQUIRED);
            }
            return "REJECTED";
        }
        throw new BusinessException(DomainErrorCode.EXAM_RETEST_RESULT_INVALID);
    }

    private boolean canViewExam(ExamView view,
                                Set<Long> visibleClassIds,
                                Set<Long> visibleExamIds,
                                RequestContext context) {
        if (!visibleExamIds.contains(view.getExamId())) {
            return false;
        }
        if (RoleMappings.isStudent(context.roleId())) {
            return true;
        }
        return view.getClassIds().stream().anyMatch(visibleClassIds::contains);
    }

    private LocalDateTime parseDateTime(String raw) throws BusinessException {
        try {
            return LocalDateTime.parse(raw, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(DomainErrorCode.EXAM_START_TIME_REQUIRED);
        }
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null
                && keyword != null
                && source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private ExamView toExamView(ExamEntity exam, List<Long> classIds) {
        return ExamView.builder()
                .examId(exam.getExamId())
                .examName(exam.getExamName())
                .paperId(exam.getPaperId())
                .paperName(exam.getPaperName())
                .status(exam.getStatus())
                .creatorId(exam.getCreatorId())
                .duration(exam.getDuration())
                .startTime(exam.getStartTime())
                .classIds(classIds)
                .build();
    }

    private List<Long> currentClassIds(Long examId) {
        return repository.listExamClasses(List.of(examId)).stream()
                .map(ExamClassRelationEntity::getClassId)
                .toList();
    }

    private <T> PageResult<T> page(List<T> source, int pageNum, int pageSize) {
        int fromIndex = Math.min((pageNum - 1) * pageSize, source.size());
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        return new PageResult<>(source.subList(fromIndex, toIndex), source.size(), pageNum, pageSize);
    }

    private String normalizeExamName(String examName) throws BusinessException {
        if (examName == null || examName.isBlank()) {
            throw new BusinessException(400, "examName 不能为空");
        }
        return examName.trim();
    }
}
