package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ScanAlert;
import io.xpipe.core.store.ShellStore;
import javafx.beans.value.ObservableValue;
import lombok.Value;

public class ScanAction implements ActionProvider {

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        return new DataStoreCallSite<ShellStore>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<ShellStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<ShellStore> getApplicableClass() {
                return ShellStore.class;
            }

            @Override
            public boolean isMajor(DataStoreEntryRef<ShellStore> o) {
                return o.get().getProvider().shouldHaveChildren();
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
                return o.get().getProvider().canHaveSubShells();
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
                return AppI18n.observable("scanConnections");
            }

            @Override
            public String getIcon(DataStoreEntryRef<ShellStore> store) {
                return "mdi2m-magnify-scan";
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public boolean requiresJavaFXPlatform() {
            return true;
        }

        @Override
        public void execute() {
            ScanAlert.showAsync(entry);
        }
    }
}
