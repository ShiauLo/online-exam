package com.maghert.examresource.service;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examresource.context.RequestContext;
import com.maghert.examresource.model.vo.ResourceFileDownloadView;
import com.maghert.examresource.model.vo.ResourcePaperTemplateView;
import com.maghert.examresource.model.vo.ResourceQuestionImageView;
import com.maghert.examresource.service.impl.ExamResourceServiceImpl;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamResourceServiceImplTests {

    private static final RequestContext TEACHER_CONTEXT = new RequestContext(3001L, 3, "req-teacher");
    private static final RequestContext ADMIN_CONTEXT = new RequestContext(2001L, 2, "req-admin");
    private static final RequestContext AUDITOR_CONTEXT = new RequestContext(5001L, 5, "req-auditor");
    private static final RequestContext STUDENT_CONTEXT = new RequestContext(4001L, 4, "req-student");
    private static final RequestContext OPS_CONTEXT = new RequestContext(6001L, 6, "req-ops");

    @Test
    void questionImageShouldAllowTeacherAndStudentAndCreateLocalPlaceholder() throws Exception {
        Path tempDir = Files.createDirectories(Path.of("target", "resource-service-tests", Long.toString(System.nanoTime())));
        ExamResourceServiceImpl service = new ExamResourceServiceImpl(tempDir.toString());

        ResourceQuestionImageView teacherResult = service.questionImage("question-img-001", null, TEACHER_CONTEXT).getData();
        ResourceQuestionImageView studentResult = service.questionImage("question-img-001", null, STUDENT_CONTEXT).getData();

        assertEquals("question-img-001", teacherResult.getImgKey());
        assertEquals("image/svg+xml", teacherResult.getContentType());
        assertTrue(teacherResult.getAccessPath().contains("question-img-001"));
        assertEquals(teacherResult.getFileName(), studentResult.getFileName());
        assertTrue(Files.exists(tempDir.resolve("question-images").resolve("question-img-001.svg")));

        BusinessException forbiddenException = assertThrows(BusinessException.class,
                () -> service.questionImage("question-img-001", null, OPS_CONTEXT));
        assertEquals(403, forbiddenException.getCode());
        assertEquals(1702, forbiddenException.getErrorCode());
    }

    @Test
    void paperTemplateShouldValidatePaperTypeAndScope() throws Exception {
        Path tempDir = Files.createDirectories(Path.of("target", "resource-service-tests", Long.toString(System.nanoTime())));
        ExamResourceServiceImpl service = new ExamResourceServiceImpl(tempDir.toString());

        ResourcePaperTemplateView result = service.paperTemplate("mix", null, TEACHER_CONTEXT).getData();

        assertEquals("mix", result.getPaperType());
        assertEquals("text/csv;charset=UTF-8", result.getContentType());
        assertTrue(result.getDownloadPath().contains("paperType=mix"));
        assertTrue(Files.exists(tempDir.resolve("paper-templates").resolve("paper-template-mix.csv")));

        BusinessException invalidTypeException = assertThrows(BusinessException.class,
                () -> service.paperTemplate("essay", null, ADMIN_CONTEXT));
        assertEquals(400, invalidTypeException.getCode());
        assertEquals(1704, invalidTypeException.getErrorCode());

        BusinessException studentForbiddenException = assertThrows(BusinessException.class,
                () -> service.paperTemplate("single", null, STUDENT_CONTEXT));
        assertEquals(403, studentForbiddenException.getCode());
        assertEquals(1702, studentForbiddenException.getErrorCode());
    }

    @Test
    void fileDownloadShouldEnforceRoleScopeByFileKeyPrefix() throws Exception {
        Path tempDir = Files.createDirectories(Path.of("target", "resource-service-tests", Long.toString(System.nanoTime())));
        ExamResourceServiceImpl service = new ExamResourceServiceImpl(tempDir.toString());
        Files.createDirectories(tempDir.resolve("exports"));
        Files.writeString(tempDir.resolve("exports").resolve("score-export-001.csv"), "scoreId,totalScore\n9101,86\n");
        Files.writeString(tempDir.resolve("exports").resolve("system-log-export-001.csv"), "logId,logType\n1,config\n");

        ResourceFileDownloadView scoreExport = service.downloadFile("score-export-001", null, TEACHER_CONTEXT).getData();
        ResourceFileDownloadView systemExport = service.downloadFile("system-log-export-001", null, AUDITOR_CONTEXT).getData();

        assertEquals("score-export-001", scoreExport.getFileKey());
        assertEquals("system-log-export-001.csv", systemExport.getFileName());
        assertTrue(Files.exists(tempDir.resolve("exports").resolve("score-export-001.csv")));
        assertTrue(Files.exists(tempDir.resolve("exports").resolve("system-log-export-001.csv")));

        BusinessException teacherForbiddenException = assertThrows(BusinessException.class,
                () -> service.downloadFile("system-log-export-001", null, TEACHER_CONTEXT));
        assertEquals(403, teacherForbiddenException.getCode());
        assertEquals(1702, teacherForbiddenException.getErrorCode());

        BusinessException invalidKeyException = assertThrows(BusinessException.class,
                () -> service.downloadFile("../bad-key", null, ADMIN_CONTEXT));
        assertEquals(400, invalidKeyException.getCode());
        assertEquals(1703, invalidKeyException.getErrorCode());

        BusinessException missingFileException = assertThrows(BusinessException.class,
                () -> service.downloadFile("paper-export-missing", null, ADMIN_CONTEXT));
        assertEquals(404, missingFileException.getCode());
        assertEquals(1701, missingFileException.getErrorCode());
    }
}
