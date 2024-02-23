package io.xpipe.app.comp.store;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.comp.base.ErrorOverlayComp;
import io.xpipe.app.comp.base.PopupMenuButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.ExceptionConverter;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.ValidationException;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StoreCreationComp extends DialogComp {

    Stage window;
    Consumer<DataStoreEntry> consumer;
    Property<DataStoreProvider> provider;
    Property<DataStore> store;
    Predicate<DataStoreProvider> filter;
    BooleanProperty busy = new SimpleBooleanProperty();
    Property<Validator> validator = new SimpleObjectProperty<>(new SimpleValidator());
    Property<String> messageProp = new SimpleStringProperty();
    BooleanProperty finished = new SimpleBooleanProperty();
    ObservableValue<DataStoreEntry> entry;
    BooleanProperty changedSinceError = new SimpleBooleanProperty();
    BooleanProperty skippable = new SimpleBooleanProperty();
    StringProperty name;
    DataStoreEntry existingEntry;
    boolean staticDisplay;

    public StoreCreationComp(
            Stage window,
            Consumer<DataStoreEntry> consumer,
            Property<DataStoreProvider> provider,
            Property<DataStore> store,
            Predicate<DataStoreProvider> filter,
            String initialName,
            DataStoreEntry existingEntry,
            boolean staticDisplay) {
        this.window = window;
        this.consumer = consumer;
        this.provider = provider;
        this.store = store;
        this.filter = filter;
        this.name = new SimpleStringProperty(initialName != null && !initialName.isEmpty() ? initialName : null);
        this.existingEntry = existingEntry;
        this.staticDisplay = staticDisplay;
        this.store.addListener((c, o, n) -> {
            changedSinceError.setValue(true);
        });
        this.name.addListener((c, o, n) -> {
            changedSinceError.setValue(true);
        });

        this.provider.addListener((c, o, n) -> {
            store.unbind();
            store.setValue(null);
            if (n != null) {
                store.setValue(n.defaultStore());
            }
        });

        this.apply(r -> {
            r.get().setPrefWidth(650);
            r.get().setPrefHeight(750);
        });

        this.validator.addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                newValue.validate();
            });
        });
        this.entry = Bindings.createObjectBinding(
                () -> {
                    if (name.getValue() == null || store.getValue() == null) {
                        return null;
                    }

                    var testE = DataStoreEntry.createNew(
                            UUID.randomUUID(),
                            DataStorage.get().getSelectedCategory().getUuid(),
                            name.getValue(),
                            store.getValue());
                    var p = provider.getValue().getDisplayParent(testE);

                    var targetCategory = p != null
                            ? p.getCategoryUuid()
                            : DataStorage.get().getSelectedCategory().getUuid();
                    var rootCategory = DataStorage.get()
                            .getRootCategory(DataStorage.get()
                                    .getStoreCategoryIfPresent(targetCategory)
                                    .orElseThrow());
                    // Don't put connections in the scripts category ever
                    if ((provider.getValue().getCreationCategory() == null
                                    || !provider.getValue()
                                            .getCreationCategory()
                                            .equals(DataStoreProvider.CreationCategory.SCRIPT))
                            && rootCategory.equals(DataStorage.get().getAllScriptsCategory())) {
                        targetCategory = DataStorage.get()
                                .getDefaultConnectionsCategory()
                                .getUuid();
                    }

                    return DataStoreEntry.createNew(
                            UUID.randomUUID(), targetCategory, name.getValue(), store.getValue());
                },
                name,
                store);
    }

    public static void showEdit(DataStoreEntry e) {
        show(
                e.getName(),
                e.getProvider(),
                e.getStore(),
                v -> true,
                newE -> {
                    ThreadHelper.runAsync(() -> {
                        if (!DataStorage.get().getStoreEntries().contains(e)) {
                            DataStorage.get().addStoreEntryIfNotPresent(newE);
                        } else {
                            DataStorage.get().updateEntry(e, newE);
                        }
                    });
                },
                true,
                e);
    }

    public static void showCreation(DataStoreProvider selected, DataStoreProvider.CreationCategory category) {
        showCreation(selected != null ? selected.defaultStore() : null, category);
    }

    public static void showCreation(DataStore base, DataStoreProvider.CreationCategory category) {
        show(
                null,
                base != null ? DataStoreProviders.byStore(base) : null,
                base,
                dataStoreProvider -> category.equals(dataStoreProvider.getCreationCategory()),
                e -> {
                    try {
                        DataStorage.get().addStoreEntryIfNotPresent(e);
                        if (e.getProvider().shouldHaveChildren()
                                && AppPrefs.get()
                                        .openConnectionSearchWindowOnConnectionCreation()
                                        .get()) {
                            ScanAlert.showAsync(e);
                        }
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).handle();
                    }
                },
                false,
                null);
    }

    private static void show(
            String initialName,
            DataStoreProvider provider,
            DataStore s,
            Predicate<DataStoreProvider> filter,
            Consumer<DataStoreEntry> con,
            boolean staticDisplay,
            DataStoreEntry existingEntry) {
        var prop = new SimpleObjectProperty<>(provider);
        var store = new SimpleObjectProperty<>(s);
        DialogComp.showWindow(
                "addConnection",
                stage -> new StoreCreationComp(
                        stage, con, prop, store, filter, initialName, existingEntry, staticDisplay));
    }

    private static boolean showInvalidConfirmAlert() {
        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("confirmInvalidStoreTitle"));
                    alert.setHeaderText(AppI18n.get("confirmInvalidStoreHeader"));
                    alert.getDialogPane()
                            .setContent(AppWindowHelper.alertContentText(AppI18n.get("confirmInvalidStoreContent")));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                    alert.getButtonTypes().clear();
                    alert.getButtonTypes().add(new ButtonType("Retry", ButtonBar.ButtonData.CANCEL_CLOSE));
                    alert.getButtonTypes().add(new ButtonType("Skip", ButtonBar.ButtonData.OK_DONE));
                })
                .map(b -> b.getButtonData().isDefaultButton())
                .orElse(false);
    }

    @Override
    protected List<Comp<?>> customButtons() {
        return List.of(new ButtonComp(AppI18n.observable("skip"), null, () -> {
                    if (showInvalidConfirmAlert()) {
                        commit();
                    } else {
                        finish();
                    }
                })
                .visible(skippable));
    }

    @Override
    protected ObservableValue<Boolean> busy() {
        return busy;
    }

    @Override
    protected void finish() {
        if (finished.get()) {
            return;
        }

        if (store.getValue() == null) {
            return;
        }

        // We didn't change anything
        if (existingEntry != null && existingEntry.getStore().equals(store.getValue())) {
            commit();
            return;
        }

        if (!validator.getValue().validate()) {
            var msg = validator
                    .getValue()
                    .getValidationResult()
                    .getMessages()
                    .getFirst()
                    .getText();
            TrackEvent.info(msg);
            var newMessage = msg;
            // Temporary fix for equal error message not showing up again
            if (Objects.equals(newMessage, messageProp.getValue())) {
                newMessage = newMessage + " ";
            }
            messageProp.setValue(newMessage);
            changedSinceError.setValue(false);
            return;
        }

        ThreadHelper.runAsync(() -> {
            try (var b = new BooleanScope(busy).start()) {
                DataStorage.get().addStoreEntryInProgress(entry.getValue());
                entry.getValue().validateOrThrow();
                commit();
            } catch (Throwable ex) {
                if (ex instanceof ValidationException) {
                    ErrorEvent.unreportable(ex);
                    skippable.set(false);
                } else {
                    skippable.set(true);
                }

                var newMessage = ExceptionConverter.convertMessage(ex);
                // Temporary fix for equal error message not showing up again
                if (Objects.equals(newMessage, messageProp.getValue())) {
                    newMessage = newMessage + " ";
                }
                messageProp.setValue(newMessage);
                changedSinceError.setValue(false);

                ErrorEvent.fromThrowable(ex).omit().handle();
            } finally {
                DataStorage.get().removeStoreEntryInProgress(entry.getValue());
            }
        });
    }

    @Override
    public Comp<?> content() {
        return Comp.of(this::createLayout);
    }

    @Override
    protected Comp<?> scrollPane(Comp<?> content) {
        var back = super.scrollPane(content);
        return new ErrorOverlayComp(back, messageProp);
    }

    @Override
    public Comp<?> bottom() {
        var disable = Bindings.createBooleanBinding(
                () -> {
                    return provider.getValue() == null
                            || store.getValue() == null
                            || !store.getValue().isComplete()
                            // When switching providers, both observables change one after another.
                            // So temporarily there might be a store class mismatch
                            || provider.getValue().getStoreClasses().stream()
                                    .noneMatch(aClass -> aClass.isAssignableFrom(
                                            store.getValue().getClass()))
                            || provider.getValue().createInsightsMarkdown(store.getValue()) == null;
                },
                provider,
                store);
        return new PopupMenuButtonComp(
                        new SimpleStringProperty("Insights >"),
                        Comp.of(() -> {
                            return provider.getValue() != null
                                    ? provider.getValue()
                                            .createInsightsComp(store)
                                            .createRegion()
                                    : null;
                        }),
                        true)
                .hide(disable)
                .styleClass("button-comp");
    }

    private Region createStoreProperties(Comp<?> comp, Validator propVal) {
        return new OptionsBuilder()
                .addComp(comp, store)
                .name("connectionName")
                .description("connectionNameDescription")
                .addString(name, false)
                .nonNull(propVal)
                .build();
    }

    private void commit() {
        if (finished.get()) {
            return;
        }
        finished.setValue(true);

        if (entry.getValue() != null) {
            consumer.accept(entry.getValue());
        }

        PlatformThread.runLaterIfNeeded(() -> {
            window.close();
        });
    }

    private Region createLayout() {
        var layout = new BorderPane();
        layout.getStyleClass().add("store-creator");
        layout.setPadding(new Insets(20));
        var providerChoice = new StoreProviderChoiceComp(filter, provider, staticDisplay);
        if (staticDisplay) {
            providerChoice.apply(struc -> struc.get().setDisable(true));
        }
        providerChoice.apply(GrowAugment.create(true, false));
        providerChoice.onSceneAssign(struc -> struc.get().requestFocus());

        SimpleChangeListener.apply(provider, n -> {
            if (n != null) {
                var d = n.guiDialog(existingEntry, store);
                var propVal = new SimpleValidator();
                var propR = createStoreProperties(d == null || d.getComp() == null ? null : d.getComp(), propVal);

                var sp = new ScrollPane(propR);
                sp.setFitToWidth(true);
                layout.setCenter(sp);

                validator.setValue(new ChainedValidator(List.of(
                        d != null && d.getValidator() != null ? d.getValidator() : new SimpleValidator(), propVal)));
            } else {
                layout.setCenter(null);
                validator.setValue(new SimpleValidator());
            }
        });

        var sep = new Separator();
        sep.getStyleClass().add("spacer");
        var top = new VBox(providerChoice.createRegion(), new Spacer(7, Orientation.VERTICAL), sep);
        top.getStyleClass().add("top");
        layout.setTop(top);
        return layout;
    }
}
