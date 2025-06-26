package dev.langchain4j.cdi.core.config.spi;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;

import org.jboss.logging.Logger;

public class LLMConfigProvider {

    private static LLMConfig llmConfig;
    private static volatile boolean initialized = false;
    private static final Logger LOGGER = Logger.getLogger(LLMConfigProvider.class);

    static {

        try {
            Enumeration<URL> roots = Thread.currentThread().getContextClassLoader().getResources("");
            System.out.println("Resources visible to context class loader resources:");
            while (roots.hasMoreElements()) {
                URL url = roots.nextElement();
                System.out.println(" - " + url);
            }
        } catch (Exception exp) {
            System.out.println("Error loading classpath resources " + exp.getMessage());
        }

        try {
            Enumeration<URL> roots = LLMConfigProvider.class.getClassLoader().getResources("");
            System.out.println("Resources visible to the loader of current class::");
            while (roots.hasMoreElements()) {
                URL url = roots.nextElement();
                System.out.println(" - " + url);
            }
        } catch (Exception exp) {
            System.out.println("Error loading classpath resources " + exp.getMessage());
        }

        ServiceLoader<LLMConfig> loader = ServiceLoader.load(LLMConfig.class,
                Thread.currentThread().getContextClassLoader());
        final List<LLMConfig> factories = new ArrayList<>();
        loader.forEach(factories::add);
        if (factories.isEmpty()) {
            System.out.println("Warning: LLMConfigProvider failed to load the LLMConfig implementation from context class loader");
            loader = ServiceLoader.load(LLMConfig.class, LLMConfig.class.getClassLoader());
            loader.forEach(factories::add);
            if (factories.isEmpty()) {
                throw new RuntimeException("No service Found for LLMConfig interface");
            }
        }
        llmConfig = factories.iterator().next(); //loader.findFirst().orElse(null);
        LOGGER.debug("Found LLMConfig interface: " + llmConfig.getClass().getName());
    }

    public static LLMConfig getLlmConfig() {
        if (!initialized) {
            initialized = true;
            llmConfig.init();
        }

        return llmConfig;
    }
}
