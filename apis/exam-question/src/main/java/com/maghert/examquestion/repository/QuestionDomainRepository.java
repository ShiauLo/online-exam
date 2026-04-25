package com.maghert.examquestion.repository;

import com.maghert.examquestion.entity.QuestionAuditLog;
import com.maghert.examquestion.entity.QuestionCategory;
import com.maghert.examquestion.entity.QuestionItem;

import java.util.List;
import java.util.Optional;

public interface QuestionDomainRepository {

    QuestionCategory saveCategory(QuestionCategory category);

    void updateCategory(QuestionCategory category);

    Optional<QuestionCategory> findCategoryById(Long categoryId);

    List<QuestionCategory> listCategories();

    QuestionItem saveQuestion(QuestionItem question);

    void updateQuestion(QuestionItem question);

    void deleteQuestion(Long questionId);

    Optional<QuestionItem> findQuestionById(Long questionId);

    List<QuestionItem> listQuestions();

    void saveAuditLog(QuestionAuditLog auditLog);
}
