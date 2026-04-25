package com.maghert.examissuecore.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maghert.examissuecore.entity.ClassSnapshotEntity;
import com.maghert.examissuecore.entity.ExamClassRelationEntity;
import com.maghert.examissuecore.entity.ExamSnapshotEntity;
import com.maghert.examissuecore.entity.IssueProcessLogEntity;
import com.maghert.examissuecore.entity.IssueRecordEntity;
import com.maghert.examissuecore.entity.UserSnapshotEntity;
import com.maghert.examissuecore.mapper.ClassSnapshotMapper;
import com.maghert.examissuecore.mapper.ExamClassRelationMapper;
import com.maghert.examissuecore.mapper.ExamSnapshotMapper;
import com.maghert.examissuecore.mapper.IssueProcessLogMapper;
import com.maghert.examissuecore.mapper.IssueRecordMapper;
import com.maghert.examissuecore.mapper.UserSnapshotMapper;
import com.maghert.examissuecore.repository.IssueDomainRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class MysqlIssueDomainRepository implements IssueDomainRepository {

    private final IssueRecordMapper issueRecordMapper;
    private final IssueProcessLogMapper issueProcessLogMapper;
    private final ClassSnapshotMapper classSnapshotMapper;
    private final ExamSnapshotMapper examSnapshotMapper;
    private final ExamClassRelationMapper examClassRelationMapper;
    private final UserSnapshotMapper userSnapshotMapper;

    public MysqlIssueDomainRepository(IssueRecordMapper issueRecordMapper,
                                      IssueProcessLogMapper issueProcessLogMapper,
                                      ClassSnapshotMapper classSnapshotMapper,
                                      ExamSnapshotMapper examSnapshotMapper,
                                      ExamClassRelationMapper examClassRelationMapper,
                                      UserSnapshotMapper userSnapshotMapper) {
        this.issueRecordMapper = issueRecordMapper;
        this.issueProcessLogMapper = issueProcessLogMapper;
        this.classSnapshotMapper = classSnapshotMapper;
        this.examSnapshotMapper = examSnapshotMapper;
        this.examClassRelationMapper = examClassRelationMapper;
        this.userSnapshotMapper = userSnapshotMapper;
    }

    @Override
    public IssueRecordEntity saveIssue(IssueRecordEntity issue) {
        issueRecordMapper.insert(issue);
        return issue;
    }

    @Override
    public IssueRecordEntity updateIssue(IssueRecordEntity issue) {
        issueRecordMapper.updateById(issue);
        return issue;
    }

    @Override
    public Optional<IssueRecordEntity> findIssueById(Long issueId) {
        return Optional.ofNullable(issueRecordMapper.selectById(issueId));
    }

    @Override
    public List<IssueRecordEntity> listIssues() {
        return issueRecordMapper.selectList(null);
    }

    @Override
    public IssueProcessLogEntity saveProcessLog(IssueProcessLogEntity log) {
        issueProcessLogMapper.insert(log);
        return log;
    }

    @Override
    public List<IssueProcessLogEntity> listProcessLogs(Long issueId) {
        if (issueId == null) {
            return List.of();
        }
        return issueProcessLogMapper.selectList(new LambdaQueryWrapper<IssueProcessLogEntity>()
                .eq(IssueProcessLogEntity::getIssueId, issueId));
    }

    @Override
    public List<ClassSnapshotEntity> findClassesByIds(Collection<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        return classSnapshotMapper.selectList(new LambdaQueryWrapper<ClassSnapshotEntity>()
                .in(ClassSnapshotEntity::getClassId, classIds));
    }

    @Override
    public List<ExamSnapshotEntity> findExamsByIds(Collection<Long> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return List.of();
        }
        return examSnapshotMapper.selectList(new LambdaQueryWrapper<ExamSnapshotEntity>()
                .in(ExamSnapshotEntity::getExamId, examIds));
    }

    @Override
    public List<ExamClassRelationEntity> listExamClasses(Collection<Long> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return List.of();
        }
        return examClassRelationMapper.selectList(new LambdaQueryWrapper<ExamClassRelationEntity>()
                .in(ExamClassRelationEntity::getExamId, examIds));
    }

    @Override
    public List<UserSnapshotEntity> findUsersByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userSnapshotMapper.selectList(new LambdaQueryWrapper<UserSnapshotEntity>()
                .in(UserSnapshotEntity::getUserId, userIds));
    }
}
