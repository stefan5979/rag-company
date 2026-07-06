package com.ai.ragdemo.agent;

import com.ai.ragdemo.entity.RagResponse;
import com.ai.ragdemo.entity.SourceDocument;
import com.ai.ragdemo.service.RagService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagChatAgent {

    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> store;

    public RagChatAgent(ChatLanguageModel chatModel,
                        EmbeddingModel embeddingModel,
                        RagService ragService) {

        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.store = ragService.getStore();
    }

    public RagResponse ask(String question) {

        // 1️⃣ 向量化
        Embedding queryEmbedding = embeddingModel.embed(question).content();

        // 2️⃣ 检索
        List<EmbeddingMatch<TextSegment>> matches =
                store.findRelevant(queryEmbedding, 5);

        // 3️⃣ 构建可解释 sources
        List<SourceDocument> sources = matches.stream().map(match -> {

            SourceDocument doc = new SourceDocument();

            String text = match.embedded().text();
            double score = match.score();

            doc.setContent(text);
            doc.setScore(score);

            // ⭐ 解释 score
            doc.setScoreLevel(explainScore(score));

            // ⭐ 命中原因（核心可解释点）
            doc.setMatchedReason(buildReason(question, text));

            // ⭐ source
            doc.setSource(
                    match.embedded().metadata().getString("source")
            );

            return doc;
        }).toList();

        // 4️⃣ 拼上下文
        String context = sources.stream()
                .map(SourceDocument::getContent)
                .reduce("", (a, b) -> a + "\n" + b);

        String prompt = """
                你是一个严谨的AI助手，请基于资料回答问题。

                资料：
                %s

                问题：
                %s
                """.formatted(context, question);

        String answer = chatModel.generate(prompt);

        // 5️⃣ 返回
        RagResponse res = new RagResponse();
        res.setAnswer(answer);
        res.setSources(sources);

        return res;
    }

    // ===== 可解释逻辑 =====

    private String explainScore(double score) {
        if (score > 0.8) return "HIGH(强相关)";
        if (score > 0.5) return "MEDIUM(中相关)";
        return "LOW(弱相关)";
    }

    private String buildReason(String question, String text) {

        // 非AI解释（工程可控）
        StringBuilder sb = new StringBuilder();

        if (text.contains("RAG")) sb.append("命中关键词:RAG ");
        if (text.contains("检索")) sb.append("命中关键词:检索 ");
        if (text.contains("生成")) sb.append("命中关键词:生成 ");

        if (sb.length() == 0) {
            sb.append("语义相似匹配");
        }

        return sb.toString();
    }
}