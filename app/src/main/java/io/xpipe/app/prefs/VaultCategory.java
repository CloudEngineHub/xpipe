package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.Validator;
import io.xpipe.core.util.XPipeInstallation;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;

import lombok.SneakyThrows;

public class VaultCategory extends AppPrefsCategory {

    private static final boolean STORAGE_DIR_FIXED = System.getProperty(XPipeInstallation.DATA_DIR_PROP) != null;

    @Override
    protected String getId() {
        return "vault";
    }

    @SneakyThrows
    public Comp<?> create() {
        var prefs = AppPrefs.get();
        var builder = new OptionsBuilder();
        if (!STORAGE_DIR_FIXED) {
            var sub =
                    new OptionsBuilder().nameAndDescription("storageDirectory").addPath(prefs.storageDirectory);
            sub.withValidator(val -> {
                sub.check(Validator.absolutePath(val, prefs.storageDirectory));
                sub.check(Validator.directory(val, prefs.storageDirectory));
            });
            builder.addTitle("storage").sub(sub);
        }

        var encryptVault = new SimpleBooleanProperty(prefs.encryptAllVaultData().get());
        encryptVault.addListener((observable, oldValue, newValue) -> {
            if (!newValue
                    && !AppWindowHelper.showConfirmationAlert(
                            "confirmVaultUnencryptTitle",
                            "confirmVaultUnencryptHeader",
                            "confirmVaultUnencryptContent")) {
                Platform.runLater(() -> {
                    encryptVault.set(true);
                });
                return;
            }

            prefs.encryptAllVaultData.setValue(newValue);
        });

        builder.addTitle("vaultSecurity")
                .sub(new OptionsBuilder()
                        .nameAndDescription("workspaceLock")
                        .addComp(
                                new ButtonComp(
                                        Bindings.createStringBinding(
                                                () -> {
                                                    return prefs.getLockCrypt().getValue() != null
                                                                    && !prefs.getLockCrypt()
                                                                            .getValue()
                                                                            .isEmpty()
                                                            ? AppI18n.get("changeLock")
                                                            : AppI18n.get("createLock");
                                                },
                                                prefs.getLockCrypt()),
                                        LockChangeAlert::show),
                                prefs.getLockCrypt())
                        .nameAndDescription("lockVaultOnHibernation")
                        .addToggle(prefs.lockVaultOnHibernation)
                        .hide(prefs.getLockCrypt()
                                .isNull()
                                .or(prefs.getLockCrypt().isEmpty()))
                        .nameAndDescription("encryptAllVaultData")
                        .addToggle(encryptVault));
        return builder.buildComp();
    }
}
