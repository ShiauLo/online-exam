package com.maghert.examclass.repository;

import com.maghert.examclass.model.ExamClassEntity;
import com.maghert.examclass.model.ExamClassAuditLogEntity;
import com.maghert.examclass.model.ExamClassImportRecordEntity;
import com.maghert.examclass.model.ExamClassMemberEntity;
import com.maghert.examclass.model.dto.ClassQueryRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClassDomainRepository {

    long countCreatedClasses(Long creatorId);

    ExamClassEntity saveClass(ExamClassEntity examClass);

    void updateClass(ExamClassEntity examClass);

    Optional<ExamClassEntity> findClassById(Long classId);

    Optional<ExamClassEntity> findClassByCode(String classCode);

    List<ExamClassEntity> queryClasses(ClassQueryRequest request);

    List<ExamClassEntity> findClassesByIds(Collection<Long> classIds);

    void deleteClass(Long classId);

    ExamClassMemberEntity saveMember(ExamClassMemberEntity member);

    void updateMember(ExamClassMemberEntity member);

    Optional<ExamClassMemberEntity> findMember(Long classId, Long studentId);

    List<ExamClassMemberEntity> findMembersByStudentId(Long studentId);

    long countApprovedMembers(Long classId);

    long countPendingMembers(Long classId);

    long countApprovedMemberships(Long studentId);

    void deleteMembersByClassId(Long classId);

    void saveImportRecord(ExamClassImportRecordEntity record);

    void saveAuditLog(ExamClassAuditLogEntity auditLog);
}
