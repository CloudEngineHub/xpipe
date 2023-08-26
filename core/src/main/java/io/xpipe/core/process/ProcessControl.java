package io.xpipe.core.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;

public interface ProcessControl extends AutoCloseable {

    @FunctionalInterface
    interface ExceptionConverter {
        Exception convert(Exception t);
    }

    ProcessControl withExceptionConverter(ExceptionConverter converter);

    void resetData();

    ExecutorService getStdoutReader();

    ExecutorService getStderrReader();

    String prepareTerminalOpen(String displayName) throws Exception;

    void closeStdin() throws IOException;

    boolean isStdinClosed();

    boolean isRunning();

    ShellDialect getShellDialect();

    void writeLine(String line) throws IOException;

    void write(byte[] b) throws IOException;

    @Override
    void close() throws Exception;

    void kill() throws Exception;

    ProcessControl start() throws Exception;

    InputStream getStdout();

    OutputStream getStdin();

    InputStream getStderr();

    Charset getCharset();
}
