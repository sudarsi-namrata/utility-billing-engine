package com.utility.billing.controller;

import com.utility.billing.scripting.GroovyScriptEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final GroovyScriptEngine scriptEngine;

    public AdminController(GroovyScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    @PostMapping("/scripts/reload")
    public ResponseEntity<Map<String, String>> reloadScripts() {
        scriptEngine.reloadScripts();
        return ResponseEntity.ok(Map.of("status", "Scripts reloaded"));
    }
}
