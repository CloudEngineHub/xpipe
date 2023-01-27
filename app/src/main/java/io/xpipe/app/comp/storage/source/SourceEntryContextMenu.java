package io.xpipe.app.comp.storage.source;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataSourceEntry;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.extension.I18n;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.augment.PopupMenuAugment;
import io.xpipe.extension.util.OsHelper;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

public class SourceEntryContextMenu<S extends CompStructure<?>> extends PopupMenuAugment<S> {

    private final SourceEntryWrapper entry;
    private final Region renameTextField;

    public SourceEntryContextMenu(boolean showOnPrimaryButton, SourceEntryWrapper entry, Region renameTextField) {
        super(showOnPrimaryButton);
        this.entry = entry;
        this.renameTextField = renameTextField;
    }

    @Override
    protected ContextMenu createContextMenu() {
        var cm = new ContextMenu();
        AppFont.normal(cm.getStyleableNode());

        for (var actionProvider : entry.getActionProviders()) {
            var name = actionProvider.getName(entry.getEntry().getSource().asNeeded());
            var icon = actionProvider.getIcon(entry.getEntry().getSource().asNeeded());
            var item = new MenuItem(null, new FontIcon(icon));
            item.setOnAction(event -> {
                try {
                    actionProvider.execute(entry.getEntry().getSource().asNeeded());
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).handle();
                }
            });
            item.textProperty().bind(name);
            // item.setDisable(!entry.getState().getValue().isUsable());
            cm.getItems().add(item);

            // actionProvider.applyToRegion(entry.getEntry().getStore().asNeeded(), region);
        }

        if (entry.getActionProviders().size() > 0) {
            cm.getItems().add(new SeparatorMenuItem());
        }

        var properties = new MenuItem(I18n.get("properties"), new FontIcon("mdi2a-application-cog"));
        properties.setOnAction(e -> {});
        //  cm.getItems().add(properties);

        var rename = new MenuItem(I18n.get("rename"), new FontIcon("mdi2r-rename-box"));
        rename.setOnAction(e -> {
            renameTextField.requestFocus();
        });
        cm.getItems().add(rename);

        var validate = new MenuItem(I18n.get("refresh"), new FontIcon("mdal-360"));
        validate.setOnAction(event -> {
            DataStorage.get().refreshAsync(entry.getEntry(), true);
        });
        cm.getItems().add(validate);

        var edit = new MenuItem(I18n.get("edit"), new FontIcon("mdal-edit"));
        edit.setOnAction(event -> entry.editDialog());
        edit.disableProperty().bind(Bindings.equal(DataSourceEntry.State.LOAD_FAILED, entry.getState()));
        cm.getItems().add(edit);

        var del = new MenuItem(I18n.get("delete"), new FontIcon("mdal-delete_outline"));
        del.setOnAction(e -> {
            entry.delete();
        });
        cm.getItems().add(del);

        if (AppPrefs.get().developerMode().getValue()) {
            cm.getItems().add(new SeparatorMenuItem());

            var openDir = new MenuItem(I18n.get("browseInternal"), new FontIcon("mdi2f-folder-open-outline"));
            openDir.setOnAction(e -> {
                OsHelper.browsePath(entry.getEntry().getDirectory());
            });
            cm.getItems().add(openDir);
        }

        return cm;
    }
}
