package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.storage.StorageFilter;
import io.xpipe.app.comp.store.GuiDsStoreCreator;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedHierarchyStore;
import javafx.beans.property.*;
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
    private final Map<ActionProvider, BooleanProperty> actionProviders;
    private final Property<ActionProvider.DefaultDataStoreCallSite<?>> defaultActionProvider;
    private final BooleanProperty deletable = new SimpleBooleanProperty();
    private final BooleanProperty expanded = new SimpleBooleanProperty();

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
                    actionProviders.put(dataStoreActionProvider, new SimpleBooleanProperty(true));
                });
        this.defaultActionProvider = new SimpleObjectProperty<>();
        setupListeners();
        update();
    }

    public boolean isInStorage() {
        return DataStorage.get().getStoreEntries().contains(entry);
    }

    public void editDialog() {
        GuiDsStoreCreator.showEdit(entry);
    }

    public void delete() {
        DataStorage.get().deleteChildren(this.entry, true);
        DataStorage.get().deleteStoreEntry(this.entry);
    }

    private void setupListeners() {
        name.addListener((c, o, n) -> {
            entry.setName(n);
        });

        expanded.addListener((c, o, n) -> {
            entry.setExpanded(n);
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
        expanded.setValue(entry.isExpanded());
        information.setValue(entry.getInformation());

        loading.setValue(entry.getState() == DataStoreEntry.State.VALIDATING);
        if (entry.getState().isUsable()) {
            try {
                summary.setValue(entry.getProvider().toSummaryString(entry.getStore(), 50));
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            }
        }

        deletable.setValue(entry.getConfiguration().isDeletable()
                || AppPrefs.get().developerDisableGuiRestrictions().getValue());

        actionProviders.keySet().forEach(dataStoreActionProvider -> {
            if (!isInStorage()) {
                actionProviders.get(dataStoreActionProvider).set(false);
                defaultActionProvider.setValue(null);
                return;
            }

            if (!entry.getState().isUsable()
                    && !dataStoreActionProvider
                            .getDataStoreCallSite()
                            .activeType()
                            .equals(ActionProvider.DataStoreCallSite.ActiveType.ALWAYS_ENABLE)) {
                actionProviders.get(dataStoreActionProvider).set(false);
                return;
            }

            var defaultProvider = ActionProvider.ALL.stream()
                    .filter(e -> e.getDefaultDataStoreCallSite() != null
                            && e.getDefaultDataStoreCallSite()
                                    .getApplicableClass()
                                    .isAssignableFrom(entry.getStore().getClass())
                            && e.getDefaultDataStoreCallSite()
                                    .isApplicable(entry.getStore().asNeeded()))
                    .findFirst()
                    .map(ActionProvider::getDefaultDataStoreCallSite)
                    .orElse(null);
            this.defaultActionProvider.setValue(defaultProvider);

            try {
                actionProviders
                        .get(dataStoreActionProvider)
                        .set(dataStoreActionProvider
                                        .getDataStoreCallSite()
                                        .getApplicableClass()
                                        .isAssignableFrom(entry.getStore().getClass())
                                && dataStoreActionProvider
                                        .getDataStoreCallSite()
                                        .isApplicable(entry.getStore().asNeeded()));
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).handle();
                actionProviders.get(dataStoreActionProvider).set(false);
            }
        });
    }

    public void refreshIfNeeded() throws Exception {
        if (entry.getState().equals(DataStoreEntry.State.COMPLETE_BUT_INVALID)
                || entry.getState().equals(DataStoreEntry.State.COMPLETE_NOT_VALIDATED)) {
            getEntry().refresh(true);
        }
    }

    public void refreshAsync() {
        ThreadHelper.runFailableAsync(() -> {
            getEntry().refresh(true);
        });
    }

    public void refreshWithChildren() throws Exception {
        getEntry().refresh(true);
        var hasChildren = DataStorage.get().refreshChildren(entry);
        PlatformThread.runLaterIfNeeded(() -> {
            expanded.set(hasChildren);
        });
    }

    public void refreshWithChildrenAsync() {
        ThreadHelper.runFailableAsync(() -> {
            refreshWithChildren();
        });
    }

    public void mutateAsync(DataStore newValue) {
        ThreadHelper.runAsync(() -> {
            var hasChildren = DataStorage.get().setAndRefresh(getEntry(), newValue);
            PlatformThread.runLaterIfNeeded(() -> {
                expanded.set(hasChildren);
            });
        });
    }

    public void executeDefaultAction() throws Exception {
        var found = getDefaultActionProvider().getValue();
        entry.updateLastUsed();
        if (found != null) {
            refreshIfNeeded();
            found.createAction(entry.getStore().asNeeded()).execute();
        } else if (getEntry().getStore() instanceof FixedHierarchyStore) {
            refreshWithChildrenAsync();
        } else {
        }
    }

    public void toggleExpanded() {
        this.expanded.set(!expanded.getValue());
    }

    @Override
    public boolean shouldShow(String filter) {
        return filter == null || getName().toLowerCase().contains(filter.toLowerCase())
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
