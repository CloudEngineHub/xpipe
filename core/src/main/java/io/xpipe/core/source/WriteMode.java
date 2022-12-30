package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.util.JacksonizedValue;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class WriteMode extends JacksonizedValue {

    public static final Replace REPLACE = new Replace();
    public static final Append APPEND = new Append();
    public static final Prepend PREPEND = new Prepend();
    private static final List<WriteMode> ALL = new ArrayList<>();

    public static void init(ModuleLayer layer) {
        if (ALL.size() == 0) {
            ALL.addAll(ServiceLoader.load(layer, WriteMode.class).stream()
                               .map(p -> p.get())
                               .toList());
        }
    }

    public static WriteMode byId(String id) {
        return ALL.stream()
                .filter(writeMode -> writeMode.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow();
    }

    public final String getId() {
        return getClass().getAnnotation(JsonTypeName.class).value();
    }

    @JsonTypeName("replace")
    public static final class Replace extends WriteMode {
    }

    @JsonTypeName("append")
    public static final class Append extends WriteMode {
    }

    @JsonTypeName("prepend")
    public static final class Prepend extends WriteMode {
    }
}
