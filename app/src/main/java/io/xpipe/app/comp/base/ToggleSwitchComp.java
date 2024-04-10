package io.xpipe.app.comp.base;

import atlantafx.base.controls.ToggleSwitch;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;

public class ToggleSwitchComp extends SimpleComp {

    private final Property<Boolean> selected;
    private final ObservableValue<String> name;

    public ToggleSwitchComp(Property<Boolean> selected, ObservableValue<String> name) {
        this.selected = selected;
        this.name = name;
    }

    @Override
    protected Region createSimple() {
        var s = new ToggleSwitch();
        s.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER) {
                s.setSelected(!s.isSelected());
                event.consume();
            }
        });
        s.getStyleClass().add("toggle-switch-comp");
        s.setSelected(selected.getValue());
        s.selectedProperty().addListener((observable, oldValue, newValue) -> {
            selected.setValue(newValue);
        });
        selected.addListener((observable, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                s.setSelected(newValue);
            });
        });
        if (name != null) {
            s.textProperty().bind(PlatformThread.sync(name));
        }
        return s;
    }
}
