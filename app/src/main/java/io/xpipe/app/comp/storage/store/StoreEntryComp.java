package io.xpipe.app.comp.storage.store;

import com.jfoenix.controls.JFXButton;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.impl.*;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import lombok.SneakyThrows;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;

public class StoreEntryComp extends SimpleComp {

    private static final double NAME_WIDTH = 0.30;
    private static final double STORE_TYPE_WIDTH = 0.08;
    private static final double DETAILS_WIDTH = 0.52;
    private static final double BUTTONS_WIDTH = 0.1;
    private static final PseudoClass FAILED = PseudoClass.getPseudoClass("failed");
    private static final PseudoClass INCOMPLETE = PseudoClass.getPseudoClass("incomplete");
    private final StoreEntryWrapper entry;

    public StoreEntryComp(StoreEntryWrapper entry) {
        this.entry = entry;
    }

    private Label createInformation() {
        var information = new Label();
        information.textProperty().bind(PlatformThread.sync(entry.getInformation()));
        information.getStyleClass().add("information");
        AppFont.header(information);
        return information;
    }

    private Label createSummary() {
        var summary = new Label();
        summary.textProperty().bind(PlatformThread.sync(entry.getSummary()));
        summary.getStyleClass().add("summary");
        AppFont.small(summary);
        return summary;
    }

    private void applyState(Node node) {
        SimpleChangeListener.apply(PlatformThread.sync(entry.getState()), val -> {
            switch (val) {
                case LOAD_FAILED -> {
                    node.pseudoClassStateChanged(FAILED, true);
                    node.pseudoClassStateChanged(INCOMPLETE, false);
                }
                case INCOMPLETE -> {
                    node.pseudoClassStateChanged(FAILED, false);
                    node.pseudoClassStateChanged(INCOMPLETE, true);
                }
                default -> {
                    node.pseudoClassStateChanged(FAILED, false);
                    node.pseudoClassStateChanged(INCOMPLETE, false);
                }
            }
        });
    }

    private Comp<?> createName() {
        var name = new LabelComp(entry.nameProperty())
                .apply(struc -> struc.get().setTextOverrun(OverrunStyle.CENTER_ELLIPSIS))
                .apply(struc -> struc.get().setPadding(new Insets(5, 5, 5, 0)));
        name.apply(s -> AppFont.header(s.get()));
        return name;
    }

    private Node createIcon() {
        var img = entry.isDisabled()
                ? "disabled_icon.png"
                : entry.getEntry()
                        .getProvider()
                        .getDisplayIconFileName(entry.getEntry().getStore());
        var imageComp = new PrettyImageComp(new SimpleStringProperty(img), 55, 45);
        var storeIcon = imageComp.createRegion();
        storeIcon.getStyleClass().add("icon");
        if (entry.getState().getValue().isUsable()) {
            new FancyTooltipAugment<>(new SimpleStringProperty(
                            entry.getEntry().getProvider().getDisplayName()))
                    .augment(storeIcon);
        }
        return storeIcon;
    }

    protected Region createContent() {
        var name = createName().createRegion();

        var size = createInformation();

        var date = new Label();
        date.textProperty().bind(AppI18n.readableDuration("usedDate", PlatformThread.sync(entry.lastAccessProperty())));
        AppFont.small(date);
        date.getStyleClass().add("date");

        var grid = new GridPane();

        var storeIcon = createIcon();

        grid.getColumnConstraints()
                .addAll(
                        createShareConstraint(grid, STORE_TYPE_WIDTH), createShareConstraint(grid, NAME_WIDTH),
                        createShareConstraint(grid, DETAILS_WIDTH), createShareConstraint(grid, BUTTONS_WIDTH));
        grid.add(storeIcon, 0, 0, 1, 2);
        grid.add(name, 1, 0);
        grid.add(date, 1, 1);
        grid.add(createSummary(), 2, 1);
        grid.add(createInformation(), 2, 0);
        grid.add(createButtonBar().createRegion(), 3, 0, 1, 2);
        grid.setVgap(5);
        GridPane.setHalignment(storeIcon, HPos.CENTER);

        AppFont.small(size);
        AppFont.small(date);

        grid.getStyleClass().add("store-entry-grid");

        applyState(grid);

        var button = new JFXButton();
        button.setGraphic(grid);
        GrowAugment.create(true, false).augment(new SimpleCompStructure<>(grid));
        button.getStyleClass().add("store-entry-comp");
        button.setMaxWidth(2000);
        button.setFocusTraversable(false);
        button.setOnAction(event -> {
            event.consume();
            ThreadHelper.runFailableAsync(() -> {
                var found = entry.getDefaultActionProvider().getValue();
                if (entry.getState().getValue().equals(DataStoreEntry.State.COMPLETE_BUT_INVALID) || found == null) {
                    entry.getEntry().refresh(true);
                    entry.getExpanded().set(true);
                }

                if (found != null) {
                    entry.getEntry().updateLastUsed();
                    found.createAction(entry.getEntry().getStore().asNeeded()).execute();
                }
            });
        });

        new ContextMenuAugment<>(false, () -> StoreEntryComp.this.createContextMenu()).augment(new SimpleCompStructure<>(button));

        return button;
    }

