package com.utility.billing.scripting;

import com.utility.billing.model.Account;
import com.utility.billing.model.MeterRead;
import com.utility.billing.model.Tariff;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Groovy-based scripting engine for configurable billing rules.
 * Mirrors CC&B's service script / plug-in pattern: business users
 * can modify rate calculations without redeploying Java code.
 */
@Component
public class GroovyScriptEngine {

    private static final Logger log = LoggerFactory.getLogger(GroovyScriptEngine.class);

    private final Map<String, Script> scriptCache = new ConcurrentHashMap<>();
    private final GroovyShell shell = new GroovyShell();

    @PostConstruct
    public void loadScripts() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:scripts/*.groovy");
            for (Resource resource : resources) {
                String name = resource.getFilename();
                if (name == null) continue;
                String scriptName = name.replace(".groovy", "");
                try (var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                    Script script = shell.parse(reader);
                    scriptCache.put(scriptName, script);
                    log.info("Loaded billing script: {}", scriptName);
                }
            }
            log.info("Loaded {} Groovy scripts", scriptCache.size());
        } catch (Exception e) {
            log.warn("Failed to load Groovy scripts: {}", e.getMessage());
        }
    }

    public void reloadScripts() {
        scriptCache.clear();
        loadScripts();
    }

    /**
     * Execute a billing calculation script.
     * The script receives meterRead, account, tariff, and usage as bound variables
     * and returns the calculated charge amount.
     */
    public BigDecimal executeRateScript(String scriptName, MeterRead read, Account account, Tariff tariff) {
        Script script = scriptCache.get(scriptName);
        if (script == null) {
            throw new IllegalArgumentException("Script not found: " + scriptName);
        }

        Binding binding = new Binding();
        binding.setVariable("meterRead", read);
        binding.setVariable("account", account);
        binding.setVariable("tariff", tariff);
        binding.setVariable("usage", read.getUsageKwh());

        // Sync because GroovyShell Scripts aren't thread-safe
        synchronized (script) {
            script.setBinding(binding);
            Object result = script.run();
            if (result instanceof BigDecimal bd) {
                return bd;
            } else if (result instanceof Number num) {
                return BigDecimal.valueOf(num.doubleValue());
            }
            throw new IllegalStateException("Script " + scriptName + " returned non-numeric result: " + result);
        }
    }

    /**
     * Execute a validation script. Returns null if valid, or an error message.
     */
    public String executeValidationScript(String scriptName, MeterRead read, MeterRead previousRead) {
        Script script = scriptCache.get(scriptName);
        if (script == null) return null; // no script = skip custom validation

        Binding binding = new Binding();
        binding.setVariable("meterRead", read);
        binding.setVariable("previousRead", previousRead);
        binding.setVariable("usage", read.getUsageKwh());

        synchronized (script) {
            script.setBinding(binding);
            Object result = script.run();
            return result instanceof String s ? s : null;
        }
    }

    public boolean hasScript(String name) {
        return scriptCache.containsKey(name);
    }
}
