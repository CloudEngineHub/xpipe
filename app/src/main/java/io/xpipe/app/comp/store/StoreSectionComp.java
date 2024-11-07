package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class StoreSectionComp extends Comp<CompStructure<VBox>> {

    public static final PseudoClass EXPANDED = PseudoClass.getPseudoClass("expanded");
    private static final PseudoClass ROOT = PseudoClass.getPseudoClass("root");
    private static final PseudoClass SUB = PseudoClass.getPseudoClass("sub");
    private static final PseudoClass ODD = PseudoClass.getPseudoClass("odd-depth");
    private static final PseudoClass EVEN = PseudoClass.getPseudoClass("even-depth");
    private final StoreSection section;
    private final boolean topLevel;

    public StoreSectionComp(StoreSection section, boolean topLevel) {
        this.section = section;
        this.topLevel = topLevel;
    }

    private Comp<CompStructure<Button>> createQuickAccessButton() {
        var quickAccessDisabled = Bindings.createBooleanBinding(
                () -> {
                    return section.getShownChildren().getList().isEmpty();
                },
                section.getShownChildren().getList());
        Consumer<StoreSection> quickAccessAction = w -> {
            ThreadHelper.runFailableAsync(() -> {
                w.getWrapper().executeDefaultAction();
            });
        };
        var quickAccessButton = new StoreQuickAccessButtonComp(section, quickAccessAction)
                .vgrow()
                .styleClass("quick-access-button")
                .apply(struc -> struc.get().setMinWidth(30))
                .apply(struc -> struc.get().setPrefWidth(30))
                .maxHeight(100)
                .accessibleText(Bindings.createStringBinding(
                        () -> {
                            return "Access " + section.getWrapper().getName().getValue();
                        },
                        section.getWrapper().getName()))
                .disable(quickAccessDisabled)
                .focusTraversableForAccessibility()
                .tooltipKey("accessSubConnections", new KeyCodeCombination(KeyCode.RIGHT));
        return quickAccessButton;
    }

    private Comp<CompStructure<Button>> createExpandButton() {
        var expandButton = new IconButtonComp(
                Bindings.createObjectBinding(
                        () -> new LabelGraphic.IconGraphic(
                                section.getWrapper().getExpanded().get()
                                                && section.getShownChildren()
                                                                .getList()
                                                                .size()
                                                        > 0
                                        ? "mdal-keyboard_arrow_down"
                                        : "mdal-keyboard_arrow_right"),
                        section.getWrapper().getExpanded(),
                        section.getShownChildren().getList()),
                () -> {
                    section.getWrapper().toggleExpanded();
                });
        expandButton
                .apply(struc -> struc.get().setMinWidth(30))
                .apply(struc -> struc.get().setPrefWidth(30))
                .focusTraversableForAccessibility()
                .tooltipKey("expand", new KeyCodeCombination(KeyCode.SPACE))
                .accessibleText(Bindings.createStringBinding(
                        () -> {
                            return "Expand " + section.getWrapper().getName().getValue();
                        },
                        section.getWrapper().getName()))
                .disable(Bindings.size(section.getShownChildren().getList()).isEqualTo(0))
                .styleClass("expand-button")
                .maxHeight(100)
                .vgrow();
        return expandButton;
    }

    @Override
    public CompStructure<VBox> createBase() {
        var entryButton = StoreEntryComp.customSection(section, topLevel);
        var quickAccessButton = createQuickAccessButton();
        var expandButton = createExpandButton();
        var buttonList = new ArrayList<Comp<?>>();
        if (entryButton.isFullSize()) {
            buttonList.add(quickAccessButton);
        }
        buttonList.add(expandButton);
        var buttons = new VerticalComp(buttonList);
        var topEntryList = new HorizontalComp(List.of(buttons, entryButton.hgrow()));
        topEntryList.apply(struc -> {
            var mainButton = struc.get().getChildren().get(1);
            mainButton.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.SPACE) {
                    section.getWrapper().toggleExpanded();
                    event.consume();
                }
                if (event.getCode() == KeyCode.RIGHT) {
                    var ref = (VBox) struc.get().getChildren().getFirst();
                    if (entryButton.isFullSize()) {
                        var btn = (Button) ref.getChildren().getFirst();
                        btn.fire();
                    }
                    event.consume();
                }
            });
        });

        // Optimization for large sections. If there are more than 20 children, only add the nodes to the scene if the
        // section is actually expanded
        var listSections = section.getShownChildren()
                .filtered(
                        storeSection -> section.getAllChildren().getList().size() <= 20
                                || section.getWrapper().getExpanded().get(),
                        section.getWrapper().getExpanded(),
                        section.getAllChildren().getList());
        var content = new ListBoxViewComp<>(
                        listSections.getList(),
                        section.getAllChildren().getList(),
                        (StoreSection e) -> {
                            return StoreSection.customSection(e, false).apply(GrowAugment.create(true, false));
                        },
                        false)
                .minHeight(0)
                .hgrow();

        var expanded = Bindings.createBooleanBinding(
                () -> {
                    return section.getWrapper().getExpanded().get()
                            && section.getShownChildren().getList().size() > 0;
                },
                section.getWrapper().getExpanded(),
                section.getShownChildren().getList());
        var full = new VerticalComp(List.of(
                topEntryList,
                Comp.separator().hide(expanded.not()),
                content.styleClass("children-content")
                        .hide(Bindings.or(
                                Bindings.not(section.getWrapper().getExpanded()),
                                Bindings.size(section.getShownChildren().getList())
                                        .isEqualTo(0)))));
        return full.styleClass("store-entry-section-comp")
                .apply(struc -> {
                    struc.get().setFillWidth(true);
                    expanded.subscribe(val -> {
                        struc.get().pseudoClassStateChanged(EXPANDED, val);
                    });
                    struc.get().pseudoClassStateChanged(EVEN, section.getDepth() % 2 == 0);
                    struc.get().pseudoClassStateChanged(ODD, section.getDepth() % 2 != 0);
                    struc.get().pseudoClassStateChanged(ROOT, topLevel);
                    struc.get().pseudoClassStateChanged(SUB, !topLevel);

                    section.getWrapper().getColor().subscribe(val -> {
                        if (!topLevel) {
                            return;
                        }

                        var newList = new ArrayList<>(struc.get().getStyleClass());
                        newList.removeIf(s -> Arrays.stream(DataColor.values())
                                .anyMatch(
                                        dataStoreColor -> dataStoreColor.getId().equals(s)));
                        newList.remove("gray");
                        newList.add("color-box");
                        if (val != null) {
                            newList.add(val.getId());
                        } else {
                            newList.add("gray");
                        }
                        struc.get().getStyleClass().setAll(newList);
                    });
                })
                .createStructure();
    }
}
