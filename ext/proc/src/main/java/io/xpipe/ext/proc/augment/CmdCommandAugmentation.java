package io.xpipe.ext.proc.augment;

import io.xpipe.core.process.ShellProcessControl;

import java.util.List;

public class CmdCommandAugmentation extends CommandAugmentation {
    @Override
    public boolean matches(String executable) {
        return executable.equals("cmd");
    }

    @Override
    protected void prepareBaseCommand(ShellProcessControl processControl, List<String> baseCommand) {
        remove(baseCommand, "/C");
    }

    @Override
    protected void modifyTerminalCommand(List<String> baseCommand, boolean hasSubCommand) {
        if (hasSubCommand) {
            baseCommand.add("/C");
        }
    }

    @Override
    protected void modifyNonTerminalCommand(List<String> baseCommand) {}
}
