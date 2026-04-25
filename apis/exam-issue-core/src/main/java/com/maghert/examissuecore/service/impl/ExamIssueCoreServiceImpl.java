package com.maghert.examissuecore.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.json.JSONUtil;
import com.maghert.examcommon.audit.AuditTrail;
import com.maghert.examcommon.auth.RoleMappings;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examissuecore.context.RequestContext;
import com.maghert.examissuecore.entity.ClassSnapshotEntity;
import com.maghert.examissuecore.entity.ExamClassRelationEntity;
import com.maghert.examissuecore.entity.ExamSnapshotEntity;
import com.maghert.examissuecore.entity.IssueProcessLogEntity;
import com.maghert.examissuecore.entity.IssueRecordEntity;
import com.maghert.examissuecore.entity.UserSnapshotEntity;
import com.maghert.examissuecore.model.dto.IssueCloseRequest;
import com.maghert.examissuecore.model.dto.IssueCreateRequest;
import com.maghert.examissuecore.model.dto.IssueHandleRequest;
import com.maghert.examissuecore.model.dto.IssueQueryRequest;
import com.maghert.examissuecore.model.dto.IssueTrackRequest;
import com.maghert.examissuecore.model.dto.IssueTransferRequest;
import com.maghert.examissuecore.model.enums.IssueAction;
import com.maghert.examissuecore.model.enums.IssueStatus;
import com.maghert.examissuecore.model.enums.IssueType;
import com.maghert.examissuecore.model.vo.IssueProcessLogView;
import com.maghert.examissuecore.model.vo.IssueTrackView;
import com.maghert.examissuecore.model.vo.IssueView;
import com.maghert.examissuecore.repository.IssueDomainRepository;
import com.maghert.examissuecore.service.ExamIssueCoreService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExamIssueCoreServiceImpl implements ExamIssueCoreService {

    private static final String CONFIRM_RESULT_CONFIRMED = "CONFIRMED";

    private final IssueDomainRepository repository;
    private final Snowflake snowflake;

    public ExamIssueCoreServiceImpl(IssueDomainRepository repository, Snowflake snowflake) {
        this.repository = repository;
        this.snowflake = snowflake;
    }

    @Override
    public ApiResponse<IssueView> create(IssueCreateRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        IssueType issueType = IssueType.from(request.getType());
        validateIssueScope(issueType, request.getExamId(), request.getClassId());

        LocalDateTime now = LocalDateTime.now();
        Long issueId = snowflake.nextId();
        Long reporterId = context.userId();
        IssueRecordEntity issue = new IssueRecordEntity()
                .setIssueId(issueId)
                .setType(issueType.name())
                .setTitle(request.getTitle().trim())
                .setDescription(request.getDesc().trim())
                .setStatus(IssueStatus.PENDING.name())
                .setReporterId(reporterId)
                .setCurrentHandlerId(null)
                .setExamId(request.getExamId())
                .setClassId(request.getClassId())
                .setLatestResult(null)
                .setLatestSolution(null)
                .setImgUrls(toJsonArray(request.getImgUrls()))
                .setAuditTrail(toAuditTrail(IssueAction.CREATED.name(), reporterId, "issue", issueId, context.requestId(),
                        "创建问题", now))
                .setCreateTime(now)
                .setUpdateTime(now);
        repository.saveIssue(issue);
        repository.saveProcessLog(buildProcessLog(issueId, IssueAction.CREATED, reporterId, null, null,
                "创建问题", context.requestId(), now));
        return ApiResponse.ok(toIssueView(issue)).withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<PageResult<IssueView>> query(IssueQueryRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        IssueType requestedType = StringUtils.hasText(request.getType()) ? IssueType.from(request.getType()) : null;
        IssueStatus requestedStatus = StringUtils.hasText(request.getStatus()) ? IssueStatus.from(request.getStatus()) : null;
        List<IssueRecordEntity> issues = repository.listIssues().stream()
                .filter(item -> request.getIssueId() == null || Objects.equals(item.getIssueId(), request.getIssueId()))
                .filter(item -> requestedType == null || requestedType.name().equals(item.getType()))
                .filter(item -> requestedStatus == null || requestedStatus.name().equals(item.getStatus()))
                .filter(item -> request.getReporterId() == null || Objects.equals(item.getReporterId(), request.getReporterId()))
                .filter(item -> request.getHandlerId() == null || Objects.equals(item.getCurrentHandlerId(), request.getHandlerId()))
                .filter(item -> canViewIssue(item, context))
                .sorted(Comparator.comparing(IssueRecordEntity::getUpdateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<IssueView> records = toIssueViews(issues);
        return ApiResponse.ok(page(records, Math.toIntExact(request.getPageNum()), Math.toIntExact(request.getPageSize())))
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<IssueView> handle(IssueHandleRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        IssueRecordEntity issue = repository.findIssueById(request.getIssueId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.ISSUE_NOT_FOUND));
        ensureHandlePermission(context, issue);
        ensureNotClosed(issue);
        if (IssueStatus.PROCESSING.name().equals(issue.getStatus())
                && issue.getCurrentHandlerId() != null
                && !Objects.equals(issue.getCurrentHandlerId(), context.userId())) {
            throw new BusinessException(DomainErrorCode.ISSUE_HANDLE_FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now();
        Long previousHandlerId = issue.getCurrentHandlerId();
        issue.setCurrentHandlerId(context.userId())
                .setStatus(IssueStatus.PROCESSING.name())
                .setLatestResult(request.getResult().trim())
                .setLatestSolution(normalizeNullable(request.getSolution()))
                .setAuditTrail(toAuditTrail(IssueAction.HANDLED.name(), context.userId(), "issue", issue.getIssueId(),
                        context.requestId(), "处理问题", now))
                .setUpdateTime(now);
        repository.updateIssue(issue);
        repository.saveProcessLog(buildProcessLog(issue.getIssueId(), IssueAction.HANDLED, context.userId(), previousHandlerId,
                context.userId(), buildHandleContent(request), context.requestId(), now));
        return ApiResponse.ok(toIssueView(issue)).withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<IssueView> transfer(IssueTransferRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        IssueRecordEntity issue = repository.findIssueById(request.getIssueId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.ISSUE_NOT_FOUND));
        ensureTransferPermission(context, issue);
        if (!IssueStatus.PROCESSING.name().equals(issue.getStatus())) {
            throw new BusinessException(DomainErrorCode.ISSUE_STATUS_INVALID);
        }
        if (!Objects.equals(issue.getCurrentHandlerId(), context.userId())) {
            throw new BusinessException(DomainErrorCode.ISSUE_TRANSFER_FORBIDDEN);
        }
        if (Objects.equals(request.getToHandlerId(), context.userId())) {
            throw new BusinessException(DomainErrorCode.ISSUE_TRANSFER_TARGET_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        Long previousHandlerId = issue.getCurrentHandlerId();
        issue.setCurrentHandlerId(request.getToHandlerId())
                .setAuditTrail(toAuditTrail(IssueAction.TRANSFERRED.name(), context.userId(), "issue", issue.getIssueId(),
                        context.requestId(), "转派问题", now))
                .setUpdateTime(now);
        repository.updateIssue(issue);
        repository.saveProcessLog(buildProcessLog(issue.getIssueId(), IssueAction.TRANSFERRED, context.userId(), previousHandlerId,
                request.getToHandlerId(), request.getReason().trim(), context.requestId(), now));
        return ApiResponse.ok(toIssueView(issue)).withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<IssueView> close(IssueCloseRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        IssueRecordEntity issue = repository.findIssueById(request.getIssueId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.ISSUE_NOT_FOUND));
        ensureClosePermission(context, issue);
        if (!IssueStatus.PROCESSING.name().equals(issue.getStatus())) {
            throw new BusinessException(DomainErrorCode.ISSUE_STATUS_INVALID);
        }
        if (!Objects.equals(issue.getCurrentHandlerId(), context.userId())) {
            throw new BusinessException(DomainErrorCode.ISSUE_CLOSE_FORBIDDEN);
        }
        if (!CONFIRM_RESULT_CONFIRMED.equals(normalizeConfirmResult(request.getConfirmResult()))) {
            throw new BusinessException(DomainErrorCode.ISSUE_CONFIRM_RESULT_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        issue.setStatus(IssueStatus.CLOSED.name())
                .setAuditTrail(toAuditTrail(IssueAction.CLOSED.name(), context.userId(), "issue", issue.getIssueId(),
                        context.requestId(), "关闭问题", now))
                .setUpdateTime(now);
        repository.updateIssue(issue);
        repository.saveProcessLog(buildProcessLog(issue.getIssueId(), IssueAction.CLOSED, context.userId(), issue.getCurrentHandlerId(),
                issue.getCurrentHandlerId(), normalizeNullable(request.getComment()), context.requestId(), now));
        return ApiResponse.ok(toIssueView(issue)).withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<IssueTrackView> track(IssueTrackRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        IssueRecordEntity issue = repository.findIssueById(request.getIssueId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.ISSUE_NOT_FOUND));
        if (!canTrackIssue(issue, context)) {
            throw new BusinessException(DomainErrorCode.ISSUE_TRACK_FORBIDDEN);
        }
        List<IssueProcessLogEntity> logs = repository.listProcessLogs(issue.getIssueId()).stream()
                .sorted(Comparator.comparing(IssueProcessLogEntity::getCreateTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return ApiResponse.ok(toTrackView(issue, logs)).withRequestId(context.requestId());
    }

    private void ensureAuthenticated(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
    }

    private void validateIssueScope(IssueType issueType, Long examId, Long classId) throws BusinessException {
        if (issueType == IssueType.EXAM && examId == null && classId == null) {
            throw new BusinessException(DomainErrorCode.ISSUE_EXAM_SCOPE_REQUIRED);
        }
        if (examId != null && repository.findExamsByIds(List.of(examId)).isEmpty()) {
            throw new BusinessException(DomainErrorCode.EXAM_NOT_FOUND);
        }
        if (classId != null && repository.findClassesByIds(List.of(classId)).isEmpty()) {
            throw new BusinessException(DomainErrorCode.CLASS_NOT_FOUND);
        }
    }

    private void ensureHandlePermission(RequestContext context, IssueRecordEntity issue) throws BusinessException {
        IssueType issueType = IssueType.from(issue.getType());
        if (RoleMappings.isAdmin(context.roleId())) {
            if (issueType != IssueType.BUSINESS) {
                throw new BusinessException(DomainErrorCode.ISSUE_HANDLE_FORBIDDEN);
            }
            return;
        }
        if (RoleMappings.isTeacher(context.roleId())) {
            if (issueType != IssueType.EXAM
                    || (!teacherCanAccessExamIssue(issue, context.userId())
                    && !Objects.equals(issue.getCurrentHandlerId(), context.userId()))) {
                throw new BusinessException(DomainErrorCode.ISSUE_HANDLE_FORBIDDEN);
            }
            return;
        }
        if (RoleMappings.isOps(context.roleId())) {
            if (issueType != IssueType.SYSTEM) {
                throw new BusinessException(DomainErrorCode.ISSUE_HANDLE_FORBIDDEN);
            }
            return;
        }
        throw new BusinessException(DomainErrorCode.ISSUE_HANDLE_FORBIDDEN);
    }

    private void ensureTransferPermission(RequestContext context, IssueRecordEntity issue) throws BusinessException {
        IssueType issueType = IssueType.from(issue.getType());
        if (RoleMappings.isAdmin(context.roleId())) {
            if (issueType != IssueType.BUSINESS) {
                throw new BusinessException(DomainErrorCode.ISSUE_TRANSFER_FORBIDDEN);
            }
            return;
        }
        if (RoleMappings.isTeacher(context.roleId())) {
            if (issueType != IssueType.EXAM
                    || (!teacherCanAccessExamIssue(issue, context.userId())
                    && !Objects.equals(issue.getCurrentHandlerId(), context.userId()))) {
                throw new BusinessException(DomainErrorCode.ISSUE_TRANSFER_FORBIDDEN);
            }
            return;
        }
        if (RoleMappings.isOps(context.roleId())) {
            if (issueType != IssueType.SYSTEM) {
                throw new BusinessException(DomainErrorCode.ISSUE_TRANSFER_FORBIDDEN);
            }
            return;
        }
        throw new BusinessException(DomainErrorCode.ISSUE_TRANSFER_FORBIDDEN);
    }

    private void ensureClosePermission(RequestContext context, IssueRecordEntity issue) throws BusinessException {
        IssueType issueType = IssueType.from(issue.getType());
        if (RoleMappings.isAdmin(context.roleId())) {
            if (issueType != IssueType.BUSINESS) {
                throw new BusinessException(DomainErrorCode.ISSUE_CLOSE_FORBIDDEN);
            }
            return;
        }
        if (RoleMappings.isTeacher(context.roleId())) {
            if (issueType != IssueType.EXAM
                    || (!teacherCanAccessExamIssue(issue, context.userId())
                    && !Objects.equals(issue.getCurrentHandlerId(), context.userId()))) {
                throw new BusinessException(DomainErrorCode.ISSUE_CLOSE_FORBIDDEN);
            }
            return;
        }
        if (RoleMappings.isOps(context.roleId())) {
            if (issueType != IssueType.SYSTEM) {
                throw new BusinessException(DomainErrorCode.ISSUE_CLOSE_FORBIDDEN);
            }
            return;
        }
        throw new BusinessException(DomainErrorCode.ISSUE_CLOSE_FORBIDDEN);
    }

    private boolean canViewIssue(IssueRecordEntity issue, RequestContext context) {
        if (RoleMappings.isAdmin(context.roleId()) || RoleMappings.isAuditor(context.roleId())) {
            return true;
        }
        if (Objects.equals(issue.getReporterId(), context.userId())) {
            return true;
        }
        if (RoleMappings.isTeacher(context.roleId())) {
            if (!IssueType.EXAM.name().equals(issue.getType())) {
                return false;
            }
            if (Objects.equals(issue.getCurrentHandlerId(), context.userId())) {
                return true;
            }
            return issue.getCurrentHandlerId() == null && teacherCanAccessExamIssue(issue, context.userId());
        }
        if (RoleMappings.isOps(context.roleId())) {
            if (!IssueType.SYSTEM.name().equals(issue.getType())) {
                return false;
            }
            return issue.getCurrentHandlerId() == null || Objects.equals(issue.getCurrentHandlerId(), context.userId());
        }
        return false;
    }

    private boolean canTrackIssue(IssueRecordEntity issue, RequestContext context) throws BusinessException {
        if (RoleMappings.isAdmin(context.roleId()) || RoleMappings.isAuditor(context.roleId())) {
            return true;
        }
        if (Objects.equals(issue.getReporterId(), context.userId())) {
            return true;
        }
        if (!Objects.equals(issue.getCurrentHandlerId(), context.userId())) {
            return false;
        }
        IssueType issueType = IssueType.from(issue.getType());
        if (RoleMappings.isTeacher(context.roleId())) {
            return issueType == IssueType.EXAM;
        }
        if (RoleMappings.isOps(context.roleId())) {
            return issueType == IssueType.SYSTEM;
        }
        return false;
    }

    private boolean teacherCanAccessExamIssue(IssueRecordEntity issue, Long teacherUserId) {
        if (teacherUserId == null) {
            return false;
        }
        if (issue.getClassId() != null) {
            Optional<ClassSnapshotEntity> clazz = repository.findClassesByIds(List.of(issue.getClassId())).stream().findFirst();
            if (clazz.isPresent() && belongsToTeacher(clazz.get(), teacherUserId)) {
                return true;
            }
        }
        if (issue.getExamId() != null) {
            Optional<ExamSnapshotEntity> exam = repository.findExamsByIds(List.of(issue.getExamId())).stream().findFirst();
            if (exam.isPresent() && Objects.equals(exam.get().getCreatorId(), teacherUserId)) {
                return true;
            }
            Set<Long> classIds = repository.listExamClasses(List.of(issue.getExamId())).stream()
                    .map(ExamClassRelationEntity::getClassId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return repository.findClassesByIds(classIds).stream().anyMatch(item -> belongsToTeacher(item, teacherUserId));
        }
        return false;
    }

    private boolean belongsToTeacher(ClassSnapshotEntity clazz, Long teacherUserId) {
        return Objects.equals(clazz.getTeacherId(), teacherUserId) || Objects.equals(clazz.getCreatedBy(), teacherUserId);
    }

    private void ensureNotClosed(IssueRecordEntity issue) throws BusinessException {
        if (IssueStatus.CLOSED.name().equals(issue.getStatus())) {
            throw new BusinessException(DomainErrorCode.ISSUE_STATUS_INVALID);
        }
    }

    private String normalizeConfirmResult(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String raw) {
        return StringUtils.hasText(raw) ? raw.trim() : null;
    }

    private String buildHandleContent(IssueHandleRequest request) {
        String solution = normalizeNullable(request.getSolution());
        if (solution == null) {
            return "处理结果：" + request.getResult().trim();
        }
        return "处理结果：" + request.getResult().trim() + "；处理方案：" + solution;
    }

    private IssueProcessLogEntity buildProcessLog(Long issueId,
                                                  IssueAction action,
                                                  Long operatorId,
                                                  Long fromHandlerId,
                                                  Long toHandlerId,
                                                  String content,
                                                  String requestId,
                                                  LocalDateTime occurredAt) {
        return new IssueProcessLogEntity()
                .setLogId(snowflake.nextId())
                .setIssueId(issueId)
                .setAction(action.name())
                .setOperatorId(operatorId)
                .setFromHandlerId(fromHandlerId)
                .setToHandlerId(toHandlerId)
                .setContent(content)
                .setAuditTrail(toAuditTrail(action.name(), operatorId, "issue", issueId, requestId, content, occurredAt))
                .setCreateTime(occurredAt)
                .setUpdateTime(occurredAt);
    }

    private String toAuditTrail(String actionType,
                                Long operatorId,
                                String targetType,
                                Long targetId,
                                String requestId,
                                String detail,
                                LocalDateTime occurredAt) {
        return JSONUtil.toJsonStr(AuditTrail.builder()
                .actionType(actionType)
                .operatorId(operatorId)
                .targetType(targetType)
                .targetId(targetId == null ? null : String.valueOf(targetId))
                .requestId(requestId)
                .detail(detail)
                .occurredAt(occurredAt)
                .build());
    }

    private String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        List<String> normalized = values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        return JSONUtil.toJsonStr(normalized);
    }

    private List<String> parseImgUrls(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return JSONUtil.toList(JSONUtil.parseArray(raw), String.class);
    }

    private PageResult<IssueView> page(List<IssueView> source, int pageNum, int pageSize) {
        if (source.isEmpty()) {
            return new PageResult<>(List.of(), 0, pageNum, pageSize);
        }
        int fromIndex = Math.min((pageNum - 1) * pageSize, source.size());
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        return new PageResult<>(source.subList(fromIndex, toIndex), source.size(), pageNum, pageSize);
    }

    private List<IssueView> toIssueViews(Collection<IssueRecordEntity> issues) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        SnapshotBundle bundle = loadSnapshots(issues, List.of());
        return issues.stream()
                .map(item -> toIssueView(item, bundle))
                .toList();
    }

    private IssueView toIssueView(IssueRecordEntity issue) {
        return toIssueView(issue, loadSnapshots(List.of(issue), List.of()));
    }

    private IssueView toIssueView(IssueRecordEntity issue, SnapshotBundle bundle) {
        UserSnapshotEntity reporter = bundle.usersById.get(issue.getReporterId());
        UserSnapshotEntity handler = bundle.usersById.get(issue.getCurrentHandlerId());
        ClassSnapshotEntity clazz = bundle.classesById.get(issue.getClassId());
        ExamSnapshotEntity exam = bundle.examsById.get(issue.getExamId());
        return IssueView.builder()
                .issueId(issue.getIssueId())
                .type(issue.getType())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .status(issue.getStatus())
                .reporterId(issue.getReporterId())
                .reporterName(reporter == null ? null : reporter.getRealName())
                .currentHandlerId(issue.getCurrentHandlerId())
                .currentHandlerName(handler == null ? null : handler.getRealName())
                .examId(issue.getExamId())
                .examName(exam == null ? null : exam.getExamName())
                .classId(issue.getClassId())
                .className(clazz == null ? null : clazz.getClassName())
                .latestResult(issue.getLatestResult())
                .latestSolution(issue.getLatestSolution())
                .imgUrls(parseImgUrls(issue.getImgUrls()))
                .createTime(issue.getCreateTime())
                .updateTime(issue.getUpdateTime())
                .build();
    }

    private IssueTrackView toTrackView(IssueRecordEntity issue, List<IssueProcessLogEntity> logs) {
        SnapshotBundle bundle = loadSnapshots(List.of(issue), logs);
        return IssueTrackView.builder()
                .issueId(issue.getIssueId())
                .type(issue.getType())
                .title(issue.getTitle())
                .status(issue.getStatus())
                .reporterId(issue.getReporterId())
                .reporterName(nameOf(issue.getReporterId(), bundle.usersById))
                .currentHandlerId(issue.getCurrentHandlerId())
                .currentHandlerName(nameOf(issue.getCurrentHandlerId(), bundle.usersById))
                .examId(issue.getExamId())
                .examName(bundle.examsById.containsKey(issue.getExamId()) ? bundle.examsById.get(issue.getExamId()).getExamName() : null)
                .classId(issue.getClassId())
                .className(bundle.classesById.containsKey(issue.getClassId()) ? bundle.classesById.get(issue.getClassId()).getClassName() : null)
                .latestResult(issue.getLatestResult())
                .latestSolution(issue.getLatestSolution())
                .imgUrls(parseImgUrls(issue.getImgUrls()))
                .createTime(issue.getCreateTime())
                .updateTime(issue.getUpdateTime())
                .logs(logs.stream().map(item -> toProcessLogView(item, bundle.usersById)).toList())
                .build();
    }

    private IssueProcessLogView toProcessLogView(IssueProcessLogEntity log, Map<Long, UserSnapshotEntity> usersById) {
        return IssueProcessLogView.builder()
                .logId(log.getLogId())
                .action(log.getAction())
                .operatorId(log.getOperatorId())
                .operatorName(nameOf(log.getOperatorId(), usersById))
                .fromHandlerId(log.getFromHandlerId())
                .fromHandlerName(nameOf(log.getFromHandlerId(), usersById))
                .toHandlerId(log.getToHandlerId())
                .toHandlerName(nameOf(log.getToHandlerId(), usersById))
                .content(log.getContent())
                .occurredAt(log.getCreateTime())
                .build();
    }

    private String nameOf(Long userId, Map<Long, UserSnapshotEntity> usersById) {
        UserSnapshotEntity user = usersById.get(userId);
        return user == null ? null : user.getRealName();
    }

    private SnapshotBundle loadSnapshots(Collection<IssueRecordEntity> issues, Collection<IssueProcessLogEntity> logs) {
        Set<Long> classIds = issues.stream()
                .map(IssueRecordEntity::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> examIds = issues.stream()
                .map(IssueRecordEntity::getExamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> userIds = new LinkedHashSet<>();
        for (IssueRecordEntity issue : issues) {
            if (issue.getReporterId() != null) {
                userIds.add(issue.getReporterId());
            }
            if (issue.getCurrentHandlerId() != null) {
                userIds.add(issue.getCurrentHandlerId());
            }
        }
        for (IssueProcessLogEntity log : logs) {
            if (log.getOperatorId() != null) {
                userIds.add(log.getOperatorId());
            }
            if (log.getFromHandlerId() != null) {
                userIds.add(log.getFromHandlerId());
            }
            if (log.getToHandlerId() != null) {
                userIds.add(log.getToHandlerId());
            }
        }
        Map<Long, ClassSnapshotEntity> classesById = repository.findClassesByIds(classIds).stream()
                .collect(Collectors.toMap(ClassSnapshotEntity::getClassId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, ExamSnapshotEntity> examsById = repository.findExamsByIds(examIds).stream()
                .collect(Collectors.toMap(ExamSnapshotEntity::getExamId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, UserSnapshotEntity> usersById = repository.findUsersByIds(userIds).stream()
                .collect(Collectors.toMap(UserSnapshotEntity::getUserId, item -> item, (left, right) -> left, LinkedHashMap::new));
        return new SnapshotBundle(classesById, examsById, usersById);
    }

    private record SnapshotBundle(Map<Long, ClassSnapshotEntity> classesById,
                                  Map<Long, ExamSnapshotEntity> examsById,
                                  Map<Long, UserSnapshotEntity> usersById) {
    }
}
