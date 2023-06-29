package io.xpipe.app.comp.store;

import com.jfoenix.controls.JFXButton;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.core.impl.FileStore;
import javafx.beans.property.Property;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DsFileHistoryComp extends SimpleComp {

    private final DataSourceProvider<?> provider;
    private final Property<FileStore> file;

    public DsFileHistoryComp(DataSourceProvider<?> provider, Property<FileStore> file) {
        this.provider = provider;
        this.file = file;
    }

    @Override
    public Region createSimple() {
        var previous = new VBox();

        List<String> cached = AppCache.get("csv-data-sources", List.class, ArrayList::new);
        if (cached.size() == 0) {
            return previous;
        }

        previous.setFillWidth(true);
        var label = new Label(AppI18n.get("recentFiles"));
        AppFont.header(label);
        previous.getChildren().add(label);

        cached.forEach(s -> {
            var graphic = provider.getDisplayIconFileName();
            var el = JfxHelper.createNamedEntry(FilenameUtils.getName(s), s, graphic);
            var b = new JFXButton();
            b.setGraphic(el);
            b.prefWidthProperty().bind(previous.widthProperty());
            b.setOnAction(e -> {
                file.setValue(FileStore.local(Path.of(s)));
            });
            previous.getChildren().add(b);
        });
        var pane = new ScrollPane(previous);
        pane.setFitToWidth(true);
        return previous;
    }
}
