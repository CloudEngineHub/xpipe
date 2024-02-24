package io.xpipe.app.ext;

import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.FailableRunnable;
import io.xpipe.core.util.ModuleLayerLoader;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public abstract class ScanProvider {

    private static List<ScanProvider> ALL;

    public static List<ScanProvider> getAll() {
        return ALL;
    }

    public ScanOperation create(DataStore store) {
        return null;
    }

    public ScanOperation create(DataStoreEntry entry, ShellControl sc) throws Exception {
        return null;
    }

    @Value
    @AllArgsConstructor
    public static class ScanOperation {
        String nameKey;
        boolean disabled;
        boolean defaultSelected;
        FailableRunnable<Exception> scanner;
        String licenseFeatureId;

        public ScanOperation(String nameKey, boolean disabled, boolean defaultSelected, FailableRunnable<Exception> scanner) {
            this.nameKey = nameKey;
            this.disabled = disabled;
            this.defaultSelected = defaultSelected;
            this.scanner = scanner;
            this.licenseFeatureId = null;
        }

        public String getLicensedFeatureId() {
            return licenseFeatureId;
        }
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL = ServiceLoader.load(layer, ScanProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .sorted(Comparator.comparing(
                            scanProvider -> scanProvider.getClass().getName()))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean requiresFullDaemon() {
            return true;
        }

        @Override
        public boolean prioritizeLoading() {
            return false;
        }
    }
}
