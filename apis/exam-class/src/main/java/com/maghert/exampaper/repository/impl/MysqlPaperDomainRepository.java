package com.maghert.exampaper.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maghert.exampaper.entity.ClassSnapshotEntity;
import com.maghert.exampaper.entity.PaperAuditLogEntity;
import com.maghert.exampaper.entity.PaperEntity;
import com.maghert.exampaper.entity.PaperPublishClassEntity;
import com.maghert.exampaper.entity.PaperQuestionBindingEntity;
import com.maghert.exampaper.entity.QuestionCategorySnapshotEntity;
import com.maghert.exampaper.entity.QuestionSnapshotEntity;
import com.maghert.exampaper.mapper.ClassSnapshotMapper;
import com.maghert.exampaper.mapper.PaperAuditLogMapper;
import com.maghert.exampaper.mapper.PaperMapper;
import com.maghert.exampaper.mapper.PaperPublishClassMapper;
import com.maghert.exampaper.mapper.PaperQuestionBindingMapper;
import com.maghert.exampaper.mapper.QuestionCategorySnapshotMapper;
import com.maghert.exampaper.mapper.QuestionSnapshotMapper;
import com.maghert.exampaper.repository.PaperDomainRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class MysqlPaperDomainRepository implements PaperDomainRepository {

    private final PaperMapper paperMapper;
    private final PaperQuestionBindingMapper paperQuestionBindingMapper;
    private final PaperPublishClassMapper paperPublishClassMapper;
    private final PaperAuditLogMapper paperAuditLogMapper;
    private final QuestionSnapshotMapper questionSnapshotMapper;
    private final QuestionCategorySnapshotMapper questionCategorySnapshotMapper;
    private final ClassSnapshotMapper classSnapshotMapper;

    public MysqlPaperDomainRepository(PaperMapper paperMapper,
                                      PaperQuestionBindingMapper paperQuestionBindingMapper,
                                      PaperPublishClassMapper paperPublishClassMapper,
                                      PaperAuditLogMapper paperAuditLogMapper,
                                      QuestionSnapshotMapper questionSnapshotMapper,
                                      QuestionCategorySnapshotMapper questionCategorySnapshotMapper,
                                      ClassSnapshotMapper classSnapshotMapper) {
        this.paperMapper = paperMapper;
        this.paperQuestionBindingMapper = paperQuestionBindingMapper;
        this.paperPublishClassMapper = paperPublishClassMapper;
        this.paperAuditLogMapper = paperAuditLogMapper;
        this.questionSnapshotMapper = questionSnapshotMapper;
        this.questionCategorySnapshotMapper = questionCategorySnapshotMapper;
        this.classSnapshotMapper = classSnapshotMapper;
    }

    @Override
    public PaperEntity savePaper(PaperEntity paper) {
        paperMapper.insert(paper);
        return paper;
    }

    @Override
    public void updatePaper(PaperEntity paper) {
        paperMapper.updateById(paper);
    }

    @Override
    public Optional<PaperEntity> findPaperById(Long paperId) {
        return Optional.ofNullable(paperMapper.selectById(paperId));
    }

    @Override
    public List<PaperEntity> listPapers() {
        return paperMapper.selectList(null);
    }

    @Override
    public void deletePaper(Long paperId) {
        paperMapper.deleteById(paperId);
    }

    @Override
    public void replacePaperQuestions(Long paperId, List<PaperQuestionBindingEntity> bindings) {
        deletePaperQuestions(paperId);
        bindings.forEach(paperQuestionBindingMapper::insert);
    }

    @Override
    public List<PaperQuestionBindingEntity> listPaperQuestions(Long paperId) {
        return paperQuestionBindingMapper.selectList(new LambdaQueryWrapper<PaperQuestionBindingEntity>()
                .eq(PaperQuestionBindingEntity::getPaperId, paperId)
                .orderByAsc(PaperQuestionBindingEntity::getSortNo));
    }

    @Override
    public void deletePaperQuestions(Long paperId) {
        paperQuestionBindingMapper.delete(new LambdaQueryWrapper<PaperQuestionBindingEntity>()
                .eq(PaperQuestionBindingEntity::getPaperId, paperId));
    }

    @Override
    public void replacePaperPublishClasses(Long paperId, List<PaperPublishClassEntity> relations) {
        deletePaperPublishClasses(paperId);
        relations.forEach(paperPublishClassMapper::insert);
    }

    @Override
    public List<PaperPublishClassEntity> listPaperPublishClasses(Long paperId) {
        return paperPublishClassMapper.selectList(new LambdaQueryWrapper<PaperPublishClassEntity>()
                .eq(PaperPublishClassEntity::getPaperId, paperId)
                .orderByAsc(PaperPublishClassEntity::getClassId));
    }

    @Override
    public void deletePaperPublishClasses(Long paperId) {
        paperPublishClassMapper.delete(new LambdaQueryWrapper<PaperPublishClassEntity>()
                .eq(PaperPublishClassEntity::getPaperId, paperId));
    }

    @Override
    public void saveAuditLog(PaperAuditLogEntity auditLog) {
        paperAuditLogMapper.insert(auditLog);
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
    public List<QuestionSnapshotEntity> listQuestions() {
        return questionSnapshotMapper.selectList(null);
    }

    @Override
    public List<QuestionCategorySnapshotEntity> findQuestionCategoriesByIds(Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        return questionCategorySnapshotMapper.selectList(new LambdaQueryWrapper<QuestionCategorySnapshotEntity>()
                .in(QuestionCategorySnapshotEntity::getCategoryId, categoryIds));
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
    public void updateQuestionReferenceCount(Long questionId, int delta) {
        QuestionSnapshotEntity question = questionSnapshotMapper.selectById(questionId);
        if (question == null) {
            return;
        }
        int current = question.getReferenceCount() == null ? 0 : question.getReferenceCount();
        int next = Math.max(0, current + delta);
        questionSnapshotMapper.updateById(new QuestionSnapshotEntity()
                .setQuestionId(questionId)
                .setReferenceCount(next));
    }
}
