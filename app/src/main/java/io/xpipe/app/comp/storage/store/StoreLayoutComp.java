package io.xpipe.app.comp.storage.store;

import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.augment.GrowAugment;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

public class StoreLayoutComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var listComp = new StoreEntryListComp().apply(GrowAugment.create(false, true));
        var r = new BorderPane();

        var listR = listComp.createRegion();
        var groupHeader = new StoreSidebarComp().createRegion();
        r.setLeft(groupHeader);
        r.setCenter(listR);
        return r;
    }
}
