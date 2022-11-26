package io.xpipe.api.connector;

import io.xpipe.beacon.*;
import io.xpipe.beacon.exchange.cli.DialogExchange;
import io.xpipe.core.dialog.DialogReference;

import java.util.Optional;

public final class XPipeConnection extends BeaconConnection {

    private XPipeConnection() {}

    public static XPipeConnection open() {
        var con = new XPipeConnection();
        con.constructSocket();
        return con;
    }

    public static void finishDialog(DialogReference reference) {
        try (var con = new XPipeConnection()) {
            con.constructSocket();
            var element = reference.getStart();
            while (true) {
                if (element != null && element.requiresExplicitUserInput()) {
                    throw new IllegalStateException();
                }

                DialogExchange.Response response = con.performSimpleExchange(DialogExchange.Request.builder()
                        .dialogKey(reference.getDialogId())
                        .build());
                element = response.getElement();
                if (response.getElement() == null) {
                    break;
                }
            }
        } catch (BeaconException e) {
            throw e;
        } catch (Exception e) {
            throw new BeaconException(e);
        }
    }

    public static void execute(Handler handler) {
        try (var con = new XPipeConnection()) {
            con.constructSocket();
            handler.handle(con);
        } catch (BeaconException e) {
            throw e;
        } catch (Exception e) {
            throw new BeaconException(e);
        }
    }

    public static <T> T execute(Mapper<T> mapper) {
        try (var con = new XPipeConnection()) {
            con.constructSocket();
            return mapper.handle(con);
        } catch (BeaconException e) {
            throw e;
        } catch (Exception e) {
            throw new BeaconException(e);
        }
    }

    public static Optional<BeaconClient> waitForStartup(Process process) {
        for (int i = 0; i < 160; i++) {
            if (process != null && !process.isAlive() && process.exitValue() != 0) {
                return Optional.empty();
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            var s = BeaconClient.tryConnect(BeaconClient.ApiClientInformation.builder().version("?").language("Java").build());
            if (s.isPresent()) {
                return s;
            }
        }
        return Optional.empty();
    }

    public static void waitForShutdown() {
        for (int i = 0; i < 40; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            var r = BeaconServer.isRunning();
            if (!r) {
                return;
            }
        }
    }

    @Override
    protected void constructSocket() {
        if (!BeaconServer.isRunning()) {
            try {
                start();
            } catch (Exception ex) {
                throw new BeaconException("Unable to start xpipe daemon", ex);
            }

            var r = waitForStartup(null);
            if (r.isEmpty()) {
                throw new BeaconException("Wait for xpipe daemon timed out");
            } else {
                beaconClient = r.get();
                return;
            }
        }

        try {
            beaconClient = BeaconClient.connect(BeaconClient.ApiClientInformation.builder().version("?").language("Java").build());
        } catch (Exception ex) {
            throw new BeaconException("Unable to connect to running xpipe daemon", ex);
        }
    }

    private void start() throws Exception {
        if (BeaconServer.tryStart() == null) {
            throw new UnsupportedOperationException("Unable to determine xpipe daemon launch command");
        }
        ;
    }

    @FunctionalInterface
    public static interface Handler {

        void handle(BeaconConnection con) throws Exception;
    }

    @FunctionalInterface
    public static interface Mapper<T> {

        T handle(BeaconConnection con) throws Exception;
    }
}
