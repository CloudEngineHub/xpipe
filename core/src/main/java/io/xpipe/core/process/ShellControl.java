package io.xpipe.core.process;

import io.xpipe.core.store.ShellMessageFormatter;
import io.xpipe.core.util.FailableFunction;
import io.xpipe.core.util.FailableSupplier;
import io.xpipe.core.util.SecretValue;
import io.xpipe.core.util.XPipeSystemId;
import lombok.NonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

public interface ShellControl extends ProcessControl {

    default boolean isLocal() {
        return getSystemId().equals(XPipeSystemId.getLocal());
    }

    ShellMessageFormatter getMessageFormatter();

    ShellControl withMessageFormatter(ShellMessageFormatter formatter);

    UUID getSystemId();

    Semaphore getCommandLock();

    ShellControl onInit(Consumer<ShellControl> pc);

    ShellControl onExit(Consumer<ShellControl> pc);

    ShellControl onFail(Consumer<Throwable> t);

    ShellControl withExceptionConverter(ExceptionConverter converter);

    String prepareTerminalOpen(String displayName) throws Exception;

    String prepareIntermediateTerminalOpen(String content, String displayName) throws Exception;

    String getSystemTemporaryDirectory();

    String getSubTemporaryDirectory();

    void checkRunning();

    default CommandControl osascriptCommand(String script) {
        return command(String.format(
                """
                osascript - "$@" <<EOF
                %s
                EOF
                """,
                script));
    }

    default byte[] executeSimpleRawBytesCommand(String command) throws Exception {
        try (CommandControl c = command(command).start()) {
            return c.readRawBytesOrThrow();
        }
    }

    default String executeSimpleStringCommand(String command) throws Exception {
        try (CommandControl c = command(command).start()) {
            return c.readStdoutOrThrow();
        }
    }

    default boolean executeSimpleBooleanCommand(String command) throws Exception {
        try (CommandControl c = command(command).start()) {
            return c.discardAndCheckExit();
        }
    }

    default void executeSimpleCommand(String command) throws Exception {
        try (CommandControl c = command(command).start()) {
            c.discardOrThrow();
        }
    }

    default String executeSimpleStringCommand(ShellDialect type, String command) throws Exception {
        try (var sub = subShell(type).start()) {
            return sub.executeSimpleStringCommand(command);
        }
    }

    ElevationResult buildElevatedCommand(String input, String prefix) throws Exception;

    void restart() throws Exception;

    OsType getOsType();

    ElevationConfig getElevationConfig() throws Exception;

    ShellControl elevated(String message, FailableFunction<ShellControl, Boolean, Exception> elevationFunction);

    default ShellControl elevationPassword(SecretValue value) {
        return elevationPassword(() -> value);
    }
    ShellControl elevationPassword(FailableSupplier<SecretValue, Exception> value);

    ShellControl initWith(String cmds);

    ShellControl initWith(List<String> cmds);

    ShellControl additionalTimeout(int ms);

    FailableSupplier<SecretValue, Exception> getElevationPassword();

    default ShellControl subShell(@NonNull ShellDialect type) {
        return subShell(p -> type.getOpenCommand(), (sc, command) -> command)
                .elevationPassword(getElevationPassword());
    }

    interface TerminalOpenFunction {

        String prepare(ShellControl sc, String command) throws Exception;
    }

    default ShellControl identicalSubShell() {
        return subShell(p -> p.getShellDialect().getOpenCommand(), (sc, command) -> command)
                .elevationPassword(getElevationPassword());
    }

    default ShellControl subShell(@NonNull String command) {
        return subShell(processControl -> command, (sc, command1) -> command1);
    }

    default ShellControl enforcedDialect(ShellDialect type) throws Exception {
        start();
        if (getShellDialect().equals(type)) {
            return this;
        } else {
            return subShell(type).start();
        }
    }

    default <T> T enforceDialect(@NonNull ShellDialect type, FailableFunction<ShellControl, T, Exception> sc) throws Exception {
        if (isRunning() && getShellDialect().equals(type)) {
            return sc.apply(this);
        } else {
            try (var sub = subShell(type).start()) {
                return sc.apply(sub);
            }
        }
    }

    ShellControl subShell(
            FailableFunction<ShellControl, String, Exception> command, TerminalOpenFunction terminalCommand);

    void executeLine(String command) throws Exception;

    void cd(String directory) throws Exception;

    @Override
    ShellControl start();

    CommandControl command(FailableFunction<ShellControl, String, Exception> command);

    CommandControl command(
            FailableFunction<ShellControl, String, Exception> command,
            FailableFunction<ShellControl, String, Exception> terminalCommand);

    default CommandControl command(String... command) {
        var c = Arrays.stream(command).filter(s -> s != null).toArray(String[]::new);
        return command(shellProcessControl -> String.join("\n", c));
    }

    default CommandControl command(List<String> command) {
        return command(shellProcessControl -> ShellDialect.flatten(command));
    }

    default CommandControl command(CommandBuilder builder) {
        return command(shellProcessControl -> builder.build(shellProcessControl));
    }

    void exitAndWait() throws IOException;
}
