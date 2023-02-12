package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.source.store.GuiDsStoreCreator;
import io.xpipe.app.comp.storage.StorageFilter;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import io.xpipe.extension.util.ActionProvider;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class StoreEntryWrapper implements StorageFilter.Filterable {

    private final Property<String> name;
    private final DataStoreEntry entry;
    private final Property<Instant> lastAccess;
    private final BooleanProperty disabled = new SimpleBooleanProperty();
    private final BooleanProperty loading = new SimpleBooleanProperty();
    private final Property<DataStoreEntry.State> state = new SimpleObjectProperty<>();
    private final StringProperty information = new SimpleStringProperty();
    private final StringProperty summary = new SimpleStringProperty();
    private final Map<ActionProvider, ObservableBooleanValue> actionProviders;
    private final ObservableValue<ActionProvider> defaultActionProvider;
    private final BooleanProperty editable = new SimpleBooleanProperty();
    private final BooleanProperty renamable = new SimpleBooleanProperty();
    private final BooleanProperty refreshable = new SimpleBooleanProperty();
    private final BooleanProperty deletable = new SimpleBooleanProperty();

    public StoreEntryWrapper(DataStoreEntry entry) {
        this.entry = entry;
        this.name = new SimpleStringProperty(entry.getName());
        this.lastAccess = new SimpleObjectProperty<>(entry.getLastAccess().minus(Duration.ofMillis(500)));
        this.actionProviders = new LinkedHashMap<>();
        ActionProvider.ALL.stream()
                .filter(dataStoreActionProvider -> {
                    return !entry.isDisabled()
                            && dataStoreActionProvider.getDataStoreCallSite() != null
                            && dataStoreActionProvider
                                    .getDataStoreCallSite()
                                    .getApplicableClass()
                                    .isAssignableFrom(entry.getStore().getClass());
                })
                .forEach(dataStoreActionProvider -> {
                    var property = Bindings.createBooleanBinding(
                            () -> {
                                if (!entry.getState().isUsable()) {
                                    return false;
                                }

                                return dataStoreActionProvider
                                        .getDataStoreCallSite()
                                        .isApplicable(entry.getStore().asNeeded());
                            },
                            disabledProperty(),
                            state,
                            lastAccess);
                    actionProviders.put(dataStoreActionProvider, property);
                });
        this.defaultActionProvider = Bindings.createObjectBinding(() -> {
            var found = actionProviders.entrySet().stream()
                    .filter(e -> e.getValue().get())
                    .filter(e -> e.getKey().getDataStoreCallSite() != null
                            && e.getKey().getDataStoreCallSite().isDefault())
                    .findFirst();
            return found.map(p -> p.getKey()).orElse(null);
        }, actionProviders.values().toArray(Observable[]::new));
        setupListeners();
        update();
    }

    public void editDialog() {
        GuiDsStoreCreator.showEdit(entry);
    }

    public void delete() {
        DataStorage.get().deleteStoreEntry(this.entry);
    }

    private void setupListeners() {
        name.addListener((c, o, n) -> {
            entry.setName(n);
        });

        entry.addListener(() -> PlatformThread.runLaterIfNeeded(() -> {
            update();
        }));
    }

    public void update() {
        // Avoid reupdating name when changed from the name property!
        if (!entry.getName().equals(name.getValue())) {
            name.setValue(entry.getName());
        }

        lastAccess.setValue(entry.getLastAccess());
        disabled.setValue(entry.isDisabled());
        state.setValue(entry.getState());
        information.setValue(
                entry.getInformation() != null
                        ? entry.getInformation()
                        : entry.isDisabled() ? null : entry.getProvider().getDisplayName());

        loading.setValue(entry.getState() == DataStoreEntry.State.VALIDATING);
        if (entry.getState().isUsable()) {
            try {
                summary.setValue(entry.getProvider().toSummaryString(entry.getStore(), 50));
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            }
        }

        editable.setValue(entry.getState() != DataStoreEntry.State.LOAD_FAILED
                && (entry.getConfiguration().isEditable()
                        || AppPrefs.get().developerDisableGuiRestrictions().get()));
        renamable.setValue(entry.getConfiguration().isRenameable()
                || AppPrefs.get().developerDisableGuiRestrictions().getValue());
        refreshable.setValue(entry.getConfiguration().isRefreshable()
                || AppPrefs.get().developerDisableGuiRestrictions().getValue());
        deletable.setValue(entry.getConfiguration().isDeletable()
                || AppPrefs.get().developerDisableGuiRestrictions().getValue());
    }

    @Override
    public boolean shouldShow(String filter) {
        return getName().toLowerCase().contains(filter.toLowerCase())
                || (summary.get() != null && summary.get().toLowerCase().contains(filter.toLowerCase()))
                || (information.get() != null && information.get().toLowerCase().contains(filter.toLowerCase()));
    }

    public String getName() {
        return name.getValue();
    }

    public Property<String> nameProperty() {
        return name;
    }

    public DataStoreEntry getEntry() {
        return entry;
    }

    public Instant getLastAccess() {
        return lastAccess.getValue();
    }

    public Property<Instant> lastAccessProperty() {
        return lastAccess;
    }

    public boolean isDisabled() {
        return disabled.get();
    }

    public BooleanProperty disabledProperty() {
        return disabled;
    }
}
