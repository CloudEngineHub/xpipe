package io.xpipe.core.process;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.util.FailableFunction;
import io.xpipe.core.util.SecretValue;
import io.xpipe.core.util.XPipeSystemId;
import lombok.NonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ShellControl extends ProcessControl {

    default boolean isLocal() {
        return getSystemId().equals(XPipeSystemId.getLocal());
    }

    UUID getSystemId();

    Semaphore getCommandLock();

    ShellControl onInit(Consumer<ShellControl> pc);

    ShellControl onExit(Consumer<ShellControl> pc);

    ShellControl withMessageFormatter(Function<String, String> formatter);

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

    default void executeSimpleCommand(String command, String failMessage) throws Exception {
        try (CommandControl c = command(command).start()) {
            c.discardOrThrow();
        } catch (ProcessOutputException out) {
            throw ProcessOutputException.withPrefix(failMessage, out);
        }
    }

    default String executeSimpleStringCommand(ShellDialect type, String command) throws Exception {
        try (var sub = subShell(type).start()) {
            return sub.executeSimpleStringCommand(command);
        }
    }

    void restart() throws Exception;

    OsType getOsType();

    boolean checkCanElevate() throws Exception;

    ShellControl elevated(String message, FailableFunction<ShellControl, Boolean, Exception> elevationFunction);

    default ShellControl elevationPassword(SecretValue value) {
        return elevationPassword(() -> value);
    }
    ShellControl elevationPassword(Dialog.FailableSupplier<SecretValue> value);

    ShellControl initWith(String cmds);

    ShellControl initWith(List<String> cmds);

    ShellControl startTimeout(int ms);

    Dialog.FailableSupplier<SecretValue> getElevationPassword();

    default ShellControl subShell(@NonNull ShellDialect type) {
        return subShell(p -> type.getOpenCommand(), new TerminalOpenFunction() {

            @Override
                    public String prepare(ShellControl sc, String command) {
                        return command;
                    }
                })
                .elevationPassword(getElevationPassword());
    }

    interface TerminalOpenFunction {

        String prepare(ShellControl sc, String command) throws Exception;
    }

    default ShellControl identicalSubShell() {
        return subShell(p -> p.getShellDialect().getOpenCommand(), new TerminalOpenFunction() {

            @Override
                    public String prepare(ShellControl sc, String command) {
                        return command;
                    }
                })
                .elevationPassword(getElevationPassword());
    }

    default ShellControl subShell(@NonNull String command) {
        return subShell(processControl -> command, new TerminalOpenFunction() {

            @Override
            public String prepare(ShellControl sc, String command) {
                return command;
            }
        });
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
    ShellControl start() throws Exception;

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
