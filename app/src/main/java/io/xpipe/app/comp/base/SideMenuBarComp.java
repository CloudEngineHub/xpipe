package io.xpipe.app.comp.base;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.Augment;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.fxcomps.impl.TooltipAugment;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.update.UpdateAvailableAlert;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.Hyperlinks;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

public class SideMenuBarComp extends Comp<CompStructure<VBox>> {

    private final Property<AppLayoutModel.Entry> value;
    private final List<AppLayoutModel.Entry> entries;

    public SideMenuBarComp(Property<AppLayoutModel.Entry> value, List<AppLayoutModel.Entry> entries) {
        this.value = value;
        this.entries = entries;
    }

    @Override
    public CompStructure<VBox> createBase() {
        var vbox = new VBox();
        vbox.setFillWidth(true);

        var selectedBorder = Bindings.createObjectBinding(
                () -> {
                    var c = Platform.getPreferences().getAccentColor().desaturate();
                    return new Background(new BackgroundFill(c,new CornerRadii(8), new Insets(5, 1, 5, 2)));
                },
                Platform.getPreferences().accentColorProperty());

        var hoverBorder = Bindings.createObjectBinding(
                () -> {
                    var c = Platform.getPreferences().getAccentColor().darker().desaturate();
                    return new Background(new BackgroundFill(c,new CornerRadii(8), new Insets(5, 1, 5, 2)));
                },
                Platform.getPreferences().accentColorProperty());

        var noneBorder = Bindings.createObjectBinding(
                () -> {
                    return Background.fill(Color.TRANSPARENT);
                },
                Platform.getPreferences().accentColorProperty());

        var selected = PseudoClass.getPseudoClass("selected");
        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            var b = new IconButtonComp(e.icon(), () -> value.setValue(e));
            var shortcut = e.combination();
            if (shortcut != null) {
                b.apply(struc -> struc.get().getProperties().put("shortcut", shortcut));
            }
            b.apply(new TooltipAugment<>(e.name(), shortcut));
            b.apply(struc -> {
                AppFont.setSize(struc.get(), 2);
                struc.get().pseudoClassStateChanged(selected, value.getValue().equals(e));
                value.addListener((c, o, n) -> {
                    PlatformThread.runLaterIfNeeded(() -> {
                        struc.get().pseudoClassStateChanged(selected, n.equals(e));
                    });
                });
            });
            b.accessibleText(e.name());

            var indicator = Comp.empty().styleClass("indicator");
            var stack = new StackComp(List.of(indicator, b)).apply(struc -> struc.get().setAlignment(Pos.CENTER_RIGHT));
            stack.apply(struc -> {
                var indicatorRegion = (Region) struc.get().getChildren().get(0);
                indicatorRegion.setMaxWidth(7);
                indicatorRegion
                        .backgroundProperty()
                        .bind(Bindings.createObjectBinding(
                                () -> {
                                    if (value.getValue().equals(e)) {
                                        return selectedBorder.get();
                                    }

                                    if (struc.get().isHover()) {
                                        return hoverBorder.get();
                                    }

                                    return noneBorder.get();
                                },
                                struc.get().hoverProperty(),
                                value,
                                hoverBorder,
                                selectedBorder,
                                noneBorder));
            });
            vbox.getChildren().add(stack.createRegion());
        }

        Augment<CompStructure<Button>> simpleBorders = struc -> {
            struc.get()
                    .backgroundProperty()
                    .bind(Bindings.createObjectBinding(
                            () -> {
                                if (struc.get().isHover()) {
                                    return hoverBorder.get();
                                }

                                return noneBorder.get();
                            },
                            struc.get().hoverProperty(),
                            value,
                            hoverBorder,
                            selectedBorder,
                            noneBorder));
        };

        {
            var b = new IconButtonComp("mdi2g-github", () -> Hyperlinks.open(Hyperlinks.GITHUB))
                    .tooltipKey("visitGithubRepository")
                    .apply(simpleBorders)
                    .accessibleTextKey("visitGithubRepository");
            b.apply(struc -> {
                AppFont.setSize(struc.get(), 2);
            });
            vbox.getChildren().add(b.createRegion());
        }

        {
            var b = new IconButtonComp("mdi2d-discord", () -> Hyperlinks.open(Hyperlinks.DISCORD))
                    .tooltipKey("discord")
                    .apply(simpleBorders)
                    .accessibleTextKey("discord");
            b.apply(struc -> {
                AppFont.setSize(struc.get(), 2);
            });
            vbox.getChildren().add(b.createRegion());
        }

        //        {
        //            var b = new IconButtonComp("mdi2t-translate", () -> Hyperlinks.open(Hyperlinks.TRANSLATE))
        //                    .tooltipKey("translate")
        //                    .apply(simpleBorders)
        //                    .accessibleTextKey("translate");
        //            b.apply(struc -> {
        //                AppFont.setSize(struc.get(), 2);
        //            });
        //            vbox.getChildren().add(b.createRegion());
        //        }

        {
            var b = new IconButtonComp(
                            "mdi2c-code-json",
                            () -> Hyperlinks.open(
                                    "http://localhost:" + AppBeaconServer.get().getPort()))
                    .tooltipKey("api")
                    .apply(simpleBorders)
                    .accessibleTextKey("api");
            b.apply(struc -> {
                AppFont.setSize(struc.get(), 2);
            });
            vbox.getChildren().add(b.createRegion());
        }

        {
            var b = new IconButtonComp("mdi2u-update", () -> UpdateAvailableAlert.showIfNeeded())
                    .tooltipKey("updateAvailableTooltip")
                    .accessibleTextKey("updateAvailableTooltip");
            b.apply(struc -> {
                AppFont.setSize(struc.get(), 2);
            });
            b.hide(PlatformThread.sync(Bindings.createBooleanBinding(
                    () -> {
                        return XPipeDistributionType.get()
                                        .getUpdateHandler()
                                        .getPreparedUpdate()
                                        .getValue()
                                == null;
                    },
                    XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate())));
            vbox.getChildren().add(b.createRegion());
        }

        var filler = new Button();
        filler.setDisable(true);
        filler.setMaxHeight(3000);
        vbox.getChildren().add(filler);
        VBox.setVgrow(filler, Priority.ALWAYS);
        filler.prefWidthProperty().bind(((Region) vbox.getChildren().getFirst()).widthProperty());
        vbox.getStyleClass().add("sidebar-comp");
        return new SimpleCompStructure<>(vbox);
    }
}
