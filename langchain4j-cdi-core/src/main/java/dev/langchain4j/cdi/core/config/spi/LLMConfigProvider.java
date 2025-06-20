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
        // Code to get additional info starts here
        System.out.println("***** LLMConfigProvider static block *****");
        System.out.println("LLMConfig.class.getClassLoader: " + LLMConfig.class.getClassLoader());
        System.out.println("Context class loader: " + Thread.currentThread().getContextClassLoader());
        try {
            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader()
                    .getResources("META-INF/services/dev.langchain4j.cdi.core.config.spi.LLMConfig");
            while (urls.hasMoreElements()) {
                System.out.println("Found service file via context class loader: " + urls.nextElement());
            }

            urls = LLMConfig.class.getClassLoader().getResources("META-INF/services/dev.langchain4j.cdi.core.config.spi.LLMConfig");
            while (urls.hasMoreElements()) {
                System.out.println("Found service file via LLMConfig class loader: " + urls.nextElement());
            }
        } catch (Exception e) {
            System.out.println("Error loading META-INF/services/dev.langchain4j.cdi.core.config.spi.LLMConfig " + e.getMessage());
        }
        // Code to get additional info ends here

        ServiceLoader<LLMConfig> loader = ServiceLoader.load(LLMConfig.class,
                Thread.currentThread().getContextClassLoader());
        final List<LLMConfig> factories = new ArrayList<>();
        loader.forEach(factories::add);
        if (factories.isEmpty()) {
            System.out.println("Fails to load the LLMConfig implementation from context class loader");
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
