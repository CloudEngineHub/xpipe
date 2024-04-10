package io.xpipe.app.terminal;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.XPipeInstallation;

public interface KittyTerminalType extends ExternalTerminalType {

    public static final ExternalTerminalType KITTY_LINUX = new Linux();
    public static final ExternalTerminalType KITTY_MACOS = new MacOs();

    private static FilePath getSocket() throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var temp = ShellTemp.getUserSpecificTempDataDirectory(sc, null);
            sc.executeSimpleCommand(sc.getShellDialect().getMkdirsCommand(temp.toString()));
            return temp.join("xpipe_kitty");
        }
    }

    private static void open(ExternalTerminalType.LaunchConfiguration configuration, CommandBuilder socketWrite)
            throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var payload = JsonNodeFactory.instance.objectNode();
            var args = configuration.getDialectLaunchCommand().buildBaseParts(sc);
            var argsArray = payload.putArray("args");
            args.forEach(argsArray::add);
            payload.put("tab_title", configuration.getColoredTitle());
            payload.put("type", "tab");
            payload.put("logo_alpha", 0.01);
            payload.put(
                    "logo", XPipeInstallation.getLocalDefaultInstallationIcon().toString());

            var json = JsonNodeFactory.instance.objectNode();
            json.put("cmd", "launch");
            json.set("payload", payload);
            json.putArray("version").add(0).add(14).add(2);
            var jsonString = json.toString();
            var echoString = "'\\eP@kitty-cmd" + jsonString + "\\e\\\\'";

            sc.executeSimpleCommand(CommandBuilder.of()
                    .add("echo", "-en", echoString, "|")
                    .add(socketWrite)
                    .addFile(getSocket()));
        }
    }

    private static void closeInitial(CommandBuilder socketWrite) throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var payload = JsonNodeFactory.instance.objectNode();
            payload.put("match", "not recent:0");

            var json = JsonNodeFactory.instance.objectNode();
            json.put("cmd", "close-tab");
            json.set("payload", payload);
            json.putArray("version").add(0).add(14).add(2);
            var jsonString = json.toString();
            var echoString = "'\\eP@kitty-cmd" + jsonString + "\\e\\\\'";

            sc.executeSimpleCommand(CommandBuilder.of()
                    .add("echo", "-en", echoString, "|")
                    .add(socketWrite)
                    .addFile(getSocket()));
        }
    }

    @Override
    default boolean supportsTabs() {
        return true;
    }

    @Override
    default String getWebsite() {
        return "https://github.com/kovidgoyal/kitty";
    }

    @Override
    default boolean isRecommended() {
        return true;
    }

    @Override
    default boolean supportsColoredTitle() {
        return true;
    }

    class Linux implements KittyTerminalType {

        @Override
        public String getId() {
            return "app.kitty";
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            try (var sc = LocalShell.getShell().start()) {
                CommandSupport.isInPathOrThrow(sc, "kitty", "Kitty", null);
                CommandSupport.isInPathOrThrow(sc, "socat", "socat", null);
            }

            var toClose = prepare();
            var socketWrite = CommandBuilder.of().add("socat", "-");
            open(configuration, socketWrite);
            if (toClose) {
                closeInitial(socketWrite);
            }
        }

        private boolean prepare() throws Exception {
            var socket = getSocket();
            try (var sc = LocalShell.getShell().start()) {
                if (sc.executeSimpleBooleanCommand(
                        "test -w " + sc.getShellDialect().fileArgument(socket))) {
                    return false;
                }

                sc.executeSimpleCommand(CommandBuilder.of()
                        .add("kitty")
                        .add(
                                "-o",
                                "allow_remote_control=socket-only",
                                "--listen-on",
                                "unix:" + getSocket(),
                                "--detach"));
                ThreadHelper.sleep(1500);
                return true;
            }
        }
    }

    class MacOs extends MacOsType implements KittyTerminalType {

        public MacOs() {
            super("app.kitty", "kitty");
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            try (var sc = LocalShell.getShell().start()) {
                CommandSupport.isInPathOrThrow(sc, "nc", "Netcat", null);
            }

            var toClose = prepare();
            var socketWrite = CommandBuilder.of().add("nc", "-U");
            open(configuration, socketWrite);
            if (toClose) {
                closeInitial(socketWrite);
            }
        }

        private boolean prepare() throws Exception {
            var socket = getSocket();
            try (var sc = LocalShell.getShell().start()) {
                if (sc.executeSimpleBooleanCommand(
                        "test -w " + sc.getShellDialect().fileArgument(socket))) {
                    return false;
                }

                sc.executeSimpleCommand(CommandBuilder.of()
                        .add("open", "-a", "kitty.app", "--args")
                        .add("-o", "allow_remote_control=socket-only", "--listen-on", "unix:" + getSocket()));
                ThreadHelper.sleep(1000);
                return true;
            }
        }
    }
}
