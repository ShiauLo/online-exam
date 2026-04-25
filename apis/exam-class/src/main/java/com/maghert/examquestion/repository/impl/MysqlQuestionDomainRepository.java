package com.maghert.examquestion.repository.impl;

import com.maghert.examquestion.entity.QuestionAuditLog;
import com.maghert.examquestion.entity.QuestionCategory;
import com.maghert.examquestion.entity.QuestionItem;
import com.maghert.examquestion.mapper.QuestionAuditLogMapper;
import com.maghert.examquestion.mapper.QuestionCategoryMapper;
import com.maghert.examquestion.mapper.QuestionItemMapper;
import com.maghert.examquestion.repository.QuestionDomainRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MysqlQuestionDomainRepository implements QuestionDomainRepository {

    private final QuestionCategoryMapper categoryMapper;
    private final QuestionItemMapper itemMapper;
    private final QuestionAuditLogMapper auditLogMapper;

    public MysqlQuestionDomainRepository(QuestionCategoryMapper categoryMapper,
                                         QuestionItemMapper itemMapper,
                                         QuestionAuditLogMapper auditLogMapper) {
        this.categoryMapper = categoryMapper;
        this.itemMapper = itemMapper;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public QuestionCategory saveCategory(QuestionCategory category) {
        categoryMapper.insert(category);
        return category;
    }

    @Override
    public void updateCategory(QuestionCategory category) {
        categoryMapper.updateById(category);
    }

    @Override
    public Optional<QuestionCategory> findCategoryById(Long categoryId) {
        return Optional.ofNullable(categoryMapper.selectById(categoryId));
    }

    @Override
    public List<QuestionCategory> listCategories() {
        return categoryMapper.selectList(null);
    }

    @Override
    public QuestionItem saveQuestion(QuestionItem question) {
        itemMapper.insert(question);
        return question;
    }

    @Override
    public void updateQuestion(QuestionItem question) {
        itemMapper.updateById(question);
    }

    @Override
    public void deleteQuestion(Long questionId) {
        itemMapper.deleteById(questionId);
    }

    @Override
    public Optional<QuestionItem> findQuestionById(Long questionId) {
        return Optional.ofNullable(itemMapper.selectById(questionId));
    }

    @Override
    public List<QuestionItem> listQuestions() {
        return itemMapper.selectList(null);
    }

    @Override
    public void saveAuditLog(QuestionAuditLog auditLog) {
        auditLogMapper.insert(auditLog);
    }
}
