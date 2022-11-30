package io.xpipe.extension;

import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.util.SimpleValidator;
import io.xpipe.extension.util.Validator;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class GuiDialog {

    Comp<?> comp;
    Validator validator;

    public GuiDialog(Comp<?> comp) {
        this.comp = comp;
        this.validator = new SimpleValidator();
    }
}
