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
            ClassLoader clsLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> roots = clsLoader.getResources("");
            System.out.println("**** Context class loader " + clsLoader.getName() + ", resource roots:");
            while (roots.hasMoreElements()) {
                URL url = roots.nextElement();
                System.out.println(" - " + url);
            }
        } catch (Exception exp) {
            System.out.println("Error loading classpath resources " + exp.getMessage());
        }

        try {
            ClassLoader clsLoader = LLMConfigProvider.class.getClassLoader();
            Enumeration<URL> roots = clsLoader.getResources("");
	    System.out.println("**** Class loader for the current class " + clsLoader.getName() + ", resource roots:");	
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
            System.out.println("LLMConfigProvider: Failed to load the LLMConfig implementation from context class loader");
            loader = ServiceLoader.load(LLMConfig.class, LLMConfig.class.getClassLoader());
            loader.forEach(factories::add);
            if (factories.isEmpty()) {
                throw new RuntimeException("No service Found for LLMConfig interface");
            } else {
                System.out.println("LLMConfigProvider: Loaded LLMConfig implementation from the class loader associated with the current class");
            }
        } else {
            System.out.println("LLMConfigProvider: Loaded LLMConfig implementation from context class loader");
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
