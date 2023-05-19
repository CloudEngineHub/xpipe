package io.xpipe.app.comp;

import io.xpipe.app.browser.BrowserComp;
import io.xpipe.app.browser.BrowserModel;
import io.xpipe.app.comp.about.AboutTabComp;
import io.xpipe.app.comp.base.SideMenuBarComp;
import io.xpipe.app.comp.storage.store.StoreLayoutComp;
import io.xpipe.app.core.AppActionLinkDetector;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.prefs.AppPrefs;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AppLayoutComp extends Comp<CompStructure<BorderPane>> {

    private final List<SideMenuBarComp.Entry> entries;
    private final Property<SideMenuBarComp.Entry> selected;

    public AppLayoutComp() {
        entries = createEntryList();
        selected = new SimpleObjectProperty<>(entries.get(0));

        shortcut(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN), structure -> {
            AppActionLinkDetector.detectOnPaste();
        });
    }

    @SneakyThrows
    private List<SideMenuBarComp.Entry> createEntryList() {
        var l = new ArrayList<>(List.of(
                new SideMenuBarComp.Entry(AppI18n.observable("connections"), "mdi2c-connection", new StoreLayoutComp()),
                new SideMenuBarComp.Entry(
                        AppI18n.observable("browser"),
                        "mdi2f-file-cabinet",
                        new BrowserComp(BrowserModel.DEFAULT)),
                // new SideMenuBarComp.Entry(AppI18n.observable("data"), "mdsal-dvr", new SourceCollectionLayoutComp()),
                new SideMenuBarComp.Entry(
                        AppI18n.observable("settings"), "mdsmz-miscellaneous_services", new PrefsComp(this)),
                // new SideMenuBarComp.Entry(AppI18n.observable("help"), "mdi2b-book-open-variant", new
                // StorageLayoutComp()),
                // new SideMenuBarComp.Entry(AppI18n.observable("account"), "mdi2a-account", new StorageLayoutComp()),
                new SideMenuBarComp.Entry(AppI18n.observable("about"), "mdi2p-package-variant", new AboutTabComp())));
        if (AppProperties.get().isDeveloperMode()) {
            l.add(new SideMenuBarComp.Entry(
                    AppI18n.observable("developer"), "mdi2b-book-open-variant", new DeveloperTabComp()));
        }

        //        l.add(new SideMenuBarComp.Entry(AppI18n.observable("abc"), "mdi2b-book-open-variant", Comp.of(() -> {
        //            var fi = new FontIcon("mdsal-dvr");
        //            fi.setIconSize(30);
        //            fi.setIconColor(Color.valueOf("#111C"));
        //            JfxHelper.addEffect(fi);
        //            return new StackPane(fi);
        //        })));

        return l;
    }

    @Override
    public CompStructure<BorderPane> createBase() {
        var map = new HashMap<SideMenuBarComp.Entry, Region>();
        entries.forEach(entry -> map.put(entry, entry.comp().createRegion()));

        var pane = new BorderPane();
        var sidebar = new SideMenuBarComp(selected, entries);
        pane.setCenter(map.get(selected.getValue()));
        pane.setRight(sidebar.createRegion());
        selected.addListener((c, o, n) -> {
            if (o != null && o.equals(entries.get(2))) {
                AppPrefs.get().save();
            }

            pane.setCenter(map.get(n));
        });
        pane.setCenter(map.get(selected.getValue()));
        pane.setPrefWidth(1280);
        pane.setPrefHeight(720);
        AppFont.normal(pane);
        return new SimpleCompStructure<>(pane);
    }

    public List<SideMenuBarComp.Entry> getEntries() {
        return entries;
    }

    public SideMenuBarComp.Entry getSelected() {
        return selected.getValue();
    }

    public Property<SideMenuBarComp.Entry> selectedProperty() {
        return selected;
    }
}
