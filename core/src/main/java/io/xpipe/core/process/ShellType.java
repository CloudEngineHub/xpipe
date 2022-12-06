package io.xpipe.core.process;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.charsetter.NewLine;

import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ShellType {

    String getScriptFileEnding();

    default String commandWithVariable(String key, String value, String command) {
        return joinCommands(getSetVariableCommand(key, value), command);
    }

    String getPauseCommand();

    String createInitFileContent(String command);

    String getOpenWithInitFileCommand(String file);

    default String flatten(List<String> command) {
        return command.stream().map(s -> s.contains(" ") && !(s.startsWith("\"") && s.endsWith("\"")) ? "\"" + s + "\"" : s).collect(Collectors.joining(" "));
    }


    default String joinCommands(String... s) {
        return String.join(getConcatenationOperator(), s);
    }

    String escape(String input);

    void elevate(ShellProcessControl control, String command, String displayCommand) throws Exception;

    default String getExitCommand() {
        return "exit";
    }

    String getExitCodeVariable();

    default String getConcatenationOperator() {
        return ";";
    }

    default String getAndConcatenationOperator() {
        return "&&";
    }

    default String getOrConcatenationOperator() {
        return "||";
    }

    String getMakeExecutableCommand(String file);

    String elevateConsoleCommand(ShellProcessControl control, String command);

    String getEchoCommand(String s, boolean toErrorStream);

    String queryShellProcessId(ShellProcessControl control) throws Exception;

    String getSetVariableCommand(String variableName, String value);

    String getPrintVariableCommand(String name);

    List<String> openCommand();

    String switchTo(String cmd);

    List<String> createMkdirsCommand(String dirs);

    List<String> createFileReadCommand(String file);

    String createFileWriteCommand(String file);

    String createFileExistsCommand(String file);

    Charset determineCharset(ShellProcessControl control) throws Exception;

    NewLine getNewLine();

    String getName();

    String getDisplayName();

    String getExecutable();

    boolean echoesInput();
}
