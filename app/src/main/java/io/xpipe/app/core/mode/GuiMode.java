package io.xpipe.app.core.mode;

import io.xpipe.app.browser.file.LocalFileSystem;
import io.xpipe.app.browser.icon.FileIconManager;
import io.xpipe.app.core.App;
import io.xpipe.app.core.AppGreetings;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.core.check.AppPtbCheck;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.update.UpdateChangelogAlert;
import io.xpipe.app.util.ThreadHelper;

import javafx.stage.Stage;

public class GuiMode extends PlatformMode {

    @Override
    public String getId() {
        return "gui";
    }

    @Override
    public void onSwitchFrom() {
        PlatformThread.runLaterIfNeededBlocking(() -> {
            TrackEvent.info("Closing windows");
            Stage.getWindows().stream().toList().forEach(w -> {
                w.hide();
            });
        });
    }

    @Override
    public void onSwitchTo() throws Throwable {
        super.onSwitchTo();

        AppGreetings.showIfNeeded();
        AppPtbCheck.check();

        TrackEvent.info("Waiting for window setup completion ...");
        PlatformThread.runLaterIfNeededBlocking(() -> {
            if (AppMainWindow.getInstance() == null) {
                try {
                    App.getApp().setupWindow();
                } catch (Throwable t) {
                    ErrorEvent.fromThrowable(t).terminal(true).handle();
                }
            }
            AppMainWindow.getInstance().show();
        });
        TrackEvent.info("Window setup complete");

        // Can be loaded async
        ThreadHelper.runFailableAsync(() -> {
            FileIconManager.loadIfNecessary();
        });
        ThreadHelper.runFailableAsync(() -> {
            LocalFileSystem.init();
        });

        UpdateChangelogAlert.showIfNeeded();
    }
}
