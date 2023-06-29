package io.xpipe.app.comp.store;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.TabPaneComp;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.function.Predicate;

public class DsDbStoreChooserComp extends SimpleComp {

    private final Property<DataStore> input;
    private final ObservableValue<DataSourceProvider<?>> provider;

    public DsDbStoreChooserComp(Property<DataStore> input, ObservableValue<DataSourceProvider<?>> provider) {
        this.input = input;
        this.provider = provider;
    }

    @Override
    protected Region createSimple() {
        var filter = Bindings.createObjectBinding(
                () -> (Predicate<DataStoreEntry>) e -> {
                    if (provider.getValue() == null) {
                        return e.getProvider().getCategory() == DataStoreProvider.DataCategory.DATABASE;
                    }

                    return provider.getValue().couldSupportStore(e.getStore());
                },
                provider);

        var connections = new TabPaneComp.Entry(
                AppI18n.observable("savedConnections"),
                "mdi2m-monitor",
                NamedStoreChoiceComp.create(filter, input, DataStoreProvider.DataCategory.DATABASE)
                        .styleClass("store-local-file-chooser"));

        var pane = new TabPaneComp(new SimpleObjectProperty<>(connections), List.of(connections));
        pane.apply(s -> AppFont.normal(s.get()));
        return pane.createRegion();
    }
}
