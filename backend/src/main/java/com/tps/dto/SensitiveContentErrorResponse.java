package com.tps.dto;

import com.tps.exception.SensitiveContentException;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SensitiveContentErrorResponse {
    private List<SensitiveContentException.FieldViolation> fields;
}
