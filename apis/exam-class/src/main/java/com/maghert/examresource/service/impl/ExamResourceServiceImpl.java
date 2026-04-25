package com.maghert.examresource.service.impl;

import com.maghert.examcommon.auth.RoleMappings;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;
import com.maghert.examcommon.resource.LocalResourceFileStore;
import com.maghert.examcommon.resource.StoredResourceFile;
import com.maghert.examcommon.web.ApiResponse;
import com.maghert.examresource.context.RequestContext;
import com.maghert.examresource.model.vo.ResourceFileDownloadView;
import com.maghert.examresource.model.vo.ResourcePaperTemplateView;
import com.maghert.examresource.model.vo.ResourceQuestionImageView;
import com.maghert.examresource.service.ExamResourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

@Service
public class ExamResourceServiceImpl implements ExamResourceService {

    private static final Set<String> PAPER_TEMPLATE_TYPES = Set.of("single", "multi", "mix");
    private static final String CONTENT_TYPE_CSV = "text/csv;charset=UTF-8";
    private static final String CONTENT_TYPE_SVG = "image/svg+xml";

    private final Path storageRoot;
    private final LocalResourceFileStore resourceFileStore;

    public ExamResourceServiceImpl(@Value("${exam.resource.local-storage-root}") String localStorageRoot) {
        this.storageRoot = Paths.get(localStorageRoot).toAbsolutePath().normalize();
        this.resourceFileStore = new LocalResourceFileStore(localStorageRoot);
    }

    @Override
    public ApiResponse<ResourceQuestionImageView> questionImage(String imgKey, String token, RequestContext context)
            throws BusinessException {
        ensureQuestionImageOperator(context);
        String normalizedImgKey = normalizeKey(imgKey);
        Path imagePath = resolveWithinRoot("question-images", normalizedImgKey + ".svg");
        ensureQuestionImageFile(imagePath, normalizedImgKey);
        return ApiResponse.ok(ResourceQuestionImageView.builder()
                        .imgKey(normalizedImgKey)
                        .fileName(imagePath.getFileName().toString())
                        .contentType(CONTENT_TYPE_SVG)
                        .accessPath("/api/resource/question/img?imgKey=" + normalizedImgKey)
                        .size(fileSize(imagePath))
                        .build())
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<ResourcePaperTemplateView> paperTemplate(String paperType, String token, RequestContext context)
            throws BusinessException {
        ensurePaperTemplateOperator(context);
        String normalizedPaperType = normalizePaperType(paperType);
        Path templatePath = resolveWithinRoot("paper-templates",
                "paper-template-" + normalizedPaperType + ".csv");
        ensurePaperTemplateFile(templatePath, normalizedPaperType);
        return ApiResponse.ok(ResourcePaperTemplateView.builder()
                        .paperType(normalizedPaperType)
                        .fileName(templatePath.getFileName().toString())
                        .contentType(CONTENT_TYPE_CSV)
                        .downloadPath("/api/resource/paper/template?paperType=" + normalizedPaperType)
                        .size(fileSize(templatePath))
                        .build())
                .withRequestId(context.requestId());
    }

    @Override
    public ApiResponse<ResourceFileDownloadView> downloadFile(String fileKey, String token, RequestContext context)
            throws BusinessException {
        ensureFileDownloadOperator(context, fileKey);
        String normalizedFileKey = normalizeKey(fileKey);
        StoredResourceFile storedFile = resourceFileStore.getExisting(normalizedFileKey, resolveFileExtension(normalizedFileKey));
        return ApiResponse.ok(ResourceFileDownloadView.builder()
                        .fileKey(normalizedFileKey)
                        .fileName(storedFile.storedFileName())
                        .contentType(resolveContentType(storedFile.extension()))
                        .downloadPath("/api/resource/file/download?fileKey=" + normalizedFileKey)
                        .size(storedFile.size())
                        .build())
                .withRequestId(context.requestId());
    }

    private void ensureQuestionImageOperator(RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isTeacher(context.roleId())
                && !RoleMappings.isAdmin(context.roleId())
                && !RoleMappings.isAuditor(context.roleId())
                && !RoleMappings.isStudent(context.roleId())) {
            throw new BusinessException(DomainErrorCode.RESOURCE_ACCESS_FORBIDDEN);
        }
    }

    private void ensurePaperTemplateOperator(RequestContext context) throws BusinessException {
        ensureAuthenticated(context);
        if (!RoleMappings.isTeacher(context.roleId()) && !RoleMappings.isAdmin(context.roleId())) {
            throw new BusinessException(DomainErrorCode.RESOURCE_ACCESS_FORBIDDEN);
        }
    }

