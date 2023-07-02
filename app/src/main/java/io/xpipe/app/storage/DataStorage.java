package io.xpipe.app.storage;

import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedHierarchyStore;
import lombok.Getter;
import lombok.NonNull;

import java.nio.file.Path;
import java.util.*;

public abstract class DataStorage {

    private static final String PERSIST_PROP = "io.xpipe.storage.persist";
    private static final String IMMUTABLE_PROP = "io.xpipe.storage.immutable";

    private static DataStorage INSTANCE;
    protected final Path dir;
    protected final List<DataStoreEntry> storeEntries;

    @Getter
    private final List<StorageListener> listeners = new ArrayList<>();

    public DataStorage() {
        this.dir = AppPrefs.get().storageDirectory().getValue();
        this.storeEntries = new ArrayList<>();
    }

    private static boolean shouldPersist() {
        if (System.getProperty(PERSIST_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(PERSIST_PROP));
        }

        return true;
    }

    public static void init() {
        INSTANCE = shouldPersist() ? new StandardStorage() : new ImpersistentStorage();
        INSTANCE.load();

        INSTANCE.storeEntries.forEach(entry -> entry.simpleRefresh());

        DataStoreProviders.getAll().forEach(dataStoreProvider -> {
            try {
                dataStoreProvider.storageInit();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        });

        INSTANCE.save();
    }

    public static void reset() {
        if (INSTANCE == null) {
            return;
        }

        INSTANCE.save();
        INSTANCE = null;
    }

    public static DataStorage get() {
        return INSTANCE;
    }

    public synchronized boolean refreshChildren(DataStoreEntry e) {
        if (!(e.getStore() instanceof FixedHierarchyStore)) {
            return false;
        }

        try {
            deleteChildren(e, true);
            var newChildren = ((FixedHierarchyStore) e.getStore()).listChildren();
            addStoreEntries(newChildren.entrySet().stream()
                    .map(stringDataStoreEntry -> DataStoreEntry.createNew(
                            UUID.randomUUID(), stringDataStoreEntry.getKey(), stringDataStoreEntry.getValue()))
                    .toArray(DataStoreEntry[]::new));
            return newChildren.size() > 0;
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return false;
        }
    }

    public synchronized void deleteChildren(DataStoreEntry e, boolean deep) {
        // Reverse to delete deepest children first
        var ordered = getStoreChildren(e, false, deep);
        Collections.reverse(ordered);

        synchronized (this) {
            this.storeEntries.removeAll(ordered);
            this.listeners.forEach(l -> l.onStoreRemove(ordered.toArray(DataStoreEntry[]::new)));
        }
        save();
    }

    public synchronized Optional<DataStoreEntry> getParent(DataStoreEntry entry, boolean display) {
        if (!entry.getState().isUsable()) {
            return Optional.empty();
        }

        var provider = entry.getProvider();
        var parent =
                display ? provider.getDisplayParent(entry.getStore()) : provider.getLogicalParent(entry.getStore());
        return parent != null ? getStoreEntryIfPresent(parent) : Optional.empty();
    }

    public synchronized List<DataStoreEntry> getStoreChildren(DataStoreEntry entry, boolean display, boolean deep) {
        var children = new ArrayList<>(getStoreEntries().stream()
                .filter(other -> {
                    if (!other.getState().isUsable()) {
                        return false;
                    }

                    var provider = other.getProvider();
                    var parent = display
                            ? provider.getDisplayParent(other.getStore())
                            : provider.getLogicalParent(other.getStore());
                    return parent != null
                            && entry.getStore().getClass().equals(parent.getClass())
                            && entry.getStore().equals(parent);
                })
                .toList());

        if (deep) {
            for (DataStoreEntry dataStoreEntry : new ArrayList<>(children)) {
                children.addAll(getStoreChildren(dataStoreEntry, display, true));
            }
        }

        return children;
    }

    public abstract Path getInternalStreamPath(@NonNull UUID uuid);

    private void checkImmutable() {
        if (System.getProperty(IMMUTABLE_PROP) != null) {
            if (Boolean.parseBoolean(System.getProperty(IMMUTABLE_PROP))) {
                throw new IllegalStateException("Storage is immutable");
            }
        }
    }

    protected Path getSourcesDir() {
        return dir.resolve("sources");
    }

    protected Path getStoresDir() {
        return dir.resolve("stores");
    }

    protected Path getStreamsDir() {
        return dir.resolve("streams");
    }

    public synchronized List<DataStore> getUsableStores() {
        return new ArrayList<>(getStoreEntries().stream()
                .filter(entry -> !entry.isDisabled())
                .map(DataStoreEntry::getStore)
                .toList());
    }

    public synchronized void renameStoreEntry(DataStoreEntry entry, String name) {
        if (getStoreEntryIfPresent(name).isPresent()) {
            throw new IllegalArgumentException("Store with name " + name + " already exists");
        }

        entry.setName(name);
    }

    public synchronized DataStoreEntry getStoreEntry(@NonNull String name, boolean acceptDisabled) {
        var entry = storeEntries.stream()
                .filter(n -> n.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Store with name " + name + " not found"));
        if (!acceptDisabled && entry.isDisabled()) {
            throw new IllegalArgumentException("Store with name " + name + " is disabled");
        }
        return entry;
    }

    public synchronized DataStoreEntry getStoreEntry(@NonNull DataStore store) {
        var entry = storeEntries.stream()
                .filter(n -> n.getStore() != null && Objects.equals(store.getClass(), n.getStore().getClass()) && store.equals(n.getStore()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        return entry;
    }

    public synchronized Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull DataStore store) {
        var entry =
                storeEntries.stream().filter(n -> store.equals(n.getStore())).findFirst();
        return entry;
    }

    public synchronized Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull String name) {
        return storeEntries.stream()
                .filter(n -> n.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public boolean setAndRefresh(DataStoreEntry entry, DataStore s) {
        var old = entry.getStore();
        deleteChildren(entry, true);
        try {
            entry.setStoreInternal(s, false);
            entry.refresh(true);
            return DataStorage.get().refreshChildren(entry);
        } catch (Exception e) {
            entry.setStoreInternal(old, false);
            entry.simpleRefresh();
            return false;
        }
    }

    public void refreshAsync(DataStoreEntry element, boolean deep) {
        ThreadHelper.runAsync(() -> {
            try {
                element.refresh(deep);
                propagateUpdate(element);
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).reportable(false).handle();
            }
            save();
        });
    }

    void propagateUpdate(DataStoreEntry origin) {
        getStoreChildren(origin, false, false).forEach(entry -> {
            entry.simpleRefresh();
            propagateUpdate(entry);
        });
    }

    public void addStoreEntry(@NonNull DataStoreEntry e) {
        synchronized (this) {
            e.setDirectory(getStoresDir().resolve(e.getUuid().toString()));
            this.storeEntries.add(e);
        }
        propagateUpdate(e);
        save();

        this.listeners.forEach(l -> l.onStoreAdd(e));
    }

    public void addStoreEntries(@NonNull DataStoreEntry... es) {
        synchronized (this) {
            for (DataStoreEntry e : es) {
                e.setDirectory(getStoresDir().resolve(e.getUuid().toString()));
                this.storeEntries.add(e);
                propagateUpdate(e);
            }
            this.listeners.forEach(l -> l.onStoreAdd(es));
        }
        save();
    }

    public DataStoreEntry addStoreEntryIfNotPresent(@NonNull String name, DataStore store) {
        var found = getStoreEntryIfPresent(store);
        if (found.isPresent()) {
            return found.get();
        }

        var e = DataStoreEntry.createNew(UUID.randomUUID(), name, store);
        addStoreEntry(e);
        return e;
    }

    public DataStoreEntry addStoreEntry(@NonNull String name, DataStore store) {
        var e = DataStoreEntry.createNew(UUID.randomUUID(), name, store);
        addStoreEntry(e);
        return e;
    }

    public Optional<String> getStoreDisplayName(DataStore store) {
        if (store == null) {
            return Optional.empty();
        }

        return DataStorage.get().getStoreEntries().stream()
                .filter(entry -> !entry.isDisabled() && entry.getStore().equals(store))
                .findFirst()
                .map(entry -> entry.getName());
    }

    public void deleteStoreEntry(@NonNull DataStoreEntry store) {
        synchronized (this) {
            this.storeEntries.remove(store);
        }
        propagateUpdate(store);
        save();
        this.listeners.forEach(l -> l.onStoreRemove(store));
    }

    public synchronized void addListener(StorageListener l) {
        this.listeners.add(l);
    }

    public abstract void load();

    public void refresh() {
        getStoreEntries().forEach(entry -> {
            entry.simpleRefresh();
        });
        save();
    }

    public abstract void save();

    public synchronized Optional<DataStoreEntry> getStoreEntry(UUID id) {
        return storeEntries.stream().filter(e -> e.getUuid().equals(id)).findAny();
    }

    public synchronized List<DataStoreEntry> getStoreEntries() {
        return new ArrayList<>(storeEntries);
    }
}
