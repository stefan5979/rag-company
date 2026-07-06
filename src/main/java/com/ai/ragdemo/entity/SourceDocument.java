package com.ai.ragdemo.entity;

import lombok.Data;

@Data
public class SourceDocument {
    private String content;
    private Double score;
    private String source;
    private String matchedReason;
    private String scoreLevel;
}