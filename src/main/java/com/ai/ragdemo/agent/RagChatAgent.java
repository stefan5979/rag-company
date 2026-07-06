package com.ai.ragdemo.agent;

import com.ai.ragdemo.entity.RagResponse;
import com.ai.ragdemo.entity.SourceDocument;
import com.ai.ragdemo.service.RagService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagChatAgent {

    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public RagChatAgent(ChatLanguageModel chatModel,
                        EmbeddingModel embeddingModel,
                        RagService ragService) {

        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = ragService.getStore();
    }

    public RagResponse ask(String question) {

        // 1️⃣ embedding
        Embedding queryEmbedding =
                embeddingModel.embed(question).content();

        // 2️⃣ Level 3 正确检索方式（0.36）
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(0.0)
                .build();

        EmbeddingSearchResult<TextSegment> result =
                embeddingStore.search(request);

        List<EmbeddingMatch<TextSegment>> matches =
                result.matches();

        // 3️⃣ 可解释 sources
        List<SourceDocument> sources = matches.stream().map(match -> {

            SourceDocument doc = new SourceDocument();

            doc.setContent(match.embedded().text());
            double score = match.score();

            doc.setScore(score);
            doc.setScoreLevel(scoreLevel(score));

            doc.setSource(match.embedded().metadata().getString("source"));

            // ⭐ Level 3：可解释性
            doc.setMatchedReason(
                    buildReason(match.embedded().text(), question)
            );

            return doc;
        }).toList();

        // 4️⃣ prompt（企业版：结构化上下文）
        StringBuilder context = new StringBuilder();

        for (SourceDocument s : sources) {
            context.append("来源片段：")
                    .append(s.getContent())
                    .append("\n");
        }

        String answer = chatModel.generate(
                "你必须基于以下资料回答问题：\n"
                        + context
                        + "\n问题：" + question
        );

        // 5️⃣ response
        RagResponse resp = new RagResponse();
        resp.setAnswer(answer);
        resp.setSources(sources);

        return resp;
    }

    // ⭐ 可解释RAG核心（Level 3）
    private String buildReason(String chunk, String question) {

        if (chunk == null) return "无匹配内容";

        String q = question.toLowerCase();
        String c = chunk.toLowerCase();

        int score = 0;

        for (String word : q.split(" ")) {
            if (c.contains(word)) {
                score++;
            }
        }

        if (score >= 3) return "高相关：多关键词命中";
        if (score == 2) return "中相关：部分关键词命中";
        if (score == 1) return "低相关：弱语义匹配";
        return "语义匹配（embedding匹配）";
    }
    private String scoreLevel(double score) {
        if (score >= 0.80) return "VERY_HIGH";
        if (score >= 0.65) return "HIGH";
        if (score >= 0.45) return "MEDIUM";
        if (score >= 0.30) return "LOW";
        return "VERY_LOW";
    }
}