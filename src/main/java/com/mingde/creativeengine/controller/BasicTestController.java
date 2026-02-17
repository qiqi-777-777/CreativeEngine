package com.mingde.creativeengine.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 基础测试控制器 - 不依赖任何外部服务
 */
@RestController
public class BasicTestController {

    @GetMapping("/")
    public String home() {
        return "CreativeEngine Backend is running successfully!";
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    @GetMapping("/api/test")
    public Map<String, Object> apiTest() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "API is working!");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    @PostMapping("/api/test")
    public Map<String, Object> apiTestPost(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "POST API is working!");
        result.put("received", request);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
