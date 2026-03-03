package com.yunlbd.flexboot4;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class BootstrapModuleInfoPrinter {

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
        boolean sms4jPresent = isPresent(classLoader, "com.yunlbd.flexboot4.config.SmsSupplierConfigDataSource");

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
        if (sms4jPresent) {
            modules.add("sms4j");
        }
        String appName = environment.getProperty("spring.application.name", "flexboot4");
        printModuleInfo(appName, modules);
    }

    private void printModuleInfo(String appName, List<String> modules) {
        String green = "\u001B[32m";
        String cyan = "\u001B[36m";
        String yellow = "\u001B[33m";
        String reset = "\u001B[0m";
        String bold = "\u001B[1m";

        String modulesStr = String.join(", ", modules);
        int moduleLineLength = modulesStr.length() + 4;
        int headerLength = Math.max("FlexBoot4 Modules Loaded".length(), moduleLineLength) + 6;

        StringBuilder banner = new StringBuilder();
        banner.append("\n");
        banner.append(green).append("╔").append("═".repeat(headerLength)).append("╗").append(reset).append("\n");
        banner.append(green).append("║ ").append(cyan).append(bold).append("FlexBoot4 Modules Loaded").append(reset).append(green);
        int padding = headerLength - "FlexBoot4 Modules Loaded".length() - 2;
        banner.append(" ".repeat(padding+1)).append("║").append(reset).append("\n");
        banner.append(green).append("╠").append("═".repeat(headerLength)).append("╣").append(reset).append("\n");
        banner.append(green).append("║ ").append(yellow).append(bold).append(appName).append(reset).append(green);
        padding = headerLength - appName.length() - 2;
        banner.append(" ".repeat(padding+1)).append("║").append(reset).append("\n");
        banner.append(green).append("╠").append("═".repeat(headerLength)).append("╣").append(reset).append("\n");
        banner.append(green).append("║ ").append(cyan).append("  ✓ ").append(modulesStr).append(reset).append(green);
        padding = headerLength - modulesStr.length() - 6;
        banner.append(" ".repeat(Math.max(0, padding+1))).append("║").append(reset).append("\n");
        banner.append(green).append("╚").append("═".repeat(headerLength)).append("╝").append(reset).append("\n");

        System.out.println(banner);
    }

    private static boolean isPresent(ClassLoader classLoader, String className) {
        return ClassUtils.isPresent(className, classLoader);
    }
}
