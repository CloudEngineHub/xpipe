package io.xpipe.app.comp.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.util.ScanAlert;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.kordamp.ikonli.javafx.FontIcon;

public class StoreCreationMenu {

    public static void addButtons(MenuButton menu) {
        var automatically = new MenuItem();
        automatically.setGraphic(new FontIcon("mdi2e-eye-plus-outline"));
        automatically.textProperty().bind(AppI18n.observable("addAutomatically"));
        automatically.setOnAction(event -> {
            ScanAlert.showAsync(null);
            event.consume();
        });
        menu.getItems().add(automatically);
        menu.getItems().add(new SeparatorMenuItem());

        menu.getItems().add(category("addHost", "mdi2h-home-plus", DataStoreProvider.CreationCategory.HOST, "ssh"));

        menu.getItems()
                .add(category("addDesktop", "mdi2c-camera-plus", DataStoreProvider.CreationCategory.DESKTOP, null));

        menu.getItems()
                .add(category("addShell", "mdi2t-text-box-multiple", DataStoreProvider.CreationCategory.SHELL, null));

        menu.getItems()
                .add(category(
                        "addScript", "mdi2s-script-text-outline", DataStoreProvider.CreationCategory.SCRIPT, "script"));

        menu.getItems()
                .add(category(
                        "addService", "mdi2c-cloud-braces", DataStoreProvider.CreationCategory.SERVICE, null));

        menu.getItems()
                .add(category(
                        "addTunnel", "mdi2v-vector-polyline-plus", DataStoreProvider.CreationCategory.TUNNEL, null));

        menu.getItems()
                .add(category(
                        "addCommand", "mdi2c-code-greater-than", DataStoreProvider.CreationCategory.COMMAND, "cmd"));

        menu.getItems()
                .add(category("addDatabase", "mdi2d-database-plus", DataStoreProvider.CreationCategory.DATABASE, null));
    }

    private static MenuItem category(
            String name, String graphic, DataStoreProvider.CreationCategory category, String defaultProvider) {
        var sub = DataStoreProviders.getAll().stream()
                .filter(dataStoreProvider -> category.equals(dataStoreProvider.getCreationCategory()))
                .toList();
        if (sub.size() < 2) {
            var item = new MenuItem();
            item.setGraphic(new FontIcon(graphic));
            item.textProperty().bind(AppI18n.observable(name));
            item.setOnAction(event -> {
                StoreCreationComp.showCreation(
                        defaultProvider != null
                                ? DataStoreProviders.byName(defaultProvider).orElseThrow()
                                : null,
                        category);
                event.consume();
            });
            return item;
        }

        var menu = new Menu();
        menu.setGraphic(new FontIcon(graphic));
        menu.textProperty().bind(AppI18n.observable(name));
        menu.setOnAction(event -> {
            if (event.getTarget() != menu) {
                return;
            }

            StoreCreationComp.showCreation(
                    defaultProvider != null
                            ? DataStoreProviders.byName(defaultProvider).orElseThrow()
                            : null,
                    category);
            event.consume();
        });
        var providers = sub.stream()
                .sorted((o1, o2) -> -o1.getModuleName().compareTo(o2.getModuleName()))
                .toList();
        for (int i = 0; i < providers.size(); i++) {
            var dataStoreProvider = providers.get(i);
            if (i > 0 && !providers.get(i - 1).getModuleName().equals(dataStoreProvider.getModuleName())) {
                menu.getItems().add(new SeparatorMenuItem());
            }

            var item = new MenuItem();
            item.textProperty().bind(dataStoreProvider.displayName());
            item.setGraphic(PrettyImageHelper.ofFixedSizeSquare(dataStoreProvider.getDisplayIconFileName(null), 16)
                    .createRegion());
            item.setOnAction(event -> {
                StoreCreationComp.showCreation(dataStoreProvider, category);
                event.consume();
            });
            menu.getItems().add(item);
        }
        return menu;
    }
}
