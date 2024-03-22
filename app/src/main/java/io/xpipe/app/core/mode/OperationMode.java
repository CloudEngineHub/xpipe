package io.xpipe.app.core.mode;

import io.xpipe.app.Main;
import io.xpipe.app.core.*;
import io.xpipe.app.core.check.AppDebugModeCheck;
import io.xpipe.app.core.check.AppTempCheck;
import io.xpipe.app.core.check.AppUserDirectoryCheck;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.*;
import io.xpipe.app.launcher.LauncherCommand;
import io.xpipe.app.launcher.LauncherInput;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.PlatformState;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.XPipeSession;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.FailableRunnable;
import io.xpipe.core.util.XPipeDaemonMode;
import io.xpipe.core.util.XPipeInstallation;
import javafx.application.Platform;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.desktop.AppReopenedEvent;
import java.awt.desktop.AppReopenedListener;
import java.awt.desktop.SystemEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public abstract class OperationMode {

    public static final String MODE_PROP = "io.xpipe.app.mode";
    public static final OperationMode BACKGROUND = new BaseMode();
    public static final OperationMode TRAY = new TrayMode();
    public static final OperationMode GUI = new GuiMode();
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("^-[DP](.+)=(.+)$");
    private static final List<OperationMode> ALL = List.of(BACKGROUND, TRAY, GUI);

    @Getter
    private static boolean inStartup;

    @Getter
    private static boolean inShutdown;

    @Getter
    private static boolean inShutdownHook;

    private static OperationMode CURRENT = null;

    public static OperationMode map(XPipeDaemonMode mode) {
        return switch (mode) {
            case BACKGROUND -> BACKGROUND;
            case TRAY -> TRAY;
            case GUI -> GUI;
        };
    }

    public static XPipeDaemonMode map(OperationMode mode) {
        if (mode == BACKGROUND) {
            return XPipeDaemonMode.BACKGROUND;
        }

        if (mode == TRAY) {
            return XPipeDaemonMode.TRAY;
        }

        if (mode == GUI) {
            return XPipeDaemonMode.GUI;
        }

        return null;
    }

    private static String[] parseProperties(String[] args) {
        List<String> newArgs = new ArrayList<>();
        for (var a : args) {
            var m = PROPERTY_PATTERN.matcher(a);
            if (m.matches()) {
                var k = m.group(1);
                var v = m.group(2);
                System.setProperty(k, v);
            } else {
                newArgs.add(a);
            }
        }
        return newArgs.toArray(String[]::new);
    }

    private static void setup(String[] args) {
        try {
            // Only for handling SIGTERM
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                TrackEvent.info("Received SIGTERM externally");
                OperationMode.shutdown(true, false);
            }));

            // Handle uncaught exceptions
            Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
                ErrorEvent.fromThrowable(ex).unhandled(true).build().handle();
            });

            //            if (true) {
            //                throw new OutOfMemoryError();
            //            }

            TrackEvent.info("Initial setup");
            AppProperties.init();
            AppState.init();
            XPipeSession.init(AppProperties.get().getBuildUuid());
            AppUserDirectoryCheck.check();
            AppTempCheck.check();
            AppLogs.init();
            AppDebugModeCheck.printIfNeeded();
            AppProperties.logArguments(args);
            AppProperties.logSystemProperties();
            AppProperties.logPassedProperties();
            TrackEvent.info("Finished initial setup");
        } catch (Throwable ex) {
            ErrorEvent.fromThrowable(ex).term().handle();
        }
    }

    public static void init(String[] args) {
        inStartup = true;
        var usedArgs = parseProperties(args);
        setup(args);
        LauncherCommand.runLauncher(usedArgs);
        inStartup = false;
        postInit(args);
    }

    public static void postInit(String[] args) {
        try {
            // This will initialize the toolkit on macos and create the dock icon
            // macOS it does not like applications that run fully in the background, so do it always
            if (OsType.getLocal().equals(OsType.MACOS)) {
                // URL open operations have to be handled in a special way on macOS!
                Desktop.getDesktop().setOpenURIHandler(e -> {
                    LauncherInput.handle(List.of(e.getURI().toString()));
                });

                // Do it this way to prevent IDE inspections from complaining
                var c = Class.forName(
                        ModuleLayer.boot().findModule("java.desktop").orElseThrow(), "com.apple.eawt.Application");
                var m = c.getDeclaredMethod("addAppEventListener", SystemEventListener.class);
                m.invoke(c.getMethod("getApplication").invoke(null), new AppReopenedListener() {
                    @Override
                    public void appReopened(AppReopenedEvent e) {
                        OperationMode.switchToAsync(OperationMode.GUI);
                    }
                });

                // Set dock icon explicitly on mac
                // This is necessary in case XPipe was started through a script as it will have no icon otherwise
                if (AppProperties.get().isDeveloperMode() && AppLogs.get().isWriteToSysout()) {
                    try {
                        var iconUrl = Main.class.getResourceAsStream("resources/img/logo/logo_macos_128x128.png");
                        if (iconUrl != null) {
                            var awtIcon = ImageIO.read(iconUrl);
                            Taskbar.getTaskbar().setIconImage(awtIcon);
                        }
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
                    }
                }
            }

            DataStoreProviders.postInit(AppExtensionManager.getInstance().getExtendedLayer());
        } catch (Throwable ex) {
            ErrorEvent.fromThrowable(ex).term().handle();
        }
    }

    public static void switchToAsync(OperationMode newMode) {
        ThreadHelper.createPlatformThread("mode switcher", false, () -> {
                    switchToSyncIfPossible(newMode);
                })
                .start();
    }

    public static void switchToSyncOrThrow(OperationMode newMode) throws Throwable {
        TrackEvent.info("Attempting to switch mode to " + newMode.getId());

        if (!newMode.isSupported()) {
            throw PlatformState.getLastError() != null
                    ? PlatformState.getLastError()
                    : new IllegalStateException("Unsupported operation mode: " + newMode.getId());
        }

        set(newMode);
    }

    public static boolean switchToSyncIfPossible(OperationMode newMode) {
        TrackEvent.info("Attempting to switch mode to " + newMode.getId());

        if (newMode.equals(TRAY) && !TRAY.isSupported()) {
            TrackEvent.info("Tray is not available, using base instead");
            set(BACKGROUND);
            return false;
        }

        if (newMode.equals(GUI) && !GUI.isSupported()) {
            TrackEvent.info("Gui is not available, using base instead");
            set(BACKGROUND);
            return false;
        }

        set(newMode);
        return true;
    }

    public static void switchUp(OperationMode newMode) {
        if (newMode == BACKGROUND) {
            return;
        }

        TrackEvent.info("Attempting to switch mode up to " + newMode.getId());

        if (newMode.equals(TRAY) && TRAY.isSupported() && OperationMode.get() == BACKGROUND) {
            set(TRAY);
            return;
        }

        if (newMode.equals(GUI) && GUI.isSupported()) {
            set(GUI);
            App.getApp().focus();
        }
    }

    public static void close() {
        set(null);
    }

    public static List<OperationMode> getAll() {
        return ALL;
    }

    public static void restart() {
        OperationMode.executeAfterShutdown(() -> {
            var exec = XPipeInstallation.createExternalAsyncLaunchCommand(
                    XPipeInstallation.getLocalDefaultInstallationBasePath(), XPipeDaemonMode.GUI, "");
            LocalShell.getShell().executeSimpleCommand(exec);
        });
    }

    public static void executeAfterShutdown(FailableRunnable<Exception> r) {
        Runnable exec = () -> {
            if (inShutdown) {
                return;
            }

            inShutdown = true;
            inShutdownHook = false;
            try {
                if (CURRENT != null) {
                    CURRENT.finalTeardown();
                }
                CURRENT = null;
                r.run();
            } catch (Throwable ex) {
                ErrorEvent.fromThrowable(ex).build().handle();
                OperationMode.halt(1);
            }

            OperationMode.halt(0);
        };

        if (Platform.isFxApplicationThread() || !Thread.currentThread().isDaemon()) {
            exec.run();
        } else {
            // Creates separate non daemon thread to force execution after shutdown even if current thread is a daemon
            var t = new Thread(exec);
            t.setDaemon(false);
            t.start();
            try {
                t.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static void halt(int code) {
        AppLogs.teardown();
        Runtime.getRuntime().halt(code);
    }

    public static void shutdown(boolean inShutdownHook, boolean hasError) {
        // In case we are stuck while in shutdown, instantly exit this application
        if (inShutdown && inShutdownHook) {
            TrackEvent.info("Received another shutdown request while in shutdown hook. Halting ...");
            OperationMode.halt(1);
        }

        if (inShutdown) {
            return;
        }

        // Run a timer to always exit after some time in case we get stuck
        if (!hasError && !AppProperties.get().isDevelopmentEnvironment()) {
            ThreadHelper.runAsync(() -> {
                ThreadHelper.sleep(25000);
                TrackEvent.info("Shutdown took too long. Halting ...");
                OperationMode.halt(1);
            });
        }

        inShutdown = true;
        OperationMode.inShutdownHook = inShutdownHook;
        try {
            if (CURRENT != null) {
                CURRENT.finalTeardown();
            }
            CURRENT = null;
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).term().handle();
            OperationMode.halt(1);
        }

        OperationMode.halt(hasError ? 1 : 0);
    }

    //    public static synchronized void reload() {
    //        ThreadHelper.create("reloader", false, () -> {
    //                    try {
    //                        switchTo(BACKGROUND);
    //                        CURRENT.finalTeardown();
    //                        CURRENT.onSwitchTo();
    //                        switchTo(GUI);
    //                    } catch (Throwable t) {
    //                        ErrorEvent.fromThrowable(t).build().handle();
    //                        OperationMode.halt(1);
    //                    }
    //                })
    //                .start();
    //    }

    private static synchronized void set(OperationMode newMode) {
        if (CURRENT == null && newMode == null) {
            return;
        }

        if (CURRENT != null && CURRENT.equals(newMode)) {
            return;
        }

        try {
            if (newMode == null) {
                shutdown(false, false);
                return;
            }

            if (CURRENT != null) {
                CURRENT.onSwitchFrom();
            }

            newMode.onSwitchTo();
            CURRENT = newMode;
        } catch (Throwable ex) {
            ErrorEvent.fromThrowable(ex).terminal(true).build().handle();
        }
    }

    public static OperationMode get() {
        return CURRENT;
    }

    public abstract boolean isSupported();

    public abstract String getId();

    public abstract void onSwitchTo() throws Throwable;

    public abstract void onSwitchFrom();

    public abstract void finalTeardown() throws Throwable;

    public ErrorHandler getErrorHandler() {
        return new SyncErrorHandler(new GuiErrorHandler());
    }
}
