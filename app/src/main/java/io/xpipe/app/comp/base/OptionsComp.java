package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class OptionsComp extends Comp<CompStructure<Pane>> {

    private final List<OptionsComp.Entry> entries;

    public OptionsComp(List<OptionsComp.Entry> entries) {
        this.entries = entries;
    }

    @Override
    public CompStructure<Pane> createBase() {
        Pane pane;
        var content = new VBox();
        content.setSpacing(7);
        pane = content;
        pane.getStyleClass().add("options-comp");

        var nameRegions = new ArrayList<Region>();

        Region firstComp = null;
        for (var entry : getEntries()) {
            Region compRegion = null;
            if (entry.comp() != null) {
                compRegion = entry.comp().createRegion();
            }
            if (firstComp == null) {
                compRegion.getStyleClass().add("first");
                firstComp = compRegion;
            }

            var showVertical = (entry.name() != null && (entry.description() != null || entry.comp() instanceof SimpleTitledPaneComp));
            if (showVertical) {
                var line = new VBox();
                line.prefWidthProperty().bind(pane.widthProperty());
                line.setSpacing(5);

                var name = new Label();
                name.getStyleClass().add("name");
                name.textProperty().bind(entry.name());
                name.setMinWidth(Region.USE_PREF_SIZE);
                name.setMinHeight(Region.USE_PREF_SIZE);
                name.setAlignment(Pos.CENTER_LEFT);
                if (compRegion != null) {
                    name.visibleProperty().bind(PlatformThread.sync(compRegion.visibleProperty()));
                    name.managedProperty().bind(PlatformThread.sync(compRegion.managedProperty()));
                }
                line.getChildren().add(name);

                if (entry.description() != null) {
                    var description = new Label();
                    description.setWrapText(true);
                    description.getStyleClass().add("description");
                    description.textProperty().bind(entry.description());
                    description.setAlignment(Pos.CENTER_LEFT);
                    description.setMinHeight(Region.USE_PREF_SIZE);
                    if (compRegion != null) {
                        description.visibleProperty().bind(PlatformThread.sync(compRegion.visibleProperty()));
                        description.managedProperty().bind(PlatformThread.sync(compRegion.managedProperty()));
                    }

                    if (entry.longDescriptionSource() != null) {
                        var markDown = new MarkdownComp(entry.longDescriptionSource(), s -> s, true).apply(struc -> struc.get().setMaxWidth(500))
                                .apply(struc -> struc.get().setMaxHeight(400));
                        var popover = new Popover(markDown.createRegion());
                        popover.setCloseButtonEnabled(false);
                        popover.setHeaderAlwaysVisible(false);
                        popover.setDetachable(true);
                        AppFont.small(popover.getContentNode());

                        var extendedDescription = new Button("... ?");
                        extendedDescription.setMinWidth(Region.USE_PREF_SIZE);
                        extendedDescription.getStyleClass().add(Styles.BUTTON_OUTLINED);
                        extendedDescription.getStyleClass().add(Styles.ACCENT);
                        extendedDescription.getStyleClass().add("long-description");
                        extendedDescription.setAccessibleText("Help");
                        AppFont.normal(extendedDescription);
                        extendedDescription.setOnAction(e -> {
                            popover.show(extendedDescription);
                            e.consume();
                        });

                        var descriptionBox = new HBox(description, new Spacer(Orientation.HORIZONTAL), extendedDescription);
                        descriptionBox.setSpacing(5);
                        HBox.setHgrow(descriptionBox, Priority.ALWAYS);
                        descriptionBox.setAlignment(Pos.CENTER_LEFT);
                        line.getChildren().add(descriptionBox);

                        if (compRegion != null) {
                            descriptionBox.visibleProperty().bind(PlatformThread.sync(compRegion.visibleProperty()));
                            descriptionBox.managedProperty().bind(PlatformThread.sync(compRegion.managedProperty()));
                        }
                    } else {
                        line.getChildren().add(description);
                    }
                }

                if (compRegion != null) {
                    compRegion.accessibleTextProperty().bind(name.textProperty());
                    if (entry.description() != null) {
                        compRegion.accessibleHelpProperty().bind(PlatformThread.sync(entry.description()));
                    }
                    line.getChildren().add(compRegion);
                }

                pane.getChildren().add(line);
            } else if (entry.name() != null) {
                var line = new HBox();
                line.setFillHeight(true);
                line.prefWidthProperty().bind(pane.widthProperty());
                line.setSpacing(8);

                var name = new Label();
                name.textProperty().bind(entry.name());
                name.prefHeightProperty().bind(line.heightProperty());
                name.setMinWidth(Region.USE_PREF_SIZE);
                name.setAlignment(Pos.CENTER_LEFT);
                if (compRegion != null) {
                    name.visibleProperty().bind(PlatformThread.sync(compRegion.visibleProperty()));
                    name.managedProperty().bind(PlatformThread.sync(compRegion.managedProperty()));
                }
                nameRegions.add(name);
                line.getChildren().add(name);

                if (compRegion != null) {
                    compRegion.accessibleTextProperty().bind(name.textProperty());
                    line.getChildren().add(compRegion);
                    HBox.setHgrow(compRegion, Priority.ALWAYS);
                }

                pane.getChildren().add(line);
            } else {
                if (compRegion != null) {
                    pane.getChildren().add(compRegion);
                }
            }
        }

        if (entries.size() == 1 && firstComp != null) {
            pane.visibleProperty().bind(PlatformThread.sync(firstComp.visibleProperty()));
            pane.managedProperty().bind(PlatformThread.sync(firstComp.managedProperty()));
        }

        if (entries.stream().anyMatch(entry -> entry.name() != null && entry.description() == null)) {
            var nameWidthBinding = Bindings.createDoubleBinding(
                    () -> {
                        return nameRegions.stream()
                                .map(Region::getWidth)
                                .filter(aDouble -> aDouble > 0.0)
                                .max(Double::compareTo)
                                .orElse(Region.USE_COMPUTED_SIZE);
                    },
                    nameRegions.stream().map(Region::widthProperty).toList().toArray(new Observable[0]));
            nameRegions.forEach(r -> r.minWidthProperty().bind(nameWidthBinding));
        }

        Region finalFirstComp = firstComp;
        pane.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (finalFirstComp != null && newValue) {
                finalFirstComp.requestFocus();
            }
        });

        return new SimpleCompStructure<>(pane);
    }

    public record Entry(
            String key,
            ObservableValue<String> description,
            String longDescriptionSource,
            ObservableValue<String> name,
            Comp<?> comp) {}
}
