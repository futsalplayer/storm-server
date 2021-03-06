package me.stormma.factory;

import me.stormma.core.http.handler.invoker.HandlerInvoker;
import me.stormma.core.http.handler.invoker.impl.DefaultHandleInvoker;
import me.stormma.core.http.handler.mapping.HandlerMapping;
import me.stormma.core.http.handler.mapping.impl.DefaultHandlerMapping;
import me.stormma.support.scanner.IClassScanner;
import me.stormma.support.scanner.impl.ClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author stormma
 * @date 2017/8/15.
 * @description 实例池
 */
public class InstancePool {

    private static final Logger logger = LoggerFactory.getLogger(InstancePool.class);

    /**
     * save instance
     */
    private static final Map<String, Object> instancePool = new ConcurrentHashMap<>();

    /**
     * HandlerMapping
     */
    private static final String HANDLER_MAPPING_KEY = "storm.handler_mapping";

    /**
     * HandlerInvoker
     */
    private static final String HANDLER_INVOKER_KEY = "storm.handler_invoker";

    /**
     * class scanner
     */
    private static final String CLASS_SCANNER = "storm.class_scanner";

    /**
     * @description get instance
     * @param key
     * @param defaultImplClass
     * @param <T>
     * @return
     */
    private static <T> T getInstance(String key, Class<?> defaultImplClass) {
        if (instancePool.containsKey(key)) {
            return (T) instancePool.get(key);
        }
        try {
            T instance = (T) defaultImplClass.newInstance();
            instancePool.put(key, instance);
            return instance;
        } catch (Exception e) {
            logger.error("get instance failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * @description get handler mapping
     * @return
     */
    public static HandlerMapping getDefaultHandlerMapping() {
        return getInstance(HANDLER_MAPPING_KEY, DefaultHandlerMapping.class);
    }

    /**
     * @description get handler invoker
     * @return
     */
    public static HandlerInvoker getDefaultHandlerInvoker() {
        return getInstance(HANDLER_INVOKER_KEY, DefaultHandleInvoker.class);
    }

    /**
     * @description get class scanner
     * @return
     */
    public static IClassScanner getClassScanner() {
        return getInstance(CLASS_SCANNER, ClassScanner.class);
    }
}
