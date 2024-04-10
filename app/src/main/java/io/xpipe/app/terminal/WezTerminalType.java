package io.xpipe.app.terminal;

import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.CommandBuilder;

import java.nio.file.Path;
import java.util.Optional;

public interface WezTerminalType extends ExternalTerminalType {

    ExternalTerminalType WEZTERM_WINDOWS = new Windows();
    ExternalTerminalType WEZTERM_LINUX = new Linux();
    ExternalTerminalType WEZTERM_MAC_OS = new MacOs();

    @Override
    default boolean supportsTabs() {
        return false;
    }

    @Override
    default String getWebsite() {
        return "https://wezfurlong.org/wezterm/index.html";
    }

    @Override
    default boolean isRecommended() {
        return false;
    }

    @Override
    default boolean supportsColoredTitle() {
        return true;
    }

    static class Windows extends WindowsType implements WezTerminalType {

        public Windows() {
            super("app.wezterm", "wezterm-gui");
        }

        @Override
        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .addFile(file.toString())
                            .add("start")
                            .add(configuration.getDialectLaunchCommand()));
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
    }

    static class Linux extends SimplePathType implements WezTerminalType {

        public Linux() {
            super("app.wezterm", "wezterm-gui", true);
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) {
            return CommandBuilder.of().add("start").addFile(configuration.getScriptFile());
        }
    }

    class MacOs extends MacOsType implements WezTerminalType {

        public MacOs() {
            super("app.wezterm", "WezTerm");
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
    }
}
