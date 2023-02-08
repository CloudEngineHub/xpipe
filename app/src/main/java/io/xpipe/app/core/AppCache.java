package io.xpipe.app.core;

import io.xpipe.app.util.JsonConfigHelper;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.extension.Cache;
import io.xpipe.extension.event.ErrorEvent;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Supplier;

public class AppCache implements Cache {

    public static UUID getCachedUserId() {
        var id = get("userId", UUID.class, UUID::randomUUID);
        update("userId", id);
        return id;
    }

    private static Path getBasePath() {
        return AppProperties.get().getDataDir().resolve("cache");
    }

    private static Path getPath(String key) {
        var name = key + ".cache";
        return getBasePath().resolve(name);
    }

    public static void clear() {
        try {
            FileUtils.cleanDirectory(getBasePath().toFile());
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    public static void clear(String key) {
        var path = getPath(key);
        if (Files.exists(path)) {
            FileUtils.deleteQuietly(path.toFile());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Class<?> type, Supplier<T> notPresent) {
        var path = getPath(key);
        if (Files.exists(path)) {
            try {
                var tree = JsonConfigHelper.readConfig(path);
                if (tree.isMissingNode()) {
                    return notPresent.get();
                }

                return (T) JacksonMapper.newMapper().treeToValue(tree, type);
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).omit().handle();
                FileUtils.deleteQuietly(path.toFile());
            }
        }
        return notPresent.get();
    }

    public static <T> void update(String key, T val) {
        var path = getPath(key);

        try {
            FileUtils.forceMkdirParent(path.toFile());
            var tree = JacksonMapper.newMapper().valueToTree(val);
            JsonConfigHelper.writeConfig(path, tree);
        } catch (Exception e) {
            ErrorEvent.fromThrowable("Could not parse cached data for key " + key, e)
                    .omitted(true)
                    .build()
                    .handle();
        }
    }

    @Override
    public <T> T getValue(String key, Class<?> type, Supplier<T> notPresent) {
        return get(key, type, notPresent);
    }

    @Override
    public <T> void updateValue(String key, T val) {
        update(key, val);
    }
}
