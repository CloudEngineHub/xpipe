package io.xpipe.app.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.util.XPipeInstallation;

import java.util.Map;
import java.util.Optional;

public class NativeBridge {

    private static MacOsLibrary macOsLibrary;
    private static boolean loadingFailed;

    public static Optional<MacOsLibrary> getMacOsLibrary() {
        if (macOsLibrary == null && !loadingFailed) {
            try {
                System.setProperty("jna.library.path", XPipeInstallation.getCurrentInstallationBasePath()
                        .resolve("Contents").resolve("runtime").resolve("Contents").resolve("Home").resolve("lib").toString());
                var l = Native.load("xpipe_bridge", MacOsLibrary.class, Map.of());
                macOsLibrary = l;
            } catch (Throwable t) {
                ErrorEvent.fromThrowable(t).handle();
                loadingFailed = true;
            }
        }
        return Optional.ofNullable(macOsLibrary);
    }

    public static interface MacOsLibrary extends Library {

        public abstract void setAppearance(NativeLong window, boolean seamlessFrame, boolean dark);
    }
}
