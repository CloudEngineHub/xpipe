package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.PlatformThread;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import lombok.Value;

public class ModalOverlayComp extends SimpleComp {

    private final Comp<?> background;
    private final Property<OverlayContent> overlayContent;

    public ModalOverlayComp(Comp<?> background, Property<OverlayContent> overlayContent) {
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

        PlatformThread.sync(overlayContent).addListener((observable, oldValue, newValue) -> {
            if (oldValue != null && newValue == null && modal.isDisplay()) {
                modal.hide(true);
                return;
            }

            if (newValue != null) {
                var l = new Label(
                        AppI18n.get(newValue.titleKey),
                        newValue.graphic != null ? newValue.graphic.createRegion() : null);
                l.setGraphicTextGap(6);
                AppFont.normal(l);
                var r = newValue.content.createRegion();
                var box = new VBox(l, r);
                box.focusedProperty().addListener((o, old, n) -> {
                    if (n) {
                        r.requestFocus();
                    }
                });
                box.setSpacing(10);
                box.setPadding(new Insets(10, 15, 15, 15));

                if (newValue.finishKey != null) {
                    var finishButton = new Button(AppI18n.get(newValue.finishKey));
                    finishButton.getStyleClass().add(Styles.ACCENT);
                    finishButton.setOnAction(event -> {
                        newValue.onFinish.run();
                        overlayContent.setValue(null);
                        event.consume();
                    });

                    var buttonBar = new ButtonBar();
                    buttonBar.getButtons().addAll(finishButton);
                    box.getChildren().add(buttonBar);
                }

                var modalBox = new ModalBox(box);
                modalBox.setOnClose(event -> {
                    overlayContent.setValue(null);
                    modal.hide(true);
                    event.consume();
                });
                modalBox.prefWidthProperty().bind(box.widthProperty());
                modalBox.prefHeightProperty().bind(box.heightProperty());
                modalBox.maxWidthProperty().bind(box.widthProperty());
                modalBox.maxHeightProperty().bind(box.heightProperty());
                modalBox.focusedProperty().addListener((o, old, n) -> {
                    if (n) {
                        box.requestFocus();
                    }
                });
                modal.show(modalBox);

                if (newValue.finishOnEnter) {
                    modalBox.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                        if (event.getCode() == KeyCode.ENTER) {
                            newValue.onFinish.run();
                            overlayContent.setValue(null);
                            event.consume();
                        }
                    });
                }

                // Wait 2 pulses before focus so that the scene can be assigned to r
                Platform.runLater(() -> {
                    Platform.runLater(() -> {
                        modalBox.requestFocus();
                    });
                });
            }
        });
        return pane;
    }

    @Value
    public static class OverlayContent {

        String titleKey;
        Comp<?> content;
        Comp<?> graphic;
        String finishKey;
        Runnable onFinish;
        boolean finishOnEnter;
    }
}
