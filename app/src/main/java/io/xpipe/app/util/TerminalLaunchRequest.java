package io.xpipe.app.util;

import io.xpipe.beacon.BeaconServerException;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@Value
public class TerminalLaunchRequest {

    UUID request;
    ProcessControl processControl;
    TerminalInitScriptConfig config;
    String workingDirectory;

    @Setter
    @NonFinal
    TerminalLaunchResult result;

    @Setter
    @NonFinal
    boolean setupCompleted;


    public Path waitForCompletion() throws BeaconServerException {
        while (true) {
            if (getResult() == null) {
                ThreadHelper.sleep(10);
                continue;
            }

            var r = getResult();
            if (r instanceof TerminalLaunchResult.ResultFailure failure) {
                var t = failure.getThrowable();
                throw new BeaconServerException(t);
            }

            return ((TerminalLaunchResult.ResultSuccess) r).getTargetScript();
        }
    }

    public CountDownLatch setupRequestAsync() {
        var latch = new CountDownLatch(1);
        ThreadHelper.runAsync(() -> {
            setupRequest();
            latch.countDown();
        });
        return latch;
    }

    public void setupRequest() {
        var wd = new WorkingDirectoryFunction() {

            @Override
            public boolean isFixed() {
                return true;
            }

            @Override
            public boolean isSpecified() {
                return workingDirectory != null;
            }

            @Override
            public FilePath apply(ShellControl shellControl) {
                if (workingDirectory == null) {
                    return null;
                }

                return new FilePath(workingDirectory);
            }
        };

        try {
            var file = ScriptHelper.createLocalExecScript(processControl.prepareTerminalOpen(config, wd));
            setResult(new TerminalLaunchResult.ResultSuccess(Path.of(file.toString())));
        } catch (Exception e) {
            setResult(new TerminalLaunchResult.ResultFailure(e));
        }
    }
}
