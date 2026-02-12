package com.yunlbd.flexboot4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class BootstrapModuleInfoPrinter {

    private static final Logger log = LoggerFactory.getLogger(BootstrapModuleInfoPrinter.class);

    private final Environment environment;

    public BootstrapModuleInfoPrinter(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

        boolean adminPresent = isPresent(classLoader, "com.yunlbd.flexboot4.controller.sys.AuthController");
        boolean kbPresent = isPresent(classLoader, "com.yunlbd.flexboot4.service.kb.KnowledgeBaseService");
        boolean mediaPresent = isPresent(classLoader, "com.yunlbd.flexboot4.media.MediaAutoConfiguration");

        Boolean mediaEnabled = environment.getProperty("media.enabled", Boolean.class);

        List<String> modules = new ArrayList<>();
        if (adminPresent) {
            modules.add("admin");
        }
        if (kbPresent) {
            modules.add("kb");
        }
        if (mediaPresent) {
            modules.add(mediaEnabled == null ? "media" : ("media(enabled=" + mediaEnabled + ")"));
        }

        String appName = environment.getProperty("spring.application.name", "flexboot4");
        String green = "\u001B[32m";
        String reset = "\u001B[0m";
        log.info("{}FlexBoot4 modules loaded for '{}': {}{}", green, appName, String.join(", ", modules), reset);
    }

    private static boolean isPresent(ClassLoader classLoader, String className) {
        return ClassUtils.isPresent(className, classLoader);
    }
}
