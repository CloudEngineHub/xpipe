package io.xpipe.app.storage;

import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.FixedHierarchyStore;
import io.xpipe.core.store.*;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import lombok.experimental.NonFinal;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
public class DataStoreEntry extends StorageElement {

    Map<String, Object> storeCache = Collections.synchronizedMap(new HashMap<>());

    @NonFinal
    Validity validity;

    @NonFinal
    @Setter
    JsonNode storeNode;

    @Getter
    @NonFinal
    DataStore store;

    @NonFinal
    Configuration configuration;

    @NonFinal
    boolean expanded;

    AtomicInteger busyCounter = new AtomicInteger();

    @Getter
    @NonFinal
    DataStoreProvider provider;

    @NonFinal
    UUID categoryUuid;

    @NonFinal
    DataStoreState storePersistentState;

    @NonFinal
    JsonNode storePersistentStateNode;

    @NonFinal
    DataStoreColor color;

    @NonFinal
    @Setter
    Set<DataStoreEntry> childrenCache = null;

    @NonFinal
    String notes;

    @NonFinal
    UUID orderBefore;

    private DataStoreEntry(
            Path directory,
            UUID uuid,
            UUID categoryUuid,
            String name,
            Instant lastUsed,
            Instant lastModified,
            JsonNode storeNode,
            boolean dirty,
            Validity validity,
            Configuration configuration,
            JsonNode storePersistentState,
            boolean expanded,
            DataStoreColor color,
            String notes, UUID orderBefore
    ) {
        super(directory, uuid, name, lastUsed, lastModified, dirty);
        this.categoryUuid = categoryUuid;
        this.store = DataStorageParser.storeFromNode(storeNode);
        this.storeNode = storeNode;
        this.validity = validity;
        this.configuration = configuration;
        this.expanded = expanded;
        this.color = color;
        this.orderBefore = orderBefore;
        this.provider = store != null
                ? DataStoreProviders.byStoreClass(store.getClass()).orElse(null)
                : null;
        this.storePersistentStateNode = storePersistentState;
        this.notes = notes;
    }

    private DataStoreEntry(
            Path directory,
            UUID uuid,
            UUID categoryUuid,
            String name,
            Instant lastUsed,
            Instant lastModified,
            DataStore store, UUID orderBefore
    ) {
        super(directory, uuid, name, lastUsed, lastModified, false);
        this.categoryUuid = categoryUuid;
        this.store = store;
        this.orderBefore = orderBefore;
        this.storeNode = null;
        this.validity = Validity.INCOMPLETE;
        this.configuration = Configuration.defaultConfiguration();
        this.expanded = false;
        this.color = null;
        this.provider = null;
        this.storePersistentStateNode = null;
    }

    public static DataStoreEntry createTempWrapper(@NonNull DataStore store) {
        return new DataStoreEntry(
                null,
                UUID.randomUUID(),
                DataStorage.get().getSelectedCategory().getUuid(),
                UUID.randomUUID().toString(),
                Instant.now(),
                Instant.now(),
                store,
                null);
    }

    public static DataStoreEntry createNew(@NonNull String name, @NonNull DataStore store) {
        return createNew(
                UUID.randomUUID(), DataStorage.get().getSelectedCategory().getUuid(), name, store);
    }

    @SneakyThrows
    public static DataStoreEntry createNew(
            @NonNull UUID uuid, @NonNull UUID categoryUuid, @NonNull String name, @NonNull DataStore store) {
        var node = DataStorageWriter.storeToNode(store);
        var validity = DataStorageParser.storeFromNode(node) == null
                ? Validity.LOAD_FAILED
                : store.isComplete() ? Validity.COMPLETE : Validity.INCOMPLETE;
        var entry = new DataStoreEntry(
                null,
                uuid,
                categoryUuid,
                name,
                Instant.now(),
                Instant.now(),
                node,
                true,
                validity,
                Configuration.defaultConfiguration(),
                null,
                false,
                null,
                null,
                null);
        return entry;
    }

