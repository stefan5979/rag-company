package com.ai.ragdemo.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
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

        // 1️⃣ 读取文件
        ClassPathResource resource = new ClassPathResource("rag-test.txt");

        String text = new String(
                resource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        // 2️⃣ 构建文档
        Document document = Document.from(text);

        // 3️⃣ 文本切分（chunk）
        List<TextSegment> segments =
                DocumentSplitters.recursive(300, 50)
                        .split(document)
                        .stream()
                        .map(segment -> TextSegment.from(
                                segment.text(),
                                Metadata.from("source", "rag-test.txt")
                        ))
                        .toList();

        // 4️⃣ 向量化 + 存储
        embeddingStore.addAll(
                embeddingModel.embedAll(segments).content(),
                segments
        );

        System.out.println("✅ RAG初始化完成，chunk数量：" + segments.size());
    }

    // 5️⃣ 对外暴露向量库
    public EmbeddingStore<TextSegment> getStore() {
        return embeddingStore;
    }
}