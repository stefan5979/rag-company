package com.ai.ragdemo.entity;

import lombok.Data;
import java.util.List;

@Data
public class RagResponse {
    private String answer;
    private List<SourceDocument> sources;
}