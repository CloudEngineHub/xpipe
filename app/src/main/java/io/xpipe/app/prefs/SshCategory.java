package io.xpipe.app.prefs;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SshCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "ssh";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-console-network-outline");
    }

    @Override
    protected BaseRegionBuilder<?, ?> create() {
        var prefs = AppPrefs.get();
        var options = new OptionsBuilder().addTitle("sshConfiguration");
        if (OsType.ofLocal() == OsType.WINDOWS) {
            options.addComp(prefs.getCustomOptions("x11WslInstance").buildComp());
        }

        AtomicReference<Region> button = new AtomicReference<>();
        var agentTest = new ButtonComp(AppI18n.observable("test"), new FontIcon("mdi2p-play"), () -> {
            ThreadHelper.runFailableAsync(() -> {
                var agent = prefs.sshAgentSocket().getValue();
                if (agent == null) {
                    agent = prefs.defaultSshAgentSocket().getValue();
                }

                if (agent == null) {
                    return;
                }

                try {
                    ProcessControlProvider.get().checkSshAgent(LocalShell.getShell(), agent);
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).expected().handle();
                    return;
                }

                Platform.runLater(() -> {
                    button.get().getStyleClass().add(Styles.SUCCESS);
                });
            });
        })
                .padding(new Insets(6, 11, 6, 5))
                .apply(struc -> struc.setAlignment(Pos.CENTER_LEFT));
        agentTest.apply(struc -> button.set(struc));


        if (OsType.ofLocal() != OsType.WINDOWS) {
            var choice = new ContextualFileReferenceChoiceComp(
                    new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()),
                    prefs.sshAgentSocket,
                    null,
                    List.of(),
                    e -> e.equals(DataStorage.get().local()),
                    false);
            choice.setPrompt(prefs.defaultSshAgentSocket);
            choice.maxWidth(600);
            options.sub(
                    new OptionsBuilder().nameAndDescription("sshAgentSocket").addComp(choice, prefs.sshAgentSocket).addComp(agentTest));
        }
        return options.buildComp();
    }
}
