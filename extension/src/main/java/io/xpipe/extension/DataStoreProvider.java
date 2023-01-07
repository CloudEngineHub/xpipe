package io.xpipe.extension;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.core.util.JacksonizedValue;
import javafx.beans.property.Property;

import java.util.List;

public interface DataStoreProvider {

    default ModuleInstall getRequiredAdditionalInstallation() {
        return null;
    }

    default void validate() throws Exception {
        getCategory();
        for (Class<?> storeClass : getStoreClasses()) {
            if (!JacksonizedValue.class.isAssignableFrom(storeClass)) {
                throw new ExtensionException(
                        String.format("Store class %s is not a Jacksonized value", storeClass.getSimpleName()));
            }
        }
    }

    default Category getCategory() {
        var c = getStoreClasses().get(0);
        if (StreamDataStore.class.isAssignableFrom(c)) {
            return Category.STREAM;
        }

        if (FileSystemStore.class.isAssignableFrom(c) || ShellStore.class.isAssignableFrom(c)) {
            return Category.SHELL;
        }

        throw new ExtensionException("Provider " + getId() + " has no set category");
    }

    default GuiDialog guiDialog(Property<DataStore> store) {
        return null;
    }

    default boolean init() throws Exception {
        return true;
    }

    default void storageInit() throws Exception {
    }

    default boolean isShareable() {
        return false;
    }

    String queryInformationString(DataStore store, int length) throws Exception;

    public String toSummaryString(DataStore store, int length);

    default String i18n(String key) {
        return I18n.get(getId() + "." + key);
    }

    default String i18nKey(String key) {
        return getId() + "." + key;
    }

    default String getDisplayName() {
        return i18n("displayName");
    }

    default String getDisplayDescription() {
        return i18n("displayDescription");
    }

    default String getModuleName() {
        var n = getClass().getPackageName();
        var i = n.lastIndexOf('.');
        return i != -1 ? n.substring(i + 1) : n;
    }

    default String getDisplayIconFileName() {
        return getModuleName() + ":" + getId() + "_icon.png";
    }

    default Dialog dialogForStore(DataStore store) {
        return null;
    }

    DataStore defaultStore();

    List<String> getPossibleNames();

    default String getId() {
        return getPossibleNames().get(0);
    }

    List<Class<?>> getStoreClasses();

    default boolean shouldShow() {
        return true;
    }

    enum Category {
        STREAM,
        SHELL,
        DATABASE;
    }
}
