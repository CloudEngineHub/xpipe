package io.xpipe.app.issue;

import io.sentry.Sentry;
import io.xpipe.app.core.*;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.update.XPipeDistributionType;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.concurrent.CountDownLatch;

public class TerminalErrorHandler implements ErrorHandler {

    private final ErrorHandler basic = new LogErrorHandler();

    @Override
    public void handle(ErrorEvent event) {
        basic.handle(event);
        handleSentry(event);

        if (!OperationMode.GUI.isSupported()) {
            event.clearAttachments();
            SentryErrorHandler.report(event, null);
            OperationMode.halt(1);
        }

        handleGui(event);
    }

    private void handleSentry(ErrorEvent event) {
        SentryErrorHandler.init();
        if (OperationMode.isInStartup()) {
            Sentry.setExtra("initError", "true");
        }
    }

    private void handleGui(ErrorEvent event) {
        if (!App.isPlatformRunning()) {
            try {
                CountDownLatch latch = new CountDownLatch(1);
                Platform.setImplicitExit(false);
                Platform.startup(latch::countDown);
                try {
                    latch.await();
                } catch (InterruptedException ignored) {
                }
            } catch (Throwable r) {
                if (!"Toolkit already initialized".equals(r.getMessage())) {
                    // Check if platform initialization has failed
                    event.clearAttachments();
                    handleSecondaryException(event, r);
                    return;
                }
            }
        }

        try {
            AppProperties.init();
            AppExtensionManager.init(false);
            AppI18n.init();
            AppStyle.init();
            ErrorHandlerComp.showAndWait(event);
            Sentry.flush(5000);
        } catch (Throwable r) {
            event.clearAttachments();
            handleSecondaryException(event, r);
            return;
        }

        if (OperationMode.isInStartup()) {
            handleProbableUpdate();
        }

        OperationMode.halt(1);
    }

    private static void handleSecondaryException(ErrorEvent event, Throwable t) {
        SentryErrorHandler.report(event, null);
        SentryErrorHandler.report(ErrorEvent.fromThrowable(t).build(), null);
        Sentry.flush(5000);
        t.printStackTrace();
        OperationMode.halt(1);
    }

    private static void handleProbableUpdate() {
        try {
            var rel = XPipeDistributionType.get().getUpdateHandler().refreshUpdateCheck();
            if (rel != null && rel.isUpdate()) {
                var update = AppWindowHelper.showBlockingAlert(alert -> {
                            alert.setAlertType(Alert.AlertType.INFORMATION);
                            alert.setTitle(AppI18n.get("updateAvailableTitle"));
                            alert.setHeaderText(AppI18n.get("updateAvailableHeader", rel.getVersion()));
                            alert.getDialogPane()
                                    .setContent(AppWindowHelper.alertContentText(AppI18n.get("updateAvailableContent")));
                            alert.getButtonTypes().clear();
                            alert.getButtonTypes().add(new ButtonType(AppI18n.get("checkOutUpdate"), ButtonBar.ButtonData.YES));
                            alert.getButtonTypes().add(new ButtonType(AppI18n.get("ignore"), ButtonBar.ButtonData.NO));
                        })
                        .map(buttonType -> buttonType.getButtonData().isDefaultButton())
                        .orElse(false);
                if (update) {
                    Hyperlinks.open(rel.getReleaseUrl());
                }
            }
        } catch (Throwable t) {
            SentryErrorHandler.report(ErrorEvent.fromThrowable(t).build(), null);
            Sentry.flush(5000);
            t.printStackTrace();
            OperationMode.halt(1);
        }
    }
}
