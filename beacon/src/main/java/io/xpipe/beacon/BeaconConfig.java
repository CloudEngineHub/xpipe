package io.xpipe.beacon;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;

@UtilityClass
public class BeaconConfig {

    public static final byte[] BODY_SEPARATOR = "\n\n".getBytes(StandardCharsets.UTF_8);
    public static final String BEACON_PORT_PROP = "io.xpipe.beacon.port";
    public static final int DEFAULT_PORT = System.getProperty("os.name").startsWith("Windows") ? 21721 : 21722;
    private static final String PRINT_MESSAGES_PROPERTY = "io.xpipe.beacon.printMessages";
    private static final String LAUNCH_DAEMON_IN_DEBUG_PROP = "io.xpipe.beacon.launchDebugDaemon";
    private static final String ATTACH_DEBUGGER_PROP = "io.xpipe.beacon.attachDebuggerToDaemon";
    private static final String EXEC_DEBUG_PROP = "io.xpipe.beacon.printDaemonOutput";
    private static final String EXEC_PROCESS_PROP = "io.xpipe.beacon.customDaemonCommand";
    private static final String DAEMON_ARGUMENTS_PROP = "io.xpipe.beacon.daemonArgs";
    private static final String LOCAL_PROXY_PROP = "io.xpipe.beacon.localProxy";

    public static boolean localProxy() {
        if (System.getProperty(LOCAL_PROXY_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(LOCAL_PROXY_PROP));
        }
        return false;
    }

    public static boolean printMessages() {
        if (System.getProperty(PRINT_MESSAGES_PROPERTY) != null) {
            return Boolean.parseBoolean(System.getProperty(PRINT_MESSAGES_PROPERTY));
        }
        return false;
    }

    public static boolean launchDaemonInDebugMode() {
        if (System.getProperty(LAUNCH_DAEMON_IN_DEBUG_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(LAUNCH_DAEMON_IN_DEBUG_PROP));
        }
        return false;
    }

    public static boolean attachDebuggerToDaemon() {
        if (System.getProperty(ATTACH_DEBUGGER_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(ATTACH_DEBUGGER_PROP));
        }
        return false;
    }

    public static boolean printDaemonOutput() {
        if (System.getProperty(EXEC_DEBUG_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(EXEC_DEBUG_PROP));
        }
        return false;
    }

    public static int getUsedPort() {
        if (System.getProperty(BEACON_PORT_PROP) != null) {
            return Integer.parseInt(System.getProperty(BEACON_PORT_PROP));
        }

        return DEFAULT_PORT;
    }

    public static String getCustomDaemonCommand() {
        if (System.getProperty(EXEC_PROCESS_PROP) != null) {
            return System.getProperty(EXEC_PROCESS_PROP);
        }

        return null;
    }

    public static String getDaemonArguments() {
        if (System.getProperty(DAEMON_ARGUMENTS_PROP) != null) {
            return System.getProperty(DAEMON_ARGUMENTS_PROP);
        }

        return null;
    }
}
