package com.ai.ragdemo.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RagService {

    private final EmbeddingModel embeddingModel;

    private final EmbeddingStore<TextSegment> embeddingStore =
            new InMemoryEmbeddingStore<>();

    public RagService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    public void init() throws Exception {

        ClassPathResource resource = new ClassPathResource("rag-test.txt");

        String text = new String(
                resource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        Document document = Document.from(text);

        List<TextSegment> segments =
                DocumentSplitters.recursive(200, 50)
                        .split(document)
                        .stream()
                        .map(segment -> {

                            Map<String, String> meta = new HashMap<>();
                            meta.put("source", "rag-test.txt");
                            meta.put("length", String.valueOf(segment.text().length()));

                            return TextSegment.from(
                                    segment.text(),
                                    Metadata.from(meta)
                            );
                        })
                        .toList();

        embeddingStore.addAll(
                embeddingModel.embedAll(segments).content(),
                segments
        );

        System.out.println("✔ Level3 RAG 初始化完成：" + segments.size());
    }

    public EmbeddingStore<TextSegment> getStore() {
        return embeddingStore;
    }
}