package io.xpipe.app.browser;

import io.xpipe.app.comp.storage.store.StoreEntryTree;
import io.xpipe.app.comp.storage.store.StoreEntryWrapper;
import io.xpipe.app.comp.storage.store.StoreViewState;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.BusyProperty;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedHierarchyStore;
import io.xpipe.core.store.ShellStore;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Point2D;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import java.util.Timer;
import java.util.TimerTask;

final class BrowserBookmarkList extends SimpleComp {

    public static final Timer DROP_TIMER = new Timer("dnd", true);
    private Point2D lastOver = new Point2D(-1, -1);
    private TimerTask activeTask;

    private final BrowserModel model;

    BrowserBookmarkList(BrowserModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var root = StoreEntryTree.createTree();
        var view = new TreeView<>(root);
        view.setShowRoot(false);
        view.getStyleClass().add("bookmark-list");
        view.setCellFactory(param -> {
            return new StoreCell(view);
        });

        PlatformThread.sync(model.getSelected()).addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                view.getSelectionModel().clearSelection();
                return;
            }

            view.getSelectionModel()
                    .select(getTreeViewItem(
                            root,
                            StoreViewState.get().getAllEntries().stream()
                                    .filter(storeEntryWrapper -> storeEntryWrapper
                                                    .getState()
                                                    .getValue()
                                                    .isUsable()
                                            && storeEntryWrapper
                                                    .getEntry()
                                                    .getStore()
                                                    .equals(newValue.getStore()))
                                    .findAny()
                                    .orElse(null)));
        });

        return view;
    }

    private static TreeItem<StoreEntryWrapper> getTreeViewItem(
            TreeItem<StoreEntryWrapper> item, StoreEntryWrapper value) {
        if (item.getValue() != null && item.getValue().equals(value)) {
            return item;
        }

        for (TreeItem<StoreEntryWrapper> child : item.getChildren()) {
            TreeItem<StoreEntryWrapper> s = getTreeViewItem(child, value);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    private final class StoreCell extends TreeCell<StoreEntryWrapper> {

        private final StringProperty img = new SimpleStringProperty();
        private final Node imageView = new PrettyImageComp(img, 20, 20).createRegion();
        private final BooleanProperty busy = new SimpleBooleanProperty(false);

        @Override
        protected double computePrefWidth(double height) {
            // This makes the cell always properly cut of any overflow of text
            return 1;
        }

        private StoreCell(TreeView<?> t) {
            disableProperty().bind(busy);
            setAccessibleRole(AccessibleRole.BUTTON);
            setGraphic(imageView);
            setTextOverrun(OverrunStyle.ELLIPSIS);
            addEventHandler(DragEvent.DRAG_OVER, mouseEvent -> {
                if (getItem() == null) {
                    return;
                }

                handleHoverTimer(getItem().getEntry().getStore(), mouseEvent);
                mouseEvent.consume();
            });
            addEventHandler(DragEvent.DRAG_EXITED, mouseEvent -> {
                activeTask = null;
                mouseEvent.consume();
            });
            addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                if (getItem() == null
                        || event.getButton() != MouseButton.PRIMARY
                        || (!getItem().getState().getValue().isUsable())) {
                    return;
                }

                ThreadHelper.runFailableAsync(() -> {
                    if (getItem().getEntry().getStore() instanceof ShellStore fileSystem) {
                        BusyProperty.execute(busy, () -> {
                            getItem().refreshIfNeeded();
                        });
                        model.openFileSystemAsync(null, fileSystem, null, busy);
                    } else if (getItem().getEntry().getStore() instanceof FixedHierarchyStore) {
                        BusyProperty.execute(busy, () -> {
                            getItem().refreshWithChildren();
                        });
                    }
                });
                event.consume();
            });
            var icon = new SimpleObjectProperty<>("mdal-keyboard_arrow_right");
            getPseudoClassStates().addListener((SetChangeListener<? super PseudoClass>) change -> {
                if (change.getSet().contains(PseudoClass.getPseudoClass("expanded"))) {
                    icon.set("mdal-keyboard_arrow_down");
                } else {
                    icon.set("mdal-keyboard_arrow_right");
                }
            });
            var button = new IconButtonComp(icon, () -> {
                        getTreeItem().setExpanded(!getTreeItem().isExpanded());
                    })
                    .apply(struc -> struc.get().setPrefWidth(25))
                    .grow(false, true)
                    .styleClass("expand-button")
                    .apply(struc -> struc.get().setFocusTraversable(false));
            setDisclosureNode(button.createRegion());

            indexProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.intValue() == 0) {
                    getStyleClass().add("first");
                } else {
                    getStyleClass().remove("first");
                }
            });
        }

        @Override
        public void updateItem(StoreEntryWrapper item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);

                // Don't set image as that would trigger image comp update
                // and cells are emptied on each change, leading to unnecessary changes
                // img.set(null);

                // Use opacity instead of visibility as visibility is kinda bugged with web views
                setOpacity(0.0);

                setFocusTraversable(false);
                setAccessibleText(null);
            } else {
                setText(item.getName());

                // Check if store is in failed state
                if (item.getEntry().getState() == DataStoreEntry.State.LOAD_FAILED) {
                    setGraphic(null);
                    setFocusTraversable(false);
                    setAccessibleText(null);
                    return;
                }

                img.set(item.getEntry()
                        .getProvider()
                        .getDisplayIconFileName(item.getEntry().getStore()));
                setOpacity(1.0);
                setFocusTraversable(true);
                setAccessibleText(
                        item.getName() + " " + item.getEntry().getProvider().getDisplayName());
            }
        }
    }

    private void handleHoverTimer(DataStore store, DragEvent event) {
        if (lastOver.getX() == event.getX() && lastOver.getY() == event.getY()) {
            return;
        }

        lastOver = (new Point2D(event.getX(), event.getY()));
        activeTask = new TimerTask() {
            @Override
            public void run() {
                if (activeTask != this) {
                    return;
                }

                Platform.runLater(() -> model.openExistingFileSystemIfPresent(null, store.asNeeded()));
            }
        };
        DROP_TIMER.schedule(activeTask, 500);
    }
}
