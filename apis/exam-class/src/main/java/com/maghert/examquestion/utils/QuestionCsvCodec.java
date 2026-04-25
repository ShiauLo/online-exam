package com.maghert.examquestion.utils;

import com.maghert.examcommon.exception.DomainErrorCode;
import com.maghert.examcommon.exception.BusinessException;
import com.maghert.examquestion.entity.QuestionItem;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

@Component
public class QuestionCsvCodec {

    private static final List<String> REQUIRED_HEADERS = List.of(
            "content",
            "type",
            "answer",
            "difficulty",
            "options");

    public List<QuestionImportRow> read(InputStream inputStream) throws BusinessException {
        List<QuestionImportRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            int lineNo = 0;
            java.util.Map<String, Integer> headerIndex = null;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> parts = split(line);
                if (firstLine && looksLikeHeader(parts)) {
                    headerIndex = buildHeaderIndex(parts);
                    firstLine = false;
                    continue;
                }
                firstLine = false;
                String content = valueOf(parts, headerIndex, 0, "content");
                String type = valueOf(parts, headerIndex, 1, "type");
                String answer = valueOf(parts, headerIndex, 2, "answer");
                String analysis = emptyToNull(valueOf(parts, headerIndex, 3, "analysis"));
                String difficultyValue = valueOf(parts, headerIndex, 4, "difficulty");
                String optionsValue = valueOf(parts, headerIndex, 5, "options");
                if (!StringUtils.hasText(content)
                        || !StringUtils.hasText(type)
                        || !StringUtils.hasText(answer)
                        || !StringUtils.hasText(difficultyValue)) {
                    rows.add(new QuestionImportRow(lineNo, null, null, List.of(), null, null, null,
                            "invalid csv format"));
                    continue;
                }
                Integer difficulty;
                try {
                    difficulty = parseDifficulty(difficultyValue);
                } catch (BusinessException exception) {
                    rows.add(new QuestionImportRow(lineNo, null, null, List.of(), null, null, null,
                            exception.getMessage()));
                    continue;
                }
                rows.add(new QuestionImportRow(
                        lineNo,
                        content,
                        type,
                        splitOptions(optionsValue),
                        answer,
                        analysis,
                        difficulty,
                        null));
            }
        } catch (IOException e) {
            throw new BusinessException(DomainErrorCode.QUESTION_IMPORT_READ_FAILED);
        }
        return rows;
    }

    public byte[] write(List<QuestionItem> questions, boolean maskSensitive) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writer.println("questionId,content,type,answer,analysis,difficulty,options,auditStatus,isDisabled,referenceCount,categoryId,creatorId");
            for (QuestionItem question : questions) {
                writer.println(String.join(",",
                        escape(String.valueOf(question.getId())),
                        escape(question.getContent()),
                        escape(question.getQuestionType()),
                        escape(maskSensitive ? "" : question.getAnswer()),
                        escape(maskSensitive ? "" : question.getAnalysis()),
                        escape(String.valueOf(question.getDifficulty())),
                        escape(String.join("|", question.getOptions())),
                        escape(question.getAuditStatus()),
                        escape(String.valueOf(Boolean.TRUE.equals(question.getDisabled()))),
                        escape(String.valueOf(question.getReferenceCount())),
                        escape(String.valueOf(question.getCategoryId())),
                        escape(String.valueOf(question.getCreatorId()))));
            }
            writer.flush();
            return outputStream.toByteArray();
        }
    }

    private List<String> split(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    private boolean looksLikeHeader(List<String> parts) {
        if (parts.isEmpty()) {
            return false;
        }
        java.util.Map<String, Integer> headerIndex = buildHeaderIndex(parts);
        return REQUIRED_HEADERS.stream().allMatch(headerIndex::containsKey)
                || "questionid".equals(parts.get(0).trim().toLowerCase());
    }

    private java.util.Map<String, Integer> buildHeaderIndex(List<String> headerParts) {
        java.util.Map<String, Integer> headerIndex = new java.util.LinkedHashMap<>();
        for (int index = 0; index < headerParts.size(); index++) {
            headerIndex.put(headerParts.get(index).trim().toLowerCase(), index);
        }
        return headerIndex;
    }

    private String valueOf(List<String> parts, java.util.Map<String, Integer> headerIndex, int fallbackIndex, String headerName) {
        if (headerIndex != null && headerIndex.containsKey(headerName)) {
            Integer index = headerIndex.get(headerName);
            return index < parts.size() ? parts.get(index) : null;
        }
        return fallbackIndex < parts.size() ? parts.get(fallbackIndex) : null;
    }

    private List<String> splitOptions(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return List.of(raw.split("\\|"));
    }

    private Integer parseDifficulty(String raw) throws BusinessException {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new BusinessException(DomainErrorCode.QUESTION_DIFFICULTY_INVALID);
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String escape(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    public record QuestionImportRow(
            int lineNumber,
            String content,
            String type,
            List<String> options,
            String answer,
            String analysis,
            Integer difficulty,
            String parseError
    ) {
    }
}
