package io.xpipe.app.terminal;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PwshTerminalType extends ExternalTerminalType.SimplePathType implements TrackableTerminalType {

    public PwshTerminalType() {
        super("app.pwsh", "pwsh", true);
    }

    @Override
    public String getWebsite() {
        return "https://learn.microsoft.com/en-us/powershell/scripting/install/installing-powershell?view=powershell-7.4";
    }

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
}