    private Comp<?> createButtonBar() {
        var list = new ArrayList<Comp<?>>();
        for (var p : entry.getActionProviders().entrySet()) {
            var actionProvider = p.getKey().getDataStoreCallSite();
            if (!actionProvider.isMajor()
                    || p.getKey().equals(entry.getDefaultActionProvider().getValue())) {
                continue;
            }

            var button = new IconButtonComp(
                    actionProvider.getIcon(entry.getEntry().getStore().asNeeded()), () -> {
                        ThreadHelper.runFailableAsync(() -> {
                            var action = actionProvider.createAction(
                                    entry.getEntry().getStore().asNeeded());
                            action.execute();
                        });
                    });
            button.apply(new FancyTooltipAugment<>(
                    actionProvider.getName(entry.getEntry().getStore().asNeeded())));
            if (actionProvider.activeType() == ActionProvider.DataStoreCallSite.ActiveType.ONLY_SHOW_IF_ENABLED) {
                button.hide(Bindings.not(p.getValue()));
            } else if (actionProvider.activeType() == ActionProvider.DataStoreCallSite.ActiveType.ALWAYS_SHOW) {
                button.disable(Bindings.not(p.getValue()));
            }
            list.add(button);
        }

        var settingsButton = createSettingsButton();
        list.add(settingsButton);
        return new HorizontalComp(list)
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_RIGHT))
                .apply(struc -> {
                    for (Node child : struc.get().getChildren()) {
                        ((Region) child)
                                .prefWidthProperty()
                                .bind((struc.get().heightProperty().divide(1.7)));
                        ((Region) child).prefHeightProperty().bind((struc.get().heightProperty()));
                    }
                });
    }

    private Comp<?> createSettingsButton() {
        var settingsButton = new IconButtonComp("mdomz-settings");
        settingsButton.styleClass("settings");
        settingsButton.apply(new ContextMenuAugment<>(true, () -> StoreEntryComp.this.createContextMenu()));
        settingsButton.apply(GrowAugment.create(false, true));
        settingsButton.apply(s -> {
            s.get().prefWidthProperty().bind(Bindings.divide(s.get().heightProperty(), 1.35));
        });
        settingsButton.apply(new FancyTooltipAugment<>("more"));
        return settingsButton;
    }

    private ContextMenu createContextMenu() {
        var contextMenu = new ContextMenu();
        AppFont.normal(contextMenu.getStyleableNode());

        for (var p : entry.getActionProviders().entrySet()) {
            var actionProvider = p.getKey().getDataStoreCallSite();
            if (actionProvider.isMajor()) {
                continue;
            }

            var name = actionProvider.getName(entry.getEntry().getStore().asNeeded());
            var icon = actionProvider.getIcon(entry.getEntry().getStore().asNeeded());
            var item = new MenuItem(null, new FontIcon(icon));
            item.setOnAction(event -> {
                ThreadHelper.runFailableAsync(() -> {
                    var action = actionProvider.createAction(
                            entry.getEntry().getStore().asNeeded());
                    action.execute();
                });
            });
            item.textProperty().bind(name);
            if (actionProvider.activeType() == ActionProvider.DataStoreCallSite.ActiveType.ONLY_SHOW_IF_ENABLED) {
                item.visibleProperty().bind(p.getValue());
            } else if (actionProvider.activeType() == ActionProvider.DataStoreCallSite.ActiveType.ALWAYS_SHOW) {
                item.disableProperty().bind(Bindings.not(p.getValue()));
            }
            contextMenu.getItems().add(item);
        }

        if (entry.getActionProviders().size() > 0) {
            contextMenu.getItems().add(new SeparatorMenuItem());
        }

        if (AppPrefs.get().developerMode().getValue()) {
            var browse = new MenuItem(AppI18n.get("browse"), new FontIcon("mdi2f-folder-open-outline"));
            browse.setOnAction(
                    event -> DesktopHelper.browsePath(entry.getEntry().getDirectory()));
            contextMenu.getItems().add(browse);
        }

        var refresh = new MenuItem(AppI18n.get("refresh"), new FontIcon("mdal-360"));
        refresh.disableProperty().bind(entry.getRefreshable().not());
        refresh.setOnAction(event -> {
            DataStorage.get().refreshAsync(entry.getEntry(), true);
        });
        contextMenu.getItems().add(refresh);

        var del = new MenuItem(AppI18n.get("delete"), new FontIcon("mdal-delete_outline"));
        del.disableProperty().bind(entry.getDeletable().not());
        del.setOnAction(event -> entry.delete());
        contextMenu.getItems().add(del);

        return contextMenu;
    }

    private ColumnConstraints createShareConstraint(Region r, double share) {
        var cc = new ColumnConstraints();
        cc.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> r.getWidth() * share, r.widthProperty()));
        return cc;
    }

    @SneakyThrows
    @Override
    protected Region createSimple() {
        var loading = new LoadingOverlayComp(Comp.of(() -> createContent()), entry.getLoading());
        var region = loading.createRegion();
        return region;
    }
}
