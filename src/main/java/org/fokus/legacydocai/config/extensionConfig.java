package org.fokus.legacydocai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class extensionConfig {

    @Bean(name = "extensionToLanguageMap")
    public Map<String, String> extensionToLanguageMap() {
        return Map.of(
                "java", "java",
                "py", "python",
                "cpp", "cpp",
                "cs", "csharp",
                "js", "javascript",
                "kt","kotlin"
        );
    }
}