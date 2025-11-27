package dev.langchain4j.cdi.core.config.spi;

import dev.langchain4j.service.IllegalConfigurationException;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base abstraction to provide configuration for LLM-related beans.
 *
 * <p>This API is intentionally lightweight and relies purely on CDI (no external config dependency). It is inspired by
 * MicroProfile/SmallRye Config, but kept minimal for portability.
 */
public abstract class LLMConfig {
    Map<String, ProducerFunction<?>> producers = new ConcurrentHashMap<>();

    /** Prefix for all LLM beans properties. */
    public static final String PREFIX = "dev.langchain4j.plugin";

    /** Called by @see LLMConfigProvider. */
    public static final String PRODUCER = "defined_bean_producer";

    public static final String CLASS = "class";
    public static final String SCOPE = "scope";

    // Backing store for default implementations. Subclasses may ignore it and override methods completely.
    protected final Properties properties = new Properties();

    //public abstract void init();

    /**
     * Default, non-breaking file-based initialization. Subclasses may override.
     * Order:
     *  1) -Dllmconfigfile (absolute or relative path)
     *  2) LLM_CONFIG_FILE environment variable
     *  3) Classpath: /llm-config.properties, /config/llm-config.properties, /META-INF/llm-config.properties
     *
     */
    public void init() {
        boolean loaded = false;
        String sysPath = System.getProperty("llmconfigfile");
        String envPath = System.getenv("LLM_CONFIG_FILE");
        try {
            if (sysPath != null && !sysPath.isBlank()) {
                File f = new File(sysPath);
                if (f.isFile()) {
                    try (FileInputStream fis = new FileInputStream(f)) {
                        properties.load(fis);
                        loaded = true;
                        System.out.println("***** [PABHAT]: Loaded LLM config from system property llmconfigfile=" + f.getAbsolutePath());

                    }
                }
            }
            if (!loaded && envPath != null && !envPath.isBlank()) {
                File f = new File(envPath);
                if (f.isFile()) {
                    try (FileInputStream fis = new FileInputStream(f)) {
                        properties.load(fis);
                        loaded = true;
                        System.out.println("***** [PABHAT]: Loaded LLM config from env LLM_CONFIG_FILE=" + f.getAbsolutePath());
                    }
                }
            }
            if (!loaded) {
                for (String candidate : new String[]{"/llm-config.properties", "/config/llm-config.properties", "/META-INF/llm-config.properties"}) {
                    try (InputStream is = LLMConfig.class.getResourceAsStream(candidate)) {
                        if (is != null) {
                            properties.load(is);
                            loaded = true;
                            System.out.println("***** [PABHAT]: Loaded LLM config from classpath " + candidate);
                            break;
                        }
                    }
                }
            }
            if (!loaded) {
                System.out.println("***** [PABHAT]: No LLM config source found; using empty Properties");

            }
        } catch (IOException e) {
            throw new RuntimeException("***** [PABHAT]: Failed to load LLM configuration", e);
        }
    }

    //public abstract Set<String> getPropertyKeys();

    /**
     * Default non-breaking implementation based on the internal Properties store.
     * Subclasses may override to provide different behavior.
     *
     * @return
     */
    public Set<String> getPropertyKeys() {
        return properties.keySet().stream()
                .map(Object::toString)
                .filter(k -> k.startsWith(PREFIX))
                .collect(Collectors.toSet());
    }

    //public abstract String getValue(String key);

    /**
     * Default non-breaking implementation based on the internal Properties store.
     * Subclasses may override to provide different behavior.
     *
     * @param key
     * @return
     */
    public String getValue(String key) {
        return properties.getProperty(key);
    }

    /**
     * Built-in fallback instance used when neither CDI nor ServiceLoader provide an implementation.
     * This anonymous subclass relies on the default implementations defined above.
     *
     * @return
     */
    public static LLMConfig fallback() {
        return new LLMConfig() {};
    }


    /**
     * Get all Langchain4j-cdi LLM beans names, prefixed by PREFIX For example:
     * dev.langchain4j.plugin.content-retriever.class -> content-retriever
     *
     * @return a set of property names
     */
    public Set<String> getBeanNames() {
        return getPropertyKeys().stream()
                .filter(key -> key.startsWith(PREFIX))
                .map(key -> key.substring(PREFIX.length() + 1))
                .map(key -> key.substring(0, key.indexOf(".")))
                .collect(Collectors.toSet());
    }

    public String getBeanPropertyValue(String beanName, String propertyName) {
        String key = PREFIX + "." + beanName + "." + propertyName;
        return getValue(key);
    }

    public void registerProducer(String producersName, ProducerFunction<?> producer) {
        producers.putIfAbsent(producersName, producer);
    }

