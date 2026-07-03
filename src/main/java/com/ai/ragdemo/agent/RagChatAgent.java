package com.ai.ragdemo.agent;

import com.ai.ragdemo.entity.RagResponse;
import com.ai.ragdemo.service.RagService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagChatAgent {

    private final RagAssistant assistant;

    public RagChatAgent(ChatLanguageModel chatLanguageModel,
                        RagService ragService) {

        // 1. 获取 retriever（可以先不用担心初始化时机）
        ContentRetriever retriever = ragService.getRetriever();

        // 2. 直接用 AiServices（先不引入 DefaultRetrievalAugmentor，避免版本坑）
        this.assistant = AiServices.builder(RagAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .contentRetriever(retriever)   // ⭐关键：RAG核心就在这里
                .build();
    }

    interface RagAssistant {

        @SystemMessage("""
            你是一个严谨的知识助手。
            只能根据提供的知识库内容回答问题。
            如果知识库没有相关信息，请回答：暂无相关信息。
            不允许编造。
        """)
        String chat(String message);
    }

    public RagResponse ask(String question) {

        String answer = assistant.chat(question);

        RagResponse response = new RagResponse();
        response.setAnswer(answer);
        response.setSources(List.of("rag-test.txt"));

        return response;
    }
}