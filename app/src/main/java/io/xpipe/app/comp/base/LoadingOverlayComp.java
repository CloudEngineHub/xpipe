package io.xpipe.app.comp.base;

import atlantafx.base.controls.RingProgressIndicator;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.StackPane;

public class LoadingOverlayComp extends Comp<CompStructure<StackPane>> {

    private final Comp<?> comp;
    private final ObservableValue<Boolean> showLoading;

    public LoadingOverlayComp(Comp<?> comp, ObservableValue<Boolean> loading) {
        this.comp = comp;
        this.showLoading = PlatformThread.sync(loading);
    }

    @Override
    public CompStructure<StackPane> createBase() {
        var compStruc = comp.createStructure();

        var loading = new RingProgressIndicator(0, false);
        loading.setProgress(-1);

        var loadingBg = new StackPane(loading);
        loadingBg.getStyleClass().add("loading-comp");

        loadingBg.setVisible(showLoading.getValue());

        var listener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean busy) {
                if (!busy) {
                    // Reduce flickering for consecutive loads
                    Thread t = new Thread(() -> {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }

                        if (!showLoading.getValue()) {
                            Platform.runLater(() -> loadingBg.setVisible(false));
                        }
                    });
                    t.setDaemon(true);
                    t.setName("loading delay");
                    t.start();
                } else {
                    ThreadHelper.runAsync(() -> {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }

                        if (showLoading.getValue()) {
                            Platform.runLater(() -> loadingBg.setVisible(true));
                        }
                    });
                }
            }
        };
        PlatformThread.sync(showLoading).addListener(listener);

        var r = compStruc.get();
        var stack = new StackPane(r, loadingBg);

        loading.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
            return Math.min(r.getHeight() - 20, 50);
        }, r.heightProperty()));
        loading.prefHeightProperty().bind(loading.prefWidthProperty());

        return new SimpleCompStructure<>(stack);
    }
}
