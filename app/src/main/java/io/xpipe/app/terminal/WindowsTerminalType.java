package io.xpipe.app.terminal;

import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FileNames;

import java.nio.file.Files;
import java.nio.file.Path;

public interface WindowsTerminalType extends ExternalTerminalType {

    public static final ExternalTerminalType WINDOWS_TERMINAL = new Standard();
    public static final ExternalTerminalType WINDOWS_TERMINAL_PREVIEW = new Preview();

    private static CommandBuilder toCommand(ExternalTerminalType.LaunchConfiguration configuration) throws Exception {
        // A weird behavior in Windows Terminal causes the trailing
        // backslash of a filepath to escape the closing quote in the title argument
        // So just remove that slash
        var fixedName = FileNames.removeTrailingSlash(configuration.getColoredTitle());

        var toExec = !ShellDialects.isPowershell(LocalShell.getShell())
                ? CommandBuilder.of().addFile(configuration.getScriptFile())
                : CommandBuilder.of()
                        .add("powershell", "-ExecutionPolicy", "Bypass", "-File")
                        .addFile(configuration.getScriptFile());
        var cmd = CommandBuilder.of().add("-w", "1", "nt");

        if (configuration.getColor() != null) {
            cmd.add("--tabColor").addQuoted(configuration.getColor().toHexString());
        }
        return cmd.add("--title").addQuoted(fixedName).add(toExec);
    }

    @Override
    default boolean supportsTabs() {
        return true;
    }

    @Override
    default boolean isRecommended() {
        return true;
    }

    @Override
    default boolean supportsColoredTitle() {
        return false;
    }

    class Standard extends SimplePathType implements WindowsTerminalType {

        public Standard() {
            super("app.windowsTerminal", "wt.exe", false);
        }

        @Override
        public String getWebsite() {
            return "https://aka.ms/terminal-preview";
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) throws Exception {
            return WindowsTerminalType.toCommand(configuration);
        }
    }

    class Preview implements WindowsTerminalType {

        @Override
        public String getWebsite() {
            return "https://aka.ms/terminal";
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(
                            CommandBuilder.of().addFile(getPath().toString()).add(toCommand(configuration)));
        }

        private Path getPath() {
            var local = System.getenv("LOCALAPPDATA");
            return Path.of(local)
                    .resolve("Microsoft\\WindowsApps\\Microsoft.WindowsTerminalPreview_8wekyb3d8bbwe\\wt.exe");
        }

        @Override
        public boolean isAvailable() {
            return Files.exists(getPath());
        }

        @Override
        public String getId() {
            return "app.windowsTerminalPreview";
        }
    }
}
