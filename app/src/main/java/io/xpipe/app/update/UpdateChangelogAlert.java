package io.xpipe.app.update;

import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.Hyperlinks;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;

public class UpdateChangelogAlert {

    private static boolean shown = false;

    public static void showIfNeeded() {
        var update = XPipeDistributionType.get().getUpdateHandler().getPerformedUpdate();
        if (update != null && !XPipeDistributionType.get().getUpdateHandler().isUpdateSucceeded()) {
            ErrorEvent.fromMessage(
                            """
            Update installation did not succeed.

            Note that you can also install the latest version manually from %s
            if there are any problems with the automatic update installation.
            """
                                    .formatted(Hyperlinks.GITHUB + "/releases/latest"))
                    .handle();
            return;
        }

        if (update == null || update.getRawDescription() == null) {
            return;
        }

        if (shown) {
            return;
        }
        shown = true;

        AppWindowHelper.showAlert(
                alert -> {
                    alert.setTitle(AppI18n.get("updateChangelogAlertTitle"));
                    alert.setAlertType(Alert.AlertType.NONE);
                    alert.initModality(Modality.NONE);

                    var markdown = new MarkdownComp(update.getRawDescription(), s -> "&nbsp;" + s).createRegion();
                    alert.getDialogPane().setContent(markdown);

                    alert.getButtonTypes().add(new ButtonType(AppI18n.get("gotIt"), ButtonBar.ButtonData.OK_DONE));
                },
                r -> r.filter(b -> b.getButtonData().isDefaultButton()).ifPresent(t -> {}));
    }
}
