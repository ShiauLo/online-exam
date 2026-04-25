package com.maghert.examissuecore.service;

import cn.hutool.core.util.IdUtil;
import com.maghert.examcommon.exception.BusinessException;
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
import com.maghert.examissuecore.model.vo.IssueTrackView;
import com.maghert.examissuecore.model.vo.IssueView;
import com.maghert.examissuecore.repository.IssueDomainRepository;
import com.maghert.examissuecore.service.impl.ExamIssueCoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamIssueCoreServiceImplTests {

    private static final RequestContext ADMIN_CONTEXT = new RequestContext(2001L, 2, "req-admin");
    private static final RequestContext TEACHER_CONTEXT = new RequestContext(3001L, 3, "req-teacher");
    private static final RequestContext FOREIGN_TEACHER_CONTEXT = new RequestContext(3002L, 3, "req-teacher-2");
    private static final RequestContext STUDENT_CONTEXT = new RequestContext(4001L, 4, "req-student");
    private static final RequestContext AUDITOR_CONTEXT = new RequestContext(5001L, 5, "req-auditor");
    private static final RequestContext OPS_CONTEXT = new RequestContext(6001L, 6, "req-ops");

    private InMemoryIssueDomainRepository repository;
    private ExamIssueCoreServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryIssueDomainRepository();
        service = new ExamIssueCoreServiceImpl(repository, IdUtil.getSnowflake(1, 1));
        seedBaseData();
    }

    @Test
    void createShouldSupportAllThreeTypesAndValidateExamScope() throws Exception {
        IssueCreateRequest businessRequest = new IssueCreateRequest();
        businessRequest.setType("business");
        businessRequest.setTitle("业务问题");
        businessRequest.setDesc("成绩页面字段异常");

        IssueView businessIssue = service.create(businessRequest, STUDENT_CONTEXT).getData();
        assertEquals("BUSINESS", businessIssue.getType());
        assertEquals("PENDING", businessIssue.getStatus());
        assertEquals(4001L, businessIssue.getReporterId());

        IssueCreateRequest examRequest = new IssueCreateRequest();
        examRequest.setType("exam");
        examRequest.setTitle("考试问题");
        examRequest.setDesc("考试页面异常");
        examRequest.setExamId(8801L);
        examRequest.setClassId(501L);
        examRequest.setImgUrls(List.of("img-1", "img-2"));

        IssueView examIssue = service.create(examRequest, STUDENT_CONTEXT).getData();
        assertEquals("EXAM", examIssue.getType());
        assertEquals(2, examIssue.getImgUrls().size());
        assertEquals("Java 期中", examIssue.getExamName());
        assertEquals("Java 1班", examIssue.getClassName());

        IssueCreateRequest systemRequest = new IssueCreateRequest();
        systemRequest.setType("system");
        systemRequest.setTitle("系统问题");
        systemRequest.setDesc("备份失败");
        IssueView systemIssue = service.create(systemRequest, OPS_CONTEXT).getData();
        assertEquals("SYSTEM", systemIssue.getType());

        IssueCreateRequest invalidExamRequest = new IssueCreateRequest();
        invalidExamRequest.setType("exam");
        invalidExamRequest.setTitle("缺少范围");
        invalidExamRequest.setDesc("未带 examId");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create(invalidExamRequest, STUDENT_CONTEXT));
        assertEquals(400, exception.getCode());
        assertEquals(1811, exception.getErrorCode());
    }

    @Test
    void queryShouldRespectVisibilityForReporterHandlerAndGlobalReader() throws Exception {
        IssueView businessIssue = service.create(createRequest("business", "业务问题", "成绩有误", null, null), STUDENT_CONTEXT).getData();
        IssueView examIssue = service.create(createRequest("exam", "考试问题", "试卷加载失败", 8801L, 501L), STUDENT_CONTEXT).getData();
        IssueView systemIssue = service.create(createRequest("system", "系统问题", "日志服务告警", null, null), TEACHER_CONTEXT).getData();

        service.handle(handleRequest(examIssue.getIssueId(), "处理中", "已排查"), TEACHER_CONTEXT);
        service.handle(handleRequest(systemIssue.getIssueId(), "处理中", "运维处理中"), OPS_CONTEXT);

        PageResult<IssueView> studentPage = service.query(queryRequest(), STUDENT_CONTEXT).getData();
        assertEquals(2, studentPage.getTotal());
        assertTrue(studentPage.getRecords().stream().allMatch(item -> Objects.equals(item.getReporterId(), 4001L)));

        PageResult<IssueView> teacherPage = service.query(queryRequest(), TEACHER_CONTEXT).getData();
        assertEquals(2, teacherPage.getTotal());
        assertTrue(teacherPage.getRecords().stream().anyMatch(item -> Objects.equals(item.getIssueId(), examIssue.getIssueId())));
        assertTrue(teacherPage.getRecords().stream().anyMatch(item -> Objects.equals(item.getIssueId(), systemIssue.getIssueId())));

        PageResult<IssueView> auditorPage = service.query(queryRequest(), AUDITOR_CONTEXT).getData();
        assertEquals(3, auditorPage.getTotal());

        PageResult<IssueView> opsPage = service.query(queryRequest(), OPS_CONTEXT).getData();
        assertEquals(1, opsPage.getTotal());
        assertEquals(systemIssue.getIssueId(), opsPage.getRecords().get(0).getIssueId());

        IssueQueryRequest filter = queryRequest();
        filter.setType("exam");
        PageResult<IssueView> adminExamOnly = service.query(filter, ADMIN_CONTEXT).getData();
        assertEquals(1, adminExamOnly.getTotal());
        assertEquals("EXAM", adminExamOnly.getRecords().get(0).getType());

        assertNotNull(businessIssue);
    }

    @Test
    void teacherQueryShouldExposePendingExamIssuesInVisibleScope() throws Exception {
        IssueView pendingExamIssue = service.create(createRequest("exam", "待接单问题", "教师页应显示", 8801L, 501L), STUDENT_CONTEXT).getData();

        PageResult<IssueView> teacherPage = service.query(queryRequest(), TEACHER_CONTEXT).getData();
        assertTrue(teacherPage.getRecords().stream().anyMatch(item -> Objects.equals(item.getIssueId(), pendingExamIssue.getIssueId())));
        assertTrue(teacherPage.getRecords().stream().anyMatch(item -> "PENDING".equals(item.getStatus())));
    }

    @Test
    void handleShouldEnforceRoleAndTeacherScope() throws Exception {
        IssueView businessIssue = service.create(createRequest("business", "业务问题", "账户异常", null, null), STUDENT_CONTEXT).getData();
        IssueView examIssue = service.create(createRequest("exam", "考试问题", "监考异常", 8801L, 501L), STUDENT_CONTEXT).getData();
        IssueView foreignExamIssue = service.create(createRequest("exam", "外班考试问题", "无权班级", 8802L, 502L), STUDENT_CONTEXT).getData();
        IssueView systemIssue = service.create(createRequest("system", "系统问题", "服务异常", null, null), STUDENT_CONTEXT).getData();

        IssueView handledBusiness = service.handle(handleRequest(businessIssue.getIssueId(), "已受理", "管理员跟进"), ADMIN_CONTEXT)
                .getData();
        assertEquals("PROCESSING", handledBusiness.getStatus());
        assertEquals(2001L, handledBusiness.getCurrentHandlerId());

        IssueView handledExam = service.handle(handleRequest(examIssue.getIssueId(), "处理中", "教师介入"), TEACHER_CONTEXT)
                .getData();
        assertEquals(3001L, handledExam.getCurrentHandlerId());
        assertEquals("处理中", handledExam.getLatestResult());

        IssueView handledSystem = service.handle(handleRequest(systemIssue.getIssueId(), "处理中", "运维排查"), OPS_CONTEXT)
                .getData();
        assertEquals(6001L, handledSystem.getCurrentHandlerId());

        BusinessException foreignTeacherException = assertThrows(BusinessException.class,
                () -> service.handle(handleRequest(foreignExamIssue.getIssueId(), "处理中", "越权"), TEACHER_CONTEXT));
        assertEquals(403, foreignTeacherException.getCode());
        assertEquals(1804, foreignTeacherException.getErrorCode());

        BusinessException studentException = assertThrows(BusinessException.class,
                () -> service.handle(handleRequest(examIssue.getIssueId(), "处理中", "学生越权"), STUDENT_CONTEXT));
        assertEquals(403, studentException.getCode());
        assertEquals(1804, studentException.getErrorCode());
    }

    @Test
    void transferAndCloseShouldRequireCurrentHandlerAndConfirmedStatus() throws Exception {
        IssueView examIssue = service.create(createRequest("exam", "考试问题", "试卷空白", 8801L, 501L), STUDENT_CONTEXT).getData();
        service.handle(handleRequest(examIssue.getIssueId(), "处理中", "教师已接手"), TEACHER_CONTEXT);

        IssueTransferRequest transferRequest = new IssueTransferRequest();
        transferRequest.setIssueId(examIssue.getIssueId());
        transferRequest.setToHandlerId(3003L);
        transferRequest.setReason("转派给备课组老师");
        IssueView transferredIssue = service.transfer(transferRequest, TEACHER_CONTEXT).getData();
        assertEquals(3003L, transferredIssue.getCurrentHandlerId());
        assertEquals("PROCESSING", transferredIssue.getStatus());

        BusinessException foreignTransferException = assertThrows(BusinessException.class,
                () -> service.transfer(transferRequest, FOREIGN_TEACHER_CONTEXT));
        assertEquals(403, foreignTransferException.getCode());
        assertEquals(1805, foreignTransferException.getErrorCode());

        IssueHandleRequest reHandle = handleRequest(examIssue.getIssueId(), "继续处理", "新教师接手");
        IssueView reHandledIssue = service.handle(reHandle, new RequestContext(3003L, 3, "req-teacher-3")).getData();
        assertEquals(3003L, reHandledIssue.getCurrentHandlerId());

        IssueCloseRequest invalidClose = new IssueCloseRequest();
        invalidClose.setIssueId(examIssue.getIssueId());
        invalidClose.setConfirmResult("rejected");
        invalidClose.setComment("申请人未确认");
        BusinessException invalidCloseException = assertThrows(BusinessException.class,
                () -> service.close(invalidClose, new RequestContext(3003L, 3, "req-close-invalid")));
        assertEquals(400, invalidCloseException.getCode());
        assertEquals(1809, invalidCloseException.getErrorCode());

        IssueCloseRequest closeRequest = new IssueCloseRequest();
        closeRequest.setIssueId(examIssue.getIssueId());
        closeRequest.setConfirmResult("confirmed");
        closeRequest.setComment("申请人确认已解决");
        IssueView closedIssue = service.close(closeRequest, new RequestContext(3003L, 3, "req-close")) .getData();
        assertEquals("CLOSED", closedIssue.getStatus());

        BusinessException closedAgainException = assertThrows(BusinessException.class,
                () -> service.handle(handleRequest(examIssue.getIssueId(), "重复处理", "不应允许"),
                new RequestContext(3003L, 3, "req-rehandle")));
        assertEquals(409, closedAgainException.getCode());
        assertEquals(1803, closedAgainException.getErrorCode());
    }

    @Test
    void trackShouldReturnOrderedLogsAndEnforceVisibility() throws Exception {
        IssueView examIssue = service.create(createRequest("exam", "考试问题", "提交异常", 8801L, 501L), STUDENT_CONTEXT).getData();
        service.handle(handleRequest(examIssue.getIssueId(), "处理中", "教师排查"), TEACHER_CONTEXT);

        IssueTransferRequest transferRequest = new IssueTransferRequest();
        transferRequest.setIssueId(examIssue.getIssueId());
        transferRequest.setToHandlerId(3003L);
        transferRequest.setReason("转派给组长");
        service.transfer(transferRequest, TEACHER_CONTEXT);
        service.handle(handleRequest(examIssue.getIssueId(), "继续处理", "组长处理"), new RequestContext(3003L, 3, "req-leader"));

        IssueCloseRequest closeRequest = new IssueCloseRequest();
        closeRequest.setIssueId(examIssue.getIssueId());
        closeRequest.setConfirmResult("confirmed");
        closeRequest.setComment("已确认");
        service.close(closeRequest, new RequestContext(3003L, 3, "req-close"));

        IssueTrackRequest trackRequest = new IssueTrackRequest();
        trackRequest.setIssueId(examIssue.getIssueId());
        IssueTrackView trackView = service.track(trackRequest, STUDENT_CONTEXT).getData();

        List<String> actions = trackView.getLogs().stream()
                .map(item -> item.getAction())
                .collect(Collectors.toList());
        assertEquals(List.of("CREATED", "HANDLED", "TRANSFERRED", "HANDLED", "CLOSED"), actions);
        assertEquals("CLOSED", trackView.getStatus());
        assertEquals("张三", trackView.getReporterName());
        assertEquals("组长老师", trackView.getCurrentHandlerName());

        BusinessException opsException = assertThrows(BusinessException.class,
                () -> service.track(trackRequest, OPS_CONTEXT));
        assertEquals(403, opsException.getCode());
        assertEquals(1808, opsException.getErrorCode());
    }

    private void seedBaseData() {
        repository.users.put(1001L, user(1001L, "超级管理员", 1));
        repository.users.put(2001L, user(2001L, "普通管理员", 2));
        repository.users.put(3001L, user(3001L, "李老师", 3));
        repository.users.put(3002L, user(3002L, "王老师", 3));
        repository.users.put(3003L, user(3003L, "组长老师", 3));
        repository.users.put(4001L, user(4001L, "张三", 4));
        repository.users.put(5001L, user(5001L, "审计员", 5));
        repository.users.put(6001L, user(6001L, "运维工程师", 6));

        repository.classes.put(501L, new ClassSnapshotEntity()
                .setClassId(501L)
                .setClassName("Java 1班")
                .setTeacherId(3001L)
                .setCreatedBy(3001L)
                .setStatus("ACTIVE"));
        repository.classes.put(502L, new ClassSnapshotEntity()
                .setClassId(502L)
                .setClassName("Python 1班")
                .setTeacherId(3002L)
                .setCreatedBy(3002L)
                .setStatus("ACTIVE"));

        repository.exams.put(8801L, new ExamSnapshotEntity()
                .setExamId(8801L)
                .setExamName("Java 期中")
                .setCreatorId(3001L)
                .setStatus("PUBLISHED"));
        repository.exams.put(8802L, new ExamSnapshotEntity()
                .setExamId(8802L)
                .setExamName("Python 期中")
                .setCreatorId(3002L)
                .setStatus("PUBLISHED"));

        repository.examClassRelations.add(new ExamClassRelationEntity().setRelationId(1L).setExamId(8801L).setClassId(501L));
        repository.examClassRelations.add(new ExamClassRelationEntity().setRelationId(2L).setExamId(8802L).setClassId(502L));
    }

    private UserSnapshotEntity user(Long userId, String realName, Integer roleId) {
        return new UserSnapshotEntity()
                .setUserId(userId)
                .setRealName(realName)
                .setRoleId(roleId);
    }

    private IssueCreateRequest createRequest(String type, String title, String desc, Long examId, Long classId) {
        IssueCreateRequest request = new IssueCreateRequest();
        request.setType(type);
        request.setTitle(title);
        request.setDesc(desc);
        request.setExamId(examId);
        request.setClassId(classId);
        return request;
    }

    private IssueHandleRequest handleRequest(Long issueId, String result, String solution) {
        IssueHandleRequest request = new IssueHandleRequest();
        request.setIssueId(issueId);
        request.setResult(result);
        request.setSolution(solution);
        return request;
    }

    private IssueQueryRequest queryRequest() {
        IssueQueryRequest request = new IssueQueryRequest();
        request.setPageNum(1L);
        request.setPageSize(20L);
        return request;
    }

    private static class InMemoryIssueDomainRepository implements IssueDomainRepository {

        private final Map<Long, IssueRecordEntity> issues = new LinkedHashMap<>();
        private final Map<Long, List<IssueProcessLogEntity>> logsByIssueId = new LinkedHashMap<>();
        private final Map<Long, ClassSnapshotEntity> classes = new LinkedHashMap<>();
        private final Map<Long, ExamSnapshotEntity> exams = new LinkedHashMap<>();
        private final List<ExamClassRelationEntity> examClassRelations = new ArrayList<>();
        private final Map<Long, UserSnapshotEntity> users = new LinkedHashMap<>();

        @Override
        public IssueRecordEntity saveIssue(IssueRecordEntity issue) {
            issues.put(issue.getIssueId(), cloneIssue(issue));
            return issue;
        }

        @Override
        public IssueRecordEntity updateIssue(IssueRecordEntity issue) {
            issues.put(issue.getIssueId(), cloneIssue(issue));
            return issue;
        }

        @Override
        public Optional<IssueRecordEntity> findIssueById(Long issueId) {
            return Optional.ofNullable(issues.get(issueId)).map(this::cloneIssue);
        }

        @Override
        public List<IssueRecordEntity> listIssues() {
            return issues.values().stream()
                    .map(this::cloneIssue)
                    .sorted(Comparator.comparing(IssueRecordEntity::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        }

        @Override
        public IssueProcessLogEntity saveProcessLog(IssueProcessLogEntity log) {
            logsByIssueId.computeIfAbsent(log.getIssueId(), key -> new ArrayList<>()).add(cloneLog(log));
            return log;
        }

        @Override
        public List<IssueProcessLogEntity> listProcessLogs(Long issueId) {
            return logsByIssueId.getOrDefault(issueId, List.of()).stream()
                    .map(this::cloneLog)
                    .sorted(Comparator.comparing(IssueProcessLogEntity::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
        }

        @Override
        public List<ClassSnapshotEntity> findClassesByIds(Collection<Long> classIds) {
            if (classIds == null || classIds.isEmpty()) {
                return List.of();
            }
            Set<Long> ids = new LinkedHashSet<>(classIds);
            return classes.values().stream()
                    .filter(item -> ids.contains(item.getClassId()))
                    .map(this::cloneClass)
                    .collect(Collectors.toList());
        }

        @Override
        public List<ExamSnapshotEntity> findExamsByIds(Collection<Long> examIds) {
            if (examIds == null || examIds.isEmpty()) {
                return List.of();
            }
            Set<Long> ids = new LinkedHashSet<>(examIds);
            return exams.values().stream()
                    .filter(item -> ids.contains(item.getExamId()))
                    .map(this::cloneExam)
                    .collect(Collectors.toList());
        }

        @Override
        public List<ExamClassRelationEntity> listExamClasses(Collection<Long> examIds) {
            if (examIds == null || examIds.isEmpty()) {
                return List.of();
            }
            Set<Long> ids = new LinkedHashSet<>(examIds);
            return examClassRelations.stream()
                    .filter(item -> ids.contains(item.getExamId()))
                    .map(this::cloneExamClassRelation)
                    .collect(Collectors.toList());
        }

        @Override
        public List<UserSnapshotEntity> findUsersByIds(Collection<Long> userIds) {
            if (userIds == null || userIds.isEmpty()) {
                return List.of();
            }
            Set<Long> ids = new LinkedHashSet<>(userIds);
            return users.values().stream()
                    .filter(item -> ids.contains(item.getUserId()))
                    .map(this::cloneUser)
                    .collect(Collectors.toList());
        }

        private IssueRecordEntity cloneIssue(IssueRecordEntity source) {
            return new IssueRecordEntity()
                    .setIssueId(source.getIssueId())
                    .setType(source.getType())
                    .setTitle(source.getTitle())
                    .setDescription(source.getDescription())
                    .setStatus(source.getStatus())
                    .setReporterId(source.getReporterId())
                    .setCurrentHandlerId(source.getCurrentHandlerId())
                    .setExamId(source.getExamId())
                    .setClassId(source.getClassId())
                    .setLatestResult(source.getLatestResult())
                    .setLatestSolution(source.getLatestSolution())
                    .setImgUrls(source.getImgUrls())
                    .setAuditTrail(source.getAuditTrail())
                    .setCreateTime(source.getCreateTime())
                    .setUpdateTime(source.getUpdateTime());
        }

        private IssueProcessLogEntity cloneLog(IssueProcessLogEntity source) {
            return new IssueProcessLogEntity()
                    .setLogId(source.getLogId())
                    .setIssueId(source.getIssueId())
                    .setAction(source.getAction())
                    .setOperatorId(source.getOperatorId())
                    .setFromHandlerId(source.getFromHandlerId())
                    .setToHandlerId(source.getToHandlerId())
                    .setContent(source.getContent())
                    .setAuditTrail(source.getAuditTrail())
                    .setCreateTime(source.getCreateTime())
                    .setUpdateTime(source.getUpdateTime());
        }

        private ClassSnapshotEntity cloneClass(ClassSnapshotEntity source) {
            return new ClassSnapshotEntity()
                    .setClassId(source.getClassId())
                    .setClassName(source.getClassName())
                    .setTeacherId(source.getTeacherId())
                    .setCreatedBy(source.getCreatedBy())
                    .setStatus(source.getStatus());
        }

        private ExamSnapshotEntity cloneExam(ExamSnapshotEntity source) {
            return new ExamSnapshotEntity()
                    .setExamId(source.getExamId())
                    .setExamName(source.getExamName())
                    .setCreatorId(source.getCreatorId())
                    .setStatus(source.getStatus());
        }

        private ExamClassRelationEntity cloneExamClassRelation(ExamClassRelationEntity source) {
            return new ExamClassRelationEntity()
                    .setRelationId(source.getRelationId())
                    .setExamId(source.getExamId())
                    .setClassId(source.getClassId());
        }

        private UserSnapshotEntity cloneUser(UserSnapshotEntity source) {
            return new UserSnapshotEntity()
                    .setUserId(source.getUserId())
                    .setRealName(source.getRealName())
                    .setRoleId(source.getRoleId());
        }
    }
}
