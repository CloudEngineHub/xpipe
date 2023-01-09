package io.xpipe.extension.util;

import io.xpipe.api.DataSource;
import io.xpipe.beacon.BeaconDaemonController;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.XPipeDaemonMode;
import io.xpipe.core.util.XPipeSession;
import io.xpipe.extension.XPipeServiceProviders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.UUID;

public class DaemonExtensionTest extends ExtensionTest {

    public static DataSource getSource(String type, DataStore store) {
        return DataSource.create(null, type, store);
    }

    public static DataSource getSource(String type, String file) {
        return DataSource.create(null, type, getResourceStore(file));
    }

    public static DataSource getSource(io.xpipe.core.source.DataSource<?> source) {
        return DataSource.create(null, source);
    }

    @BeforeAll
    public static void setup() throws Exception {
        JacksonMapper.initModularized(ModuleLayer.boot());
        XPipeServiceProviders.load(ModuleLayer.boot());
        XPipeSession.init(UUID.randomUUID());
        BeaconDaemonController.start(XPipeDaemonMode.TRAY);
    }

    @AfterAll
    public static void teardown() throws Exception {
        BeaconDaemonController.stop();
    }
}
