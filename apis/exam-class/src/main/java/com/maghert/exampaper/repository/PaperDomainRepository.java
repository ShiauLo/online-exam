package com.maghert.exampaper.repository;

import com.maghert.exampaper.entity.ClassSnapshotEntity;
import com.maghert.exampaper.entity.PaperAuditLogEntity;
import com.maghert.exampaper.entity.PaperEntity;
import com.maghert.exampaper.entity.PaperPublishClassEntity;
import com.maghert.exampaper.entity.PaperQuestionBindingEntity;
import com.maghert.exampaper.entity.QuestionCategorySnapshotEntity;
import com.maghert.exampaper.entity.QuestionSnapshotEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaperDomainRepository {

    PaperEntity savePaper(PaperEntity paper);

    void updatePaper(PaperEntity paper);

    Optional<PaperEntity> findPaperById(Long paperId);

    List<PaperEntity> listPapers();

    void deletePaper(Long paperId);

    void replacePaperQuestions(Long paperId, List<PaperQuestionBindingEntity> bindings);

    List<PaperQuestionBindingEntity> listPaperQuestions(Long paperId);

    void deletePaperQuestions(Long paperId);

    void replacePaperPublishClasses(Long paperId, List<PaperPublishClassEntity> relations);

    List<PaperPublishClassEntity> listPaperPublishClasses(Long paperId);

    void deletePaperPublishClasses(Long paperId);

    void saveAuditLog(PaperAuditLogEntity auditLog);

    List<QuestionSnapshotEntity> findQuestionsByIds(Collection<Long> questionIds);

    List<QuestionSnapshotEntity> listQuestions();

    List<QuestionCategorySnapshotEntity> findQuestionCategoriesByIds(Collection<Long> categoryIds);

    List<ClassSnapshotEntity> findClassesByIds(Collection<Long> classIds);

    void updateQuestionReferenceCount(Long questionId, int delta);
}
