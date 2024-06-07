package io.xpipe.ext.base.service;

import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class FixedServiceStoreProvider extends AbstractServiceStoreProvider {

    @Override
    public DataStoreEntry getSyntheticParent(DataStoreEntry store) {
        FixedServiceStore s = store.getStore().asNeeded();
        return DataStorage.get().getOrCreateNewSyntheticEntry(s.getHost().get(), "Services",
                FixedServiceGroupStore.builder().parent(s.getDisplayParent().get().ref()).build());
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("fixedService");
    }

    @Override
    public ObservableValue<String> informationString(StoreEntryWrapper wrapper) {
        FixedServiceStore s = wrapper.getEntry().getStore().asNeeded();
        return new SimpleStringProperty("Port " + s.getRemotePort());
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(FixedServiceStore.class);
    }
}
