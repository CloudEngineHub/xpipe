package io.xpipe.app.comp.storage.store;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.layout.*;

public class DenseStoreEntryComp extends StoreEntryComp {

    private final boolean showIcon;

    public DenseStoreEntryComp(StoreEntryWrapper entry, boolean showIcon, Comp<?> content) {
        super(entry, content);
        this.showIcon = showIcon;
    }

    protected Region createContent() {
        var name = createName().createRegion();

        var grid = new GridPane();
        grid.setHgap(8);

        if (showIcon) {
            var storeIcon = createIcon(30, 25);
            grid.getColumnConstraints().add(new ColumnConstraints(30));
            grid.add(storeIcon, 0, 0);
            GridPane.setHalignment(storeIcon, HPos.CENTER);
        } else {
            grid.add(new Region(), 0, 0);
            grid.getColumnConstraints().add(new ColumnConstraints(0));
        }

        var custom = new ColumnConstraints(content != null ? 300 : 0);
        custom.setHalignment(HPos.RIGHT);
        custom.setMinWidth(Region.USE_PREF_SIZE);
        custom.setMaxWidth(Region.USE_PREF_SIZE);

        var info = new ColumnConstraints(content != null ? 300 : 600);
        info.setHalignment(HPos.LEFT);
        info.setMinWidth(Region.USE_PREF_SIZE);
        info.setMaxWidth(Region.USE_PREF_SIZE);

        var nameCC = new ColumnConstraints();
        nameCC.setMinWidth(100);
        nameCC.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(nameCC);
        grid.add(name, 1, 0);

        grid.add(createInformation(), 2, 0);
        grid.getColumnConstraints().addAll(info, custom);

        var cr = content != null ? content.createRegion() : new Region();
        var bb = createButtonBar().createRegion();
        var controls = new HBox(cr, bb);
        controls.setFillHeight(true);
        controls.setAlignment(Pos.CENTER_RIGHT);
        controls.setSpacing(10);
        HBox.setHgrow(cr, Priority.ALWAYS);
        grid.add(controls, 3, 0);

        GrowAugment.create(true, false).augment(grid);

        grid.getStyleClass().add("store-entry-grid");
        grid.getStyleClass().add("dense");

        applyState(grid);
        return grid;
    }
}
