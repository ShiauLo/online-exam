package com.maghert.examcommon.resource;

import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examcommon.exception.DomainErrorCode;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalResourceFileStore {

    private final Path storageRoot;

    public LocalResourceFileStore(String storageRoot) {
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    public StoredResourceFile writeCsv(String fileKey, String content) throws BusinessException {
        return write(fileKey, ".csv", content.getBytes(StandardCharsets.UTF_8));
    }

    public StoredResourceFile write(String fileKey, String extension, byte[] content) throws BusinessException {
        Path target = resolveExportFile(fileKey, extension);
        try {
            Files.write(target, content);
            return new StoredResourceFile(normalizeFileKey(fileKey), normalizeExtension(extension), target, Files.size(target));
        } catch (IOException exception) {
            throw new BusinessException(DomainErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    public StoredResourceFile getExisting(String fileKey, String extension) throws BusinessException {
        Path target = resolveExportFile(fileKey, extension);
        if (!Files.exists(target)) {
            throw new BusinessException(DomainErrorCode.RESOURCE_NOT_FOUND);
        }
        try {
            return new StoredResourceFile(normalizeFileKey(fileKey), normalizeExtension(extension), target, Files.size(target));
        } catch (IOException exception) {
            throw new BusinessException(DomainErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    public Path resolveExportFile(String fileKey, String extension) throws BusinessException {
        String normalizedFileKey = normalizeFileKey(fileKey);
        String normalizedExtension = normalizeExtension(extension);
        try {
            Files.createDirectories(storageRoot);
            Path target = storageRoot.resolve("exports")
                    .resolve(normalizedFileKey + normalizedExtension)
                    .normalize();
            if (!target.startsWith(storageRoot)) {
                throw new BusinessException(DomainErrorCode.RESOURCE_KEY_INVALID);
            }
            Files.createDirectories(target.getParent());
            return target;
        } catch (IOException exception) {
            throw new BusinessException(DomainErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private String normalizeFileKey(String fileKey) throws BusinessException {
        if (!StringUtils.hasText(fileKey)) {
            throw new BusinessException(DomainErrorCode.RESOURCE_KEY_INVALID);
        }
        String normalized = fileKey.trim();
        if (!normalized.matches("[A-Za-z0-9._-]+")) {
            throw new BusinessException(DomainErrorCode.RESOURCE_KEY_INVALID);
        }
        return normalized;
    }

    private String normalizeExtension(String extension) throws BusinessException {
        if (!StringUtils.hasText(extension)) {
            throw new BusinessException(DomainErrorCode.RESOURCE_KEY_INVALID);
        }
        String normalized = extension.trim();
        if (!normalized.startsWith(".")) {
            normalized = "." + normalized;
        }
        if (!normalized.matches("\\.[A-Za-z0-9]+")) {
            throw new BusinessException(DomainErrorCode.RESOURCE_KEY_INVALID);
        }
        return normalized;
    }
}
