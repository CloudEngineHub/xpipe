package io.xpipe.app.ext;

import io.xpipe.app.comp.base.StoreToggleComp;
import io.xpipe.app.comp.base.SystemStateComp;
import io.xpipe.app.comp.store.StoreEntryComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.util.LabelGraphic;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.SingletonSessionStore;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

public interface SingletonSessionStoreProvider extends DataStoreProvider {

    @Override
    default ObservableBooleanValue busy(StoreEntryWrapper wrapper) {
        return Bindings.createBooleanBinding(
                () -> {
                    SingletonSessionStore<?> s = wrapper.getEntry().getStore().asNeeded();
                    return s.isSessionEnabled() != s.isSessionRunning();
                },
                wrapper.getCache());
    }

    @Override
    default StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        var t = createToggleComp(sec);
        return StoreEntryComp.create(sec.getWrapper(), t, preferLarge);
    }

    default StoreToggleComp createToggleComp(StoreSection sec) {
        var enabled = new SimpleBooleanProperty();
        sec.getWrapper().getCache().subscribe((newValue) -> {
            SingletonSessionStore<?> s = sec.getWrapper().getEntry().getStore().asNeeded();
            enabled.set(s.isSessionEnabled());
        });

        ObservableValue<LabelGraphic> g = enabled.map(aBoolean -> aBoolean ?
                new LabelGraphic.IconGraphic("mdi2c-circle-slice-8") : new LabelGraphic.IconGraphic("mdi2p-power"));
        var t = new StoreToggleComp(null, g, sec, enabled, aBoolean -> {
            SingletonSessionStore<?> s = sec.getWrapper().getEntry().getStore().asNeeded();
            if (s.isSessionEnabled() != aBoolean) {
                ThreadHelper.runFailableAsync(() -> {
                    if (aBoolean) {
                        s.startSessionIfNeeded();
                    } else {
                        s.stopSessionIfNeeded();
                    }
                });
            }
        });
        t.tooltipKey("enabled");
        return t;
    }

    default Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(Bindings.createObjectBinding(
                () -> {
                    SingletonSessionStore<?> s = w.getEntry().getStore().asNeeded();
                    if (!s.isSessionEnabled()) {
                        return SystemStateComp.State.OTHER;
                    }

                    return s.isSessionRunning() ? SystemStateComp.State.SUCCESS : SystemStateComp.State.FAILURE;
                },
                w.getCache()));
    }
}
