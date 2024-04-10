package io.xpipe.app.util;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class JfxHelper {

    public static Region createNamedEntry(ObservableValue<String> nameString, ObservableValue<String> descString, String image) {
        var header = new Label();
        header.textProperty().bind(nameString);
        AppFont.header(header);
        var desc = new Label();
        desc.textProperty().bind(descString);
        AppFont.small(desc);
        var text = new VBox(header, new Spacer(), desc);
        text.setAlignment(Pos.CENTER_LEFT);

        if (image == null) {
            return text;
        }

        var size = 40;
        var graphic = PrettyImageHelper.ofFixedSizeSquare(image, size).createRegion();

        var hbox = new HBox(graphic, text);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setFillHeight(true);
        hbox.setSpacing(10);

        //        graphic.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> header.getHeight() +
        // desc.getHeight() + 2,
        //                header.heightProperty(), desc.heightProperty()));
        //        graphic.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> header.getHeight() +
        // desc.getHeight() + 2,
        //                header.heightProperty(), desc.heightProperty()));

        return hbox;
    }
}