    public Object getBeanPropertyValue(String beanName, String propertyName, Type type) {
        ParameterizedType parameterizedType = null;
        Class<?> clazz;
        if (type instanceof ParameterizedType) {
            parameterizedType = (ParameterizedType) type;
            clazz = (Class<?>) parameterizedType.getRawType();
        } else {
            clazz = (Class<?>) type;
        }
        String stringValue = getBeanPropertyValue(beanName, propertyName);
        if (clazz == ProducerFunction.class && stringValue != null) {
            return producers.get(stringValue);
        }
        if (stringValue == null) return null;
        stringValue = stringValue.trim();
        try {
            return getObject(clazz, parameterizedType, stringValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported type for value conversion: " + type, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object getObject(Class<?> clazz, ParameterizedType parameterizedType, String stringValue)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (clazz == String.class) return stringValue;
        if (clazz == Duration.class) {
            String durationString = stringValue.toUpperCase();
            if (!durationString.startsWith("PT")) durationString = "PT" + durationString;
            return Duration.parse(durationString);
        }
        if (clazz == Integer.class || clazz == int.class) return Integer.valueOf(stringValue);
        if (clazz == Long.class || clazz == long.class) return Long.valueOf(stringValue);
        if (clazz == Boolean.class || clazz == boolean.class) return Boolean.valueOf(stringValue);
        if (clazz == Double.class || clazz == double.class) return Double.valueOf(stringValue);
        // Enum support
        if (clazz.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Class<? extends Enum> enumClass = (Class<? extends Enum<?>>) clazz;
            //noinspection unchecked
            return Enum.valueOf(enumClass, stringValue.substring(stringValue.lastIndexOf(".") + 1));
        }
        if (parameterizedType != null) {
            // Try to resolve generic parameter (e.g., List<SomeEnum>)
            List<Object> list = new ArrayList<>();
            Type arg = parameterizedType.getActualTypeArguments()[0];
            for (String val : stringValue.split(",")) {
                list.add(getObject((Class<?>) arg, null, val));
            }
            if (clazz.isAssignableFrom(List.class)) return list;
            if (clazz.isAssignableFrom(Set.class)) return Set.copyOf(list);
        }
        return clazz.getConstructor(String.class).newInstance(stringValue);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object getBeanPropertyValue(Instance<Object> lookup, String beanName, String propertyName, Type type) {
        String stringValue = getBeanPropertyValue(beanName, propertyName);
        if (stringValue == null) return null;
        if (stringValue.startsWith("lookup:")) {
            String lookupableBean = stringValue.substring("lookup:".length());
            switch (lookupableBean) {
                case "@default":
                    if (type instanceof ParameterizedType) return selectByBeanManager((ParameterizedType) type);
                    else return lookup.select((Class) type).get();
                case "@all":
                    if (type instanceof ParameterizedType pt) {
                        Type actualTypeArgument = pt.getActualTypeArguments()[0];
                        Stream<?> toReturn;
                        if (actualTypeArgument instanceof ParameterizedType) {
                            toReturn = selectAllByBeanManager((ParameterizedType) actualTypeArgument).stream();
                        } else {
                            toReturn = lookup.select((Class<?>) actualTypeArgument).stream();
                        }
                        if (pt.getRawType().equals(List.class)) {
                            return toReturn.toList();
                        } else if (pt.getRawType().equals(Set.class)) {
                            return toReturn.collect(Collectors.toSet());
                        } else {
                            throw new IllegalConfigurationException("@all can only be used with List or Set");
                        }
                    } else {
                        throw new IllegalConfigurationException("Cannot use @all for non generic types");
                    }
                default:
                    return getInstance(lookup, (Class<?>) type, lookupableBean).get();
            }
        } else {
            return getBeanPropertyValue(beanName, propertyName, type);
        }
    }

    private static java.util.function.Supplier<BeanManager> beanManagerSupplier =
            () -> CDI.current().getBeanManager();

    /** For tests only: override how BeanManager is obtained. */
    public static void setBeanManagerSupplier(java.util.function.Supplier<BeanManager> supplier) {
        beanManagerSupplier = (supplier == null) ? () -> CDI.current().getBeanManager() : supplier;
    }

    private Object selectByBeanManager(ParameterizedType type) {
        BeanManager bm = beanManagerSupplier.get();
        Set<Bean<?>> beans = bm.getBeans(type);
        if (beans.isEmpty()) {
            throw new IllegalConfigurationException("The type " + type + " is not found in the CDI container.");
        }
        Bean<?> bean = bm.resolve(beans);
        var ctx = bm.createCreationalContext(bean);
        return bm.getReference(bean, type, ctx);
    }

    private List<Object> selectAllByBeanManager(ParameterizedType type) {
        BeanManager bm = beanManagerSupplier.get();
        Set<Bean<?>> beans = bm.getBeans(type);
        if (beans.isEmpty()) {
            throw new IllegalConfigurationException("The type " + type + " is not found in the CDI container.");
        }
        List<Object> beansList = new ArrayList<>();
        for (Bean<?> bean : beans) {
            var ctx = bm.createCreationalContext(bean);
            beansList.add(bm.getReference(bean, type, ctx));
        }
        return beansList;
    }

    private <T> Instance<T> getInstance(Instance<Object> lookup, Class<T> clazz, String lookupName) {
        if (lookupName == null || lookupName.isBlank()) return lookup.select(clazz);
        return lookup.select(clazz, NamedLiteral.of(lookupName));
    }

    public Set<String> getPropertyNamesForBean(String beanName) {
        String configPrefix = PREFIX + "." + beanName + ".config.";
        return getPropertyKeys().stream()
                .map(Object::toString)
                .filter(prop -> prop.startsWith(configPrefix))
                .map(prop -> prop.substring(configPrefix.length()))
                .collect(Collectors.toSet());
    }
}
