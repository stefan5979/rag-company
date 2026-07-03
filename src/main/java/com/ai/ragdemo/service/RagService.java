package com.ai.ragdemo.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

        // 1. 读取文件
        ClassPathResource resource = new ClassPathResource("rag-test.txt");
        String text = new String(resource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        Document document = Document.from(text);

        // 2. 切分文本（chunk）
        List<TextSegment> segments =
                DocumentSplitters.recursive(300, 50)
                        .split(document)
                        .stream()
                        .map(segment -> TextSegment.from(
                                segment.text(),
                                segment.metadata().put("source", "rag-test.txt")
                        ))
                        .toList();
        // 3. 生成embedding并存入内存向量库
        embeddingStore.addAll(
                embeddingModel.embedAll(segments).content(),
                segments
        );

        System.out.println("RAG知识库初始化完成，分片数量：" + segments.size());
    }

    public ContentRetriever getRetriever() {

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.5)
                .build();
    }
}