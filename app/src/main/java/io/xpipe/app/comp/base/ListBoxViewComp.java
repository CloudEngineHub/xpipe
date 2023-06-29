package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ListBoxViewComp<T> extends Comp<CompStructure<ScrollPane>> {

    private static final PseudoClass ODD = PseudoClass.getPseudoClass("odd");
    private static final PseudoClass EVEN = PseudoClass.getPseudoClass("even");

    private final ObservableList<T> shown;
    private final ObservableList<T> all;
    private final Function<T, Comp<?>> compFunction;

    public ListBoxViewComp(ObservableList<T> shown, ObservableList<T> all, Function<T, Comp<?>> compFunction) {
        this.shown = PlatformThread.sync(shown);
        this.all = PlatformThread.sync(all);
        this.compFunction = compFunction;
    }

    @Override
    public CompStructure<ScrollPane> createBase() {
        Map<T, Region> cache = new HashMap<>();

        VBox listView = new VBox();
        listView.setFocusTraversable(false);

        refresh(listView, shown, cache, false);
        listView.requestLayout();

        shown.addListener((ListChangeListener<? super T>) (c) -> {
            refresh(listView, c.getList(), cache, true);
        });

        all.addListener((ListChangeListener<? super T>) c -> {
            cache.keySet().retainAll(c.getList());
        });

        var scroll = new ScrollPane(listView);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToWidth(true);

        return new SimpleCompStructure<>(scroll);
    }

    private void refresh(VBox listView, List<? extends T> c, Map<T, Region> cache, boolean asynchronous) {
        Runnable update = () -> {
            var newShown = c.stream()
                    .map(v -> {
                        if (!cache.containsKey(v)) {
                            cache.put(v, compFunction.apply(v).createRegion());
                        }

                        return cache.get(v);
                    })
                    .toList();

            for (int i = 0; i < newShown.size(); i++) {
                var r = newShown.get(i);
                r.pseudoClassStateChanged(ODD, false);
                r.pseudoClassStateChanged(EVEN, false);
                r.pseudoClassStateChanged(i % 2 == 0 ? EVEN : ODD, true);
            }

            if (!listView.getChildren().equals(newShown)) {
                listView.getChildren().setAll(newShown);
                listView.layout();
            }
        };

        if (asynchronous) {
            Platform.runLater(update);
        } else {
            PlatformThread.runLaterIfNeeded(update);
        }
    }
}
