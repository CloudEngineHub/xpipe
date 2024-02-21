package io.xpipe.app.comp.store;

import atlantafx.base.theme.Styles;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ScanAlert;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class StoreIntroComp extends SimpleComp {

    @Override
    public Region createSimple() {
        var title = new Label(AppI18n.get("storeIntroTitle"));
        title.getStyleClass().add(Styles.TEXT_BOLD);
        AppFont.setSize(title, 7);

        var introDesc = new Label(AppI18n.get("storeIntroDescription"));
        introDesc.setWrapText(true);
        introDesc.setMaxWidth(470);

        var scanButton = new Button(AppI18n.get("detectConnections"), new FontIcon("mdi2m-magnify"));
        scanButton.setOnAction(event -> ScanAlert.showAsync(DataStorage.get().local()));
        scanButton.setDefaultButton(true);
        var scanPane = new StackPane(scanButton);
        scanPane.setAlignment(Pos.CENTER);

        var img = PrettyImageHelper.ofSvg(new SimpleStringProperty("Wave.svg"), 80, 150)
                .createRegion();
        var text = new VBox(title, introDesc);
        text.setSpacing(5);
        text.setAlignment(Pos.CENTER_LEFT);
        var hbox = new HBox(img, text);
        hbox.setSpacing(35);
        hbox.setAlignment(Pos.CENTER);

        var v = new VBox(
                hbox, scanPane
                //                new Separator(Orientation.HORIZONTAL),
                //                documentation,
                //                docLinkPane
                );
        v.setMinWidth(Region.USE_PREF_SIZE);
        v.setMaxWidth(Region.USE_PREF_SIZE);
        v.setMinHeight(Region.USE_PREF_SIZE);
        v.setMaxHeight(Region.USE_PREF_SIZE);

        v.setSpacing(10);
        v.getStyleClass().add("intro");

        var sp = new StackPane(v);
        sp.setAlignment(Pos.CENTER);
        sp.setPickOnBounds(false);
        return sp;
    }
}
