package com.tps.exception;

import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class SensitiveContentException extends IllegalArgumentException {

    private final List<FieldViolation> fields;

    public SensitiveContentException(List<FieldViolation> fields) {
        super(buildMessage(fields));
        this.fields = List.copyOf(fields);
    }

    private static String buildMessage(List<FieldViolation> fields) {
        String labels = fields.stream()
                .map(FieldViolation::label)
                .distinct()
                .collect(Collectors.joining("、"));
        return labels + "包含敏感词，请修改后再提交";
    }

    public record FieldViolation(String field, String label, String message) {
    }
}
