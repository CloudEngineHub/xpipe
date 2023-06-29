package io.xpipe.app.comp.storage.store;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.store.GuiDsStoreCreator;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class StoreCreationBarComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var newStreamStore = new ButtonComp(
                        AppI18n.observable("addCommand"), new FontIcon("mdi2c-code-greater-than"), () -> {
                            GuiDsStoreCreator.showCreation(
                                    v -> v.getDisplayCategory().equals(DataStoreProvider.DisplayCategory.COMMAND));
                        })
                .styleClass(Styles.FLAT)
                .shortcut(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN))
                .apply(new FancyTooltipAugment<>("addCommand"));

        var newHostStore = new ButtonComp(AppI18n.observable("addHost"), new FontIcon("mdi2h-home-plus"), () -> {
                    GuiDsStoreCreator.showCreation(
                            v -> v.getDisplayCategory().equals(DataStoreProvider.DisplayCategory.HOST));
                })
                .styleClass(Styles.FLAT)
                .shortcut(new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN))
                .apply(new FancyTooltipAugment<>("addHost"));

        var newShellStore = new ButtonComp(
                        AppI18n.observable("addShell"), new FontIcon("mdi2t-text-box-multiple"), () -> {
                            GuiDsStoreCreator.showCreation(
                                    v -> v.getDisplayCategory().equals(DataStoreProvider.DisplayCategory.SHELL));
                        })
                .styleClass(Styles.FLAT)
                .shortcut(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN))
                .apply(new FancyTooltipAugment<>("addShell"));

        var newDbStore = new ButtonComp(AppI18n.observable("addDatabase"), new FontIcon("mdi2d-database-plus"), () -> {
                    GuiDsStoreCreator.showCreation(
                            v -> v.getDisplayCategory().equals(DataStoreProvider.DisplayCategory.DATABASE));
                })
                .styleClass(Styles.FLAT)
                .shortcut(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN))
                .apply(new FancyTooltipAugment<>("addDatabase"));

        var box = new VerticalComp(List.of(newHostStore, newShellStore, newStreamStore, newDbStore))
                .apply(struc -> struc.get().setFillWidth(true));
        box.apply(s -> AppFont.medium(s.get()));
        var bar = box.createRegion();
        bar.getStyleClass().add("bar");
        bar.getStyleClass().add("store-creation-bar");
        return bar;
    }
}
