package io.xpipe.app.browser;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.storage.DataColor;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import lombok.Getter;

@Getter
public abstract class BrowserSessionTab {

    protected final BooleanProperty busy = new SimpleBooleanProperty();
    protected final BrowserAbstractSessionModel<?> browserModel;
    protected final String name;
    protected final Property<BrowserSessionTab> splitTab = new SimpleObjectProperty<>();

    public BrowserSessionTab(BrowserAbstractSessionModel<?> browserModel, String name) {
        this.browserModel = browserModel;
        this.name = name;
    }

    public abstract Comp<?> comp();

    public abstract boolean canImmediatelyClose();

    public abstract void init() throws Exception;

    public abstract void close();

    public abstract String getIcon();

    public abstract DataColor getColor();

    public boolean isCloseable() {
        return true;
    }
}
