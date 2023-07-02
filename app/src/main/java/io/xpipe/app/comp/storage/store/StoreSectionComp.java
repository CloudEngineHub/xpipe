package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class StoreSectionComp extends Comp<CompStructure<VBox>> {

    private static final PseudoClass ODD = PseudoClass.getPseudoClass("odd-depth");
    private static final PseudoClass EVEN = PseudoClass.getPseudoClass("even-depth");
    public static final PseudoClass EXPANDED = PseudoClass.getPseudoClass("expanded");

    private final StoreSection section;

    public StoreSectionComp(StoreSection section) {
        this.section = section;
    }

    @Override
    public CompStructure<VBox> createBase() {
        var root = StandardStoreEntryComp.customSection(section).apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS));
        var button = new IconButtonComp(
                        Bindings.createStringBinding(
                                () -> section.getWrapper().getExpanded().get()
                                                && section.getChildren().size() > 0
                                        ? "mdal-keyboard_arrow_down"
                                        : "mdal-keyboard_arrow_right",
                                section.getWrapper().getExpanded()),
                        () -> {
                            section.getWrapper().toggleExpanded();
                        })
                .apply(struc -> struc.get().setPrefWidth(30))
                .focusTraversable()
                .accessibleText("Expand")
                .disable(BindingsHelper.persist(
                        Bindings.size(section.getChildren()).isEqualTo(0)))
                .grow(false, true)
                .styleClass("expand-button");
        List<Comp<?>> topEntryList = List.of(button, root);

        var all = section.getChildren();
        var shown = BindingsHelper.filteredContentBinding(
                all,
                StoreViewState.get()
                        .getFilterString()
                        .map(s -> (storeEntrySection -> storeEntrySection.shouldShow(s))));
        var content = new ListBoxViewComp<>(shown, all, (StoreSection e) -> {
                    return StoreSection.customSection(e).apply(GrowAugment.create(true, false));
                }).hgrow();

        var expanded = Bindings.createBooleanBinding(() -> {
            return section.getWrapper().getExpanded().get() && section.getChildren().size() > 0;
        }, section.getWrapper().getExpanded(), section.getChildren());

        return new VerticalComp(List.of(
                        new HorizontalComp(topEntryList)
                                .apply(struc -> struc.get().setFillHeight(true)),
                        Comp.separator().visible(expanded),
                        new HorizontalComp(List.of(content))
                                .styleClass("content")
                                .apply(struc -> struc.get().setFillHeight(true))
                                .hide(BindingsHelper.persist(Bindings.or(
                                        Bindings.not(section.getWrapper().getExpanded()),
                                        Bindings.size(section.getChildren()).isEqualTo(0))))))
                .styleClass("store-entry-section-comp")
                .apply(struc -> {
                    struc.get().setFillWidth(true);
                    SimpleChangeListener.apply(expanded, val -> {
                        struc.get().pseudoClassStateChanged(EXPANDED, val);
                    });
                    struc.get().pseudoClassStateChanged(EVEN, section.getDepth() % 2 == 0);
                    struc.get().pseudoClassStateChanged(ODD, section.getDepth() % 2 != 0);
                })
                .createStructure();
    }
}
