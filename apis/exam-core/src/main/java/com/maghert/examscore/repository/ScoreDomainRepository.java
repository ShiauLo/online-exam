package com.maghert.examscore.repository;

import com.maghert.examscore.entity.ClassSnapshotEntity;
import com.maghert.examscore.entity.ExamSnapshotEntity;
import com.maghert.examscore.entity.PaperSnapshotEntity;
import com.maghert.examscore.entity.ScoreAppealEntity;
import com.maghert.examscore.entity.ScoreChangeLogEntity;
import com.maghert.examscore.entity.ScoreDetailEntity;
import com.maghert.examscore.entity.ScoreRecordEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScoreDomainRepository {

    List<ScoreRecordEntity> listScores();

    List<ScoreRecordEntity> listScoresByExamId(Long examId);

    List<ScoreRecordEntity> listScoresByExamIdAndClassId(Long examId, Long classId);

    Optional<ScoreRecordEntity> findScoreById(Long scoreId);

    Optional<ScoreRecordEntity> findScoreByExamIdAndStudentId(Long examId, Long studentId);

    Optional<ExamSnapshotEntity> findExamById(Long examId);

    Optional<PaperSnapshotEntity> findPaperById(Long paperId);

    ScoreRecordEntity updateScore(ScoreRecordEntity score);

    ScoreDetailEntity updateScoreDetail(ScoreDetailEntity detail);

    List<ScoreDetailEntity> listScoreDetails(Long scoreId);

    List<ClassSnapshotEntity> findClassesByIds(Collection<Long> classIds);

    Optional<ScoreAppealEntity> findAppealById(Long appealId);

    List<ScoreAppealEntity> listAppealsByScoreIds(Collection<Long> scoreIds);

    ScoreAppealEntity saveAppeal(ScoreAppealEntity appeal);

    ScoreAppealEntity updateAppeal(ScoreAppealEntity appeal);

    void saveScoreChangeLog(ScoreChangeLogEntity changeLog);
}
