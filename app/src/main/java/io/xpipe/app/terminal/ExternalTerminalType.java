package io.xpipe.app.terminal;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;
import lombok.Getter;
import lombok.Value;
import lombok.With;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public interface ExternalTerminalType extends PrefsChoiceValue {

    ExternalTerminalType CMD = new SimplePathType("app.cmd", "cmd.exe", true) {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            if (configuration.getScriptDialect().equals(ShellDialects.CMD)) {
                return CommandBuilder.of().add("/c").addFile(configuration.getScriptFile());
            }

            return CommandBuilder.of().add("/c").add(configuration.getDialectLaunchCommand());
        }
    };

    ExternalTerminalType POWERSHELL = new SimplePathType("app.powershell", "powershell", true) {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            if (configuration.getScriptDialect().equals(ShellDialects.POWERSHELL)) {
                return CommandBuilder.of()
                        .add("-ExecutionPolicy", "Bypass")
                        .add("-File")
                        .addFile(configuration.getScriptFile());
            }

            return CommandBuilder.of()
                    .add("-ExecutionPolicy", "Bypass")
                    .add("-EncodedCommand")
                    .add(sc -> {
                        var base64 = Base64.getEncoder()
                                .encodeToString(configuration
                                        .getDialectLaunchCommand()
                                        .buildBase(sc)
                                        .getBytes(StandardCharsets.UTF_16LE));
                        return "\"" + base64 + "\"";
                    });
        }
    };

    ExternalTerminalType PWSH = new SimplePathType("app.pwsh", "pwsh", true) {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-ExecutionPolicy", "Bypass")
                    .add("-EncodedCommand")
                    .add(sc -> {
                        // Fix for https://github.com/PowerShell/PowerShell/issues/18530#issuecomment-1325691850
                        var c = "$env:PSModulePath=\"\";"
                                + configuration.getDialectLaunchCommand().buildBase(sc);
                        var base64 = Base64.getEncoder().encodeToString(c.getBytes(StandardCharsets.UTF_16LE));
                        return "\"" + base64 + "\"";
                    });
        }
    };
    ExternalTerminalType GNOME_TERMINAL = new PathCheckType("app.gnomeTerminal", "gnome-terminal", true) {
        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            try (ShellControl pc = LocalShell.getShell()) {
                CommandSupport.isInPathOrThrow(
                        pc, executable, toTranslatedString().getValue(), null);

                var toExecute = CommandBuilder.of()
                        .add(executable, "-v", "--title")
                        .addQuoted(configuration.getColoredTitle())
                        .add("--")
                        .addFile(configuration.getScriptFile())
                        // In order to fix this bug which also affects us:
                        // https://askubuntu.com/questions/1148475/launching-gnome-terminal-from-vscode
                        .envrironment("GNOME_TERMINAL_SCREEN", sc -> "");
            }
        }
    };
    ExternalTerminalType KONSOLE = new SimplePathType("app.konsole", "konsole", true) {

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            // Note for later: When debugging konsole launches, it will always open as a child process of
            // IntelliJ/XPipe even though we try to detach it.
            // This is not the case for production where it works as expected
            return CommandBuilder.of().add("--new-tab", "-e").addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType XFCE = new SimplePathType("app.xfce", "xfce4-terminal", true) {
        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("--tab", "--title")
                    .addQuoted(configuration.getColoredTitle())
                    .add("--command")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType ELEMENTARY = new SimplePathType("app.elementaryTerminal", "io.elementary.terminal", true) {
        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("--new-tab").add("-e").addFile(configuration.getColoredTitle());
        }
    };
    ExternalTerminalType TILIX = new SimplePathType("app.tilix", "tilix", true) {
        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-t")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType TERMINATOR = new SimplePathType("app.terminator", "terminator", true) {
        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-e")
                    .addFile(configuration.getScriptFile())
                    .add("-T")
                    .addQuoted(configuration.getColoredTitle())
                    .add("--new-tab");
        }
    };
    ExternalTerminalType TERMINOLOGY = new SimplePathType("app.terminology", "terminology", true) {
        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-T")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-2")
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType COOL_RETRO_TERM = new SimplePathType("app.coolRetroTerm", "cool-retro-term", true) {
        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-T")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType GUAKE = new SimplePathType("app.guake", "guake", true) {
        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-n", "~")
                    .add("-r")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType TILDA = new SimplePathType("app.tilda", "tilda", true) {
        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("-c").addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType XTERM = new SimplePathType("app.xterm", "xterm", true) {
        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-title")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType DEEPIN_TERMINAL = new SimplePathType("app.deepinTerminal", "deepin-terminal", true) {
        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("-C").addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType Q_TERMINAL = new SimplePathType("app.qTerminal", "qterminal", true) {
        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("-e").addQuoted(configuration.getColoredTitle());
        }
    };
    ExternalTerminalType MACOS_TERMINAL = new MacOsType("app.macosTerminal", "Terminal") {
        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            try (ShellControl pc = LocalShell.getShell()) {
                var suffix = "\"" + configuration.getScriptFile().toString().replaceAll("\"", "\\\\\"") + "\"";
                pc.osascriptCommand(String.format(
                                """
                                activate application "Terminal"
                                delay 1
                                tell app "Terminal" to do script %s
                                """,
                                suffix))
                        .execute();
            }
        }
    };
    ExternalTerminalType ITERM2 = new MacOsType("app.iterm2", "iTerm") {
        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var app = this.getApplicationPath();
            if (app.isEmpty()) {
                throw new IllegalStateException("iTerm installation not found");
            }

            try (ShellControl pc = LocalShell.getShell()) {
                var a = app.get().toString();
                pc.osascriptCommand(String.format(
                                """
                                if application "%s" is not running then
                                    launch application "%s"
                                    delay 1
                                    tell application "%s"
                                        tell current tab of current window
                                            close
                                        end tell
                                    end tell
                                end if
                                tell application "%s"
                                    activate
                                    create window with default profile command "%s"
                                end tell
                                """,
                                a,
                                a,
                                a,
                                a,
                                configuration.getScriptFile().toString().replaceAll("\"", "\\\\\"")))
                        .execute();
            }
        }
    };
    ExternalTerminalType WARP = new MacOsType("app.warp", "Warp") {

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public boolean shouldClear() {
            return false;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("Warp.app")
                            .addFile(configuration.getScriptFile()));
        }
    };
    ExternalTerminalType CUSTOM = new CustomTerminalType();
    List<ExternalTerminalType> WINDOWS_TERMINALS = List.of(
            TabbyTerminalType.TABBY_WINDOWS,
            AlacrittyTerminalType.ALACRITTY_WINDOWS,
            WezTerminalType.WEZTERM_WINDOWS,
            WindowsTerminalType.WINDOWS_TERMINAL_PREVIEW,
            WindowsTerminalType.WINDOWS_TERMINAL,
            CMD,
            PWSH,
            POWERSHELL);
    List<ExternalTerminalType> LINUX_TERMINALS = List.of(
            WezTerminalType.WEZTERM_LINUX,
            KONSOLE,
            XFCE,
            ELEMENTARY,
            GNOME_TERMINAL,
            TILIX,
            TERMINATOR,
            KittyTerminalType.KITTY_LINUX,
            TERMINOLOGY,
            COOL_RETRO_TERM,
            GUAKE,
            AlacrittyTerminalType.ALACRITTY_LINUX,
            TILDA,
            XTERM,
            DEEPIN_TERMINAL,
            Q_TERMINAL);
    List<ExternalTerminalType> MACOS_TERMINALS = List.of(
            ITERM2,
            TabbyTerminalType.TABBY_MAC_OS,
            AlacrittyTerminalType.ALACRITTY_MAC_OS,
            KittyTerminalType.KITTY_MACOS,
            WARP,
            WezTerminalType.WEZTERM_MAC_OS,
            MACOS_TERMINAL);

    @SuppressWarnings("TrivialFunctionalExpressionUsage")
    List<ExternalTerminalType> ALL = ((Supplier<List<ExternalTerminalType>>) () -> {
                var all = new ArrayList<ExternalTerminalType>();
                if (OsType.getLocal().equals(OsType.WINDOWS)) {
                    all.addAll(WINDOWS_TERMINALS);
                }
                if (OsType.getLocal().equals(OsType.LINUX)) {
                    all.addAll(LINUX_TERMINALS);
                }
                if (OsType.getLocal().equals(OsType.MACOS)) {
                    all.addAll(MACOS_TERMINALS);
                }
                // Prefer with tabs
                all.sort(Comparator.comparingInt(o -> (o.supportsTabs() ? -1 : 0)));
                all.add(CUSTOM);
                return all;
            })
            .get();

    static ExternalTerminalType determineDefault(ExternalTerminalType existing) {
        // Check for incompatibility with fallback shell
        if (ExternalTerminalType.CMD.equals(existing)
                && !ProcessControlProvider.get().getEffectiveLocalDialect().equals(ShellDialects.CMD)) {
            return ExternalTerminalType.POWERSHELL;
        }

        if (existing != null) {
            return existing;
        }

        return ALL.stream()
                .filter(externalTerminalType -> !externalTerminalType.equals(CUSTOM))
                .filter(terminalType -> terminalType.isAvailable())
                .findFirst()
                .orElse(null);
    }

    boolean supportsTabs();

    default String getWebsite() {
        return null;
    }

    boolean isRecommended();

    boolean supportsColoredTitle();

    default boolean shouldClear() {
        return true;
    }

    default void launch(LaunchConfiguration configuration) throws Exception {}

    abstract class WindowsType extends ExternalApplicationType.WindowsType implements ExternalTerminalType {

        public WindowsType(String id, String executable) {
            super(id, executable);
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var location = determineFromPath();
            if (location.isEmpty()) {
                location = determineInstallation();
                if (location.isEmpty()) {
                    throw new IOException("Unable to find installation of "
                            + toTranslatedString().getValue());
                }
            }

            execute(location.get(), configuration);
        }

        protected abstract void execute(Path file, LaunchConfiguration configuration) throws Exception;
    }

    @Value
    class LaunchConfiguration {
        DataStoreColor color;
        String coloredTitle;
        String cleanTitle;

        @With
        FilePath scriptFile;

        ShellDialect scriptDialect;

        public CommandBuilder getDialectLaunchCommand() {
            var open = scriptDialect.getOpenScriptCommand(scriptFile.toString());
            return open;
        }

        public CommandBuilder appendDialectLaunchCommand(CommandBuilder b) {
            var open = getDialectLaunchCommand();
            b.add(open);
            return b;
        }
    }

    abstract class MacOsType extends ExternalApplicationType.MacApplication implements ExternalTerminalType {

        public MacOsType(String id, String applicationName) {
            super(id, applicationName);
        }
    }

    @Getter
    abstract class PathCheckType extends ExternalApplicationType.PathApplication implements ExternalTerminalType {

        public PathCheckType(String id, String executable, boolean explicitAsync) {
            super(id, executable, explicitAsync);
        }
    }

    @Getter
    abstract class SimplePathType extends PathCheckType {

        public SimplePathType(String id, String executable, boolean explicitAsync) {
            super(id, executable, explicitAsync);
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var args = toCommand(configuration);
            launch(configuration.getColoredTitle(), args);
        }

        protected abstract CommandBuilder toCommand(LaunchConfiguration configuration) throws Exception;
    }
}
