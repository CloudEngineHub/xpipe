package io.xpipe.ext.proc;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.SingleSelectionField;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleTextControl;
import com.dlsc.preferencesfx.model.Setting;
import io.xpipe.app.prefs.TranslatableComboBoxControl;
import io.xpipe.extension.prefs.PrefsChoiceValue;
import io.xpipe.extension.prefs.PrefsHandler;
import io.xpipe.extension.prefs.PrefsProvider;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import java.util.List;

public class ProcPrefs extends PrefsProvider {

    private final BooleanProperty enableCaching = new SimpleBooleanProperty(true);

    public ObservableBooleanValue enableCaching() {
        return enableCaching;
    }

    private final ObjectProperty<TerminalType> terminalType = new SimpleObjectProperty<>(TerminalType.getDefault());
    private final SimpleListProperty<TerminalType> terminalTypeList = new SimpleListProperty<>(
            FXCollections.observableArrayList(PrefsChoiceValue.getSupported(TerminalType.class)));
    private final SingleSelectionField<TerminalType> terminalTypeControl =
            Field.ofSingleSelectionType(terminalTypeList, terminalType).render(() -> new TranslatableComboBoxControl<>());

    // Custom terminal
    // ===============
    private final StringProperty customTerminalCommand = new SimpleStringProperty("");
    private final StringField customTerminalCommandControl = editable(
            StringField.ofStringType(customTerminalCommand).render(() -> new SimpleTextControl()),
            terminalType.isEqualTo(TerminalType.CUSTOM));

    public ObservableValue<TerminalType> terminalType() {
        return terminalType;
    }

    public ObservableValue<String> customTerminalCommand() {
        return customTerminalCommand;
    }

    @Override
    public void addPrefs(PrefsHandler handler) {
        handler.addSetting(
                List.of("integrations"),
                "proc.terminal",
                Setting.of("app.defaultProgram", terminalTypeControl, terminalType),
                TerminalType.class);
        handler.addSetting(
                List.of("integrations"),
                "proc.terminal",
                Setting.of("proc.customTerminalCommand", customTerminalCommandControl, customTerminalCommand),
                String.class);
    }
}
