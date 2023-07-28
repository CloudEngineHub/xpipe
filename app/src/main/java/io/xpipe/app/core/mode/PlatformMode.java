package io.xpipe.app.core.mode;

import io.xpipe.app.comp.storage.store.StoreViewState;
import io.xpipe.app.core.*;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.update.UpdateAvailableAlert;
import io.xpipe.app.util.PlatformState;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Application;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class PlatformMode extends OperationMode {

    @Override
    public boolean isSupported() {
        PlatformState.initPlatform();
        return PlatformState.getCurrent() == PlatformState.RUNNING;
    }

    protected void platformSetup() throws Throwable {
        if (App.getApp() != null) {
            return;
        }

        TrackEvent.info("mode", "Platform mode initial setup");
        var r = PlatformState.initPlatform();
        if (r.isPresent()) {
            throw r.get();
        }

        AppFont.loadFonts();
        AppTheme.init();
        AppStyle.init();
        AppImages.init();
        AppLayoutModel.init();
        TrackEvent.info("mode", "Finished essential component initialization before platform");

        TrackEvent.info("mode", "Launching application ...");
        ThreadHelper.create("app", false, () -> {
                    TrackEvent.info("mode", "Application thread started");
                    Application.launch(App.class);
                })
                .start();

        TrackEvent.info("mode", "Waiting for platform application startup ...");
        while (App.getApp() == null) {
            ThreadHelper.sleep(100);
        }

        // If we downloaded an update, and decided to no longer automatically update, don't remind us!
        // You can still update manually in the about tab
        if (AppPrefs.get().automaticallyUpdate().get()) {
            UpdateAvailableAlert.showIfNeeded();
        }

        StoreViewState.init();
    }

    protected void waitForPlatform() {
        // The platform thread waits for the shutdown hook to finish in case SIGTERM is sent.
        // Therefore, we do not wait for the platform when being in a shutdown hook.
        if (PlatformState.getCurrent() == PlatformState.RUNNING
                && !Platform.isFxApplicationThread()
                && !OperationMode.isInShutdownHook()) {
            TrackEvent.info("mode", "Waiting for platform thread ...");
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(latch::countDown);
            try {
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    TrackEvent.info("mode", "Platform wait timed out");
                }
            } catch (InterruptedException ignored) {
            }
            TrackEvent.info("mode", "Synced with platform thread");
        } else {
            TrackEvent.info("mode", "Not waiting for platform thread");
        }
    }

    @Override
    public void finalTeardown() throws Throwable {
        TrackEvent.info("mode", "Shutting down platform components");
        onSwitchFrom();
        PlatformState.teardown();
        TrackEvent.info("mode", "Platform shutdown finished");
        BACKGROUND.finalTeardown();
    }
}
