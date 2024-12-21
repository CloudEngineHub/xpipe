package io.xpipe.app.comp.base;

import atlantafx.base.controls.ModalPaneSkin;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.NodeHelper;
import io.xpipe.app.util.PlatformThread;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;

public class ModalOverlayComp extends SimpleComp {

    private final Comp<?> background;
    private final Property<ModalOverlay> overlayContent;

    public ModalOverlayComp(Comp<?> background, Property<ModalOverlay> overlayContent) {
        this.background = background;
        this.overlayContent = overlayContent;
    }

    @Override
    protected Region createSimple() {
        var bgRegion = background.createRegion();
        var modal = new ModalPane();
        AppFont.small(modal);
        modal.focusedProperty().addListener((observable, oldValue, newValue) -> {
            var c = modal.getContent();
            if (newValue && c != null) {
                c.requestFocus();
            }
        });
        modal.getStyleClass().add("modal-overlay-comp");
        var pane = new StackPane(bgRegion, modal);
        pane.setPickOnBounds(false);
        pane.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                if (modal.isDisplay()) {
                    modal.requestFocus();
                } else {
                    bgRegion.requestFocus();
                }
            }
        });

        modal.contentProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                overlayContent.setValue(null);
                bgRegion.setDisable(false);
            }

            if (newValue != null) {
                bgRegion.setDisable(true);
            }
        });

        modal.displayProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                modal.hide(true);
            }
        });

        overlayContent.addListener((observable, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (oldValue != null && newValue == null && modal.isDisplay()) {
                    modal.hide(true);
                    return;
                }

                if (newValue != null) {
                    var modalBox = toBox(newValue);
                    modal.show(modalBox);
                    modal.setPersistent(newValue.isPersistent());
                    if (newValue.isPersistent()) {
                        var closeButton = modalBox.lookup(".close-button");
                        if (closeButton != null) {
                            closeButton.setVisible(false);
                        }
                    }

                    //                if (newValue.isFinishOnEnter()) {
                    //                    modalBox.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    //                        if (event.getCode() == KeyCode.ENTER) {
                    //                            newValue.getOnFinish().run();
                    //                            overlayContent.setValue(null);
                    //                            event.consume();
                    //                        }
                    //                    });
                    //                }

                    // Wait 2 pulses before focus so that the scene can be assigned to r
                    Platform.runLater(() -> {
                        Platform.runLater(() -> {
                            modalBox.requestFocus();
                        });
                    });
                }
            });
        });

        var current = overlayContent.getValue();
        if (current != null) {
            var modalBox = toBox(current);
            modal.setPersistent(current.isPersistent());
            modal.show(modalBox);
            if (current.isPersistent()) {
                var closeButton = modalBox.lookup(".close-button");
                if (closeButton != null) {
                    closeButton.setVisible(false);
                }
            }
            modalBox.requestFocus();
        }

        return pane;
    }

    private ModalBox toBox(ModalOverlay newValue) {
        var l = new Label(
                AppI18n.get(newValue.getTitleKey()),
                newValue.getGraphic() != null ? newValue.getGraphic().createRegion() : null);
        l.setGraphicTextGap(6);
        AppFont.normal(l);
        var r = newValue.getContent().createRegion();
        var box = new VBox(l, r);
        box.focusedProperty().addListener((o, old, n) -> {
            if (n) {
                r.requestFocus();
            }
        });
        box.setSpacing(10);
        box.setPadding(new Insets(10, 15, 15, 15));

        var buttonBar = new ButtonBar();
        for (var mb : newValue.getButtons()) {
            buttonBar.getButtons().add(toButton(mb));
        }
        box.getChildren().add(buttonBar);

        var modalBox = new ModalBox(box);
        modalBox.setOnClose(event -> {
            overlayContent.setValue(null);
            event.consume();
        });
        modalBox.setMinWidth(100);
        modalBox.setMinHeight(100);
        modalBox.prefWidthProperty().bind(box.widthProperty());
        modalBox.prefHeightProperty().bind(box.heightProperty());
        modalBox.maxWidthProperty().bind(box.widthProperty());
        modalBox.maxHeightProperty().bind(box.heightProperty());
        modalBox.focusedProperty().addListener((o, old, n) -> {
            if (n) {
                box.requestFocus();
            }
        });
        return modalBox;
    }

    private Button toButton(ModalButton mb) {
        var button = new Button(AppI18n.get(mb.getKey()));
        if (mb.isDefaultButton()) {
            button.getStyleClass().add(Styles.ACCENT);
        }
        if (mb.getAugment() != null) {
            mb.getAugment().accept(button);
        }
        button.setOnAction(event -> {
            if (mb.getAction() != null) {
                mb.getAction().run();
            }
            if (mb.isClose()) {
                overlayContent.setValue(null);
            }
            event.consume();
        });
        return button;
    }
}
