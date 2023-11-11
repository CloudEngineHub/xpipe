package io.xpipe.ext.base.script;

import io.xpipe.app.comp.base.DropdownComp;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.comp.base.StoreToggleComp;
import io.xpipe.app.comp.base.SystemStateComp;
import io.xpipe.app.comp.store.DenseStoreEntryComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppExtensionManager;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.DataStoreChoiceComp;
import io.xpipe.app.fxcomps.impl.DataStoreListChoiceComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.Identifiers;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SimpleScriptStoreProvider implements DataStoreProvider {

    @Override
    public Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(new SimpleObjectProperty<>(SystemStateComp.State.SUCCESS));
    }

    @Override
    public Comp<?> customEntryComp(StoreSection sec, boolean preferLarge) {
        SimpleScriptStore s = sec.getWrapper().getEntry().getStore().asNeeded();
        if (sec.getWrapper().getValidity().getValue() != DataStoreEntry.Validity.COMPLETE) {
            return new DenseStoreEntryComp(sec.getWrapper(), true, null);
        }

        var groupWrapper = StoreViewState.get().getEntryWrapper(s.getGroup().getEntry());

        var def = new StoreToggleComp("base.isDefault", sec, s.getState().isDefault(), aBoolean -> {
            var state = s.getState();
            state.setDefault(aBoolean);
            s.setState(state);
        });

        var bring = new StoreToggleComp("base.bringToShells", sec, s.getState().isBringToShell(), aBoolean -> {
            var state = s.getState();
            state.setBringToShell(aBoolean);
            s.setState(state);
        });

        // Disable selection if parent group is already made default
        def.disable(BindingsHelper.map(groupWrapper.getPersistentState(), o -> {
            ScriptStore.State state = (ScriptStore.State) o;
            return state.isDefault();
        }));

        // Disable selection if parent group is already brings
        bring.disable(BindingsHelper.map(groupWrapper.getPersistentState(), o -> {
            ScriptStore.State state = (ScriptStore.State) o;
            return state.isBringToShell();
        }));

        var dropdown = new DropdownComp(List.of(def, bring));
        return new DenseStoreEntryComp(sec.getWrapper(), true, dropdown);
    }

    @Override
    public boolean shouldHaveChildren() {
        return false;
    }

    @Override
    public boolean shouldEdit() {
        return true;
    }

    @Override
    public boolean isShareable() {
        return true;
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        SimpleScriptStore st = store.getStore().asNeeded();
        return st.getGroup().get();
    }

    @Override
    public CreationCategory getCreationCategory() {
        return CreationCategory.SCRIPT;
    }

    @Override
    public String getId() {
        return "script";
    }

    @SneakyThrows
    @Override
    public String getDisplayIconFileName(DataStore store) {
        if (store == null) {
            return "proc:shellEnvironment_icon.svg";
        }

        SimpleScriptStore st = store.asNeeded();
        return (String) Class.forName(
                        AppExtensionManager.getInstance()
                                .getExtendedLayer()
                                .findModule("io.xpipe.ext.proc")
                                .orElseThrow(),
                        "io.xpipe.ext.proc.ShellDialectChoiceComp")
                .getDeclaredMethod("getImageName", ShellDialect.class)
                .invoke(null, st.getMinimumDialect());
    }

    @SneakyThrows
    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        SimpleScriptStore st = store.getValue().asNeeded();

        var group = new SimpleObjectProperty<>(st.getGroup());
        Property<ShellDialect> dialect = new SimpleObjectProperty<>(st.getMinimumDialect());
        var others =
                new SimpleListProperty<>(FXCollections.observableArrayList(new ArrayList<>(st.getEffectiveScripts())));
        Property<String> commandProp = new SimpleObjectProperty<>(st.getCommands());
        var type = new SimpleObjectProperty<>(st.getExecutionType());

        Comp<?> choice = (Comp<?>) Class.forName(
                        AppExtensionManager.getInstance()
                                .getExtendedLayer()
                                .findModule("io.xpipe.ext.proc")
                                .orElseThrow(),
                        "io.xpipe.ext.proc.ShellDialectChoiceComp")
                .getDeclaredConstructor(Property.class)
                .newInstance(dialect);
        return new OptionsBuilder()
                .name("snippets")
                .description("snippetsDescription")
                .longDescription("base:scriptDependencies")
                .addComp(
                        new DataStoreListChoiceComp<>(
                                others,
                                ScriptStore.class,
                                scriptStore -> !scriptStore.get().equals(entry) && !others.contains(scriptStore), StoreViewState.get().getAllScriptsCategory()
                        ),
                        others)
                .name("minimumShellDialect")
                .description("minimumShellDialectDescription")
                .longDescription("base:scriptCompatibility")
                .addComp(choice, dialect)
                .nonNull()
                .name("scriptContents")
                .description("scriptContentsDescription")
                .longDescription("base:script")
                .addComp(
                        new IntegratedTextAreaComp(commandProp, false, "commands", Bindings.createStringBinding(() -> {
                            return dialect.getValue() != null
                                    ? dialect.getValue().getScriptFileEnding()
                                    : "sh";
                        })),
                        commandProp)
                .name("executionType")
                .description("executionTypeDescription")
                .longDescription("base:executionType")
                .addComp(new ScriptStoreTypeChoiceComp(type), type)
                .name("scriptGroup")
                .description("scriptGroupDescription")
                .addComp(
                        new DataStoreChoiceComp<>(
                                DataStoreChoiceComp.Mode.OTHER, null, group, ScriptGroupStore.class, null, StoreViewState.get().getAllScriptsCategory()),
                        group)
                .nonNull()
                .bind(
                        () -> {
                            return SimpleScriptStore.builder()
                                    .group(group.get())
                                    .minimumDialect(dialect.getValue())
                                    .scripts(new ArrayList<>(others.get()))
                                    .description(st.getDescription())
                                    .commands(commandProp.getValue())
                                    .executionType(type.get())
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public boolean editByDefault() {
        return true;
    }

    @Override
    public boolean canMoveCategories() {
        return false;
    }

    @Override
    public ObservableValue<String> informationString(StoreEntryWrapper wrapper) {
        SimpleScriptStore scriptStore = wrapper.getEntry().getStore().asNeeded();
        return new SimpleStringProperty((scriptStore.getMinimumDialect() != null
                ? scriptStore.getMinimumDialect().getDisplayName() + " "
                : "")
                + (scriptStore.getExecutionType() == SimpleScriptStore.ExecutionType.TERMINAL_ONLY
                ? "Terminal"
                : scriptStore.getExecutionType() == SimpleScriptStore.ExecutionType.DUMB_ONLY
                ? "Background"
                : "")
                + " Snippet");
    }

    @Override
    public void storageInit() throws Exception {
        DataStorage.get()
                .addStoreEntryIfNotPresent(DataStoreEntry.createNew(
                        UUID.fromString("a9945ad2-db61-4304-97d7-5dc4330691a7"),
                        DataStorage.CUSTOM_SCRIPTS_CATEGORY_UUID,
                        "My scripts",
                        ScriptGroupStore.builder().build()));

        for (PredefinedScriptGroup value : PredefinedScriptGroup.values()) {
            ScriptGroupStore store = ScriptGroupStore.builder()
                    .description(value.getDescription())
                    .build();
            var e = DataStorage.get()
                    .addStoreEntryIfNotPresent(DataStoreEntry.createNew(
                            UUID.nameUUIDFromBytes(("a " + value.getName()).getBytes(StandardCharsets.UTF_8)),
                            DataStorage.PREDEFINED_SCRIPTS_CATEGORY_UUID,
                            value.getName(),
                            store));
            e.setStoreInternal(store, false);
            e.setExpanded(value.isExpanded());
            value.setEntry(e.ref());
        }

        for (PredefinedScriptStore value : PredefinedScriptStore.values()) {
            var previous = DataStorage.get().getStoreEntryIfPresent(value.getUuid());
            var store = value.getScriptStore().get();
            if (previous.isPresent()) {
                previous.get().setStoreInternal(store, false);
                value.setEntry(previous.get().ref());
            } else {
                var e = DataStoreEntry.createNew(
                        value.getUuid(), DataStorage.PREDEFINED_SCRIPTS_CATEGORY_UUID, value.getName(), store);
                DataStorage.get().addStoreEntryIfNotPresent(e);
                value.setEntry(e.ref());
            }
        }
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(SimpleScriptStore.class);
    }

    @Override
    public DataStore defaultStore() {
        return SimpleScriptStore.builder()
                .scripts(List.of())
                .executionType(SimpleScriptStore.ExecutionType.TERMINAL_ONLY)
                .build();
    }

    @Override
    public List<String> getPossibleNames() {
        return Identifiers.get("script");
    }
}
