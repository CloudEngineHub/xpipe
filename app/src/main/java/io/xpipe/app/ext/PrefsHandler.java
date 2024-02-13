package io.xpipe.app.ext;

import io.xpipe.app.fxcomps.Comp;
import javafx.beans.property.Property;

public interface PrefsHandler {

    <T> void addSetting(String id, Class<T> c, Property<T> property, Comp<?> comp);
}
