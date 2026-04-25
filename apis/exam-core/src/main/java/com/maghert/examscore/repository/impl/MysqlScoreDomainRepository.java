package com.maghert.examscore.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maghert.examscore.entity.ClassSnapshotEntity;
import com.maghert.examscore.entity.ExamSnapshotEntity;
import com.maghert.examscore.entity.PaperSnapshotEntity;
import com.maghert.examscore.entity.ScoreAppealEntity;
import com.maghert.examscore.entity.ScoreChangeLogEntity;
import com.maghert.examscore.entity.ScoreDetailEntity;
import com.maghert.examscore.entity.ScoreRecordEntity;
import com.maghert.examscore.mapper.ClassSnapshotMapper;
import com.maghert.examscore.mapper.ExamSnapshotMapper;
import com.maghert.examscore.mapper.PaperSnapshotMapper;
import com.maghert.examscore.mapper.ScoreAppealMapper;
import com.maghert.examscore.mapper.ScoreChangeLogMapper;
import com.maghert.examscore.mapper.ScoreDetailMapper;
import com.maghert.examscore.mapper.ScoreRecordMapper;
import com.maghert.examscore.repository.ScoreDomainRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class MysqlScoreDomainRepository implements ScoreDomainRepository {

    private final ScoreRecordMapper scoreRecordMapper;
    private final ScoreDetailMapper scoreDetailMapper;
    private final ClassSnapshotMapper classSnapshotMapper;
    private final ExamSnapshotMapper examSnapshotMapper;
    private final PaperSnapshotMapper paperSnapshotMapper;
    private final ScoreAppealMapper scoreAppealMapper;
    private final ScoreChangeLogMapper scoreChangeLogMapper;

    public MysqlScoreDomainRepository(ScoreRecordMapper scoreRecordMapper,
                                      ScoreDetailMapper scoreDetailMapper,
                                      ClassSnapshotMapper classSnapshotMapper,
                                      ExamSnapshotMapper examSnapshotMapper,
                                      PaperSnapshotMapper paperSnapshotMapper,
                                      ScoreAppealMapper scoreAppealMapper,
                                      ScoreChangeLogMapper scoreChangeLogMapper) {
        this.scoreRecordMapper = scoreRecordMapper;
        this.scoreDetailMapper = scoreDetailMapper;
        this.classSnapshotMapper = classSnapshotMapper;
        this.examSnapshotMapper = examSnapshotMapper;
        this.paperSnapshotMapper = paperSnapshotMapper;
        this.scoreAppealMapper = scoreAppealMapper;
        this.scoreChangeLogMapper = scoreChangeLogMapper;
    }

    @Override
    public List<ScoreRecordEntity> listScores() {
        return scoreRecordMapper.selectList(null);
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
    public List<ScoreRecordEntity> listScoresByExamIdAndClassId(Long examId, Long classId) {
        return scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecordEntity>()
                .eq(ScoreRecordEntity::getExamId, examId)
                .eq(ScoreRecordEntity::getClassId, classId));
    }

    @Override
    public Optional<ScoreRecordEntity> findScoreById(Long scoreId) {
        return Optional.ofNullable(scoreRecordMapper.selectById(scoreId));
    }

    @Override
    public Optional<ScoreRecordEntity> findScoreByExamIdAndStudentId(Long examId, Long studentId) {
        return scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecordEntity>()
                        .eq(ScoreRecordEntity::getExamId, examId)
                        .eq(ScoreRecordEntity::getStudentId, studentId))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<ExamSnapshotEntity> findExamById(Long examId) {
        return Optional.ofNullable(examSnapshotMapper.selectOne(new LambdaQueryWrapper<ExamSnapshotEntity>()
                .eq(ExamSnapshotEntity::getExamId, examId)));
    }

    @Override
    public Optional<PaperSnapshotEntity> findPaperById(Long paperId) {
        return Optional.ofNullable(paperSnapshotMapper.selectOne(new LambdaQueryWrapper<PaperSnapshotEntity>()
                .eq(PaperSnapshotEntity::getPaperId, paperId)));
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
    public List<ScoreDetailEntity> listScoreDetails(Long scoreId) {
        if (scoreId == null) {
            return List.of();
        }
        return scoreDetailMapper.selectList(new LambdaQueryWrapper<ScoreDetailEntity>()
                .eq(ScoreDetailEntity::getScoreId, scoreId));
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
    public Optional<ScoreAppealEntity> findAppealById(Long appealId) {
        return Optional.ofNullable(scoreAppealMapper.selectById(appealId));
    }

    @Override
    public List<ScoreAppealEntity> listAppealsByScoreIds(Collection<Long> scoreIds) {
        if (scoreIds == null || scoreIds.isEmpty()) {
            return List.of();
        }
        return scoreAppealMapper.selectList(new LambdaQueryWrapper<ScoreAppealEntity>()
                .in(ScoreAppealEntity::getScoreId, scoreIds));
    }

    @Override
    public ScoreAppealEntity saveAppeal(ScoreAppealEntity appeal) {
        scoreAppealMapper.insert(appeal);
        return appeal;
    }

    @Override
    public ScoreAppealEntity updateAppeal(ScoreAppealEntity appeal) {
        scoreAppealMapper.updateById(appeal);
        return appeal;
    }

    @Override
    public void saveScoreChangeLog(ScoreChangeLogEntity changeLog) {
        scoreChangeLogMapper.insert(changeLog);
    }
}
