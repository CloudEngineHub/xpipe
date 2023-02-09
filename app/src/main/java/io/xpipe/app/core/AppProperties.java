package io.xpipe.app.core;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;
import lombok.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;

@Value
public class AppProperties {

    public static final Path DEFAULT_DATA_DIR = Path.of(System.getProperty("user.home"), ".xpipe");
    private static final String DATA_DIR_PROP = "io.xpipe.app.dataDir";
    private static final String EXTENSION_PATHS_PROP = "io.xpipe.app.extensions";
    private static AppProperties INSTANCE;
    String version;
    String build;
    UUID buildUuid;
    String sentryUrl;
    boolean image;
    Path dataDir;
    List<Path> extensionPaths;

    public AppProperties() {
        image = ModuleHelper.isImage();

        Properties props = new Properties();
        AppResources.with(AppResources.XPIPE_MODULE, "app.properties", p -> {
            if (Files.exists(p)) {
                try (var in = Files.newInputStream(p)) {
                    props.load(in);
                } catch (IOException e) {
                    ErrorEvent.fromThrowable(e).omitted(true).build().handle();
                }
            }
        });

        version = Optional.ofNullable(props.getProperty("version")).orElse("dev");
        build = Optional.ofNullable(props.getProperty("build")).orElse("unknown");
        buildUuid = Optional.ofNullable(System.getProperty("io.xpipe.app.buildId"))
                .map(UUID::fromString)
                .orElse(UUID.randomUUID());
        sentryUrl = System.getProperty("io.xpipe.app.sentryUrl");

        extensionPaths = parseExtensionPaths();
        dataDir = parseDataDir();
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
                .tag("extensionPaths", INSTANCE.extensionPaths)
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

    private static Path parseDataDir() {
        if (System.getProperty(DATA_DIR_PROP) != null) {
            try {
                return Path.of(System.getProperty(DATA_DIR_PROP));
            } catch (InvalidPathException ignored) {
            }
        }

        return Path.of(System.getProperty("user.home"), ".xpipe");
    }

    private static List<Path> parseExtensionPaths() {
        if (System.getProperty(EXTENSION_PATHS_PROP) != null) {
            return Arrays.stream(System.getProperty(EXTENSION_PATHS_PROP).split(File.pathSeparator))
                    .<Optional<Path>>map(s -> {
                        try {
                            return Optional.of(Path.of(s));
                        } catch (InvalidPathException ignored) {
                            return Optional.empty();
                        }
                    })
                    .flatMap(Optional::stream)
                    .toList();
        }

        return List.of();
    }

    public Path getDataDir() {
        return dataDir;
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

    public List<Path> getExtensionPaths() {
        return extensionPaths;
    }
}
