package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ComboTextFieldComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.store.StoreCreationComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.SecretRetrievalStrategy;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;

import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class IdentityChoiceComp extends Comp<CompStructure<HBox>> {

    public IdentityChoiceComp(
            ObjectProperty<DataStoreEntryRef<IdentityStore>> selectedReference,
            Property<String> inPlaceUser,
            ObservableValue<SecretRetrievalStrategy> password,
            ObservableValue<SshIdentityStrategy> identityStrategy,
            boolean allowUserInput) {
        this.selectedReference = selectedReference;
        this.inPlaceUser = inPlaceUser;
        this.password = password;
        this.identityStrategy = identityStrategy;
        this.allowUserInput = allowUserInput;
    }

    private final ObjectProperty<DataStoreEntryRef<IdentityStore>> selectedReference;
    private final Property<String> inPlaceUser;
    private final ObservableValue<SecretRetrievalStrategy> password;
    private final ObservableValue<SshIdentityStrategy> identityStrategy;
    private final boolean allowUserInput;

    @Override
    public CompStructure<HBox> createBase() {
        var addButton = new ButtonComp(null, new FontIcon("mdi2a-account-multiple-plus"), () -> {
            var canSync = DataStorage.get()
                    .getStoreCategoryIfPresent(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID)
                    .isPresent();
            var id = canSync
                    ? SyncedIdentityStore.builder()
                            .username(inPlaceUser.getValue())
                            .password(password.getValue())
                            .sshIdentity(identityStrategy.getValue())
                            .build()
                    : LocalIdentityStore.builder()
                            .username(inPlaceUser.getValue())
                            .password(password.getValue())
                            .sshIdentity(identityStrategy.getValue())
                            .build();
            StoreCreationComp.showCreation(
                    id,
                    DataStoreCreationCategory.IDENTITY,
                    dataStoreEntry -> {
                        PlatformThread.runLaterIfNeeded(() -> {
                            applyRef(dataStoreEntry.ref());
                        });
                    },
                    false);
        });
        addButton
                .styleClass(Styles.RIGHT_PILL)
                .disable(selectedReference.isNotNull())
                .grow(false, true)
                .tooltipKey("addReusableIdentity");

        var nodes = new ArrayList<Comp<?>>();
        nodes.add(createComboBox());
        nodes.add(addButton);
        var layout = new HorizontalComp(nodes).apply(struc -> struc.get().setFillHeight(true));

        layout.apply(struc -> {
            struc.get().focusedProperty().addListener((observable, oldValue, newValue) -> {
                struc.get().getChildren().getFirst().requestFocus();
            });
        });

        return new SimpleCompStructure<>(layout.createStructure().get());
    }

    private String formatName(DataStoreEntry storeEntry) {
        IdentityStore id = storeEntry.getStore().asNeeded();
        var suffix = id instanceof LocalIdentityStore
                ? AppI18n.get("localIdentity")
                : id instanceof SyncedIdentityStore && storeEntry.isPerUserStore()
                        ? AppI18n.get("userIdentity")
                        : AppI18n.get("globalIdentity");
        return storeEntry.getName() + " (" + suffix + ")";
    }

    private void applyRef(DataStoreEntryRef<IdentityStore> newRef) {
        this.selectedReference.setValue(newRef);
    }

    private Comp<?> createComboBox() {
        var map = new LinkedHashMap<String, DataStoreEntryRef<IdentityStore>>();
        for (DataStoreEntry storeEntry : DataStorage.get().getStoreEntries()) {
            if (storeEntry.getValidity().isUsable() && storeEntry.getStore() instanceof IdentityStore) {
                map.put(formatName(storeEntry), storeEntry.ref());
            }
        }

        StoreViewState.get().getAllEntries().getList().addListener((ListChangeListener<? super StoreEntryWrapper>)
                c -> {
                    map.clear();
                    for (DataStoreEntry storeEntry : DataStorage.get().getStoreEntries()) {
                        if (storeEntry.getValidity().isUsable() && storeEntry.getStore() instanceof IdentityStore) {
                            map.put(formatName(storeEntry), storeEntry.ref());
                        }
                    }
                });

        var prop = new SimpleStringProperty();
        if (inPlaceUser.getValue() != null) {
            prop.setValue(inPlaceUser.getValue());
        } else if (selectedReference.getValue() != null) {
            prop.setValue(formatName(selectedReference.getValue().get()));
        }

        prop.addListener((observable, oldValue, newValue) -> {
            var ex = map.get(newValue);
            applyRef(ex);

            if (ex == null) {
                inPlaceUser.setValue(newValue);
            } else {
                inPlaceUser.setValue(null);
            }
        });

        selectedReference.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                PlatformThread.runLaterIfNeeded(() -> {
                    var s = formatName(newValue.get());
                    prop.setValue(s);
                });
            }
        });

        var combo = new ComboTextFieldComp(prop, map.keySet().stream().toList(), param -> {
            return new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        return;
                    }

                    setText(item);
                }
            };
        });
        combo.apply(struc -> struc.get().setEditable(allowUserInput));
        combo.hgrow();
        combo.styleClass(Styles.LEFT_PILL);
        combo.grow(false, true);
        combo.apply(struc -> {
            var binding = Bindings.createStringBinding(
                    () -> {
                        if (selectedReference.get() != null) {
                            return selectedReference.get().get().getName();
                        }

                        return AppI18n.get("defineNewIdentity");
                    },
                    AppPrefs.get().language(),
                    selectedReference);
            struc.get().promptTextProperty().bind(binding);
        });
        return combo;
    }
}
