package com.ai.ragdemo.controller;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class TestController {

    private final ChatLanguageModel model;

    public TestController(ChatLanguageModel model) {
        this.model = model;
    }

    @GetMapping
    public String test(@RequestParam String msg) {
        return model.generate(msg);
    }
}