package com.ai.ragdemo.controller;

import com.ai.ragdemo.agent.RagChatAgent;
import com.ai.ragdemo.entity.RagResponse;
import com.ai.ragdemo.entity.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagChatAgent ragChatAgent;

    public RagController(RagChatAgent ragChatAgent) {
        this.ragChatAgent = ragChatAgent;
    }

    @GetMapping("/chat")
    public Result<RagResponse> chat(@RequestParam String question) {

        RagResponse response = ragChatAgent.ask(question);

        return Result.success(response);
    }
}