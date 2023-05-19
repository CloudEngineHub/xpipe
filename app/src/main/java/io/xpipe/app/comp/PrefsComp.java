package io.xpipe.app.comp;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ClearCacheAlert;
import javafx.geometry.Pos;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.MasterDetailPane;

public class PrefsComp extends SimpleComp {

    private final AppLayoutComp layout;

    public PrefsComp(AppLayoutComp layout) {
        this.layout = layout;
    }

    @Override
    protected Region createSimple() {
        return createButtonOverlay();
    }

    private Region createButtonOverlay() {
        var pfx = AppPrefs.get().createControls().getView();
        pfx.getStyleClass().add("prefs");
        MasterDetailPane p = (MasterDetailPane) pfx.getCenter();
        p.dividerPositionProperty().setValue(0.27);

        var clearCaches = new ButtonComp(AppI18n.observable("clearCaches"), null, ClearCacheAlert::show).createRegion();
        // var reload = new ButtonComp(AppI18n.observable("reload"), null, () -> OperationMode.reload()).createRegion();
        var leftButtons = new HBox(clearCaches);
        leftButtons.setAlignment(Pos.CENTER);
        leftButtons.prefWidthProperty().bind(((Region) p.getDetailNode()).widthProperty());

        var leftPane = new AnchorPane(leftButtons);
        leftPane.setPickOnBounds(false);
        AnchorPane.setBottomAnchor(leftButtons, 15.0);
        AnchorPane.setLeftAnchor(leftButtons, 15.0);

        var stack = new StackPane(pfx, leftPane);
        stack.setPickOnBounds(false);
        AppFont.medium(stack);

        return stack;
    }
}
