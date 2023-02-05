package io.xpipe.extension.util;

import io.xpipe.core.process.OsType;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.XPipeInstallation;

import java.nio.file.Files;
import java.nio.file.Path;

public class DesktopShortcuts {

    private static void createWindowsShortcut(String target, String name) throws Exception {
        var icon = XPipeInstallation.getLocalDefaultInstallationIcon();
        var content = String.format(
                """
                        set "TARGET=%s"
                        set "SHORTCUT=%%HOMEDRIVE%%%%HOMEPATH%%\\Desktop\\%s.lnk"
                        set PWS=powershell.exe -ExecutionPolicy Bypass -NoLogo -NonInteractive -NoProfile

                        %%PWS%% -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%%SHORTCUT%%'); $S.IconLocation='%s'; $S.TargetPath = '%%TARGET%%'; $S.Save()"
                        """,
                target, name, icon.toString());
        ShellStore.local().create().executeSimpleCommand(content);
    }

    private static void createLinuxShortcut(String target, String name) throws Exception {
        var icon = XPipeInstallation.getLocalDefaultInstallationIcon();
        var content = String.format(
                """
                        [Desktop Entry]
                        Type=Application
                        Name=%s
                        Comment=Open with X-Pipe
                        TryExec=/opt/xpipe/app/bin/xpiped
                        Exec=/opt/xpipe/cli/bin/xpipe open %s
                        Icon=%s
                        Terminal=false
                        Categories=Utility;Development;Office;
                        """,
                name, target, icon.toString());
        var file = Path.of("~/Desktop/" + name + ".desktop").toRealPath();
        Files.writeString(file, content);
        file.toFile().setExecutable(true);
    }

    private static void createMacOSShortcut(String target, String name) throws Exception {
        var icon = XPipeInstallation.getLocalDefaultInstallationIcon();
        var content = String.format(
                """
                        #!/bin/bash
                        open %s
                        """,
                target);
        var file = Path.of("~/Desktop/" + name + ".command").toRealPath();
        Files.writeString(file, content);
        file.toFile().setExecutable(true);

        var iconScriptContent = String.format(
                """
                       iconSource="%s"
                       iconDestination="%s"
                       icon=/tmp/`basename $iconSource`
                       rsrc=/tmp/icon.rsrc
                       cp $iconSource $icon
                       sips -i $icon
                       DeRez -only icns $icon > $rsrc
                       SetFile -a C $iconDestination
                       Rez -append $rsrc -o $iconDestination
                        """,
                icon, target);
        ShellStore.local().create().executeSimpleCommand(iconScriptContent);
    }

    public static void create(String target, String name) throws Exception {
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
             createWindowsShortcut(target, name);
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
             createLinuxShortcut(target, name);
        } else {
             createMacOSShortcut(target, name);
        }
    }
}
