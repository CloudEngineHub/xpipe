package io.xpipe.core.process;

import io.xpipe.core.util.FailableConsumer;
import io.xpipe.core.util.FailableFunction;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.function.Function;

public interface CommandControl extends ProcessControl {

    // Keep these out of a normal exit code range
    int UNASSIGNED_EXIT_CODE = -80001;
    int EXIT_TIMEOUT_EXIT_CODE = -80002;
    int START_FAILED_EXIT_CODE = -80003;
    int INTERNAL_ERROR_EXIT_CODE = -80004;

    enum TerminalExitMode {
        KEEP_OPEN,
        CLOSE
    }

    CommandControl withExceptionConverter(ExceptionConverter converter);

    CommandControl withErrorFormatter(Function<String, String> formatter);

    CommandControl terminalExitMode(TerminalExitMode mode);

    CommandControl doesNotObeyReturnValueConvention();

    CommandControl complex();

    CommandControl notComplex();

    CommandControl withWorkingDirectory(String directory);

    default void execute() throws Exception {
        try (var c = start()) {
            c.discardOrThrow();
        }
    }

    default boolean executeAndCheck() throws Exception {
        try (var c = start()) {
            return c.discardAndCheckExit();
        }
    }

    ShellControl getParent();

    InputStream startExternalStdout() throws Exception;

    OutputStream startExternalStdin() throws Exception;

    boolean waitFor();

    CommandControl withCustomCharset(Charset charset);

    int getExitCode();

    default CommandControl elevated(String message) {
        return elevated(message, (v) -> true);
    }

    CommandControl elevated(String message, FailableFunction<ShellControl, Boolean, Exception> elevationFunction);

    @Override
    CommandControl start() throws Exception;

    CommandControl exitTimeout(Integer timeout);

    void withStdoutOrThrow(FailableConsumer<InputStreamReader, Exception> c);

    String readStdoutDiscardErr() throws Exception;

    void discardOrThrow() throws Exception;

    void accumulateStdout(Consumer<String> con);

    void accumulateStderr(Consumer<String> con);

    byte[] readRawBytesOrThrow() throws Exception;

    String readStdoutOrThrow() throws Exception;

    String readStdoutAndWait() throws Exception;

    default boolean discardAndCheckExit() throws ProcessOutputException {
        try {
            discardOrThrow();
            return true;
        } catch (ProcessOutputException ex) {
            if (ex.isTimeOut()) {
                throw ex;
            }

            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    void discardOut();

    void discardErr();
}
