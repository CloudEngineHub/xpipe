package io.xpipe.app.core;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.util.ModuleHelper;
import lombok.Getter;
import lombok.Value;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Value
public class AppProperties {

    public static final Path DEFAULT_DATA_DIR = Path.of(System.getProperty("user.home"), ".xpipe");
    private static final String DATA_DIR_PROP = "io.xpipe.app.dataDir";
    private static final String EXTENSION_PATHS_PROP = "io.xpipe.app.extensions";
    private static AppProperties INSTANCE;
    boolean fullVersion;
    String version;
    String build;
    UUID buildUuid;
    String sentryUrl;
    String arch;
    boolean image;
    boolean staging;
    @Getter
    Path dataDir;
    boolean showcase;

    public AppProperties() {
        image = ModuleHelper.isImage();
        fullVersion = Optional.ofNullable(System.getProperty("io.xpipe.app.fullVersion"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        version =
                Optional.ofNullable(System.getProperty("io.xpipe.app.version")).orElse("dev");
        build = Optional.ofNullable(System.getProperty("io.xpipe.app.build")).orElse("unknown");
        buildUuid = Optional.ofNullable(System.getProperty("io.xpipe.app.buildId"))
                .map(UUID::fromString)
                .orElse(UUID.randomUUID());
        sentryUrl = System.getProperty("io.xpipe.app.sentryUrl");
        arch = System.getProperty("io.xpipe.app.arch");
        staging = Optional.ofNullable(System.getProperty("io.xpipe.app.staging"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        dataDir = parseDataDir();
        showcase = Optional.ofNullable(System.getProperty("io.xpipe.app.showcase"))
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    public static void logSystemProperties() {
        for (var e : System.getProperties().entrySet()) {
            if (List.of("user.dir").contains(e.getKey())) {
                TrackEvent.info("Detected system property " + e.getKey() + "=" + e.getValue());
            }
        }
    }

    public static void logArguments(String[] args) {
        TrackEvent.withInfo("Detected arguments")
                .tag("list", Arrays.asList(args))
                .handle();
    }

    public static void logPassedProperties() {
        TrackEvent.withInfo("Loaded properties")
                .tag("version", INSTANCE.version)
                .tag("build", INSTANCE.build)
                .tag("dataDir", INSTANCE.dataDir)
                .tag("fullVersion", INSTANCE.fullVersion)
                .build();

        for (var e : System.getProperties().entrySet()) {
            if (e.getKey().toString().contains("io.xpipe")) {
                TrackEvent.info("Detected xpipe property " + e.getKey() + "=" + e.getValue());
            }
        }
    }

    public static void init() {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = new AppProperties();
    }

    public static AppProperties get() {
        return INSTANCE;
    }

    private Path parseDataDir() {
        if (System.getProperty(DATA_DIR_PROP) != null) {
            try {
                return Path.of(System.getProperty(DATA_DIR_PROP));
            } catch (InvalidPathException ignored) {
            }
        }

        return Path.of(System.getProperty("user.home"), isStaging() ? ".xpipe_stage" : ".xpipe");
    }

    public String getVersion() {
        return version;
    }

    public boolean isDeveloperMode() {
        if (AppPrefs.get() == null) {
            return false;
        }

        return AppPrefs.get().developerMode().getValue();
    }

    public boolean isImage() {
        return image;
    }

    public String getBuild() {
        return build;
    }
}
