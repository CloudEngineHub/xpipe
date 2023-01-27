package io.xpipe.app.launcher;

import io.xpipe.core.util.XPipeDaemonMode;
import picocli.CommandLine;

public class LauncherModeConverter implements CommandLine.ITypeConverter<XPipeDaemonMode> {

    @Override
    public XPipeDaemonMode convert(String value) throws Exception {
        return XPipeDaemonMode.get(value);
    }
}
