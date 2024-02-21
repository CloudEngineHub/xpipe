package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.SideSplitPaneComp;
import io.xpipe.app.core.AppActionLinkDetector;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.fxcomps.SimpleComp;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;

public class StoreLayoutComp extends SimpleComp {

    public StoreLayoutComp() {
        shortcut(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN), structure -> {
            AppActionLinkDetector.detectOnPaste();
        });
    }

    @Override
    protected Region createSimple() {
        var struc = new SideSplitPaneComp(new StoreSidebarComp(), new StoreEntryListComp())
                .withInitialWidth(AppLayoutModel.get().getSavedState().getSidebarWidth())
                .withOnDividerChange(aDouble -> {
                    AppLayoutModel.get().getSavedState().setSidebarWidth(aDouble);
                })
                .createStructure();
        struc.getLeft().setMinWidth(260);
        struc.getLeft().setMaxWidth(500);
        struc.get().getStyleClass().add("store-layout");
        return struc.get();
    }
}
