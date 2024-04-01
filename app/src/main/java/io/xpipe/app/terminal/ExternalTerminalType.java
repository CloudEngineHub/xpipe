package io.xpipe.app.terminal;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.util.*;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;
import lombok.Getter;
import lombok.Value;
import lombok.With;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public interface ExternalTerminalType extends PrefsChoiceValue {

    ExternalTerminalType CMD = new SimplePathType("app.cmd", "cmd.exe", true) {

        @Override
        public boolean supportsTabs() {
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

    ExternalTerminalType ALACRITTY_WINDOWS = new SimplePathType("app.alacritty", "alacritty", false) {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            var b = CommandBuilder.of();
            if (configuration.getColor() != null) {
                b.add("-o")
                        .addQuoted("colors.primary.background='%s'"
                                .formatted(configuration.getColor().toHexString()));
            }

            // Alacritty is bugged and will not accept arguments with spaces even if they are correctly passed/escaped
            // So this will not work when the script file has spaces
            return b.add("-t")
                    .addQuoted(configuration.getCleanTitle())
                    .add("-e")
                    .add(configuration.getDialectLaunchCommand());
        }
    };
    ExternalTerminalType TABBY_WINDOWS = new WindowsType("app.tabby", "Tabby") {

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
            // Tabby has a very weird handling of output, even detaching with start does not prevent it from printing
            if (configuration.getScriptDialect().equals(ShellDialects.CMD)) {
                // It also freezes with any other input than .bat files, why?
                LocalShell.getShell().executeSimpleCommand(CommandBuilder.of()
                                .addFile(file.toString())
                                .add("run")
                                .addFile(configuration.getScriptFile())
                                .discardOutput());
            }

            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .addFile(file.toString())
                            .add("run")
                            .add(configuration.getDialectLaunchCommand())
                            .discardOutput());
        }

        @Override
        protected Optional<Path> determineInstallation() {
            var perUser = WindowsRegistry.readString(
                            WindowsRegistry.HKEY_CURRENT_USER,
                            "SOFTWARE\\71445fac-d6ef-5436-9da7-5a323762d7f5",
                            "InstallLocation")
                    .map(p -> p + "\\Tabby.exe")
                    .map(Path::of);
            if (perUser.isPresent()) {
                return perUser;
            }

            var systemWide = WindowsRegistry.readString(
                            WindowsRegistry.HKEY_LOCAL_MACHINE,
                            "SOFTWARE\\71445fac-d6ef-5436-9da7-5a323762d7f5",
                            "InstallLocation")
                    .map(p -> p + "\\Tabby.exe")
                    .map(Path::of);
            return systemWide;
        }
    };
    ExternalTerminalType WEZ_WINDOWS = new WindowsType("app.wezterm", "wezterm-gui") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
            LocalShell.getShell().executeSimpleCommand(CommandBuilder.of().addFile(file.toString()).add("start").add(configuration.getDialectLaunchCommand()));
        }

        @Override
        protected Optional<Path> determineInstallation() {
            Optional<String> launcherDir;
            launcherDir = WindowsRegistry.readString(
                            WindowsRegistry.HKEY_LOCAL_MACHINE,
                            "Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\{BCF6F0DA-5B9A-408D-8562-F680AE6E1EAF}_is1",
                            "InstallLocation")
                    .map(p -> p + "\\wezterm-gui.exe");
            return launcherDir.map(Path::of);
        }
    };
    ExternalTerminalType WEZ_LINUX = new SimplePathType("app.wezterm", "wezterm-gui", true) {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("start").addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType GNOME_TERMINAL = new PathCheckType("app.gnomeTerminal", "gnome-terminal", true) {

        @Override
        public boolean supportsTabs() {
            return false;
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
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-n", "~")
                    .add("-r")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType ALACRITTY_LINUX = new SimplePathType("app.alacritty", "alacritty", true) {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-t")
                    .addQuoted(configuration.getCleanTitle())
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
                                a, a, a, a, configuration.getScriptFile().toString().replaceAll("\"", "\\\\\"")))
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
    ExternalTerminalType TABBY_MAC_OS = new MacOsType("app.tabby", "Tabby") {
        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("Tabby.app")
                            .add("-n", "--args", "run")
                            .addFile(configuration.getScriptFile()));
        }
    };
    ExternalTerminalType ALACRITTY_MACOS = new MacOsType("app.alacritty", "Alacritty") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("Alacritty.app")
                            .add("-n", "--args", "-t")
                            .addQuoted(configuration.getCleanTitle())
                            .add("-e")
                            .addFile(configuration.getScriptFile()));
        }
    };
    ExternalTerminalType WEZ_MACOS = new MacOsType("app.wezterm", "WezTerm") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var c = CommandBuilder.of()
                    .addFile(getApplicationPath()
                            .orElseThrow()
                            .resolve("Contents")
                            .resolve("MacOS")
                            .resolve("wezterm-gui")
                            .toString())
                    .add("start")
                    .add(configuration.getDialectLaunchCommand());
            ExternalApplicationHelper.startAsync(c);
        }
    };
    ExternalTerminalType KITTY_MACOS = new MacOsType("app.kitty", "kitty") {

        @Override
        public boolean supportsTabs() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            if (!MacOsPermissions.waitForAccessibilityPermissions()) {
                return;
            }

            try (ShellControl pc = LocalShell.getShell()) {
                pc.osascriptCommand(String.format(
                                """
                                        if application "Kitty" is running then
                                            tell application "Kitty" to activate
                                            tell application "System Events" to tell process "Kitty" to keystroke "t" using command down
                                        else
                                            tell application "Kitty" to activate
                                        end if
                                        delay 1
                                        tell application "System Events"
                                            tell process "Kitty"
                                                keystroke "%s"
                                                delay 0.01
                                                key code 36
                                            end tell
                                        end tell
                                        """,
                                configuration.getScriptFile().toString().replaceAll("\"", "\\\\\"")))
                        .execute();
            }
        }
    };
    ExternalTerminalType CUSTOM = new CustomType();
    List<ExternalTerminalType> WINDOWS_TERMINALS = List.of(
            TABBY_WINDOWS,
            ALACRITTY_WINDOWS,
            WEZ_WINDOWS,
            WindowsTerminalType.WINDOWS_TERMINAL_PREVIEW,
            WindowsTerminalType.WINDOWS_TERMINAL,
            CMD,
            PWSH,
            POWERSHELL);
    List<ExternalTerminalType> LINUX_TERMINALS = List.of(
            WEZ_LINUX,
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
            ALACRITTY_LINUX,
            TILDA,
            XTERM,
            DEEPIN_TERMINAL,
            Q_TERMINAL);
    List<ExternalTerminalType> MACOS_TERMINALS =
            List.of(ITERM2, TABBY_MAC_OS, ALACRITTY_MACOS, KITTY_MACOS, WARP, WEZ_MACOS, MACOS_TERMINAL);

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

    default boolean supportsColoredTitle() {
        return true;
    }

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

    class CustomType extends ExternalApplicationType implements ExternalTerminalType {

        public CustomType() {
            super("app.custom");
        }

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var custom = AppPrefs.get().customTerminalCommand().getValue();
            if (custom == null || custom.isBlank()) {
                throw ErrorEvent.expected(new IllegalStateException("No custom terminal command specified"));
            }

            var format = custom.toLowerCase(Locale.ROOT).contains("$cmd") ? custom : custom + " $CMD";
            try (var pc = LocalShell.getShell()) {
                var toExecute = ExternalApplicationHelper.replaceFileArgument(format, "CMD", configuration.getScriptFile().toString());
                // We can't be sure whether the command is blocking or not, so always make it not blocking
                if (pc.getOsType().equals(OsType.WINDOWS)) {
                    toExecute = "start \"" + configuration.getCleanTitle() + "\" " + toExecute;
                } else {
                    toExecute = "nohup " + toExecute + " </dev/null &>/dev/null & disown";
                }
                pc.executeSimpleCommand(toExecute);
            }
        }

        @Override
        public boolean isAvailable() {
            return true;
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
