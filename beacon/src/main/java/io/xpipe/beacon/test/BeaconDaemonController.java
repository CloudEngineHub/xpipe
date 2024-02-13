package io.xpipe.beacon.test;

import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.BeaconServer;
import io.xpipe.core.util.XPipeDaemonMode;
import io.xpipe.core.util.XPipeInstallation;

import java.io.IOException;

public class BeaconDaemonController {

    private static boolean alreadyStarted;

    public static void start(XPipeDaemonMode mode) throws Exception {
        if (BeaconServer.isReachable()) {
            alreadyStarted = true;
            return;
        }

        var custom = false;
        Process process;
        if ((process = BeaconServer.tryStartCustom()) != null) {
            custom = true;
        } else {
            var defaultBase = XPipeInstallation.getLocalDefaultInstallationBasePath();
            process = BeaconServer.start(defaultBase, mode);
        }

        waitForStartup(process, custom);
        if (!BeaconServer.isReachable()) {
            throw new AssertionError();
        }
    }

    public static void stop() throws Exception {
        if (alreadyStarted) {
            return;
        }

        if (!BeaconServer.isReachable()) {
            return;
        }

        var client = BeaconClient.establishConnection(BeaconClient.ApiClientInformation.builder()
                .version("?")
                .language("Java API Test")
                .build());
        if (!BeaconServer.tryStop(client)) {
            throw new AssertionError();
        }
        waitForShutdown();
    }

    private static void waitForStartup(Process process, boolean custom) throws IOException {
        for (int i = 0; i < 160; i++) {
            // Breaks when using nohup & disown
            //            if (process != null && !custom && !process.isAlive()) {
            //                throw new IOException("Daemon start failed");
            //            }

            if (process != null && custom && !process.isAlive() && process.exitValue() != 0) {
                throw new IOException("Custom launch command failed");
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            var s = BeaconClient.tryEstablishConnection(BeaconClient.ApiClientInformation.builder()
                    .version("?")
                    .language("Java")
                    .build());
            if (s.isPresent()) {
                return;
            }
        }

        throw new IOException("Wait for daemon start up timed out");
    }

    private static void waitForShutdown() {
        for (int i = 0; i < 40; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            var r = BeaconServer.isReachable();
            if (!r) {
                return;
            }
        }
    }
}
