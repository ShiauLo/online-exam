package com.maghert.examscore.service;

import cn.hutool.core.util.IdUtil;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.web.PageResult;
import com.maghert.examscore.context.RequestContext;
import com.maghert.examscore.entity.ClassSnapshotEntity;
import com.maghert.examscore.entity.ExamSnapshotEntity;
import com.maghert.examscore.entity.PaperSnapshotEntity;
import com.maghert.examscore.entity.ScoreAppealEntity;
import com.maghert.examscore.entity.ScoreChangeLogEntity;
import com.maghert.examscore.entity.ScoreDetailEntity;
import com.maghert.examscore.entity.ScoreRecordEntity;
import com.maghert.examscore.model.dto.ScoreApplyRecheckRequest;
import com.maghert.examscore.model.dto.ScoreAutoScoreRequest;
import com.maghert.examscore.model.dto.ScoreAnalyzeRequest;
import com.maghert.examscore.model.dto.ScoreDetailRequest;
import com.maghert.examscore.model.dto.ScoreHandleAppealRequest;
import com.maghert.examscore.model.dto.ScoreManualScoreRequest;
import com.maghert.examscore.model.dto.ScorePublishRequest;
import com.maghert.examscore.model.dto.ScoreQueryRequest;
import com.maghert.examscore.model.dto.ScoreUpdateRequest;
import com.maghert.examscore.model.vo.ScoreAnalyzeView;
import com.maghert.examscore.model.vo.ScoreApplyRecheckView;
import com.maghert.examscore.model.vo.ScoreAutoScoreView;
import com.maghert.examscore.model.vo.ScoreDetailView;
import com.maghert.examscore.model.vo.ScoreExportView;
import com.maghert.examscore.model.vo.ScoreHandleAppealView;
import com.maghert.examscore.model.vo.ScoreManualScoreView;
import com.maghert.examscore.model.vo.ScorePublishView;
import com.maghert.examscore.model.vo.ScoreQueryView;
import com.maghert.examscore.model.vo.ScoreUpdateView;
import com.maghert.examscore.repository.ScoreDomainRepository;
import com.maghert.examscore.service.impl.ExamScoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamScoreServiceImplTests {

    private static final RequestContext TEACHER_CONTEXT = new RequestContext(3001L, 3, "req-teacher");
    private static final RequestContext FOREIGN_TEACHER_CONTEXT = new RequestContext(3002L, 3, "req-teacher-2");
    private static final RequestContext SUPER_ADMIN_CONTEXT = new RequestContext(1001L, 1, "req-super-admin");
    private static final RequestContext ADMIN_CONTEXT = new RequestContext(2001L, 2, "req-admin");
    private static final RequestContext AUDITOR_CONTEXT = new RequestContext(5001L, 5, "req-auditor");
    private static final RequestContext STUDENT_CONTEXT = new RequestContext(4001L, 4, "req-student");
    private static final RequestContext OPS_CONTEXT = new RequestContext(6001L, 6, "req-ops");

    private InMemoryScoreDomainRepository repository;
    private ExamScoreServiceImpl service;
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        repository = new InMemoryScoreDomainRepository();
        tempDir = Files.createDirectories(Path.of("target", "score-service-tests", Long.toString(System.nanoTime())));
        service = new ExamScoreServiceImpl(repository, IdUtil.getSnowflake(1, 1), tempDir.toString());
        seedBaseData();
    }

    @Test
    void analyzeShouldReturnTeacherOwnClassSummaryAndRespectDimensions() throws Exception {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                59, "SCORED", LocalDateTime.of(2026, 5, 1, 10, 30)).setPublishedAt(null)
                .setObjectiveScore(49)
                .setSubjectiveScore(10));
        repository.saveScore(seedScore(9102L, 8801L, "Java 期中", 501L, "Java 1班", 4003L, "王五",
                60, "PUBLISHED", LocalDateTime.of(2026, 5, 1, 10, 40)).setObjectiveScore(50)
                .setSubjectiveScore(10));
        repository.saveScore(seedScore(9103L, 8801L, "Java 期中", 501L, "Java 1班", 4004L, "赵六",
                89, "PUBLISHED", LocalDateTime.of(2026, 5, 1, 10, 45)).setObjectiveScore(79)
                .setSubjectiveScore(10));
        repository.saveScore(seedScore(9104L, 8801L, "Java 期中", 501L, "Java 1班", 4005L, "孙七",
                90, "PUBLISHED", LocalDateTime.of(2026, 5, 1, 10, 50)).setObjectiveScore(80)
                .setSubjectiveScore(10));
        repository.saveScore(seedScore(9105L, 8801L, "Java 期中", 501L, "Java 1班", 4006L, "周八",
                69, "SCORING", LocalDateTime.of(2026, 5, 1, 10, 55)).setPublishedAt(null)
                .setObjectiveScore(69)
                .setSubjectiveScore(0));
        repository.saveScore(seedScore(9106L, 8801L, "Java 期中", 502L, "Python 1班", 4002L, "李四",
                100, "PUBLISHED", LocalDateTime.of(2026, 5, 1, 11, 0)).setObjectiveScore(90)
                .setSubjectiveScore(10));

        ScoreAnalyzeRequest teacherRequest = new ScoreAnalyzeRequest();
        teacherRequest.setExamId(8801L);

        ScoreAnalyzeView teacherResult = service.analyze(teacherRequest, TEACHER_CONTEXT).getData();

        assertEquals(5, teacherResult.getTotalParticipants());
        assertEquals(4, teacherResult.getFinishedParticipants());
        assertEquals(new BigDecimal("74.50"), teacherResult.getOverview().getAverageScore());
        assertEquals(90, teacherResult.getOverview().getHighestScore());
        assertEquals(59, teacherResult.getOverview().getLowestScore());
        assertEquals(3, teacherResult.getOverview().getPassCount());
        assertEquals(new BigDecimal("75.00"), teacherResult.getOverview().getPassRate());
        assertEquals(1, teacherResult.getStatusDistribution().stream()
                .filter(item -> "SCORING".equals(item.getStatus()))
                .findFirst()
                .orElseThrow()
                .getCount());
        assertEquals(1, teacherResult.getScoreRangeDistribution().stream()
                .filter(item -> "0-59".equals(item.getRangeLabel()))
                .findFirst()
                .orElseThrow()
                .getCount());
        assertEquals(1, teacherResult.getScoreRangeDistribution().stream()
                .filter(item -> "60-69".equals(item.getRangeLabel()))
                .findFirst()
                .orElseThrow()
                .getCount());
        assertEquals(1, teacherResult.getScoreRangeDistribution().stream()
                .filter(item -> "80-89".equals(item.getRangeLabel()))
                .findFirst()
                .orElseThrow()
                .getCount());
        assertEquals(1, teacherResult.getScoreRangeDistribution().stream()
                .filter(item -> "90-100".equals(item.getRangeLabel()))
                .findFirst()
                .orElseThrow()
                .getCount());
        assertEquals(1, teacherResult.getClassDistribution().size());
        assertEquals(501L, teacherResult.getClassDistribution().get(0).getClassId());

        ScoreAnalyzeRequest dimensionRequest = new ScoreAnalyzeRequest();
        dimensionRequest.setExamId(8801L);
        dimensionRequest.setDimensions(List.of("status"));

        ScoreAnalyzeView dimensionResult = service.analyze(dimensionRequest, TEACHER_CONTEXT).getData();
        assertEquals(5, dimensionResult.getTotalParticipants());
        assertEquals(4, dimensionResult.getFinishedParticipants());
        assertEquals(null, dimensionResult.getOverview());
        assertEquals(null, dimensionResult.getScoreRangeDistribution());
        assertEquals(null, dimensionResult.getClassDistribution());
        assertEquals(4, dimensionResult.getStatusDistribution().size());
    }

    @Test
    void analyzeShouldAllowAdminAndAuditorAndSupportClassFilter() throws Exception {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                50, "SCORED", LocalDateTime.of(2026, 5, 1, 10, 30)).setPublishedAt(null));
        repository.saveScore(seedScore(9102L, 8801L, "Java 期中", 502L, "Python 1班", 4002L, "李四",
                100, "PUBLISHED", LocalDateTime.of(2026, 5, 1, 10, 35)));

        ScoreAnalyzeRequest adminRequest = new ScoreAnalyzeRequest();
        adminRequest.setExamId(8801L);

        ScoreAnalyzeView adminResult = service.analyze(adminRequest, ADMIN_CONTEXT).getData();
        assertEquals(2, adminResult.getTotalParticipants());
        assertEquals(2, adminResult.getFinishedParticipants());
        assertEquals(2, adminResult.getClassDistribution().size());

        ScoreAnalyzeRequest auditorRequest = new ScoreAnalyzeRequest();
        auditorRequest.setExamId(8801L);
        auditorRequest.setClassId(502L);

        ScoreAnalyzeView auditorResult = service.analyze(auditorRequest, AUDITOR_CONTEXT).getData();
        assertEquals(1, auditorResult.getTotalParticipants());
        assertEquals(1, auditorResult.getClassDistribution().size());
        assertEquals(502L, auditorResult.getClassDistribution().get(0).getClassId());
    }

    @Test
    void analyzeShouldRejectForbiddenAndInvalidDimension() {
        ScoreAnalyzeRequest request = new ScoreAnalyzeRequest();
        request.setExamId(8801L);

        BusinessException studentException = assertThrows(BusinessException.class,
                () -> service.analyze(request, STUDENT_CONTEXT));
        assertEquals(403, studentException.getCode());
        assertEquals(1614, studentException.getErrorCode());

        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                80, "SCORED", LocalDateTime.of(2026, 5, 1, 10, 30)).setPublishedAt(null));
        request.setClassId(502L);
        BusinessException foreignTeacherException = assertThrows(BusinessException.class,
                () -> service.analyze(request, TEACHER_CONTEXT));
        assertEquals(403, foreignTeacherException.getCode());
        assertEquals(1614, foreignTeacherException.getErrorCode());

        ScoreAnalyzeRequest invalidDimensionRequest = new ScoreAnalyzeRequest();
        invalidDimensionRequest.setExamId(8801L);
        invalidDimensionRequest.setDimensions(List.of("invalid"));
        BusinessException invalidDimensionException = assertThrows(BusinessException.class,
                () -> service.analyze(invalidDimensionRequest, ADMIN_CONTEXT));
        assertEquals(400, invalidDimensionException.getCode());
        assertEquals(1615, invalidDimensionException.getErrorCode());
    }

    @Test
    void analyzeShouldReturnZeroStatsWhenExamExistsButScoresAreEmpty() throws Exception {
        ScoreAnalyzeRequest request = new ScoreAnalyzeRequest();
        request.setExamId(8803L);

        ScoreAnalyzeView result = service.analyze(request, ADMIN_CONTEXT).getData();

        assertEquals(0, result.getTotalParticipants());
        assertEquals(0, result.getFinishedParticipants());
        assertEquals(new BigDecimal("0.00"), result.getOverview().getAverageScore());
        assertEquals(0, result.getOverview().getPassCount());
        assertEquals(0, result.getClassDistribution().size());
        assertEquals(0, result.getScoreRangeDistribution().stream()
                .mapToInt(ScoreAnalyzeView.ScoreRangeDistributionItem::getCount)
                .sum());
    }

    @Test
    void autoScoreShouldSupportStudentTeacherAndAdminWithCurrentStateTransition() throws Exception {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                0, "PENDING", LocalDateTime.of(2026, 5, 1, 10, 30)).setPublishedAt(null)
                .setObjectiveScore(0)
                .setSubjectiveScore(0)
                .setTotalScore(0));
        repository.scoreDetails.add(new ScoreDetailEntity()
                .setDetailId(1001L)
                .setScoreId(9101L)
                .setExamId(8801L)
                .setStudentId(4001L)
                .setQuestionId(7001L)
                .setSortNo(1)
                .setQuestionType("single")
                .setQuestionStem("JDK 默认编码")
                .setStudentAnswer(" A ")
                .setCorrectAnswer("a")
                .setAssignedScore(5)
                .setScore(0)
                .setCorrect(false));
        repository.scoreDetails.add(new ScoreDetailEntity()
                .setDetailId(1002L)
                .setScoreId(9101L)
                .setExamId(8801L)
                .setStudentId(4001L)
                .setQuestionId(7002L)
                .setSortNo(2)
                .setQuestionType("multi")
                .setQuestionStem("选择 JVM 相关项")
                .setStudentAnswer("C,A")
                .setCorrectAnswer("A , C")
                .setAssignedScore(10)
                .setScore(0)
                .setCorrect(false));
        repository.scoreDetails.add(new ScoreDetailEntity()
                .setDetailId(1003L)
                .setScoreId(9101L)
                .setExamId(8801L)
                .setStudentId(4001L)
                .setQuestionId(7003L)
                .setSortNo(3)
                .setQuestionType("subjective")
                .setQuestionStem("说明类加载流程")
                .setAssignedScore(15)
                .setScore(6));

        ScoreAutoScoreRequest studentRequest = new ScoreAutoScoreRequest();
        studentRequest.setExamId(8801L);
        studentRequest.setStudentId(4001L);

        ScoreAutoScoreView studentResult = service.autoScore(studentRequest, STUDENT_CONTEXT).getData();
        assertEquals(15, studentResult.getObjectiveScore());
        assertEquals(21, studentResult.getTotalScore());
        assertEquals("SCORING", studentResult.getStatus());
        assertEquals(2, studentResult.getScoredQuestionCount());
        assertEquals(15, repository.scores.get(9101L).getObjectiveScore());
        assertEquals("SCORING", repository.scores.get(9101L).getStatus());

        repository.scoreDetails.stream()
                .filter(item -> Objects.equals(item.getQuestionId(), 7003L))
                .findFirst()
                .orElseThrow()
                .setReviewComment("");

        ScoreAutoScoreView teacherResult = service.autoScore(studentRequest, TEACHER_CONTEXT).getData();
        assertEquals("SCORED", teacherResult.getStatus());

        repository.scoreDetails.stream()
                .filter(item -> Objects.equals(item.getQuestionId(), 7002L))
                .findFirst()
                .orElseThrow()
                .setStudentAnswer("B,C");
        ScoreAutoScoreView adminResult = service.autoScore(studentRequest, ADMIN_CONTEXT).getData();
        assertEquals(5, adminResult.getObjectiveScore());
        assertEquals(11, adminResult.getTotalScore());
        assertEquals("SCORED", adminResult.getStatus());
        assertEquals(false, repository.scoreDetails.stream()
                .filter(item -> Objects.equals(item.getQuestionId(), 7002L))
                .findFirst()
                .orElseThrow()
                .getCorrect());
    }

    @Test
    void autoScoreShouldRejectForbiddenRoleForeignTeacherAndPublishedRecord() {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                76, "PUBLISHED", LocalDateTime.of(2026, 5, 1, 10, 30)));

        ScoreAutoScoreRequest request = new ScoreAutoScoreRequest();
        request.setExamId(8801L);
        request.setStudentId(4001L);

        BusinessException opsException = assertThrows(BusinessException.class,
                () -> service.autoScore(request, OPS_CONTEXT));
        assertEquals(403, opsException.getCode());
        assertEquals(1612, opsException.getErrorCode());

        repository.scores.get(9101L).setStatus("PENDING").setPublishedAt(null);
        BusinessException foreignTeacherException = assertThrows(BusinessException.class,
                () -> service.autoScore(request, FOREIGN_TEACHER_CONTEXT));
        assertEquals(403, foreignTeacherException.getCode());
        assertEquals(1612, foreignTeacherException.getErrorCode());

        repository.scores.get(9101L).setStatus("PUBLISHED");
        BusinessException publishedException = assertThrows(BusinessException.class,
                () -> service.autoScore(request, ADMIN_CONTEXT));
        assertEquals(409, publishedException.getCode());
        assertEquals(1613, publishedException.getErrorCode());

        ScoreAutoScoreRequest foreignStudentRequest = new ScoreAutoScoreRequest();
        foreignStudentRequest.setExamId(8801L);
        foreignStudentRequest.setStudentId(4002L);
        BusinessException studentException = assertThrows(BusinessException.class,
                () -> service.autoScore(foreignStudentRequest, STUDENT_CONTEXT));
        assertEquals(403, studentException.getCode());
        assertEquals(1612, studentException.getErrorCode());
    }

    @Test
    void queryShouldReturnTeacherOwnClassesOnly() throws Exception {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                86, "PUBLISHED", LocalDateTime.of(2026, 5, 1, 10, 30)));
        repository.saveScore(seedScore(9102L, 8802L, "Python 期中", 502L, "Python 1班", 4002L, "李四",
                92, "PUBLISHED", LocalDateTime.of(2026, 5, 2, 10, 30)));

        ScoreQueryRequest request = new ScoreQueryRequest();
        request.setPageNum(1L);
        request.setPageSize(10L);

        PageResult<ScoreQueryView> result = service.query(request, TEACHER_CONTEXT).getData();

        assertEquals(1, result.getTotal());
        assertEquals(9101L, result.getRecords().get(0).getScoreId());
    }

    @Test
    void queryShouldAllowAdminAndAuditorReadOnlyFilters() throws Exception {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                86, "PUBLISHED", LocalDateTime.of(2026, 5, 1, 10, 30)));
        repository.saveScore(seedScore(9102L, 8801L, "Java 期中", 501L, "Java 1班", 4003L, "王五",
                74, "SCORED", LocalDateTime.of(2026, 5, 1, 10, 31)));
        repository.saveScore(seedScore(9103L, 8802L, "Python 期中", 502L, "Python 1班", 4002L, "李四",
                92, "PUBLISHED", LocalDateTime.of(2026, 5, 2, 10, 30)));

        ScoreQueryRequest request = new ScoreQueryRequest();
        request.setExamId(8801L);
        request.setKeyword("Java");
        request.setStatus("PUBLISHED");
        request.setPageNum(1L);
        request.setPageSize(10L);

        PageResult<ScoreQueryView> adminResult = service.query(request, ADMIN_CONTEXT).getData();
        PageResult<ScoreQueryView> auditorResult = service.query(request, AUDITOR_CONTEXT).getData();

        assertEquals(1, adminResult.getTotal());
        assertEquals(9101L, adminResult.getRecords().get(0).getScoreId());
        assertEquals(1, auditorResult.getTotal());
    }

    @Test
    void queryShouldOnlyAllowStudentQuerySelf() throws Exception {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                86, "PUBLISHED", LocalDateTime.of(2026, 5, 1, 10, 30)));
        repository.saveScore(seedScore(9102L, 8802L, "Python 期中", 502L, "Python 1班", 4002L, "李四",
                92, "PUBLISHED", LocalDateTime.of(2026, 5, 2, 10, 30)));

        ScoreQueryRequest request = new ScoreQueryRequest();
        request.setPageNum(1L);
        request.setPageSize(10L);

        PageResult<ScoreQueryView> result = service.query(request, STUDENT_CONTEXT).getData();
        assertEquals(1, result.getTotal());
        assertEquals(4001L, result.getRecords().get(0).getStudentId());

        ScoreQueryRequest forbiddenRequest = new ScoreQueryRequest();
        forbiddenRequest.setStudentId(4002L);
        forbiddenRequest.setPageNum(1L);
        forbiddenRequest.setPageSize(10L);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.query(forbiddenRequest, STUDENT_CONTEXT));
        assertEquals(403, exception.getCode());
        assertEquals(1602, exception.getErrorCode());
    }

    @Test
    void detailShouldReturnOwnScoreAndOrderedQuestionDetails() throws Exception {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                86, "PUBLISHED", LocalDateTime.of(2026, 5, 1, 10, 30)));
        repository.scoreDetails.add(new ScoreDetailEntity()
                .setDetailId(1002L)
                .setScoreId(9101L)
                .setExamId(8801L)
                .setStudentId(4001L)
                .setQuestionId(7002L)
                .setSortNo(2)
                .setQuestionType("subjective")
                .setQuestionStem("说明 JVM 内存模型")
                .setStudentAnswer("学生答案 2")
                .setAssignedScore(10)
                .setScore(8)
                .setCorrect(false)
                .setReviewComment("要点不全"));
        repository.scoreDetails.add(new ScoreDetailEntity()
                .setDetailId(1001L)
                .setScoreId(9101L)
                .setExamId(8801L)
                .setStudentId(4001L)
                .setQuestionId(7001L)
                .setSortNo(1)
                .setQuestionType("single")
                .setQuestionStem("JDK 版本")
                .setStudentAnswer("A")
                .setCorrectAnswer("A")
                .setAssignedScore(5)
                .setScore(5)
                .setCorrect(true));

        ScoreDetailRequest request = new ScoreDetailRequest();
        request.setExamId(8801L);
        request.setStudentId(4001L);

        ScoreDetailView result = service.detail(request, STUDENT_CONTEXT).getData();

        assertEquals(9101L, result.getScoreId());
        assertEquals(2, result.getQuestionDetails().size());
        assertEquals(7001L, result.getQuestionDetails().get(0).getQuestionId());
        assertEquals(7002L, result.getQuestionDetails().get(1).getQuestionId());
    }

    @Test
    void detailShouldRejectForeignTeacherAndUnknownRole() {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                86, "PUBLISHED", LocalDateTime.of(2026, 5, 1, 10, 30)));

        ScoreDetailRequest request = new ScoreDetailRequest();
        request.setExamId(8801L);
        request.setStudentId(4001L);

        BusinessException foreignTeacherException = assertThrows(BusinessException.class,
                () -> service.detail(request, FOREIGN_TEACHER_CONTEXT));
        assertEquals(403, foreignTeacherException.getCode());
        assertEquals(1603, foreignTeacherException.getErrorCode());

        ScoreQueryRequest queryRequest = new ScoreQueryRequest();
        queryRequest.setPageNum(1L);
        queryRequest.setPageSize(10L);
        BusinessException opsException = assertThrows(BusinessException.class,
                () -> service.query(queryRequest, OPS_CONTEXT));
        assertEquals(403, opsException.getCode());
        assertEquals(1602, opsException.getErrorCode());
    }

    @Test
    void publishShouldAllowTeacherOrAdminPublishScoredRecords() throws Exception {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                86, "SCORED", LocalDateTime.of(2026, 5, 1, 10, 30)).setPublishedAt(null));
        repository.saveScore(seedScore(9102L, 8801L, "Java 期中", 501L, "Java 1班", 4003L, "王五",
                74, "PUBLISHED", LocalDateTime.of(2026, 5, 1, 11, 0)));

        ScorePublishRequest request = new ScorePublishRequest();
        request.setExamId(8801L);
        request.setClassId(501L);

        ScorePublishView teacherResult = service.publish(request, TEACHER_CONTEXT).getData();
        ScorePublishView adminResult = service.publish(request, ADMIN_CONTEXT).getData();

        assertEquals("PUBLISHED", teacherResult.getStatus());
        assertEquals(1, teacherResult.getPublishedCount());
        assertEquals("PUBLISHED", repository.scores.get(9101L).getStatus());
        assertEquals(0, adminResult.getPublishedCount());
    }

    @Test
    void publishShouldRejectForeignTeacherAndUnscoredRecords() {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                86, "SCORED", LocalDateTime.of(2026, 5, 1, 10, 30)).setPublishedAt(null));
        repository.saveScore(seedScore(9102L, 8802L, "Python 期中", 502L, "Python 1班", 4002L, "李四",
                92, "SCORING", LocalDateTime.of(2026, 5, 2, 10, 30)).setPublishedAt(null));

        ScorePublishRequest foreignRequest = new ScorePublishRequest();
        foreignRequest.setExamId(8801L);
        foreignRequest.setClassId(501L);
        BusinessException foreignTeacherException = assertThrows(BusinessException.class,
                () -> service.publish(foreignRequest, FOREIGN_TEACHER_CONTEXT));
        assertEquals(403, foreignTeacherException.getCode());
        assertEquals(1604, foreignTeacherException.getErrorCode());

        ScorePublishRequest conflictRequest = new ScorePublishRequest();
        conflictRequest.setExamId(8802L);
        conflictRequest.setClassId(502L);
        BusinessException conflictException = assertThrows(BusinessException.class,
                () -> service.publish(conflictRequest, ADMIN_CONTEXT));
        assertEquals(409, conflictException.getCode());
        assertEquals(1606, conflictException.getErrorCode());
    }

    @Test
    void manualScoreShouldAllowTeacherOrAdminUpdateSubjectiveQuestion() throws Exception {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                76, "SCORING", LocalDateTime.of(2026, 5, 1, 10, 30)).setPublishedAt(null));
        repository.scoreDetails.add(new ScoreDetailEntity()
                .setDetailId(1001L)
                .setScoreId(9101L)
                .setExamId(8801L)
                .setStudentId(4001L)
                .setQuestionId(7001L)
                .setSortNo(1)
                .setQuestionType("single")
                .setQuestionStem("JDK 版本")
                .setAssignedScore(5)
                .setScore(5)
                .setCorrect(true));
        repository.scoreDetails.add(new ScoreDetailEntity()
                .setDetailId(1002L)
                .setScoreId(9101L)
                .setExamId(8801L)
                .setStudentId(4001L)
                .setQuestionId(7002L)
                .setSortNo(2)
                .setQuestionType("subjective")
                .setQuestionStem("说明 JVM 内存模型")
                .setAssignedScore(10)
                .setScore(0));
        repository.scoreDetails.add(new ScoreDetailEntity()
                .setDetailId(1003L)
                .setScoreId(9101L)
                .setExamId(8801L)
                .setStudentId(4001L)
                .setQuestionId(7003L)
                .setSortNo(3)
                .setQuestionType("subjective")
                .setQuestionStem("解释 GC Roots")
                .setAssignedScore(10)
                .setScore(10)
                .setReviewComment(""));

        ScoreManualScoreRequest teacherRequest = new ScoreManualScoreRequest();
        teacherRequest.setExamId(8801L);
        teacherRequest.setStudentId(4001L);
        teacherRequest.setQuestionId(7002L);
        teacherRequest.setScore(8);
        teacherRequest.setComment("要点基本完整");

        ScoreManualScoreView teacherResult = service.manualScore(teacherRequest, TEACHER_CONTEXT).getData();

        assertEquals("SCORED", teacherResult.getStatus());
        assertEquals(18, teacherResult.getSubjectiveScore());
        assertEquals(84, teacherResult.getTotalScore());
        assertEquals("要点基本完整", repository.scoreDetails.stream()
                .filter(item -> Objects.equals(item.getQuestionId(), 7002L))
                .findFirst()
                .orElseThrow()
                .getReviewComment());

        ScoreManualScoreRequest adminRequest = new ScoreManualScoreRequest();
        adminRequest.setExamId(8801L);
        adminRequest.setStudentId(4001L);
        adminRequest.setQuestionId(7003L);
        adminRequest.setScore(9);

        ScoreManualScoreView adminResult = service.manualScore(adminRequest, ADMIN_CONTEXT).getData();
        assertEquals("SCORED", adminResult.getStatus());
        assertEquals(17, adminResult.getSubjectiveScore());
        assertEquals(83, adminResult.getTotalScore());
        assertEquals("", adminResult.getReviewComment());
    }

    @Test
    void manualScoreShouldRejectForeignTeacherPublishedRecordAndInvalidQuestion() {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                76, "PUBLISHED", LocalDateTime.of(2026, 5, 1, 10, 30)));
        repository.scoreDetails.add(new ScoreDetailEntity()
                .setDetailId(1001L)
                .setScoreId(9101L)
                .setExamId(8801L)
                .setStudentId(4001L)
                .setQuestionId(7001L)
                .setSortNo(1)
                .setQuestionType("single")
                .setQuestionStem("JDK 版本")
                .setAssignedScore(5)
                .setScore(5)
                .setCorrect(true));
        repository.scoreDetails.add(new ScoreDetailEntity()
                .setDetailId(1002L)
                .setScoreId(9101L)
                .setExamId(8801L)
                .setStudentId(4001L)
                .setQuestionId(7002L)
                .setSortNo(2)
                .setQuestionType("subjective")
                .setQuestionStem("说明 JVM 内存模型")
                .setAssignedScore(10)
                .setScore(0));

        ScoreManualScoreRequest foreignRequest = new ScoreManualScoreRequest();
        foreignRequest.setExamId(8801L);
        foreignRequest.setStudentId(4001L);
        foreignRequest.setQuestionId(7002L);
        foreignRequest.setScore(8);
        BusinessException foreignTeacherException = assertThrows(BusinessException.class,
                () -> service.manualScore(foreignRequest, FOREIGN_TEACHER_CONTEXT));
        assertEquals(403, foreignTeacherException.getCode());
        assertEquals(1607, foreignTeacherException.getErrorCode());

        ScoreManualScoreRequest publishedRequest = new ScoreManualScoreRequest();
        publishedRequest.setExamId(8801L);
        publishedRequest.setStudentId(4001L);
        publishedRequest.setQuestionId(7002L);
        publishedRequest.setScore(8);
        BusinessException publishedException = assertThrows(BusinessException.class,
                () -> service.manualScore(publishedRequest, ADMIN_CONTEXT));
        assertEquals(409, publishedException.getCode());
        assertEquals(1610, publishedException.getErrorCode());

        repository.scores.get(9101L).setStatus("SCORING");
        ScoreManualScoreRequest objectiveRequest = new ScoreManualScoreRequest();
        objectiveRequest.setExamId(8801L);
        objectiveRequest.setStudentId(4001L);
        objectiveRequest.setQuestionId(7001L);
        objectiveRequest.setScore(5);
        BusinessException objectiveException = assertThrows(BusinessException.class,
                () -> service.manualScore(objectiveRequest, ADMIN_CONTEXT));
        assertEquals(409, objectiveException.getCode());
        assertEquals(1611, objectiveException.getErrorCode());

        ScoreManualScoreRequest invalidScoreRequest = new ScoreManualScoreRequest();
        invalidScoreRequest.setExamId(8801L);
        invalidScoreRequest.setStudentId(4001L);
        invalidScoreRequest.setQuestionId(7002L);
        invalidScoreRequest.setScore(11);
        BusinessException invalidScoreException = assertThrows(BusinessException.class,
                () -> service.manualScore(invalidScoreRequest, ADMIN_CONTEXT));
        assertEquals(400, invalidScoreException.getCode());
        assertEquals(1608, invalidScoreException.getErrorCode());
    }

    @Test
    void applyRecheckShouldCreatePendingAppealAndReflectInQuery() throws Exception {
        LocalDateTime publishedAt = LocalDateTime.now().minusDays(2);
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                86, "PUBLISHED", publishedAt));
        repository.scoreDetails.add(new ScoreDetailEntity()
                .setDetailId(1001L)
                .setScoreId(9101L)
                .setExamId(8801L)
                .setStudentId(4001L)
                .setQuestionId(7001L)
                .setSortNo(1)
                .setQuestionType("subjective")
                .setQuestionStem("说明 JVM 内存模型")
                .setAssignedScore(20)
                .setScore(16));

        ScoreApplyRecheckRequest request = new ScoreApplyRecheckRequest();
        request.setExamId(8801L);
        request.setStudentId(4001L);
        request.setQuestionId(7001L);
        request.setReason("主观题给分偏低");

        ScoreApplyRecheckView result = service.applyRecheck(request, STUDENT_CONTEXT).getData();

        assertNotNull(result.getAppealId());
        assertEquals("PENDING", result.getStatus());
        assertEquals("pending", result.getRecheckStatus());
        assertEquals(1, repository.appeals.size());

        ScoreQueryRequest queryRequest = new ScoreQueryRequest();
        queryRequest.setPageNum(1L);
        queryRequest.setPageSize(10L);
        ScoreQueryView queryView = service.query(queryRequest, STUDENT_CONTEXT).getData().getRecords().get(0);
        assertEquals("published", queryView.getPublishStatus());
        assertEquals("pending", queryView.getRecheckStatus());
        assertEquals(result.getAppealId(), queryView.getAppealId());

        BusinessException duplicateException = assertThrows(BusinessException.class,
                () -> service.applyRecheck(request, STUDENT_CONTEXT));
        assertEquals(409, duplicateException.getCode());
        assertEquals(1617, duplicateException.getErrorCode());
    }

    @Test
    void applyRecheckShouldRejectExpiredPublishedRecordAndForeignStudent() {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                86, "PUBLISHED", LocalDateTime.now().minusDays(8)));
        repository.scoreDetails.add(new ScoreDetailEntity()
                .setDetailId(1001L)
                .setScoreId(9101L)
                .setExamId(8801L)
                .setStudentId(4001L)
                .setQuestionId(7001L)
                .setSortNo(1)
                .setQuestionType("subjective")
                .setAssignedScore(20)
                .setScore(16));

        ScoreApplyRecheckRequest expiredRequest = new ScoreApplyRecheckRequest();
        expiredRequest.setExamId(8801L);
        expiredRequest.setStudentId(4001L);
        expiredRequest.setQuestionId(7001L);
        expiredRequest.setReason("超过时限");

        BusinessException expiredException = assertThrows(BusinessException.class,
                () -> service.applyRecheck(expiredRequest, STUDENT_CONTEXT));
        assertEquals(409, expiredException.getCode());
        assertEquals(1617, expiredException.getErrorCode());

        ScoreApplyRecheckRequest foreignStudentRequest = new ScoreApplyRecheckRequest();
        foreignStudentRequest.setExamId(8801L);
        foreignStudentRequest.setStudentId(4002L);
        foreignStudentRequest.setQuestionId(7001L);
        foreignStudentRequest.setReason("非本人");
        BusinessException foreignStudentException = assertThrows(BusinessException.class,
                () -> service.applyRecheck(foreignStudentRequest, STUDENT_CONTEXT));
        assertEquals(403, foreignStudentException.getCode());
        assertEquals(1616, foreignStudentException.getErrorCode());
    }

    @Test
    void handleAppealShouldSupportTeacherApproveAndRejectFlow() throws Exception {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                86, "PUBLISHED", LocalDateTime.now().minusDays(1)));
        repository.appeals.put(9301L, new ScoreAppealEntity()
                .setAppealId(9301L)
                .setScoreId(9101L)
                .setExamId(8801L)
                .setClassId(501L)
                .setStudentId(4001L)
                .setQuestionId(7001L)
                .setAppealReason("重新复核")
                .setStatus("PENDING")
                .setCreateTime(LocalDateTime.now().minusHours(2))
                .setUpdateTime(LocalDateTime.now().minusHours(2)));

        ScoreHandleAppealRequest approveRequest = new ScoreHandleAppealRequest();
        approveRequest.setAppealId(9301L);
        approveRequest.setHandleResult("approved");
        approveRequest.setReason("同意重新批阅");

        ScoreHandleAppealView approveResult = service.handleAppeal(approveRequest, TEACHER_CONTEXT).getData();
        assertEquals("APPROVED", approveResult.getStatus());
        assertEquals("processed", approveResult.getRecheckStatus());
        assertEquals("SCORING", approveResult.getScoreStatus());
        assertEquals("SCORING", repository.scores.get(9101L).getStatus());
        assertEquals(null, repository.scores.get(9101L).getPublishedAt());

        repository.saveScore(seedScore(9102L, 8802L, "Python 期中", 502L, "Python 1班", 4002L, "李四",
                90, "PUBLISHED", LocalDateTime.now().minusDays(1)));
        repository.appeals.put(9302L, new ScoreAppealEntity()
                .setAppealId(9302L)
                .setScoreId(9102L)
                .setExamId(8802L)
                .setClassId(502L)
                .setStudentId(4002L)
                .setQuestionId(8001L)
                .setAppealReason("重新复核")
                .setStatus("PENDING")
                .setCreateTime(LocalDateTime.now().minusHours(1))
                .setUpdateTime(LocalDateTime.now().minusHours(1)));

        ScoreHandleAppealRequest forbiddenRequest = new ScoreHandleAppealRequest();
        forbiddenRequest.setAppealId(9302L);
        forbiddenRequest.setHandleResult("rejected");
        forbiddenRequest.setReason("非本人班级");
        BusinessException forbiddenException = assertThrows(BusinessException.class,
                () -> service.handleAppeal(forbiddenRequest, TEACHER_CONTEXT));
        assertEquals(403, forbiddenException.getCode());
        assertEquals(1619, forbiddenException.getErrorCode());
    }

    @Test
    void exportShouldRespectTeacherScopeAndReturnMetadata() throws Exception {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                86, "PUBLISHED", LocalDateTime.now().minusDays(1)));
        repository.saveScore(seedScore(9102L, 8802L, "Python 期中", 502L, "Python 1班", 4002L, "李四",
                92, "PUBLISHED", LocalDateTime.now().minusDays(1)));

        ScoreExportView teacherResult = service.export(null, null, false, TEACHER_CONTEXT).getData();
        ScoreExportView adminResult = service.export(null, null, true, ADMIN_CONTEXT).getData();

        assertEquals(1, teacherResult.getRecordCount());
        assertEquals(false, teacherResult.getIncludeAnalysis());
        assertEquals(2, adminResult.getRecordCount());
        assertEquals(true, adminResult.getIncludeAnalysis());
        assertNotNull(adminResult.getFileKey());
        assertEquals(".csv", adminResult.getFileName().substring(adminResult.getFileName().length() - 4));
        assertTrue(Files.readString(tempDir.resolve("exports").resolve(teacherResult.getFileKey() + ".csv"))
                .contains("9101"));
        assertTrue(Files.readString(tempDir.resolve("exports").resolve(adminResult.getFileKey() + ".csv"))
                .contains("analysisIncluded,true"));
    }

    @Test
    void updateShouldRequireSuperAdminAndPersistChangeLog() throws Exception {
        repository.saveScore(seedScore(9101L, 8801L, "Java 期中", 501L, "Java 1班", 4001L, "张三",
                86, "PUBLISHED", LocalDateTime.now().minusDays(1))
                .setObjectiveScore(60)
                .setSubjectiveScore(26)
                .setTotalScore(86));

        ScoreUpdateRequest request = new ScoreUpdateRequest();
        request.setScoreId(9101L);
        request.setNewScore(95);
        request.setReason("复核后调分");
        request.setApproverId(1002L);

        ScoreUpdateView result = service.update(request, SUPER_ADMIN_CONTEXT).getData();

        assertEquals(86, result.getPreviousTotalScore());
        assertEquals(95, result.getTotalScore());
        assertEquals(35, result.getSubjectiveScore());
        assertEquals(1, repository.changeLogs.size());
        assertEquals(95, repository.scores.get(9101L).getTotalScore());
        assertEquals(35, repository.scores.get(9101L).getSubjectiveScore());

        BusinessException forbiddenException = assertThrows(BusinessException.class,
                () -> service.update(request, ADMIN_CONTEXT));
        assertEquals(403, forbiddenException.getCode());
        assertEquals(1623, forbiddenException.getErrorCode());

        ScoreUpdateRequest invalidApproverRequest = new ScoreUpdateRequest();
        invalidApproverRequest.setScoreId(9101L);
        invalidApproverRequest.setNewScore(96);
        invalidApproverRequest.setReason("审批人非法");
        invalidApproverRequest.setApproverId(1001L);
        BusinessException invalidApproverException = assertThrows(BusinessException.class,
                () -> service.update(invalidApproverRequest, SUPER_ADMIN_CONTEXT));
        assertEquals(400, invalidApproverException.getCode());
        assertEquals(1624, invalidApproverException.getErrorCode());
    }

    private void seedBaseData() {
        repository.classes.put(501L, new ClassSnapshotEntity()
                .setClassId(501L)
                .setClassName("Java 1班")
                .setTeacherId(3001L)
                .setCreatedBy(3001L)
                .setStatus("active"));
        repository.classes.put(502L, new ClassSnapshotEntity()
                .setClassId(502L)
                .setClassName("Python 1班")
                .setTeacherId(3002L)
                .setCreatedBy(3002L)
                .setStatus("active"));
        repository.exams.put(8801L, new ExamSnapshotEntity()
                .setExamId(8801L)
                .setExamName("Java 期中")
                .setPaperId(9901L)
                .setCreatorId(3001L));
        repository.exams.put(8802L, new ExamSnapshotEntity()
                .setExamId(8802L)
                .setExamName("Python 期中")
                .setPaperId(9902L)
                .setCreatorId(3002L));
        repository.exams.put(8803L, new ExamSnapshotEntity()
                .setExamId(8803L)
                .setExamName("空成绩考试")
                .setPaperId(9903L)
                .setCreatorId(3001L));
        repository.papers.put(9901L, new PaperSnapshotEntity()
                .setPaperId(9901L)
                .setPassScore(60)
                .setTotalScore(100));
        repository.papers.put(9902L, new PaperSnapshotEntity()
                .setPaperId(9902L)
                .setPassScore(70)
                .setTotalScore(100));
        repository.papers.put(9903L, new PaperSnapshotEntity()
                .setPaperId(9903L)
                .setPassScore(60)
                .setTotalScore(100));
    }

    private ScoreRecordEntity seedScore(Long scoreId,
                                        Long examId,
                                        String examName,
                                        Long classId,
                                        String className,
                                        Long studentId,
                                        String studentName,
                                        Integer totalScore,
                                        String status,
                                        LocalDateTime publishedAt) {
        return new ScoreRecordEntity()
                .setScoreId(scoreId)
                .setExamId(examId)
                .setExamName(examName)
                .setPaperId(9901L)
                .setPaperName("期中试卷")
                .setClassId(classId)
                .setClassName(className)
                .setStudentId(studentId)
                .setStudentName(studentName)
                .setTotalScore(totalScore)
                .setObjectiveScore(Math.max(0, totalScore - 10))
                .setSubjectiveScore(10)
                .setStatus(status)
                .setSubmittedAt(publishedAt.minusMinutes(20))
                .setPublishedAt(publishedAt)
                .setRequestId("seed-" + scoreId)
                .setCreateTime(publishedAt.minusMinutes(30))
                .setUpdateTime(publishedAt);
    }

    private static final class InMemoryScoreDomainRepository implements ScoreDomainRepository {

        private final Map<Long, ScoreRecordEntity> scores = new LinkedHashMap<>();
        private final List<ScoreDetailEntity> scoreDetails = new ArrayList<>();
        private final Map<Long, ClassSnapshotEntity> classes = new LinkedHashMap<>();
        private final Map<Long, ExamSnapshotEntity> exams = new LinkedHashMap<>();
        private final Map<Long, PaperSnapshotEntity> papers = new LinkedHashMap<>();
        private final Map<Long, ScoreAppealEntity> appeals = new LinkedHashMap<>();
        private final List<ScoreChangeLogEntity> changeLogs = new ArrayList<>();

        @Override
        public List<ScoreRecordEntity> listScores() {
            return new ArrayList<>(scores.values());
        }

        @Override
        public List<ScoreRecordEntity> listScoresByExamId(Long examId) {
            return scores.values().stream()
                    .filter(item -> Objects.equals(item.getExamId(), examId))
                    .toList();
        }

        @Override
        public List<ScoreRecordEntity> listScoresByExamIdAndClassId(Long examId, Long classId) {
            return scores.values().stream()
                    .filter(item -> Objects.equals(item.getExamId(), examId)
                            && Objects.equals(item.getClassId(), classId))
                    .toList();
        }

        @Override
        public Optional<ScoreRecordEntity> findScoreById(Long scoreId) {
            return Optional.ofNullable(scores.get(scoreId));
        }

        @Override
        public Optional<ScoreRecordEntity> findScoreByExamIdAndStudentId(Long examId, Long studentId) {
            return scores.values().stream()
                    .filter(item -> Objects.equals(item.getExamId(), examId)
                            && Objects.equals(item.getStudentId(), studentId))
                    .findFirst();
        }

        @Override
        public Optional<ExamSnapshotEntity> findExamById(Long examId) {
            return Optional.ofNullable(exams.get(examId));
        }

        @Override
        public Optional<PaperSnapshotEntity> findPaperById(Long paperId) {
            return Optional.ofNullable(papers.get(paperId));
        }

        @Override
        public ScoreRecordEntity updateScore(ScoreRecordEntity score) {
            scores.put(score.getScoreId(), score);
            return score;
        }

        @Override
        public ScoreDetailEntity updateScoreDetail(ScoreDetailEntity detail) {
            for (int index = 0; index < scoreDetails.size(); index++) {
                if (Objects.equals(scoreDetails.get(index).getDetailId(), detail.getDetailId())) {
                    scoreDetails.set(index, detail);
                    return detail;
                }
            }
            scoreDetails.add(detail);
            return detail;
        }

        @Override
        public List<ScoreDetailEntity> listScoreDetails(Long scoreId) {
            return scoreDetails.stream()
                    .filter(item -> Objects.equals(item.getScoreId(), scoreId))
                    .toList();
        }

        @Override
        public List<ClassSnapshotEntity> findClassesByIds(java.util.Collection<Long> classIds) {
            return classIds.stream()
                    .map(classes::get)
                    .filter(Objects::nonNull)
                    .toList();
        }

        @Override
        public Optional<ScoreAppealEntity> findAppealById(Long appealId) {
            return Optional.ofNullable(appeals.get(appealId));
        }

        @Override
        public List<ScoreAppealEntity> listAppealsByScoreIds(java.util.Collection<Long> scoreIds) {
            return appeals.values().stream()
                    .filter(item -> scoreIds.contains(item.getScoreId()))
                    .toList();
        }

        @Override
        public ScoreAppealEntity saveAppeal(ScoreAppealEntity appeal) {
            appeals.put(appeal.getAppealId(), appeal);
            return appeal;
        }

        @Override
        public ScoreAppealEntity updateAppeal(ScoreAppealEntity appeal) {
            appeals.put(appeal.getAppealId(), appeal);
            return appeal;
        }

        @Override
        public void saveScoreChangeLog(ScoreChangeLogEntity changeLog) {
            changeLogs.add(changeLog);
        }

        private void saveScore(ScoreRecordEntity score) {
            scores.put(score.getScoreId(), score);
        }
    }
}
