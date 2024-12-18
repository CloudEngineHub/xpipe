package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.util.FailableSupplier;

import java.io.IOException;
import java.util.Optional;

public class CommandSupport {

    public static String getPath(ShellControl sc) throws Exception {
        var path = sc.command(sc.getShellDialect().getPrintEnvironmentVariableCommand("PATH"))
                .readStdoutOrThrow();
        return path;
    }

    public static String getLibraryPath(ShellControl sc) throws Exception {
        var path = sc.command(sc.getShellDialect().getPrintEnvironmentVariableCommand("LD_LIBRARY_PATH"))
                .readStdoutOrThrow();
        return path;
    }

    public static boolean isRoot(ShellControl shellControl) throws Exception {
        if (shellControl.getOsType() == OsType.WINDOWS) {
            return false;
        }

        var isRoot = shellControl.executeSimpleBooleanCommand("test \"${EUID:-$(id -u)}\" -eq 0");
        return isRoot;
    }

    public static Optional<String> findProgram(ShellControl processControl, String name) throws Exception {
        var out = processControl
                .command(processControl.getShellDialect().getWhichCommand(name))
                .readStdoutIfPossible();
        return out.flatMap(s -> s.lines().findFirst()).map(String::trim);
    }

    public static boolean isInPath(ShellControl processControl, String executable) throws Exception {
        return processControl.executeSimpleBooleanCommand(
                processControl.getShellDialect().getWhichCommand(executable));
    }

    public static boolean isInPathSilent(ShellControl processControl, String executable) {
        try {
            return processControl.executeSimpleBooleanCommand(
                    processControl.getShellDialect().getWhichCommand(executable));
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            return false;
        }
    }

    public static void isInPathOrThrow(
            ShellControl processControl, String executable, String displayName, DataStoreEntry connection)
            throws Exception {
        if (!isInPath(processControl, executable)) {
            throw ErrorEvent.expected(new IOException(displayName + " executable " + executable + " not found in PATH"
                    + (connection != null ? " on system " + connection.getName() : "")));
        }
    }

    public static void isSupported(FailableSupplier<Boolean> supplier, String displayName, DataStoreEntry connection)
            throws Exception {
        if (!supplier.get()) {
            throw ErrorEvent.expected(new IOException(displayName + " is not supported"
                    + (connection != null ? " on system " + connection.getName() : "")));
        }
    }
}
