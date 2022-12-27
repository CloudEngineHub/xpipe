package io.xpipe.core.util;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ProcessOutputException;
import io.xpipe.core.process.ShellProcessControl;

import java.util.List;

public class XPipeInstallation {

    public static String createExternalAsyncLaunchCommand(String installationBase, String arguments) {
        var suffix = (arguments != null ? " " + arguments : "");
        if (OsType.getLocal().equals(OsType.LINUX)) {
            return "nohup \"" + installationBase + "/app/bin/xpiped\" --external" + suffix + " & disown";
        } else if (OsType.getLocal().equals(OsType.MAC)) {
            return "nohup \"" + installationBase + "/Contents/MacOS/xpiped\" --external" + suffix + " & disown";
        }

        return "\"" + FileNames.join(installationBase, XPipeInstallation.getDaemonExecutablePath(OsType.getLocal())) + "\" --external" + suffix;
    }

    public static String createExternalLaunchCommand(String command, String arguments) {
        var suffix = (arguments != null ? " " + arguments : "");
        return "\"" + command + "\" --external" + suffix;
    }

    public static String getInstallationBasePathForCLI(ShellProcessControl p, String cliExecutable) throws Exception {
        var defaultInstallation =  getDefaultInstallationBasePath(p, true);
        if (cliExecutable == null) {
            return defaultInstallation;
        }

        if (p.getOsType().equals(OsType.LINUX) && cliExecutable.equals("/usr/bin/xpipe")) {
            return defaultInstallation;
        }

        if (FileNames.startsWith(cliExecutable, defaultInstallation)) {
            return defaultInstallation;
        }

        if (p.getOsType().equals(OsType.MAC)) {
            return FileNames.getParent(FileNames.getParent(FileNames.getParent(FileNames.getParent(cliExecutable))));
        } else {
            return FileNames.getParent(FileNames.getParent(cliExecutable));
        }
    }

    public static String queryInstallationVersion(ShellProcessControl p, String exec) throws Exception {
        try (CommandProcessControl c = p.command(List.of(exec, "version")).start()) {
            return c.readOrThrow();
        } catch (ProcessOutputException ex) {
            return "?";
        }
    }

    public static String getInstallationExecutable(ShellProcessControl p, String installation) throws Exception {
        var executable = getDaemonExecutablePath(p.getOsType());
        var file = FileNames.join(installation, executable);
        return file;
    }

    public static String getDataBasePath(ShellProcessControl p) throws Exception {
        if (p.getOsType().equals(OsType.WINDOWS)) {
            var base = p.executeStringSimpleCommand(p.getShellType().getPrintVariableCommand("userprofile"));
            return FileNames.join(base, ".xpipe");
        } else {
            return FileNames.join("~", ".xpipe");
        }
    }

    public static String getLocalDefaultInstallationBasePath(boolean acceptCustomHome) {
        var customHome = System.getenv("XPIPE_HOME");
        if (customHome != null && !customHome.isEmpty() && acceptCustomHome) {
            return customHome;
        }

        String path = null;
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            var base = System.getenv("LOCALAPPDATA");
            path = FileNames.join(base, "X-Pipe");
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            path = "/opt/xpipe";
        } else {
            path = "~/Applications/X-Pipe.app";
        }

        return path;
    }

    public static String getDefaultInstallationBasePath(ShellProcessControl p, boolean acceptPortable) throws Exception {
        if (acceptPortable) {
            var customHome = p.executeStringSimpleCommand(p.getShellType().getPrintVariableCommand("XPIPE_HOME"));
            if (!customHome.isEmpty()) {
                return customHome;
            }
        }

        String path = null;
        if (p.getOsType().equals(OsType.WINDOWS)) {
            var base = p.executeStringSimpleCommand(p.getShellType().getPrintVariableCommand("LOCALAPPDATA"));
            path = FileNames.join(base, "X-Pipe");
        } else if (p.getOsType().equals(OsType.LINUX)) {
            path = "/opt/xpipe";
        } else {
            path = "~/Applications/X-Pipe.app";
        }

        return path;
    }

    public static String getDaemonDebugScriptPath(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("app", "scripts", "xpiped_debug.bat");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("app", "scripts", "xpiped_debug.sh");
        } else {
            return FileNames.join("Content", "scripts", "xpiped_debug.sh");
        }
    }

    public static String getDaemonDebugAttachScriptPath(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("app", "scripts", "xpiped_debug_attach.bat");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("app", "scripts", "xpiped_debug_attach.sh");
        } else {
            return FileNames.join("Content", "scripts", "xpiped_debug_attach.sh");
        }
    }

    public static String getDaemonExecutablePath(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("app", "xpiped.exe");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("app", "bin", "xpiped");
        } else {
            return FileNames.join("Content", "MacOS", "xpiped");
        }
    }
}
