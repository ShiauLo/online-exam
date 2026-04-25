package com.maghert.examclass.service.impl;

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
import com.maghert.examclass.model.dto.ClassQueryRequest;
import com.maghert.examclass.model.dto.ClassQuitRequest;
import com.maghert.examclass.model.dto.ClassRemoveStudentRequest;
import com.maghert.examclass.model.dto.ClassUpdateRequest;
import com.maghert.examclass.model.vo.ClassQueryItemVO;
import com.maghert.examclass.repository.ClassDomainRepository;
import com.maghert.examclass.service.ClassService;
import com.maghert.examcommon.auth.RoleMappings;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;
import com.maghert.examcommon.web.ApiResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClassServiceImpl implements ClassService {

    private final Snowflake snowflake;
    private final ClassDomainRepository repository;

    public ClassServiceImpl(Snowflake snowflake, ClassDomainRepository repository) {
        this.snowflake = snowflake;
        this.repository = repository;
    }

    @Override
    public ApiResponse<?> create(ClassCreateRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isAdmin(context.roleId()) && !RoleMappings.isTeacher(context.roleId())) {
            throw forbidden("无操作权限");
        }

        Long teacherId = request.getTeacherId();
        if (RoleMappings.isTeacher(context.roleId())) {
            teacherId = context.userId();
            if (repository.countCreatedClasses(context.userId()) >= 1) {
                throw new BusinessException(DomainErrorCode.TEACHER_CLASS_LIMIT_EXCEEDED);
            }
        }

        Long classId = snowflake.nextId();
        LocalDateTime now = LocalDateTime.now();
        ExamClassEntity examClass = ExamClassEntity.builder()
                .classId(classId)
                .classCode(buildClassCode(classId))
                .className(request.getClassName())
                .description(request.getDescription())
                .teacherId(teacherId)
                .forced(Boolean.TRUE.equals(request.getForced()))
                .status("active")
                .createdBy(context.userId())
                .createTime(now)
                .updateTime(now)
                .build();
        repository.saveClass(examClass);
        saveAuditLog(classId, null, context.userId(), "class.create", null,
                "classCode=%s,className=%s,status=%s".formatted(
                        examClass.getClassCode(), examClass.getClassName(), examClass.getStatus()),
                context.requestId(), now);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("classId", classId);
        payload.put("classCode", examClass.getClassCode());
        payload.put("status", examClass.getStatus());
        return ApiResponse.ok(payload).withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> query(ClassQueryRequest request, RequestContext context) {
        List<ExamClassEntity> source = repository.queryClasses(request);
        if (RoleMappings.isTeacher(context.roleId())) {
            source = source.stream()
                    .filter(item -> Objects.equals(context.userId(), item.getCreatedBy()))
                    .toList();
        } else if (RoleMappings.isStudent(context.roleId())) {
            Collection<Long> visibleClassIds = repository.findMembersByStudentId(context.userId()).stream()
                    .filter(item -> item.getStatus() == ClassMemberStatus.APPROVED || item.getStatus() == ClassMemberStatus.PENDING)
                    .map(ExamClassMemberEntity::getClassId)
                    .collect(Collectors.toSet());
            source = source.stream()
                    .filter(item -> visibleClassIds.contains(item.getClassId()))
                    .toList();
        } else if (request.getStudentId() != null) {
            Collection<Long> matchedClassIds = repository.findMembersByStudentId(request.getStudentId()).stream()
                    .map(ExamClassMemberEntity::getClassId)
                    .collect(Collectors.toSet());
            source = source.stream()
                    .filter(item -> matchedClassIds.contains(item.getClassId()))
                    .toList();
        }

        List<ClassQueryItemVO> result = source.stream()
                .sorted(Comparator.comparing(ExamClassEntity::getCreateTime).reversed())
                .map(item -> ClassQueryItemVO.builder()
                        .classId(item.getClassId())
                        .classCode(item.getClassCode())
                        .className(item.getClassName())
                        .description(item.getDescription())
                        .teacherId(item.getTeacherId())
                        .forced(item.getForced())
                        .status(item.getStatus())
                        .approvedMemberCount(repository.countApprovedMembers(item.getClassId()))
                        .pendingMemberCount(repository.countPendingMembers(item.getClassId()))
                        .build())
                .toList();
        return ApiResponse.ok(result).withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> update(ClassUpdateRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        ExamClassEntity examClass = getRequiredClass(request.getClassId());
        ensureClassOperator(context, examClass);
        if (StringUtils.hasText(request.getClassName())) {
            examClass.setClassName(request.getClassName());
        }
        if (request.getDescription() != null) {
            examClass.setDescription(request.getDescription());
        }
        if (RoleMappings.isAdmin(context.roleId()) && request.getTeacherId() != null) {
            examClass.setTeacherId(request.getTeacherId());
        }
        if (RoleMappings.isAdmin(context.roleId()) && request.getForced() != null) {
            examClass.setForced(request.getForced());
        }
        if (StringUtils.hasText(request.getStatus())) {
            examClass.setStatus(request.getStatus());
        }
        examClass.setUpdateTime(LocalDateTime.now());
        repository.updateClass(examClass);
        saveAuditLog(examClass.getClassId(), null, context.userId(), "class.update", null,
                "className=%s,status=%s,teacherId=%s,forced=%s".formatted(
                        examClass.getClassName(), examClass.getStatus(), examClass.getTeacherId(), examClass.getForced()),
                context.requestId());
        return ApiResponse.ok("ok").withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> delete(ClassDeleteRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isAdmin(context.roleId())) {
            throw forbidden("仅管理员可删除班级");
        }
        ExamClassEntity examClass = getRequiredClass(request.getClassId());
        if (repository.countApprovedMembers(examClass.getClassId()) > 0) {
            throw new BusinessException(DomainErrorCode.CLASS_DELETE_STILL_HAS_STUDENTS);
        }
        repository.deleteMembersByClassId(examClass.getClassId());
        repository.deleteClass(examClass.getClassId());
        saveAuditLog(examClass.getClassId(), null, context.userId(), "class.delete", null,
                "classCode=%s,className=%s".formatted(examClass.getClassCode(), examClass.getClassName()),
                context.requestId());
        return ApiResponse.ok("ok").withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> applyJoin(ClassApplyJoinRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isStudent(context.roleId()) || !Objects.equals(request.getStudentId(), context.userId())) {
            throw forbidden("仅学生本人可申请入班");
        }
        ExamClassEntity examClass = repository.findClassByCode(request.getClassCode())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.CLASS_NOT_FOUND));
        if (repository.countApprovedMemberships(request.getStudentId()) >= 3) {
            throw new BusinessException(DomainErrorCode.STUDENT_CLASS_LIMIT_EXCEEDED);
        }
        ExamClassMemberEntity existing = repository.findMember(examClass.getClassId(), request.getStudentId()).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == ClassMemberStatus.APPROVED) {
                throw new BusinessException(DomainErrorCode.STUDENT_ALREADY_IN_CLASS);
            }
            if (existing.getStatus() == ClassMemberStatus.PENDING) {
                throw new BusinessException(DomainErrorCode.PENDING_CLASS_APPLICATION_CONFLICT);
            }
            existing.setStatus(ClassMemberStatus.PENDING);
            existing.setRemark(request.getRemark());
            existing.setReason(null);
            existing.setOperatedBy(context.userId());
            existing.setApplyTime(LocalDateTime.now());
            existing.setDecisionTime(null);
            existing.setUpdateTime(LocalDateTime.now());
            repository.updateMember(existing);
            saveAuditLog(examClass.getClassId(), existing.getMemberId(), context.userId(), "class.apply-join",
                    request.getStudentId(),
                    "status=%s,remark=%s".formatted(existing.getStatus(), defaultDetail(existing.getRemark())),
                    context.requestId());
        } else {
            ExamClassMemberEntity member = ExamClassMemberEntity.builder()
                    .memberId(snowflake.nextId())
                    .classId(examClass.getClassId())
                    .studentId(request.getStudentId())
                    .status(ClassMemberStatus.PENDING)
                    .remark(request.getRemark())
                    .operatedBy(context.userId())
                    .applyTime(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            repository.saveMember(member);
            saveAuditLog(examClass.getClassId(), member.getMemberId(), context.userId(), "class.apply-join",
                    request.getStudentId(),
                    "status=%s,remark=%s".formatted(member.getStatus(), defaultDetail(member.getRemark())),
                    context.requestId());
        }
        return ApiResponse.ok("ok").withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> approveJoin(ClassApproveJoinRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        ExamClassEntity examClass = getRequiredClass(request.getClassId());
        ensureClassOperator(context, examClass);
        List<Long> targetStudentIds = resolveApprovalStudentIds(request);
        List<ExamClassMemberEntity> members = new ArrayList<>();
        for (Long studentId : targetStudentIds) {
            members.add(getRequiredMember(request.getClassId(), studentId));
        }
        for (ExamClassMemberEntity member : members) {
            if (member.getStatus() != ClassMemberStatus.PENDING) {
                throw new BusinessException(DomainErrorCode.CLASS_APPROVAL_STATUS_CONFLICT);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        ClassMemberStatus targetStatus = "approve".equalsIgnoreCase(request.getApproveResult())
                ? ClassMemberStatus.APPROVED
                : ClassMemberStatus.REJECTED;
        for (ExamClassMemberEntity member : members) {
            member.setStatus(targetStatus);
            member.setReason(request.getReason());
            member.setOperatedBy(context.userId());
            member.setDecisionTime(now);
            member.setUpdateTime(now);
            repository.updateMember(member);
            saveAuditLog(member.getClassId(), member.getMemberId(), context.userId(), "class.approve-join",
                    member.getStudentId(),
                    "status=%s,reason=%s".formatted(member.getStatus(), defaultDetail(request.getReason())),
                    context.requestId(), now);
        }
        return ApiResponse.ok("ok").withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> removeStudent(ClassRemoveStudentRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        ExamClassEntity examClass = getRequiredClass(request.getClassId());
        ensureClassOperator(context, examClass);
        ExamClassMemberEntity member = getRequiredMember(request.getClassId(), request.getStudentId());
        member.setStatus(ClassMemberStatus.REMOVED);
        member.setReason(request.getReason());
        member.setOperatedBy(context.userId());
        member.setDecisionTime(LocalDateTime.now());
        member.setUpdateTime(LocalDateTime.now());
        repository.updateMember(member);
        saveAuditLog(member.getClassId(), member.getMemberId(), context.userId(), "class.remove-student",
                member.getStudentId(),
                "status=%s,reason=%s".formatted(member.getStatus(), defaultDetail(request.getReason())),
                context.requestId());
        return ApiResponse.ok("ok").withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> quit(ClassQuitRequest request, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isStudent(context.roleId()) || !Objects.equals(request.getStudentId(), context.userId())) {
            throw forbidden("仅学生本人可退班");
        }
        ExamClassEntity examClass = getRequiredClass(request.getClassId());
        if (Boolean.TRUE.equals(examClass.getForced())) {
            throw new BusinessException(DomainErrorCode.MANDATORY_CLASS_QUIT_FORBIDDEN);
        }
        ExamClassMemberEntity member = getRequiredMember(request.getClassId(), request.getStudentId());
        member.setStatus(ClassMemberStatus.QUIT);
        member.setOperatedBy(context.userId());
        member.setDecisionTime(LocalDateTime.now());
        member.setUpdateTime(LocalDateTime.now());
        repository.updateMember(member);
        saveAuditLog(member.getClassId(), member.getMemberId(), context.userId(), "class.quit",
                member.getStudentId(), "status=%s".formatted(member.getStatus()), context.requestId());
        return ApiResponse.ok("ok").withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<?> importClasses(MultipartFile file, Long defaultTeacherId, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isAdmin(context.roleId())) {
            throw forbidden("仅管理员可导入班级数据");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(DomainErrorCode.CLASS_IMPORT_FILE_REQUIRED);
        }

        int importedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            Map<String, Integer> headerIndex = null;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> parts = splitCsv(line);
                if (headerIndex == null && looksLikeClassHeader(parts)) {
                    headerIndex = buildHeaderIndex(parts);
                    continue;
                }
                String className = valueOf(parts, headerIndex, 0, "classname");
                String description = valueOf(parts, headerIndex, 1, "description");
                String teacherId = valueOf(parts, headerIndex, 2, "teacherid");
                String forced = valueOf(parts, headerIndex, 3, "forced");
                String status = valueOf(parts, headerIndex, 4, "status");
                if (!StringUtils.hasText(className)) {
                    skippedCount++;
                    errors.add("line " + lineNo + ": className is required");
                    continue;
                }
                try {
                    long importedClassId = snowflake.nextId();
                    repository.saveClass(ExamClassEntity.builder()
                            .classId(importedClassId)
                            .classCode(buildClassCode(importedClassId))
                            .className(className.trim())
                            .description(description)
                            .teacherId(resolveTeacherId(teacherId, defaultTeacherId))
                            .forced(Boolean.parseBoolean(defaultString(forced, "false")))
                            .status(defaultString(status, "active"))
                            .createdBy(context.userId())
                            .createTime(LocalDateTime.now())
                            .updateTime(LocalDateTime.now())
                            .build());
                    importedCount++;
                } catch (BusinessException ex) {
                    skippedCount++;
                    errors.add("line " + lineNo + ": " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            throw new BusinessException(DomainErrorCode.CLASS_IMPORT_READ_FAILED);
        }

        repository.saveImportRecord(ExamClassImportRecordEntity.builder()
                .recordId(snowflake.nextId())
                .fileName(file.getOriginalFilename())
                .defaultTeacherId(defaultTeacherId)
                .importedCount(importedCount)
                .skippedCount(skippedCount)
                .operatorId(context.userId())
                .remark(errors.isEmpty() ? "ok" : String.join("; ", errors))
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fileName", file.getOriginalFilename());
        payload.put("defaultTeacherId", defaultTeacherId);
        payload.put("importedCount", importedCount);
        payload.put("skippedCount", skippedCount);
        payload.put("errors", errors);
        return ApiResponse.ok(payload).withRequestId(context.requestId());
    }

    @Override
    public String export(Long classId, RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        List<ExamClassEntity> exportable;
        if (classId != null) {
            ExamClassEntity examClass = getRequiredClass(classId);
            ensureExportScope(context, examClass);
            exportable = List.of(examClass);
        } else {
            exportable = queryAllVisibleClasses(context);
        }
        List<String> lines = new ArrayList<>();
        lines.add("classId,classCode,className,description,status,teacherId,forced,approvedMemberCount,pendingMemberCount,createdBy,createTime,updateTime");
        exportable.forEach(item -> lines.add("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s".formatted(
                item.getClassId(),
                item.getClassCode(),
                sanitizeCsv(item.getClassName()),
                sanitizeCsv(item.getDescription()),
                item.getStatus(),
                item.getTeacherId(),
                Boolean.TRUE.equals(item.getForced()),
                repository.countApprovedMembers(item.getClassId()),
                repository.countPendingMembers(item.getClassId()),
                item.getCreatedBy(),
                item.getCreateTime(),
                item.getUpdateTime())));
        return String.join(System.lineSeparator(), lines);
    }

    private List<ExamClassEntity> queryAllVisibleClasses(RequestContext context) {
        ClassQueryRequest query = new ClassQueryRequest();
        List<ExamClassEntity> classes = repository.queryClasses(query);
        if (RoleMappings.isAdmin(context.roleId()) || RoleMappings.isAuditor(context.roleId())) {
            return classes;
        }
        if (RoleMappings.isTeacher(context.roleId())) {
            return classes.stream()
                    .filter(item -> Objects.equals(context.userId(), item.getCreatedBy()))
                    .toList();
        }
        if (RoleMappings.isStudent(context.roleId())) {
            Collection<Long> classIds = repository.findMembersByStudentId(context.userId()).stream()
                    .filter(item -> item.getStatus() == ClassMemberStatus.APPROVED || item.getStatus() == ClassMemberStatus.PENDING)
                    .map(ExamClassMemberEntity::getClassId)
                    .collect(Collectors.toSet());
            return classes.stream()
                    .filter(item -> classIds.contains(item.getClassId()))
                    .toList();
        }
        return List.of();
    }

    private void ensureClassOperator(RequestContext context, ExamClassEntity examClass) throws BusinessException {
        if (RoleMappings.isAdmin(context.roleId())) {
            return;
        }
        if (RoleMappings.isTeacher(context.roleId()) && Objects.equals(context.userId(), examClass.getCreatedBy())) {
            return;
        }
        throw new BusinessException(DomainErrorCode.TEACHER_CLASS_FORBIDDEN);
    }

    private void ensureExportScope(RequestContext context, ExamClassEntity examClass) throws BusinessException {
        if (RoleMappings.isAdmin(context.roleId()) || RoleMappings.isAuditor(context.roleId())) {
            return;
        }
        if (RoleMappings.isTeacher(context.roleId()) && Objects.equals(context.userId(), examClass.getCreatedBy())) {
            return;
        }
        throw new BusinessException(DomainErrorCode.CLASS_EXPORT_FORBIDDEN);
    }

    private ExamClassEntity getRequiredClass(Long classId) throws BusinessException {
        return repository.findClassById(classId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.CLASS_NOT_FOUND));
    }

    private ExamClassMemberEntity getRequiredMember(Long classId, Long studentId) throws BusinessException {
        return repository.findMember(classId, studentId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.CLASS_MEMBER_NOT_FOUND));
    }

    private void ensureAuthenticated(RequestContext context) throws BusinessException {
        if (context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
    }

    private Long resolveTeacherId(String rawTeacherId, Long defaultTeacherId) throws BusinessException {
        if (StringUtils.hasText(rawTeacherId)) {
            try {
                return Long.parseLong(rawTeacherId.trim());
            } catch (NumberFormatException e) {
                throw new BusinessException(DomainErrorCode.CLASS_TEACHER_ID_INVALID);
            }
        }
        return defaultTeacherId;
    }

    private List<Long> resolveApprovalStudentIds(ClassApproveJoinRequest request) throws BusinessException {
        Set<Long> studentIds = new java.util.LinkedHashSet<>();
        if (request.getStudentId() != null) {
            studentIds.add(request.getStudentId());
        }
        if (request.getStudentIds() != null) {
            request.getStudentIds().stream()
                    .filter(Objects::nonNull)
                    .forEach(studentIds::add);
        }
        if (studentIds.isEmpty()) {
            throw new BusinessException(DomainErrorCode.CLASS_APPROVAL_TARGET_REQUIRED);
        }
        return List.copyOf(studentIds);
    }

    private List<String> splitCsv(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                quoted = !quoted;
            } else if (ch == ',' && !quoted) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    private String partOrNull(List<String> parts, int index) {
        return index < parts.size() ? parts.get(index) : null;
    }

    private boolean looksLikeClassHeader(List<String> parts) {
        if (parts.isEmpty()) {
            return false;
        }
        String first = parts.get(0).trim().toLowerCase();
        return "classname".equals(first) || "classid".equals(first);
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headerParts) {
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (int index = 0; index < headerParts.size(); index++) {
            headerIndex.put(headerParts.get(index).trim().toLowerCase(), index);
        }
        return headerIndex;
    }

    private String valueOf(List<String> parts, Map<String, Integer> headerIndex, int fallbackIndex, String headerName) {
        if (headerIndex != null && headerIndex.containsKey(headerName)) {
            return partOrNull(parts, headerIndex.get(headerName));
        }
        return partOrNull(parts, fallbackIndex);
    }

    private String defaultString(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String sanitizeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void saveAuditLog(Long classId,
                              Long memberId,
                              Long operatorId,
                              String actionType,
                              Long targetStudentId,
                              String detail,
                              String requestId) {
        saveAuditLog(classId, memberId, operatorId, actionType, targetStudentId, detail, requestId, LocalDateTime.now());
    }

    private void saveAuditLog(Long classId,
                              Long memberId,
                              Long operatorId,
                              String actionType,
                              Long targetStudentId,
                              String detail,
                              String requestId,
                              LocalDateTime operateTime) {
        repository.saveAuditLog(ExamClassAuditLogEntity.builder()
                .auditLogId(snowflake.nextId())
                .classId(classId)
                .memberId(memberId)
                .operatorId(operatorId)
                .actionType(actionType)
                .targetStudentId(targetStudentId)
                .detail(detail)
                .requestId(requestId)
                .operateTime(operateTime)
                .createTime(operateTime)
                .updateTime(operateTime)
                .build());
    }

    private String defaultDetail(String value) {
        return value == null ? "" : value;
    }

    private BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }

    private String buildClassCode(Long classId) {
        String suffix = String.valueOf(classId);
        if (suffix.length() > 8) {
            suffix = suffix.substring(suffix.length() - 8);
        }
        return "CLS" + suffix;
    }
}