    private void ensureFileDownloadOperator(RequestContext context, String fileKey) throws BusinessException {
        ensureAuthenticated(context);
        String normalizedFileKey = normalizeKey(fileKey);
        String lowerKey = normalizedFileKey.toLowerCase(Locale.ROOT);
        if (lowerKey.startsWith("system-log-export-")) {
            if (!RoleMappings.isAdmin(context.roleId()) && !RoleMappings.isAuditor(context.roleId())) {
                throw new BusinessException(DomainErrorCode.RESOURCE_ACCESS_FORBIDDEN);
            }
            return;
        }
        if (lowerKey.startsWith("question-export-")
                || lowerKey.startsWith("paper-export-")
                || lowerKey.startsWith("score-export-")) {
            if (!RoleMappings.isTeacher(context.roleId())
                    && !RoleMappings.isAdmin(context.roleId())
                    && !RoleMappings.isAuditor(context.roleId())) {
                throw new BusinessException(DomainErrorCode.RESOURCE_ACCESS_FORBIDDEN);
            }
            return;
        }
        if (!RoleMappings.isAdmin(context.roleId())) {
            throw new BusinessException(DomainErrorCode.RESOURCE_ACCESS_FORBIDDEN);
        }
    }

    private void ensureAuthenticated(RequestContext context) throws BusinessException {
        if (context == null || context.userId() == null || context.roleId() == null) {
            throw new BusinessException(DomainErrorCode.AUTHENTICATION_REQUIRED);
        }
    }

    private String normalizeKey(String key) throws BusinessException {
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(DomainErrorCode.RESOURCE_KEY_INVALID);
        }
        String normalized = key.trim();
        if (!normalized.matches("[A-Za-z0-9._-]+")) {
            throw new BusinessException(DomainErrorCode.RESOURCE_KEY_INVALID);
        }
        return normalized;
    }

    private String normalizePaperType(String paperType) throws BusinessException {
        if (!StringUtils.hasText(paperType)) {
            throw new BusinessException(DomainErrorCode.RESOURCE_TEMPLATE_TYPE_INVALID);
        }
        String normalized = paperType.trim().toLowerCase(Locale.ROOT);
        if (!PAPER_TEMPLATE_TYPES.contains(normalized)) {
            throw new BusinessException(DomainErrorCode.RESOURCE_TEMPLATE_TYPE_INVALID);
        }
        return normalized;
    }

    private Path resolveWithinRoot(String folderName, String fileName) throws BusinessException {
        try {
            Files.createDirectories(storageRoot);
            Path target = storageRoot.resolve(folderName).resolve(fileName).normalize();
            if (!target.startsWith(storageRoot)) {
                throw new BusinessException(DomainErrorCode.RESOURCE_KEY_INVALID);
            }
            Files.createDirectories(target.getParent());
            return target;
        } catch (IOException exception) {
            throw new BusinessException(DomainErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private void ensureQuestionImageFile(Path imagePath, String imgKey) throws BusinessException {
        if (Files.exists(imagePath)) {
            return;
        }
        String svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" width="480" height="240" viewBox="0 0 480 240">
                  <rect width="480" height="240" fill="#f5f1e8"/>
                  <rect x="24" y="24" width="432" height="192" rx="20" fill="#1f4b3f"/>
                  <text x="240" y="106" text-anchor="middle" font-size="28" fill="#fefcf6">question image</text>
                  <text x="240" y="146" text-anchor="middle" font-size="18" fill="#fefcf6">%s</text>
                </svg>
                """.formatted(imgKey);
        writeIfAbsent(imagePath, svgContent);
    }

    private void ensurePaperTemplateFile(Path templatePath, String paperType) throws BusinessException {
        if (Files.exists(templatePath)) {
            return;
        }
        String content = switch (paperType) {
            case "single" -> "questionType,stem,optionA,optionB,optionC,optionD,answer,score\n"
                    + "single,Java 中 final 的作用是什么？,修饰常量,删除类,新建线程,关闭 JVM,A,5\n";
            case "multi" -> "questionType,stem,optionA,optionB,optionC,optionD,answer,score\n"
                    + "multi,以下哪些属于 JVM 区域？,堆,方法区,虚拟机栈,浏览器缓存,A|B|C,10\n";
            default -> "questionType,stem,options,answer,score\n"
                    + "single,示例单选题,A|B|C|D,A,5\n"
                    + "subjective,请简述 Spring Bean 生命周期,,参考要点,15\n";
        };
        writeIfAbsent(templatePath, content);
    }

    private void writeIfAbsent(Path targetPath, String content) throws BusinessException {
        try {
            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException(DomainErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private Long fileSize(Path path) throws BusinessException {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            throw new BusinessException(DomainErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private String resolveFileExtension(String fileKey) {
        return fileKey.toLowerCase(Locale.ROOT).contains("export") ? ".csv" : ".txt";
    }

    private String resolveContentType(String extension) {
        return ".csv".equalsIgnoreCase(extension) ? CONTENT_TYPE_CSV : "text/plain;charset=UTF-8";
    }
}
