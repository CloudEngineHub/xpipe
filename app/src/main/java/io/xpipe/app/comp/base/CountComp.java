package io.xpipe.app.comp.base;

import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;

public class CountComp<T> extends Comp<CompStructure<Label>> {

    private final ObservableList<T> sub;
    private final ObservableList<T> all;

    public CountComp(ObservableList<T> sub, ObservableList<T> all) {
        this.sub = PlatformThread.sync(sub);
        this.all = PlatformThread.sync(all);
    }

    @Override
    public CompStructure<Label> createBase() {
        var label = new Label();
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setAlignment(Pos.CENTER);
        label.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> {
                            if (sub.size() == all.size()) {
                                return all.size() + "";
                            } else {
                                return "" + sub.size() + "/" + all.size();
                            }
                        },
                        sub,
                        all));
        label.getStyleClass().add("count-comp");
        return new SimpleCompStructure<>(label);
    }
}
