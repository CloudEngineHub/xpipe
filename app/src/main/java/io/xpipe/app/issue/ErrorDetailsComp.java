package io.xpipe.app.issue;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.core.util.Deobfuscator;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ErrorDetailsComp extends SimpleComp {

    private ErrorEvent event;

    private Region createStrackTraceContent() {
        if (event.getThrowable() != null) {
            String stackTrace = Deobfuscator.deobfuscateToString(event.getThrowable());
            stackTrace = stackTrace.replace("\t", "");
            var tf = new TextArea(stackTrace);
            AppFont.verySmall(tf);
            tf.setWrapText(false);
            tf.setEditable(false);
            tf.setPadding(new Insets(10));
            return tf;
        }

        return new Region();
    }

    @Override
    protected Region createSimple() {
        var tb = Comp.of(this::createStrackTraceContent);
        tb.apply(r -> AppFont.small(r.get()));
        return tb.createRegion();
    }
}