    @SneakyThrows
    private static DataStoreEntry createExisting(
            @NonNull Path directory,
            @NonNull UUID uuid,
            @NonNull UUID categoryUuid,
            @NonNull String name,
            @NonNull Instant lastUsed,
            @NonNull Instant lastModified,
            JsonNode storeNode,
            Configuration configuration,
            JsonNode storePersistentState,
            boolean expanded,
            DataStoreColor color,
            String notes,
            UUID orderBeforeEntry) {
        return new DataStoreEntry(
                directory,
                uuid,
                categoryUuid,
                name,
                lastUsed,
                lastModified,
                storeNode,
                false,
                Validity.INCOMPLETE,
                configuration,
                storePersistentState,
                expanded,
                color,
                notes,
                orderBeforeEntry);
    }

    public static Optional<DataStoreEntry> fromDirectory(Path dir) throws Exception {
        ObjectMapper mapper = JacksonMapper.getDefault();

        var entryFile = dir.resolve("entry.json");
        var storeFile = dir.resolve("store.json");
        var stateFile = dir.resolve("state.json");
        var notesFile = dir.resolve("notes.md");
        if (!Files.exists(entryFile) || !Files.exists(storeFile)) {
            return Optional.empty();
        }

        if (!Files.exists(stateFile)) {
            stateFile = entryFile;
        }

        var json = mapper.readTree(entryFile.toFile());
        var stateJson = mapper.readTree(stateFile.toFile());
        var uuid = UUID.fromString(json.required("uuid").textValue());
        var categoryUuid = Optional.ofNullable(json.get("categoryUuid"))
                .map(jsonNode -> UUID.fromString(jsonNode.textValue()))
                .orElse(DataStorage.DEFAULT_CATEGORY_UUID);
        var name = json.required("name").textValue();

        var persistentState = stateJson.get("persistentState");
        var lastUsed = Optional.ofNullable(stateJson.get("lastUsed"))
                .map(jsonNode -> jsonNode.textValue())
                .map(Instant::parse)
                .orElse(Instant.EPOCH);
        var lastModified = Optional.ofNullable(stateJson.get("lastModified"))
                .map(jsonNode -> jsonNode.textValue())
                .map(Instant::parse)
                .orElse(Instant.EPOCH);
        var order = Optional.ofNullable(stateJson.get("orderBefore"))
                .map(node -> {
                    try {
                        return mapper.treeToValue(node, UUID.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .orElse(null);
        var configuration = Optional.ofNullable(json.get("configuration"))
                .map(node -> {
                    try {
                        return mapper.treeToValue(node, Configuration.class);
                    } catch (JsonProcessingException e) {
                        return Configuration.defaultConfiguration();
                    }
                })
                .orElse(Configuration.defaultConfiguration());
        var expanded = Optional.ofNullable(stateJson.get("expanded"))
                .map(jsonNode -> jsonNode.booleanValue())
                .orElse(true);
        var color = Optional.ofNullable(stateJson.get("color"))
                .map(node -> {
                    try {
                        return mapper.treeToValue(node, DataStoreColor.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .orElse(null);

        String notes = null;
        if (Files.exists(notesFile)) {
            notes = Files.readString(notesFile);
        }
        if (notes != null && notes.isBlank()) {
            notes = null;
        }

        // Store loading is prone to errors.
        JsonNode storeNode = null;
        try {
            storeNode = DataStorageEncryption.readPossiblyEncryptedNode(mapper.readTree(storeFile.toFile()));
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
        return Optional.of(createExisting(
                dir,
                uuid,
                categoryUuid,
                name,
                lastUsed,
                lastModified,
                storeNode,
                configuration,
                persistentState,
                expanded,
                color,
                notes,
                order
        ));
    }

    public void setOrderBefore(UUID uuid) {
        var changed = !Objects.equals(orderBefore, uuid);
        this.orderBefore = uuid;
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof DataStoreEntry e && e.getUuid().equals(getUuid()));
    }

    @Override
    public String toString() {
        return getName();
    }

    public void incrementBusyCounter() {
        var r = busyCounter.incrementAndGet() == 1;
        if (r) {
            notifyUpdate(false, false);
        }
    }

    public boolean decrementBusyCounter() {
        var r = busyCounter.decrementAndGet() == 0;
        if (r) {
            notifyUpdate(false, false);
        }
        return r;
    }

    public <T extends DataStore> DataStoreEntryRef<T> ref() {
        return new DataStoreEntryRef<>(this);
    }

    public void setStoreCache(String key, Object value) {
        if (!Objects.equals(storeCache.put(key, value), value)) {
            notifyUpdate(false, false);
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T extends DataStoreState> T getStorePersistentState() {
        if (!(store instanceof StatefulDataStore<?> sds)) {
            return null;
        }

        if (storePersistentStateNode == null && storePersistentState == null) {
            storePersistentState = sds.createDefaultState();
            storePersistentStateNode = JacksonMapper.getDefault().valueToTree(storePersistentState);
        } else if (storePersistentState == null) {
            storePersistentState =
                    JacksonMapper.getDefault().treeToValue(storePersistentStateNode, sds.getStateClass());
            if (storePersistentState == null) {
                storePersistentState = sds.createDefaultState();
                storePersistentStateNode = JacksonMapper.getDefault().valueToTree(storePersistentState);
            }
        }
        return (T) storePersistentState;
    }

    public void setStorePersistentState(DataStoreState value) {
        var changed = !Objects.equals(storePersistentState, value);
        this.storePersistentState = value;
        this.storePersistentStateNode = JacksonMapper.getDefault().valueToTree(value);
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        notifyUpdate(false, true);
    }

    public void setCategoryUuid(UUID categoryUuid) {
        var changed = !Objects.equals(this.categoryUuid, categoryUuid);
        this.categoryUuid = categoryUuid;
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    @Override
    public Path[] getShareableFiles() {
        var notes = directory.resolve("notes.md");
        var list = List.of(directory.resolve("store.json"), directory.resolve("entry.json"));
        return Stream.concat(list.stream(), Files.exists(notes) ? Stream.of(notes) : Stream.of()).toArray(Path[]::new);
    }

    public void writeDataToDisk() throws Exception {
        if (!dirty) {
            return;
        }

        ObjectMapper mapper = JacksonMapper.getDefault();
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        ObjectNode stateObj = JsonNodeFactory.instance.objectNode();
        obj.put("uuid", uuid.toString());
        obj.put("name", name);
        obj.put("categoryUuid", categoryUuid.toString());
        stateObj.put("lastUsed", lastUsed.toString());
        stateObj.put("lastModified", lastModified.toString());
        stateObj.set("color", mapper.valueToTree(color));
        stateObj.set("persistentState", storePersistentStateNode);
        obj.set("configuration", mapper.valueToTree(configuration));
        stateObj.put("expanded", expanded);
        stateObj.put("orderBefore", orderBefore != null ? orderBefore.toString() : null);

        var entryString = mapper.writeValueAsString(obj);
        var stateString = mapper.writeValueAsString(stateObj);
        var storeString = mapper.writeValueAsString(DataStorageEncryption.encryptNodeIfNeeded(storeNode));

        FileUtils.forceMkdir(directory.toFile());
        Files.writeString(directory.resolve("state.json"), stateString);
        Files.writeString(directory.resolve("entry.json"), entryString);
        Files.writeString(directory.resolve("store.json"), storeString);
        var notesFile = directory.resolve("notes.md");
        if (Files.exists(notesFile) && notes == null) {
            Files.delete(notesFile);
        } else if (notes != null) {
            Files.writeString(notesFile, notes);
        }
        dirty = false;
    }

    public void setExpanded(boolean expanded) {
        var changed = expanded != this.expanded;
        this.expanded = expanded;
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    public void setNotes(String newNotes) {
        var changed = !Objects.equals(notes, newNotes);
        this.notes = newNotes;
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    public void setColor(DataStoreColor newColor) {
        var changed = !Objects.equals(color, newColor);
        this.color = newColor;
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    public boolean isDisabled() {
        return validity == Validity.LOAD_FAILED;
    }

    public void applyChanges(DataStoreEntry e) {
        name = e.getName();
        storeNode = e.storeNode;
        store = e.store;
        validity = e.validity;
        provider = e.provider;
        childrenCache = null;
        validity = store == null ? Validity.LOAD_FAILED : store.isComplete() ? Validity.COMPLETE : Validity.INCOMPLETE;
        storePersistentState = e.storePersistentState;
        storePersistentStateNode = e.storePersistentStateNode;
        notifyUpdate(false, true);
    }

    public void setStoreInternal(DataStore store, boolean updateTime) {
        var changed = !Objects.equals(this.store, store);
        if (!changed) {
            return;
        }

        this.store = store;
        this.storeNode = DataStorageWriter.storeToNode(store);
        if (updateTime) {
            lastModified = Instant.now();
        }
        childrenCache = null;
        dirty = true;
    }

    public void reassignStore() {
        this.storeNode = DataStorageWriter.storeToNode(store);
        dirty = true;
    }

    public void validate() {
        try {
            validateOrThrow();
        } catch (Throwable ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }
    }

    public void validateOrThrow() throws Throwable {
        if (store == null) {
            return;
        }

        try {
            store.checkComplete();
            incrementBusyCounter();
            if (store instanceof ValidatableStore l) {
                l.validate();
            } else if (store instanceof FixedHierarchyStore h) {
                childrenCache = h.listChildren(this).stream()
                        .map(DataStoreEntryRef::get)
                        .collect(Collectors.toSet());
            }
        } finally {
            decrementBusyCounter();
        }
    }

    public void refreshStore() {
        if (validity == Validity.LOAD_FAILED) {
            return;
        }

        var newStore = DataStorageParser.storeFromNode(storeNode);
        if (newStore == null) {
            store = null;
            validity = Validity.LOAD_FAILED;
            return;
        }

        var newComplete = newStore.isComplete();
        if (!newComplete) {
            validity = Validity.INCOMPLETE;
            store = newStore;
            return;
        }

        if (!newStore.equals(store)) {
            store = newStore;
        }
        validity = Validity.COMPLETE;
        // Don't count this as modification as this is done always
        notifyUpdate(false, false);
    }

    @SneakyThrows
    public void initializeEntry() {
        if (store instanceof ExpandedLifecycleStore lifecycleStore) {
            try {
                incrementBusyCounter();
                notifyUpdate(false, false);
                lifecycleStore.initializeValidate();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            } finally {
                decrementBusyCounter();
                notifyUpdate(false, false);
            }
        }
    }

    @SneakyThrows
    public void finalizeEntry() {
        if (store instanceof ExpandedLifecycleStore lifecycleStore) {
            try {
                incrementBusyCounter();
                notifyUpdate(false, false);
                lifecycleStore.finalizeValidate();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            } finally {
                decrementBusyCounter();
                notifyUpdate(false, false);
            }
        }
    }

    public boolean shouldSave() {
        return getStore() != null;
    }

    @Getter
    public enum Validity {
        @JsonProperty("loadFailed")
        LOAD_FAILED(false),
        @JsonProperty("incomplete")
        INCOMPLETE(false),
        @JsonProperty("complete")
        COMPLETE(true);

        private final boolean isUsable;

        Validity(boolean isUsable) {
            this.isUsable = isUsable;
        }
    }
}
