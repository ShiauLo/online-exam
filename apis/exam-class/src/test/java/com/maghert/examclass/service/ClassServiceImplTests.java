package com.maghert.examclass.service;

import cn.hutool.core.lang.Snowflake;
import com.maghert.examclass.context.RequestContext;
import com.maghert.examclass.model.ClassMemberStatus;
import com.maghert.examclass.model.ExamClassAuditLogEntity;
import com.maghert.examclass.model.ExamClassEntity;
import com.maghert.examclass.model.ExamClassImportRecordEntity;
import com.maghert.examclass.model.ExamClassMemberEntity;
import com.maghert.examclass.model.dto.ClassApplyJoinRequest;
import com.maghert.examclass.model.dto.ClassApproveJoinRequest;
import com.maghert.examclass.model.dto.ClassCreateRequest;
import com.maghert.examclass.model.dto.ClassDeleteRequest;
import com.maghert.examclass.model.dto.ClassQuitRequest;
import com.maghert.examclass.model.dto.ClassQueryRequest;
import com.maghert.examclass.model.dto.ClassRemoveStudentRequest;
import com.maghert.examclass.model.vo.ClassQueryItemVO;
import com.maghert.examclass.repository.ClassDomainRepository;
import com.maghert.examclass.service.impl.ClassServiceImpl;
import com.maghert.examcommon.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassServiceImplTests {

    private InMemoryClassDomainRepository repository;
    private ClassServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryClassDomainRepository();
        service = new ClassServiceImpl(new Snowflake(1, 1), repository);
    }

    @Test
    void teacherShouldRespectOwnedClassLimit() throws Exception {
        RequestContext teacher = new RequestContext(3001L, 3, "req-teacher");
        service.create(classCreateRequest("Class A", false), teacher);

        assertEquals(1, repository.auditLogs.size());
        assertEquals("class.create", repository.auditLogs.get(0).getActionType());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.create(classCreateRequest("Class B", false), teacher));
        assertEquals(409, exception.getCode());
    }

    @Test
    void studentShouldRespectJoinLimit() throws Exception {
        RequestContext admin = new RequestContext(2001L, 2, "req-admin");
        RequestContext student = new RequestContext(4001L, 4, "req-student");
        for (int i = 1; i <= 3; i++) {
            Long classId = ((Number) ((Map<?, ?>) service.create(classCreateRequest("Class " + i, false), admin)
                    .getData()).get("classId")).longValue();
            repository.saveMember(ExamClassMemberEntity.builder()
                    .memberId((long) i)
                    .classId(classId)
                    .studentId(4001L)
                    .status(ClassMemberStatus.APPROVED)
                    .applyTime(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build());
        }

        Long nextClassId = ((Number) ((Map<?, ?>) service.create(classCreateRequest("Class 4", false), admin)
                .getData()).get("classId")).longValue();
        String classCode = repository.findClassById(nextClassId).orElseThrow().getClassCode();
        ClassApplyJoinRequest request = new ClassApplyJoinRequest();
        request.setClassCode(classCode);
        request.setStudentId(4001L);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.applyJoin(request, student));
        assertEquals(409, exception.getCode());
    }

    @Test
    void forcedClassShouldRejectQuit() throws Exception {
        RequestContext admin = new RequestContext(2001L, 2, "req-admin");
        RequestContext student = new RequestContext(4001L, 4, "req-student");
        Long classId = ((Number) ((Map<?, ?>) service.create(classCreateRequest("Forced Class", true), admin)
                .getData()).get("classId")).longValue();
        repository.saveMember(ExamClassMemberEntity.builder()
                .memberId(1L)
                .classId(classId)
                .studentId(4001L)
                .status(ClassMemberStatus.APPROVED)
                .applyTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());

        ClassQuitRequest request = new ClassQuitRequest();
        request.setClassId(classId);
        request.setStudentId(4001L);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.quit(request, student));
        assertEquals(403, exception.getCode());
    }

    @Test
    void teacherShouldNotOperateOtherTeachersClass() throws Exception {
        RequestContext admin = new RequestContext(2001L, 2, "req-admin");
        RequestContext teacher2 = new RequestContext(3002L, 3, "req-teacher-2");
        Long classId = ((Number) ((Map<?, ?>) service.create(classCreateRequest("Other Class", false), admin)
                .getData()).get("classId")).longValue();

        ClassRemoveStudentRequest request = new ClassRemoveStudentRequest();
        request.setClassId(classId);
        request.setStudentId(4001L);
        request.setReason("remove");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.removeStudent(request, teacher2));
        assertEquals(403, exception.getCode());
    }

    @Test
    void deleteShouldFailWhenApprovedStudentStillExists() throws Exception {
        RequestContext admin = new RequestContext(2001L, 2, "req-admin");
        Long classId = ((Number) ((Map<?, ?>) service.create(classCreateRequest("Has Student", false), admin)
                .getData()).get("classId")).longValue();
        repository.saveMember(ExamClassMemberEntity.builder()
                .memberId(1L)
                .classId(classId)
                .studentId(4001L)
                .status(ClassMemberStatus.APPROVED)
                .applyTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());

        ClassDeleteRequest request = new ClassDeleteRequest();
        request.setClassId(classId);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.delete(request, admin));
        assertEquals(409, exception.getCode());
    }

    @Test
    void importAndExportShouldUseRepositoryState() throws Exception {
        RequestContext admin = new RequestContext(2001L, 2, "req-admin");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "classes.csv",
                "text/csv",
                "className,description,teacherId,forced\nJava 1,desc,3001,false\nJava 2,desc2,,true"
                        .getBytes(StandardCharsets.UTF_8));

        Map<?, ?> payload = (Map<?, ?>) service.importClasses(file, 3002L, admin).getData();

        assertEquals(2, payload.get("importedCount"));
        assertEquals(0, payload.get("skippedCount"));
        assertEquals(1, repository.importRecords.size());

        String exported = service.export(null, admin);
        assertTrue(exported.contains("Java 1"));
        assertTrue(exported.contains("Java 2"));
        assertTrue(exported.contains("approvedMemberCount"));
        assertTrue(exported.contains("description"));
        assertTrue(exported.contains("createdBy"));
        assertFalse(exported.isBlank());
    }

    @Test
    void importShouldAcceptExportHeaderTemplate() throws Exception {
        RequestContext admin = new RequestContext(2001L, 2, "req-admin");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "classes-export.csv",
                "text/csv",
                ("classId,classCode,className,description,status,teacherId,forced,approvedMemberCount,pendingMemberCount,createdBy,createTime,updateTime\n" +
                        "1,CLS00000001,Java Exported,desc,active,3001,true,0,0,2001,2026-04-03T10:00:00,2026-04-03T10:00:00")
                        .getBytes(StandardCharsets.UTF_8));

        Map<?, ?> payload = (Map<?, ?>) service.importClasses(file, null, admin).getData();

        assertEquals(1, payload.get("importedCount"));
        assertEquals(0, payload.get("skippedCount"));
        assertTrue(repository.queryClasses(new ClassQueryRequest()).stream()
                .anyMatch(item -> "Java Exported".equals(item.getClassName()) && Boolean.TRUE.equals(item.getForced())));
    }

    @Test
    void queryShouldReturnApprovedAndPendingMemberCounts() throws Exception {
        RequestContext admin = new RequestContext(2001L, 2, "req-admin");
        Long classId = ((Number) ((Map<?, ?>) service.create(classCreateRequest("Counted Class", false), admin)
                .getData()).get("classId")).longValue();
        repository.saveMember(ExamClassMemberEntity.builder()
                .memberId(1L)
                .classId(classId)
                .studentId(4001L)
                .status(ClassMemberStatus.APPROVED)
                .applyTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());
        repository.saveMember(ExamClassMemberEntity.builder()
                .memberId(2L)
                .classId(classId)
                .studentId(4002L)
                .status(ClassMemberStatus.PENDING)
                .applyTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());

        @SuppressWarnings("unchecked")
        List<ClassQueryItemVO> records = (List<ClassQueryItemVO>) service.query(new ClassQueryRequest(), admin).getData();

        ClassQueryItemVO target = records.stream()
                .filter(item -> classId.equals(item.getClassId()))
                .findFirst()
                .orElseThrow();
        assertEquals(1L, target.getApprovedMemberCount());
        assertEquals(1L, target.getPendingMemberCount());
    }

    @Test
    void approveJoinShouldSupportBatchStudentIds() throws Exception {
        RequestContext admin = new RequestContext(2001L, 2, "req-admin");
        Long classId = ((Number) ((Map<?, ?>) service.create(classCreateRequest("Batch Class", false), admin)
                .getData()).get("classId")).longValue();
        repository.saveMember(ExamClassMemberEntity.builder()
                .memberId(1L)
                .classId(classId)
                .studentId(4001L)
                .status(ClassMemberStatus.PENDING)
                .applyTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());
        repository.saveMember(ExamClassMemberEntity.builder()
                .memberId(2L)
                .classId(classId)
                .studentId(4002L)
                .status(ClassMemberStatus.PENDING)
                .applyTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());

        ClassApproveJoinRequest request = new ClassApproveJoinRequest();
        request.setClassId(classId);
        request.setStudentIds(List.of(4001L, 4002L));
        request.setApproveResult("approve");

        service.approveJoin(request, admin);

        assertEquals(ClassMemberStatus.APPROVED, repository.findMember(classId, 4001L).orElseThrow().getStatus());
        assertEquals(ClassMemberStatus.APPROVED, repository.findMember(classId, 4002L).orElseThrow().getStatus());
        assertEquals(3, repository.auditLogs.size());
        assertEquals(List.of("class.create", "class.approve-join", "class.approve-join"),
                repository.auditLogs.stream().map(ExamClassAuditLogEntity::getActionType).toList());
    }

    @Test
    void approveJoinShouldRequireAtLeastOneStudentTarget() throws Exception {
        RequestContext admin = new RequestContext(2001L, 2, "req-admin");
        Long classId = ((Number) ((Map<?, ?>) service.create(classCreateRequest("Empty Batch Class", false), admin)
                .getData()).get("classId")).longValue();

        ClassApproveJoinRequest request = new ClassApproveJoinRequest();
        request.setClassId(classId);
        request.setApproveResult("approve");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.approveJoin(request, admin));
        assertEquals(400, exception.getCode());
    }

    private ClassCreateRequest classCreateRequest(String name, boolean forced) {
        ClassCreateRequest request = new ClassCreateRequest();
        request.setClassName(name);
        request.setForced(forced);
        request.setTeacherId(3001L);
        return request;
    }

    private static final class InMemoryClassDomainRepository implements ClassDomainRepository {

        private final Map<Long, ExamClassEntity> classes = new LinkedHashMap<>();
        private final Map<Long, ExamClassMemberEntity> members = new LinkedHashMap<>();
        private final List<ExamClassImportRecordEntity> importRecords = new ArrayList<>();
        private final List<ExamClassAuditLogEntity> auditLogs = new ArrayList<>();

        @Override
        public long countCreatedClasses(Long creatorId) {
            return classes.values().stream().filter(item -> creatorId.equals(item.getCreatedBy())).count();
        }

        @Override
        public ExamClassEntity saveClass(ExamClassEntity examClass) {
            classes.put(examClass.getClassId(), examClass);
            return examClass;
        }

        @Override
        public void updateClass(ExamClassEntity examClass) {
            classes.put(examClass.getClassId(), examClass);
        }

        @Override
        public Optional<ExamClassEntity> findClassById(Long classId) {
            return Optional.ofNullable(classes.get(classId));
        }

        @Override
        public Optional<ExamClassEntity> findClassByCode(String classCode) {
            return classes.values().stream()
                    .filter(item -> classCode.equals(item.getClassCode()))
                    .findFirst();
        }

        @Override
        public List<ExamClassEntity> queryClasses(ClassQueryRequest request) {
            return classes.values().stream()
                    .filter(item -> request.getClassId() == null || request.getClassId().equals(item.getClassId()))
                    .filter(item -> request.getTeacherId() == null || request.getTeacherId().equals(item.getTeacherId()))
                    .filter(item -> request.getKeyword() == null || item.getClassName().contains(request.getKeyword()))
                    .filter(item -> request.getStatus() == null || request.getStatus().equals(item.getStatus()))
                    .sorted(Comparator.comparing(ExamClassEntity::getCreateTime).reversed())
                    .toList();
        }

        @Override
        public List<ExamClassEntity> findClassesByIds(Collection<Long> classIds) {
            return classes.values().stream()
                    .filter(item -> classIds.contains(item.getClassId()))
                    .toList();
        }

        @Override
        public void deleteClass(Long classId) {
            classes.remove(classId);
        }

        @Override
        public ExamClassMemberEntity saveMember(ExamClassMemberEntity member) {
            members.put(member.getMemberId(), member);
            return member;
        }

        @Override
        public void updateMember(ExamClassMemberEntity member) {
            members.put(member.getMemberId(), member);
        }

        @Override
        public Optional<ExamClassMemberEntity> findMember(Long classId, Long studentId) {
            return members.values().stream()
                    .filter(item -> classId.equals(item.getClassId()) && studentId.equals(item.getStudentId()))
                    .findFirst();
        }

        @Override
        public List<ExamClassMemberEntity> findMembersByStudentId(Long studentId) {
            return members.values().stream()
                    .filter(item -> studentId.equals(item.getStudentId()))
                    .collect(Collectors.toList());
        }

        @Override
        public long countApprovedMembers(Long classId) {
            return members.values().stream()
                    .filter(item -> classId.equals(item.getClassId()) && item.getStatus() == ClassMemberStatus.APPROVED)
                    .count();
        }

        @Override
        public long countPendingMembers(Long classId) {
            return members.values().stream()
                    .filter(item -> classId.equals(item.getClassId()) && item.getStatus() == ClassMemberStatus.PENDING)
                    .count();
        }

        @Override
        public long countApprovedMemberships(Long studentId) {
            return members.values().stream()
                    .filter(item -> studentId.equals(item.getStudentId()) && item.getStatus() == ClassMemberStatus.APPROVED)
                    .count();
        }

        @Override
        public void deleteMembersByClassId(Long classId) {
            members.entrySet().removeIf(entry -> classId.equals(entry.getValue().getClassId()));
        }

        @Override
        public void saveImportRecord(ExamClassImportRecordEntity record) {
            importRecords.add(record);
        }

        @Override
        public void saveAuditLog(ExamClassAuditLogEntity auditLog) {
            auditLogs.add(auditLog);
        }
    }
}
