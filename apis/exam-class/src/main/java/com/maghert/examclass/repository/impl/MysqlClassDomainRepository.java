package com.maghert.examclass.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maghert.examclass.mapper.ExamClassImportRecordMapper;
import com.maghert.examclass.mapper.ExamClassAuditLogMapper;
import com.maghert.examclass.mapper.ExamClassMapper;
import com.maghert.examclass.mapper.ExamClassMemberMapper;
import com.maghert.examclass.model.ClassMemberStatus;
import com.maghert.examclass.model.ExamClassAuditLogEntity;
import com.maghert.examclass.model.ExamClassEntity;
import com.maghert.examclass.model.ExamClassImportRecordEntity;
import com.maghert.examclass.model.ExamClassMemberEntity;
import com.maghert.examclass.model.dto.ClassQueryRequest;
import com.maghert.examclass.repository.ClassDomainRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class MysqlClassDomainRepository implements ClassDomainRepository {

    private final ExamClassMapper classMapper;
    private final ExamClassMemberMapper memberMapper;
    private final ExamClassImportRecordMapper importRecordMapper;
    private final ExamClassAuditLogMapper auditLogMapper;

    public MysqlClassDomainRepository(ExamClassMapper classMapper,
                                      ExamClassMemberMapper memberMapper,
                                      ExamClassImportRecordMapper importRecordMapper,
                                      ExamClassAuditLogMapper auditLogMapper) {
        this.classMapper = classMapper;
        this.memberMapper = memberMapper;
        this.importRecordMapper = importRecordMapper;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public long countCreatedClasses(Long creatorId) {
        return classMapper.selectCount(new LambdaQueryWrapper<ExamClassEntity>()
                .eq(ExamClassEntity::getCreatedBy, creatorId));
    }

    @Override
    public ExamClassEntity saveClass(ExamClassEntity examClass) {
        classMapper.insert(examClass);
        return examClass;
    }

    @Override
    public void updateClass(ExamClassEntity examClass) {
        classMapper.updateById(examClass);
    }

    @Override
    public Optional<ExamClassEntity> findClassById(Long classId) {
        return Optional.ofNullable(classMapper.selectById(classId));
    }

    @Override
    public Optional<ExamClassEntity> findClassByCode(String classCode) {
        return Optional.ofNullable(classMapper.selectOne(new LambdaQueryWrapper<ExamClassEntity>()
                .eq(ExamClassEntity::getClassCode, classCode)));
    }

    @Override
    public List<ExamClassEntity> queryClasses(ClassQueryRequest request) {
        LambdaQueryWrapper<ExamClassEntity> wrapper = new LambdaQueryWrapper<>();
        if (request.getClassId() != null) {
            wrapper.eq(ExamClassEntity::getClassId, request.getClassId());
        }
        if (request.getTeacherId() != null) {
            wrapper.eq(ExamClassEntity::getTeacherId, request.getTeacherId());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(ExamClassEntity::getClassName, request.getKeyword());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(ExamClassEntity::getStatus, request.getStatus());
        }
        wrapper.orderByDesc(ExamClassEntity::getCreateTime);
        return classMapper.selectList(wrapper);
    }

    @Override
    public List<ExamClassEntity> findClassesByIds(Collection<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        return classMapper.selectList(new LambdaQueryWrapper<ExamClassEntity>()
                .in(ExamClassEntity::getClassId, classIds)
                .orderByDesc(ExamClassEntity::getCreateTime));
    }

    @Override
    public void deleteClass(Long classId) {
        classMapper.deleteById(classId);
    }

    @Override
    public ExamClassMemberEntity saveMember(ExamClassMemberEntity member) {
        memberMapper.insert(member);
        return member;
    }

    @Override
    public void updateMember(ExamClassMemberEntity member) {
        memberMapper.updateById(member);
    }

    @Override
    public Optional<ExamClassMemberEntity> findMember(Long classId, Long studentId) {
        return Optional.ofNullable(memberMapper.selectOne(new LambdaQueryWrapper<ExamClassMemberEntity>()
                .eq(ExamClassMemberEntity::getClassId, classId)
                .eq(ExamClassMemberEntity::getStudentId, studentId)));
    }

    @Override
    public List<ExamClassMemberEntity> findMembersByStudentId(Long studentId) {
        return memberMapper.selectList(new LambdaQueryWrapper<ExamClassMemberEntity>()
                .eq(ExamClassMemberEntity::getStudentId, studentId));
    }

    @Override
    public long countApprovedMembers(Long classId) {
        return memberMapper.selectCount(new LambdaQueryWrapper<ExamClassMemberEntity>()
                .eq(ExamClassMemberEntity::getClassId, classId)
                .eq(ExamClassMemberEntity::getStatus, ClassMemberStatus.APPROVED));
    }

    @Override
    public long countPendingMembers(Long classId) {
        return memberMapper.selectCount(new LambdaQueryWrapper<ExamClassMemberEntity>()
                .eq(ExamClassMemberEntity::getClassId, classId)
                .eq(ExamClassMemberEntity::getStatus, ClassMemberStatus.PENDING));
    }

    @Override
    public long countApprovedMemberships(Long studentId) {
        return memberMapper.selectCount(new LambdaQueryWrapper<ExamClassMemberEntity>()
                .eq(ExamClassMemberEntity::getStudentId, studentId)
                .eq(ExamClassMemberEntity::getStatus, ClassMemberStatus.APPROVED));
    }

    @Override
    public void deleteMembersByClassId(Long classId) {
        memberMapper.delete(new LambdaQueryWrapper<ExamClassMemberEntity>()
                .eq(ExamClassMemberEntity::getClassId, classId));
    }

    @Override
    public void saveImportRecord(ExamClassImportRecordEntity record) {
        importRecordMapper.insert(record);
    }

    @Override
    public void saveAuditLog(ExamClassAuditLogEntity auditLog) {
        auditLogMapper.insert(auditLog);
    }
}
