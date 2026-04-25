package com.maghert.examcommon.resource;

import java.nio.file.Path;

public record StoredResourceFile(String fileKey, String extension, Path path, long size) {

    public String storedFileName() {
        return fileKey + extension;
    }
}
