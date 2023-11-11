package io.xpipe.app.comp.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.util.ScanAlert;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.kordamp.ikonli.javafx.FontIcon;

public class StoreCreationMenu {

    public static void addButtons(MenuButton menu) {
        {
            var automatically = new MenuItem();
            automatically.setGraphic(new FontIcon("mdi2e-eye-plus-outline"));
            automatically.textProperty().bind(AppI18n.observable("addAutomatically"));
            automatically.setOnAction(event -> {
                ScanAlert.showAsync(null);
                event.consume();
            });
            menu.getItems().add(automatically);
            menu.getItems().add(new SeparatorMenuItem());
        }

        {
            var host = new MenuItem();
            host.setGraphic(new FontIcon("mdi2h-home-plus"));
            host.textProperty().bind(AppI18n.observable("addHost"));
            host.setOnAction(event -> {
                GuiDsStoreCreator.showCreation(DataStoreProviders.byName("ssh").orElseThrow(),
                        v -> DataStoreProvider.CreationCategory.HOST.equals(v.getCreationCategory()));
                event.consume();
            });
            menu.getItems().add(host);
        }
        {
            var shell = new MenuItem();
            shell.setGraphic(new FontIcon("mdi2t-text-box-multiple"));
            shell.textProperty().bind(AppI18n.observable("addShell"));
            shell.setOnAction(event -> {
                GuiDsStoreCreator.showCreation(null,
                        v -> DataStoreProvider.CreationCategory.SHELL.equals(v.getCreationCategory()));
                event.consume();
            });
            menu.getItems().add(shell);
        }
        {
            var cmd = new MenuItem();
            cmd.setGraphic(new FontIcon("mdi2c-code-greater-than"));
            cmd.textProperty().bind(AppI18n.observable("addCommand"));
            cmd.setOnAction(event -> {
                GuiDsStoreCreator.showCreation(DataStoreProviders.byName("cmd").orElseThrow(),
                        v -> DataStoreProvider.CreationCategory.COMMAND.equals(v.getCreationCategory()));
                event.consume();
            });
            menu.getItems().add(cmd);
        }
        {
            var db = new MenuItem();
            db.setGraphic(new FontIcon("mdi2d-database-plus"));
            db.textProperty().bind(AppI18n.observable("addDatabase"));
            db.setOnAction(event -> {
                GuiDsStoreCreator.showCreation(null,
                        v -> DataStoreProvider.CreationCategory.DATABASE.equals(v.getCreationCategory()));
                event.consume();
            });
            menu.getItems().add(db);
        }
        {
            var tunnel = new MenuItem();
            tunnel.setGraphic(new FontIcon("mdi2v-vector-polyline-plus"));
            tunnel.textProperty().bind(AppI18n.observable("addTunnel"));
            tunnel.setOnAction(event -> {
                GuiDsStoreCreator.showCreation(null,
                        v -> DataStoreProvider.CreationCategory.TUNNEL.equals(v.getCreationCategory()));
                event.consume();
            });
            menu.getItems().add(tunnel);
        }
        {
            var script = new MenuItem();
            script.setGraphic(new FontIcon("mdi2s-script-text-outline"));
            script.textProperty().bind(AppI18n.observable("addScript"));
            script.setOnAction(event -> {
                GuiDsStoreCreator.showCreation(DataStoreProviders.byName("script").orElseThrow(),
                                               v -> DataStoreProvider.CreationCategory.SCRIPT.equals(v.getCreationCategory()));
                event.consume();
            });
            menu.getItems().add(script);
        }
    }

}
