package io.xpipe.app.storage;

import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class ImpersistentStorage extends DataStorage {

    @Override
    public void load() {
        // Add temporary collection it is not added yet
        getInternalCollection();
    }

    @Override
    public void save() {
        var sourcesDir = getSourcesDir();
        var storesDir = getStoresDir();

        TrackEvent.info("Storage persistence is disabled. Deleting storage contents ...");
        try {
            if (Files.exists(sourcesDir)) {
                FileUtils.cleanDirectory(sourcesDir.toFile());
            }
            if (Files.exists(storesDir)) {
                FileUtils.cleanDirectory(storesDir.toFile());
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).build().handle();
        }
    }

    @Override
    public Path getInternalStreamPath(@NonNull UUID uuid) {
        var newDir = FileUtils.getTempDirectory().toPath().resolve(uuid.toString());
        return newDir;
    }
}
