package com.maghert.examissuecore.repository;

import com.maghert.examissuecore.entity.ClassSnapshotEntity;
import com.maghert.examissuecore.entity.ExamClassRelationEntity;
import com.maghert.examissuecore.entity.ExamSnapshotEntity;
import com.maghert.examissuecore.entity.IssueProcessLogEntity;
import com.maghert.examissuecore.entity.IssueRecordEntity;
import com.maghert.examissuecore.entity.UserSnapshotEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IssueDomainRepository {

    IssueRecordEntity saveIssue(IssueRecordEntity issue);

    IssueRecordEntity updateIssue(IssueRecordEntity issue);

    Optional<IssueRecordEntity> findIssueById(Long issueId);

    List<IssueRecordEntity> listIssues();

    IssueProcessLogEntity saveProcessLog(IssueProcessLogEntity log);

    List<IssueProcessLogEntity> listProcessLogs(Long issueId);

    List<ClassSnapshotEntity> findClassesByIds(Collection<Long> classIds);

    List<ExamSnapshotEntity> findExamsByIds(Collection<Long> examIds);

    List<ExamClassRelationEntity> listExamClasses(Collection<Long> examIds);

    List<UserSnapshotEntity> findUsersByIds(Collection<Long> userIds);
}
