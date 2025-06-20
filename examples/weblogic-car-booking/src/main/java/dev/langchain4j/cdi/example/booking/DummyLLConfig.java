package dev.langchain4j.cdi.example.booking;

import static dev.langchain4j.cdi.core.config.spi.LLMConfig.PREFIX;

import dev.langchain4j.cdi.core.config.spi.LLMConfig;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class DummyLLConfig implements LLMConfig {
    Properties properties = new Properties();
    final String llmConfigProperties = "META-INF/llm-config.properties";

    @Override
    public void init() {
        System.out.println("***** DummyLLConfig#init *****");
        System.out.println("LLMConfig.class.getClassLoader: " + DummyLLConfig.class.getClassLoader());
        System.out.println("Context class loader: " + Thread.currentThread().getContextClassLoader());
        try (InputStream input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(llmConfigProperties)) {
            if (input != null) {
                properties.load(input);
                System.out.println("Loaded LLMConfig from context class loader");
                // Validate a sample key from the LLM Config file
                System.out.println("dev.langchain4j.plugin.chat-model.class: " + properties.getProperty("dev.langchain4j.plugin.chat-model.class"));
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading properties file from context loader", e);
        }

        try (InputStream input = LLMConfig.class.getClassLoader().getResourceAsStream(llmConfigProperties)) {
            if (input != null) {
                properties.load(input);
                System.out.println("Loaded LLMConfig from current module");
                // Validate a sample key from the LLM Config file
                System.out.println("dev.langchain4j.plugin.chat-model.class: " + properties.getProperty("dev.langchain4j.plugin.chat-model.class"));
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading properties file from context loader", e);
        }
        throw new RuntimeException("Properties file not found!");
    }

    @Override
    public Set<String> getBeanNames() {
        return properties.keySet().stream().map(Object::toString)
                .filter(prop -> prop.startsWith(PREFIX))
                .map(prop -> prop.substring(PREFIX.length() + 1, prop.indexOf(".", PREFIX.length() + 2)))
                .collect(Collectors.toSet());
    }

    @Override
    public <T> T getBeanPropertyValue(String beanName, String propertyName, Class<T> type) {
        String value=properties.getProperty(PREFIX + "." + beanName + "." + propertyName);
        if ( value==null)
            return null;
        if ( type==String.class)
            return (T) value;
        if ( type== Duration.class)
            return (T) Duration.parse(value);
        try {
            return type.getConstructor(String.class).newInstance(value);
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Set<String> getPropertyNamesForBean(String beanName) {
        String configPrefix = PREFIX + "." + beanName + ".config.";
        return properties.keySet().stream().map(Object::toString)
                .filter(prop -> prop.startsWith(configPrefix))
                .map(prop -> prop.substring(configPrefix.length()))
                .collect(Collectors.toSet());
    }
}
