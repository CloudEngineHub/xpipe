package io.xpipe.app.comp.source.store;

import io.xpipe.app.comp.base.InstallExtensionComp;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.comp.base.MessageComp;
import io.xpipe.app.comp.base.MultiStepComp;
import io.xpipe.app.core.AppExtensionManager;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.DownloadModuleInstall;
import io.xpipe.extension.I18n;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.ExceptionConverter;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.augment.GrowAugment;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import io.xpipe.extension.fxcomps.util.SimpleChangeListener;
import io.xpipe.extension.util.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class GuiDsStoreCreator extends MultiStepComp.Step<CompStructure<?>> {

    MultiStepComp parent;
    Property<DataStoreProvider> provider;
    Property<DataStore> input;
    DataStoreProvider.Category generalType;
    BooleanProperty busy = new SimpleBooleanProperty();
    Property<Validator> validator = new SimpleObjectProperty<>(new SimpleValidator());
    Property<String> messageProp = new SimpleStringProperty();
    MessageComp message = new MessageComp(messageProp, 10000);
    BooleanProperty finished = new SimpleBooleanProperty();
    Property<DataStoreEntry> entry = new SimpleObjectProperty<>();
    BooleanProperty changedSinceError = new SimpleBooleanProperty();
    StringProperty name;

    public GuiDsStoreCreator(
            MultiStepComp parent,
            Property<DataStoreProvider> provider,
            Property<DataStore> input,
            DataStoreProvider.Category generalType,
            String initialName) {
        super(null);
        this.parent = parent;
        this.provider = provider;
        this.input = input;
        this.generalType = generalType;
        this.name = new SimpleStringProperty(initialName);
        this.input.addListener((c, o, n) -> {
            changedSinceError.setValue(true);
        });
        this.name.addListener((c, o, n) -> {
            changedSinceError.setValue(true);
        });

        this.provider.addListener((c, o, n) -> {
            input.unbind();
            input.setValue(null);
            if (n != null) {
                input.setValue(n.defaultStore());
            }
        });

        this.apply(r -> {
            r.get().setPrefWidth(AppFont.em(30));
            r.get().setPrefHeight(AppFont.em(35));
        });
    }

    public static void showEdit(DataStoreEntry e) {
        show(e.getName(), e.getProvider(), e.getStore(), e.getProvider().getCategory(), newE -> {
            ThreadHelper.runAsync(() -> {
                e.applyChanges(newE);
                if (!DataStorage.get().getStores().contains(e)) {
                    DataStorage.get().addStore(e);
                }
                DataStorage.get().refresh();
            });
        });
    }

    public static void showCreation(DataStoreProvider.Category cat) {

        show(null, null, null, cat, e -> {
            try {
                DataStorage.get().addStore(e);
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).handle();
            }
        });
    }

    public static void show(
            String initialName,
            DataStoreProvider provider,
            DataStore s,
            DataStoreProvider.Category cat,
            Consumer<DataStoreEntry> con) {
        var prop = new SimpleObjectProperty<DataStoreProvider>(provider);
        var store = new SimpleObjectProperty<DataStore>(s);
        var name = cat == DataStoreProvider.Category.SHELL
                ? "addShellTitle"
                : cat == DataStoreProvider.Category.DATABASE ? "addDatabaseTitle" : "addStreamTitle";
        Platform.runLater(() -> {
            var stage = AppWindowHelper.sideWindow(
                    I18n.get(name),
                    window -> {
                        return new MultiStepComp() {

                            private final GuiDsStoreCreator creator =
                                    new GuiDsStoreCreator(this, prop, store, cat, initialName);

                            @Override
                            protected List<Entry> setup() {
                                return List.of(new Entry(I18n.observable("a"), creator));
                            }

                            @Override
                            protected void finish() {
                                window.close();
                                if (creator.entry.getValue() != null) {
                                    con.accept(creator.entry.getValue());
                                }
                            }
                        };
                    },
                    false,
                    null);
            stage.show();
        });
    }

    private static boolean showInvalidConfirmAlert() {
        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(I18n.get("confirmInvalidStoreTitle"));
                    alert.setHeaderText(I18n.get("confirmInvalidStoreHeader"));
                    alert.setContentText(I18n.get("confirmInvalidStoreContent"));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                })
                .map(b -> b.getButtonData().isDefaultButton())
                .orElse(false);
    }

    private Region createStoreProperties(Comp<?> comp, Validator propVal) {
        return new DynamicOptionsBuilder(false)
                .addComp((ObservableValue<String>) null, comp, input)
                .addTitle(I18n.observable("properties"))
                .addString(I18n.observable("name"), name, false)
                .nonNull(propVal)
                .bind(
                        () -> {
                            if (name.getValue() == null || input.getValue() == null) {
                                return null;
                            }

                            return DataStoreEntry.createNew(UUID.randomUUID(), name.getValue(), input.getValue());
                        },
                        entry)
                .build();
    }

    @Override
    public CompStructure<? extends Region> createBase() {
        var layout = new BorderPane();
        var providerChoice = new DsStoreProviderChoiceComp(generalType, provider);
        providerChoice.apply(GrowAugment.create(true, false));

        SimpleChangeListener.apply(provider, n -> {
            if (n != null) {
                var install = n.getRequiredAdditionalInstallation();
                if (install != null && AppExtensionManager.getInstance().isInstalled(install)) {
                    layout.setCenter(new InstallExtensionComp((DownloadModuleInstall) install).createRegion());
                    validator.setValue(new SimpleValidator());
                    return;
                }

                var d = n.guiDialog(input);

                if (d == null || d.getComp() == null) {
                    layout.setCenter(null);
                    validator.setValue(new SimpleValidator());
                    return;
                }

                var propVal = new SimpleValidator();
                var propR = createStoreProperties(d.getComp(), propVal);
                var box = new VBox(propR);
                box.setSpacing(7);

                layout.setCenter(box);

                validator.setValue(new ChainedValidator(List.of(d.getValidator(), propVal)));
            } else {
                layout.setCenter(null);
                validator.setValue(new SimpleValidator());
            }
        });

        layout.setBottom(message.createRegion());

        var sep = new Separator();
        sep.getStyleClass().add("spacer");
        var top = new VBox(providerChoice.createRegion(), sep);
        top.getStyleClass().add("top");
        layout.setTop(top);
        // layout.getStyleClass().add("data-input-creation-step");
        return new LoadingOverlayComp(Comp.of(() -> layout), busy).createStructure();
    }

    @Override
    public boolean canContinue() {
        if (provider.getValue() != null) {
            var install = provider.getValue().getRequiredAdditionalInstallation();
            if (install != null && !AppExtensionManager.getInstance().isInstalled(install)) {
                ThreadHelper.runAsync(() -> {
                    try (var ignored = new BusyProperty(busy)) {
                        AppExtensionManager.getInstance().installIfNeeded(install);
                        /*
                        TODO: Use reload
                         */
                        finished.setValue(true);
                        OperationMode.shutdown(false, false);
                        PlatformThread.runLaterIfNeeded(parent::next);
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).handle();
                    }
                });
                return false;
            }
        }

        if (finished.get()) {
            return true;
        }

        if (input.getValue() == null) {
            return false;
        }

        if (messageProp.getValue() != null && !changedSinceError.get()) {
            if (showInvalidConfirmAlert()) {
                return true;
            }
        }

        if (!validator.getValue().validate()) {
            var msg = validator
                    .getValue()
                    .getValidationResult()
                    .getMessages()
                    .get(0)
                    .getText();
            TrackEvent.info(msg);
            messageProp.setValue(msg);
            message.show();
            changedSinceError.setValue(false);
            return false;
        }

        ThreadHelper.runAsync(() -> {
            try (var b = new BusyProperty(busy)) {
                entry.getValue().setStore(input.getValue());
                entry.getValue().refresh(true);
                finished.setValue(true);
                PlatformThread.runLaterIfNeeded(parent::next);
            } catch (Exception ex) {
                messageProp.setValue(ExceptionConverter.convertMessage(ex));
                message.show();
                changedSinceError.setValue(false);
                ErrorEvent.fromThrowable(ex).omit().reportable(false).handle();
            }
        });
        return false;
    }
}
