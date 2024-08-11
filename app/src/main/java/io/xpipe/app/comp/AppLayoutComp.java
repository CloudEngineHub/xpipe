package io.xpipe.app.comp;

import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.comp.base.SideMenuBarComp;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.util.Map;
import java.util.stream.Collectors;

public class AppLayoutComp extends Comp<CompStructure<Pane>> {

    private final AppLayoutModel model = AppLayoutModel.get();

    @Override
    public CompStructure<Pane> createBase() {
        Map<Comp<?>, ObservableValue<Boolean>> map = model.getEntries().stream()
                .filter(entry -> entry.comp() != null)
                .collect(Collectors.toMap(
                        entry -> entry.comp(),
                        entry -> Bindings.createBooleanBinding(
                                () -> {
                                    return model.getSelected().getValue().equals(entry);
                                },
                                model.getSelected())));
        var multi = new MultiContentComp(map);
        multi.styleClass("background");

        var pane = new BorderPane();
        var sidebar = new SideMenuBarComp(model.getSelected(), model.getEntries());
        StackPane multiR = (StackPane) multi.createRegion();
        pane.setCenter(multiR);
        var sidebarR = sidebar.createRegion();
        pane.setRight(sidebarR);
        model.getSelected().addListener((c, o, n) -> {
            if (o != null && o.equals(model.getEntries().get(2))) {
                AppPrefs.get().save();
                DataStorage.get().saveAsync();
            }

            if (o != null && o.equals(model.getEntries().get(1))) {
                StoreViewState.get().updateDisplay();
            }
        });
        pane.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            sidebarR.getChildrenUnmodifiable().forEach(node -> {
                var shortcut = (KeyCodeCombination) node.getProperties().get("shortcut");
                if (shortcut != null && shortcut.match(event)) {
                    ((ButtonBase) ((Parent) node).getChildrenUnmodifiable().get(1)).fire();
                    event.consume();
                    return;
                }
            });
        });
        AppFont.normal(pane);
        pane.getStyleClass().add("layout");
        return new SimpleCompStructure<>(pane);
    }
}
