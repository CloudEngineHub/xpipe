package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public abstract class ExternalApplicationType implements PrefsChoiceValue {

    private final String id;

    public ExternalApplicationType(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public abstract boolean isSelectable();

    public abstract boolean isAvailable();

    @Override
    public String toString() {
        return getId();
    }

    public static class MacApplication extends ExternalApplicationType {

        protected final String applicationName;

        public MacApplication(String id, String applicationName) {
            super(id);
            this.applicationName = applicationName;
        }

        protected Optional<Path> getApplicationPath() {
            try (ShellControl pc = LocalStore.getShell().start()) {
                try (var c = pc.command(String.format(
                                "/System/Library/Frameworks/CoreServices.framework/Versions/A/Frameworks/LaunchServices.framework/Versions/A/Support/lsregister "
                                        + "-dump | grep -o \"/.*%s.app\" | grep -v -E \"Caches|TimeMachine|Temporary|.Trash|/Volumes/%s\" | uniq",
                                applicationName, applicationName))
                        .start()) {
                    var path = c.readStdoutDiscardErr();
                    if (c.getExitCode() != 0 || path.isBlank()) {
                        return Optional.empty();
                    }
                    return Optional.of(Path.of(path));
                }
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return Optional.empty();
            }
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.MACOS);
        }

        @Override
        public boolean isAvailable() {
            return getApplicationPath().isPresent();
        }
    }

    public abstract static class PathApplication extends ExternalApplicationType {

        protected final String executable;

        public PathApplication(String id, String executable) {
            super(id);
            this.executable = executable;
        }

        public boolean isAvailable() {
            try (ShellControl pc = LocalStore.getShell()) {
                return pc.executeSimpleBooleanCommand(pc.getShellDialect().getWhichCommand(executable));
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return false;
            }
        }
    }

    public abstract static class WindowsFullPathType extends ExternalApplicationType {

        public WindowsFullPathType(String id) {
            super(id);
        }

        protected abstract Optional<Path> determinePath();

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.WINDOWS);
        }

        @Override
        public boolean isAvailable() {
            var path = determinePath();
            return path.isPresent() && Files.exists(path.get());
        }
    }
}
