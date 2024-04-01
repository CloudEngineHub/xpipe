package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.core.process.ProcessControl;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.TerminalInitScriptConfig;
import io.xpipe.core.util.FailableFunction;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class TerminalLauncher {

    public static void openDirect(String title, FailableFunction<ShellControl,String, Exception> command) throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var type = AppPrefs.get().terminalType().getValue();
            if (type == null) {
                throw ErrorEvent.expected(new IllegalStateException(AppI18n.get("noTerminalSet")));
            }
            var script = ScriptHelper.constructTerminalInitFile(sc.getShellDialect(), sc, ignored -> null, List.of(),
                    command.apply(sc), new TerminalInitScriptConfig(title, type.shouldClear() && AppPrefs.get().clearTerminalOnInit().get()));
            var config = new ExternalTerminalType.LaunchConfiguration(null, title, title, script, sc.getShellDialect());
            type.launch(config);
        }
    }

    public static void open(String title, ProcessControl cc) throws Exception {
        open(null, title, null, cc);
    }

    public static void open(DataStoreEntry entry, String title, String directory, ProcessControl cc) throws Exception {
        var type = AppPrefs.get().terminalType().getValue();
        if (type == null) {
            throw ErrorEvent.expected(new IllegalStateException(AppI18n.get("noTerminalSet")));
        }

        var color = entry != null ? DataStorage.get().getRootForEntry(entry).getColor() : null;
        var prefix = entry != null && color != null && type.supportsColoredTitle() ? color.getEmoji() + " " : "";
        var cleanTitle = (title != null ? title : entry != null ? entry.getName() : "?");
        var adjustedTitle = prefix + cleanTitle;
        var terminalConfig = new TerminalInitScriptConfig(
                adjustedTitle,
                type.shouldClear() && AppPrefs.get().clearTerminalOnInit().get());

        var request = UUID.randomUUID();
        var d = ProcessControlProvider.get().getEffectiveLocalDialect();
        var launcherScript = d.terminalLauncherScript(request, adjustedTitle);
        var preparationScript = ScriptHelper.createLocalExecScript(launcherScript);
        var config = new ExternalTerminalType.LaunchConfiguration(
                entry != null ? color : null, adjustedTitle, cleanTitle, preparationScript, d);
        var latch = TerminalLauncherManager.submitAsync(request, cc, terminalConfig, directory);
        try {
            type.launch(config);
            latch.await();
        } catch (Exception ex) {
            throw ErrorEvent.expected(new IOException(
                    "Unable to launch terminal " + type.toTranslatedString().getValue() + ": " + ex.getMessage(), ex));
        }
    }
}
