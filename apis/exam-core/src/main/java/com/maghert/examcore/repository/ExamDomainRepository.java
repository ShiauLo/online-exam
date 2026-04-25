package com.maghert.examcore.repository;

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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamDomainRepository {

    ExamEntity saveExam(ExamEntity exam);

    void replaceExamClasses(Long examId, List<ExamClassRelationEntity> relations);

    void replaceExamStudents(Long examId, List<ExamStudentRelationEntity> relations);

    ExamStatusLogEntity saveExamStatusLog(ExamStatusLogEntity log);

    Optional<PaperSnapshotEntity> findPaperById(Long paperId);

    Optional<ExamEntity> findExamById(Long examId);

    Optional<ExamRetestApplyEntity> findRetestApplyById(Long retestApplyId);

    Optional<ExamRetestApplyEntity> findRetestApplyByExamIdAndStudentId(Long examId, Long studentId);

    List<PaperPublishClassSnapshotEntity> listPaperPublishClasses(Long paperId);

    List<ClassSnapshotEntity> findClassesByIds(Collection<Long> classIds);

    ExamEntity updateExam(ExamEntity exam);

    ExamRetestApplyEntity saveRetestApply(ExamRetestApplyEntity apply);

    ExamRetestApplyEntity updateRetestApply(ExamRetestApplyEntity apply);

    List<ExamEntity> listExams();

    List<ExamClassRelationEntity> listExamClasses(Collection<Long> examIds);

    List<ClassMemberSnapshotEntity> listClassMembersByStudentId(Long studentId);

    List<ClassMemberSnapshotEntity> listClassMembersByStudentIds(Collection<Long> studentIds);

    List<ExamStudentRelationEntity> listExamStudents(Collection<Long> examIds);

    List<PaperQuestionSnapshotEntity> listPaperQuestions(Long paperId);

    List<QuestionSnapshotEntity> findQuestionsByIds(Collection<Long> questionIds);

    List<UserSnapshotEntity> findUsersByIds(Collection<Long> userIds);

    List<ScoreRecordEntity> listScoresByExamId(Long examId);

    Optional<ScoreRecordEntity> findScoreByExamIdAndStudentId(Long examId, Long studentId);

    List<ScoreDetailEntity> listScoreDetails(Long scoreId);

    ScoreRecordEntity updateScore(ScoreRecordEntity score);

    ScoreDetailEntity updateScoreDetail(ScoreDetailEntity detail);

    void replaceExamScores(Long examId, List<ScoreRecordEntity> scores, List<ScoreDetailEntity> details);
}
