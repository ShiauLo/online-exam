package com.maghert.examcore.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maghert.examcore.entity.ClassSnapshotEntity;
import com.maghert.examcore.entity.ClassMemberSnapshotEntity;
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
import com.maghert.examcore.mapper.ClassSnapshotMapper;
import com.maghert.examcore.mapper.ClassMemberSnapshotMapper;
import com.maghert.examcore.mapper.ExamClassRelationMapper;
import com.maghert.examcore.mapper.ExamMapper;
import com.maghert.examcore.mapper.ExamRetestApplyMapper;
import com.maghert.examcore.mapper.ExamStatusLogMapper;
import com.maghert.examcore.mapper.ExamStudentRelationMapper;
import com.maghert.examcore.mapper.PaperQuestionSnapshotMapper;
import com.maghert.examcore.mapper.PaperPublishClassSnapshotMapper;
import com.maghert.examcore.mapper.PaperSnapshotMapper;
import com.maghert.examcore.mapper.QuestionSnapshotMapper;
import com.maghert.examcore.mapper.ScoreDetailMapper;
import com.maghert.examcore.mapper.ScoreRecordMapper;
import com.maghert.examcore.mapper.UserSnapshotMapper;
import com.maghert.examcore.repository.ExamDomainRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class MysqlExamDomainRepository implements ExamDomainRepository {

    private final ExamMapper examMapper;
    private final ExamClassRelationMapper examClassRelationMapper;
    private final ExamRetestApplyMapper examRetestApplyMapper;
    private final ExamStatusLogMapper examStatusLogMapper;
    private final ExamStudentRelationMapper examStudentRelationMapper;
    private final PaperSnapshotMapper paperSnapshotMapper;
    private final PaperQuestionSnapshotMapper paperQuestionSnapshotMapper;
    private final PaperPublishClassSnapshotMapper paperPublishClassSnapshotMapper;
    private final QuestionSnapshotMapper questionSnapshotMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final ScoreDetailMapper scoreDetailMapper;
    private final UserSnapshotMapper userSnapshotMapper;
    private final ClassSnapshotMapper classSnapshotMapper;
    private final ClassMemberSnapshotMapper classMemberSnapshotMapper;

    public MysqlExamDomainRepository(ExamMapper examMapper,
                                     ExamClassRelationMapper examClassRelationMapper,
                                     ExamRetestApplyMapper examRetestApplyMapper,
                                     ExamStatusLogMapper examStatusLogMapper,
                                     ExamStudentRelationMapper examStudentRelationMapper,
                                     PaperSnapshotMapper paperSnapshotMapper,
                                     PaperQuestionSnapshotMapper paperQuestionSnapshotMapper,
                                     PaperPublishClassSnapshotMapper paperPublishClassSnapshotMapper,
                                     QuestionSnapshotMapper questionSnapshotMapper,
                                     ScoreRecordMapper scoreRecordMapper,
                                     ScoreDetailMapper scoreDetailMapper,
                                     UserSnapshotMapper userSnapshotMapper,
                                     ClassSnapshotMapper classSnapshotMapper,
                                     ClassMemberSnapshotMapper classMemberSnapshotMapper) {
        this.examMapper = examMapper;
        this.examClassRelationMapper = examClassRelationMapper;
        this.examRetestApplyMapper = examRetestApplyMapper;
        this.examStatusLogMapper = examStatusLogMapper;
        this.examStudentRelationMapper = examStudentRelationMapper;
        this.paperSnapshotMapper = paperSnapshotMapper;
        this.paperQuestionSnapshotMapper = paperQuestionSnapshotMapper;
        this.paperPublishClassSnapshotMapper = paperPublishClassSnapshotMapper;
        this.questionSnapshotMapper = questionSnapshotMapper;
        this.scoreRecordMapper = scoreRecordMapper;
        this.scoreDetailMapper = scoreDetailMapper;
        this.userSnapshotMapper = userSnapshotMapper;
        this.classSnapshotMapper = classSnapshotMapper;
        this.classMemberSnapshotMapper = classMemberSnapshotMapper;
    }

    @Override
    public ExamEntity saveExam(ExamEntity exam) {
        examMapper.insert(exam);
        return exam;
    }

    @Override
    public void replaceExamClasses(Long examId, List<ExamClassRelationEntity> relations) {
        examClassRelationMapper.delete(new LambdaQueryWrapper<ExamClassRelationEntity>()
                .eq(ExamClassRelationEntity::getExamId, examId));
        relations.forEach(examClassRelationMapper::insert);
    }

    @Override
    public void replaceExamStudents(Long examId, List<ExamStudentRelationEntity> relations) {
        examStudentRelationMapper.delete(new LambdaQueryWrapper<ExamStudentRelationEntity>()
                .eq(ExamStudentRelationEntity::getExamId, examId));
        relations.forEach(examStudentRelationMapper::insert);
    }

    @Override
    public ExamStatusLogEntity saveExamStatusLog(ExamStatusLogEntity log) {
        examStatusLogMapper.insert(log);
        return log;
    }

    @Override
    public Optional<PaperSnapshotEntity> findPaperById(Long paperId) {
        return Optional.ofNullable(paperSnapshotMapper.selectById(paperId));
    }

    @Override
    public Optional<ExamEntity> findExamById(Long examId) {
        return Optional.ofNullable(examMapper.selectById(examId));
    }

    @Override
    public Optional<ExamRetestApplyEntity> findRetestApplyById(Long retestApplyId) {
        return Optional.ofNullable(examRetestApplyMapper.selectById(retestApplyId));
    }

    @Override
    public Optional<ExamRetestApplyEntity> findRetestApplyByExamIdAndStudentId(Long examId, Long studentId) {
        if (examId == null || studentId == null) {
            return Optional.empty();
        }
        return examRetestApplyMapper.selectList(new LambdaQueryWrapper<ExamRetestApplyEntity>()
                        .eq(ExamRetestApplyEntity::getExamId, examId)
                        .eq(ExamRetestApplyEntity::getStudentId, studentId))
                .stream()
                .findFirst();
    }

    @Override
    public List<PaperPublishClassSnapshotEntity> listPaperPublishClasses(Long paperId) {
        return paperPublishClassSnapshotMapper.selectList(new LambdaQueryWrapper<PaperPublishClassSnapshotEntity>()
                .eq(PaperPublishClassSnapshotEntity::getPaperId, paperId));
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
    public ExamEntity updateExam(ExamEntity exam) {
        examMapper.updateById(exam);
        return exam;
    }

    @Override
    public ExamRetestApplyEntity saveRetestApply(ExamRetestApplyEntity apply) {
        examRetestApplyMapper.insert(apply);
        return apply;
    }

    @Override
    public ExamRetestApplyEntity updateRetestApply(ExamRetestApplyEntity apply) {
        examRetestApplyMapper.updateById(apply);
        return apply;
    }

    @Override
    public List<ExamEntity> listExams() {
        return examMapper.selectList(null);
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
    public List<ClassMemberSnapshotEntity> listClassMembersByStudentId(Long studentId) {
        if (studentId == null) {
            return List.of();
        }
        return classMemberSnapshotMapper.selectList(new LambdaQueryWrapper<ClassMemberSnapshotEntity>()
                .eq(ClassMemberSnapshotEntity::getStudentId, studentId));
    }

    @Override
    public List<ClassMemberSnapshotEntity> listClassMembersByStudentIds(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        return classMemberSnapshotMapper.selectList(new LambdaQueryWrapper<ClassMemberSnapshotEntity>()
                .in(ClassMemberSnapshotEntity::getStudentId, studentIds));
    }

    @Override
    public List<ExamStudentRelationEntity> listExamStudents(Collection<Long> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return List.of();
        }
        return examStudentRelationMapper.selectList(new LambdaQueryWrapper<ExamStudentRelationEntity>()
                .in(ExamStudentRelationEntity::getExamId, examIds));
    }

    @Override
    public List<PaperQuestionSnapshotEntity> listPaperQuestions(Long paperId) {
        if (paperId == null) {
            return List.of();
        }
        return paperQuestionSnapshotMapper.selectList(new LambdaQueryWrapper<PaperQuestionSnapshotEntity>()
                .eq(PaperQuestionSnapshotEntity::getPaperId, paperId));
    }

    @Override
    public List<QuestionSnapshotEntity> findQuestionsByIds(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        return questionSnapshotMapper.selectList(new LambdaQueryWrapper<QuestionSnapshotEntity>()
                .in(QuestionSnapshotEntity::getQuestionId, questionIds));
    }

    @Override
    public List<UserSnapshotEntity> findUsersByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userSnapshotMapper.selectList(new LambdaQueryWrapper<UserSnapshotEntity>()
                .in(UserSnapshotEntity::getUserId, userIds));
    }

    @Override
    public List<ScoreRecordEntity> listScoresByExamId(Long examId) {
        if (examId == null) {
            return List.of();
        }
        return scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecordEntity>()
                .eq(ScoreRecordEntity::getExamId, examId));
    }

    @Override
    public Optional<ScoreRecordEntity> findScoreByExamIdAndStudentId(Long examId, Long studentId) {
        if (examId == null || studentId == null) {
            return Optional.empty();
        }
        return scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecordEntity>()
                        .eq(ScoreRecordEntity::getExamId, examId)
                        .eq(ScoreRecordEntity::getStudentId, studentId))
                .stream()
                .findFirst();
    }

    @Override
    public List<ScoreDetailEntity> listScoreDetails(Long scoreId) {
        if (scoreId == null) {
            return List.of();
        }
        return scoreDetailMapper.selectList(new LambdaQueryWrapper<ScoreDetailEntity>()
                .eq(ScoreDetailEntity::getScoreId, scoreId));
    }

    @Override
    public ScoreRecordEntity updateScore(ScoreRecordEntity score) {
        scoreRecordMapper.updateById(score);
        return score;
    }

    @Override
    public ScoreDetailEntity updateScoreDetail(ScoreDetailEntity detail) {
        scoreDetailMapper.updateById(detail);
        return detail;
    }

    @Override
    public void replaceExamScores(Long examId, List<ScoreRecordEntity> scores, List<ScoreDetailEntity> details) {
        scoreRecordMapper.delete(new LambdaQueryWrapper<ScoreRecordEntity>()
                .eq(ScoreRecordEntity::getExamId, examId));
        scores.forEach(scoreRecordMapper::insert);
        details.forEach(scoreDetailMapper::insert);
    }
}
