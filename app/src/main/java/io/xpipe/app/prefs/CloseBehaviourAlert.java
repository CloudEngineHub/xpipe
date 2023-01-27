package io.xpipe.app.prefs;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.extension.I18n;
import io.xpipe.extension.prefs.PrefsChoiceValue;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

public class CloseBehaviourAlert {

    public static boolean showIfNeeded() {
        boolean set = AppCache.get("closeBehaviourSet", Boolean.class, () -> false);
        if (set) {
            return true;
        }

        Property<CloseBehaviour> prop =
                new SimpleObjectProperty<>(AppPrefs.get().closeBehaviour().getValue());
        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(I18n.get("closeBehaviourAlertTitle"));
                    alert.setHeaderText(I18n.get("closeBehaviourAlertTitleHeader"));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);

                    ToggleGroup group = new ToggleGroup();
                    var vb = new VBox();
                    vb.setSpacing(7);
                    for (var cb : PrefsChoiceValue.getSupported(CloseBehaviour.class)) {
                        RadioButton rb = new RadioButton(cb.toTranslatedString());
                        rb.setToggleGroup(group);
                        rb.selectedProperty().addListener((c, o, n) -> {
                            if (n) {
                                prop.setValue(cb);
                            }
                        });
                        if (prop.getValue().equals(cb)) {
                            rb.setSelected(true);
                        }
                        vb.getChildren().add(rb);
                    }
                    alert.getDialogPane().setContent(vb);
                })
                .filter(b -> b.getButtonData().isDefaultButton())
                .map(t -> {
                    AppCache.update("closeBehaviourSet", true);
                    AppPrefs.get().setFromExternal(AppPrefs.get().closeBehaviour(), prop.getValue());
                    return true;
                })
                .orElse(false);
    }
}
