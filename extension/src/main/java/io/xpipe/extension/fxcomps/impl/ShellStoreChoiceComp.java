package io.xpipe.extension.fxcomps.impl;

import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.DataStoreProviders;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.util.CustomComboBoxBuilder;
import io.xpipe.extension.util.XPipeDaemon;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;

import java.util.Optional;
import java.util.function.Predicate;

@AllArgsConstructor
public class ShellStoreChoiceComp<T extends ShellStore> extends SimpleComp {

    public static ShellStoreChoiceComp<ShellStore> proxy(Property<ShellStore> selected) {
        return new ShellStoreChoiceComp<>(Mode.PROXY_CHOICE, null, selected, ShellStore.class, shellStore -> true);
    }

    public static ShellStoreChoiceComp<ShellStore> host(Property<ShellStore> selected) {
        return new ShellStoreChoiceComp<>(Mode.HOST_CHOICE, null, selected, ShellStore.class, shellStore -> true);
    }

    public static ShellStoreChoiceComp<ShellStore> proxy(ShellStore self, Property<ShellStore> selected) {
        return new ShellStoreChoiceComp<>(Mode.PROXY_CHOICE, self, selected, ShellStore.class, shellStore -> true);
    }

    public static ShellStoreChoiceComp<ShellStore> host(ShellStore self, Property<ShellStore> selected) {
        return new ShellStoreChoiceComp<>(Mode.HOST_CHOICE, self, selected, ShellStore.class, shellStore -> true);
    }

    public static enum Mode {
        HOST_CHOICE,
        PROXY_CHOICE
    }

    private final Mode mode;
    private final T self;
    private final Property<T> selected;
    private final Class<T> storeClass;
    private final Predicate<T> applicableCheck;

    protected Region createGraphic(T s) {
        var provider = DataStoreProviders.byStore(s);
        var imgView =
                new PrettyImageComp(new SimpleStringProperty(provider.getDisplayIconFileName()), 16, 16).createRegion();

        var name = XPipeDaemon.getInstance().getNamedStores().stream()
                .filter(e -> e.equals(s))
                .findAny()
                .flatMap(store -> {
                    if (ShellStore.isLocal(store.asNeeded()) && mode == Mode.PROXY_CHOICE) {
                        return Optional.of(I18n.get("none"));
                    }

                    return XPipeDaemon.getInstance().getStoreName(store);
                })
                .orElse(I18n.get("unknown"));

        return new Label(name, imgView);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Region createSimple() {
        var comboBox =
                new CustomComboBoxBuilder<T>(selected, this::createGraphic, new Label(I18n.get("none")), n -> true);
        comboBox.setUnknownNode(t -> createGraphic(t));

        var available = XPipeDaemon.getInstance().getNamedStores().stream()
                .filter(s -> s != self)
                .filter(s -> storeClass.isAssignableFrom(s.getClass()) && applicableCheck.test((T) s))
                .map(s -> (ShellStore) s)
                .toList();

        available.forEach(s -> comboBox.add((T) s));
        ComboBox<Node> cb = comboBox.build();
        cb.getStyleClass().add("choice-comp");
        cb.setMaxWidth(2000);
        return cb;
    }
}
