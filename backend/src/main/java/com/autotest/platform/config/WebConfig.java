package com.autotest.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${platform.artifact.dir:artifacts}")
    private String artifactDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = new File(artifactDir).getAbsolutePath() + File.separator;
        registry.addResourceHandler("/api/artifact/**")
                .addResourceLocations("file:" + location);
    }
}
