package io.xpipe.app.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.extension.event.ErrorEvent;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigHelper {

    public static JsonNode readConfig(Path in) {
        JsonNode node = JsonNodeFactory.instance.objectNode();
        try {
            if (Files.exists(in)) {
                ObjectMapper o = JacksonMapper.newMapper();
                node = o.readTree(Files.readAllBytes(in));
            }
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).build().handle();
        }
        return node;
    }

    public static void writeConfig(Path out, JsonNode node) {
        try {
            FileUtils.forceMkdirParent(out.toFile());
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).build().handle();
            return;
        }

        var writer = new StringWriter();
        JsonFactory f = new JsonFactory();
        try (JsonGenerator g = f.createGenerator(writer).setPrettyPrinter(new DefaultPrettyPrinter())) {
            JacksonMapper.newMapper().writeTree(g, node);
            var newContent = writer.toString();
            Files.writeString(out, newContent);
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).build().handle();
        }
    }
}
