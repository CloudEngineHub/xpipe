package io.xpipe.core.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public class JacksonMapper {

    private static final ObjectMapper BASE = new ObjectMapper();
    private static final ObjectMapper DEFAULT;
    private static ObjectMapper INSTANCE = new ObjectMapper();
    private static boolean init = false;

    static {
        ObjectMapper objectMapper = BASE;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        objectMapper.setVisibility(objectMapper
                                           .getSerializationConfig()
                                           .getDefaultVisibilityChecker()
                                           .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                                           .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                                           .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                                           .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
                                           .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE));

        var modules = findModules(ModuleLayer.boot());
        objectMapper.registerModules(modules);

        INSTANCE = BASE.copy();
        DEFAULT = BASE.copy();
    }

    public static synchronized void configure(Consumer<ObjectMapper> mapper) {
        mapper.accept(INSTANCE);
    }

    public static synchronized void initClassBased() {
        initModularized(null);
    }

    public static synchronized void initModularized(ModuleLayer layer) {
        List<Module> MODULES = findModules(layer);
        INSTANCE.registerModules(MODULES);
        init = true;
    }

    private static List<Module> findModules(ModuleLayer layer) {
        ArrayList<Module> modules = new ArrayList<Module>();
        ServiceLoader<Module> loader =
                layer != null ? ServiceLoader.load(layer, Module.class) : ServiceLoader.load(Module.class);
        for (Module module : loader) {
            modules.add(module);
        }
        return modules;
    }

    /**
     * Constructs a new ObjectMapper that is able to map all required X-Pipe classes and also possible extensions.
     */
    public static ObjectMapper newMapper() {
        if (!JacksonMapper.isInit()) {
            return DEFAULT;
        }
        return INSTANCE.copy();
    }

    public static ObjectMapper getDefault() {
        if (!JacksonMapper.isInit()) {
            return DEFAULT;
        }
        return INSTANCE;
    }

    public static boolean isInit() {
        return init;
    }
}
