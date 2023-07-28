package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.ApplicationHelper;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialects;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public interface ExternalEditorType extends PrefsChoiceValue {

    ExternalEditorType NOTEPAD = new WindowsFullPathType("app.notepad") {
        @Override
        protected Optional<Path> determinePath() {
            return Optional.of(Path.of(System.getenv("SystemRoot") + "\\System32\\notepad.exe"));
        }
    };

    ExternalEditorType VSCODE_WINDOWS = new WindowsFullPathType("app.vscode") {

        @Override
        public boolean canOpenDirectory() {
            return true;
        }

        @Override
        protected Optional<Path> determinePath() {
            // Try to locate if it is in the Path
            try (var cc = LocalStore.getShell()
                    .command(ShellDialects.getPlatformDefault().getWhichCommand("code.cmd"))
                    .start()) {
                var out = cc.readStdoutDiscardErr();
                var exit = cc.getExitCode();
                if (exit == 0) {
                    var first = out.lines().findFirst();
                    if (first.isPresent()) {
                        return first.map(Path::of);
                    }
                }
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).omit().handle();
            }

            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("Microsoft VS Code")
                    .resolve("bin")
                    .resolve("code.cmd"));
        }

        @Override
        public boolean detach() {
            return false;
        }
    };
    ExternalEditorType NOTEPADPLUSPLUS_WINDOWS = new WindowsFullPathType("app.notepad++") {

        @Override
        protected Optional<Path> determinePath() {
            Optional<String> launcherDir;
            launcherDir = WindowsRegistry.readString(WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Notepad++", null)
                    .map(p -> p + "\\notepad++.exe");
            return launcherDir.map(Path::of);
        }
    };

    LinuxPathType VSCODE_LINUX = new LinuxPathType("app.vscode", "code") {

        @Override
        public boolean canOpenDirectory() {
            return true;
        }
    };

    LinuxPathType KATE = new LinuxPathType("app.kate", "kate");

    LinuxPathType GEDIT = new LinuxPathType("app.gedit", "gedit");

    LinuxPathType LEAFPAD = new LinuxPathType("app.leafpad", "leafpad");

    LinuxPathType MOUSEPAD = new LinuxPathType("app.mousepad", "mousepad");

    LinuxPathType PLUMA = new LinuxPathType("app.pluma", "pluma");

    class MacOsEditor extends ExternalApplicationType.MacApplication implements ExternalEditorType {

        public MacOsEditor(String id, String applicationName) {
            super(id, applicationName);
        }

        @Override
        public void launch(Path file) throws Exception {
            var execFile = getApplicationPath();
            if (execFile.isEmpty()) {
                throw new IOException("Application " + applicationName + ".app not found");
            }

            ApplicationHelper.executeLocalApplication(
                    shellControl -> String.format(
                            "open -a %s %s",
                            shellControl
                                    .getShellDialect()
                                    .fileArgument(execFile.orElseThrow().toString()),
                            shellControl.getShellDialect().fileArgument(file.toString())),
                    false);
        }
    }

    ExternalEditorType TEXT_EDIT = new MacOsEditor("app.textEdit", "TextEdit");

    ExternalEditorType BBEDIT = new MacOsEditor("app.bbedit", "BBEdit");

    ExternalEditorType SUBLIME_MACOS = new MacOsEditor("app.sublime", "Sublime Text");

    ExternalEditorType VSCODE_MACOS = new MacOsEditor("app.vscode", "Visual Studio Code") {

        @Override
        public boolean canOpenDirectory() {
            return true;
        }
    };

    ExternalEditorType CUSTOM = new ExternalEditorType() {

        @Override
        public void launch(Path file) throws Exception {
            var customCommand = AppPrefs.get().customEditorCommand().getValue();
            if (customCommand == null || customCommand.isBlank()) {
                throw new IllegalStateException("No custom editor command specified");
            }

            var format = customCommand.toLowerCase(Locale.ROOT).contains("$file") ? customCommand : customCommand + " $FILE";
            ApplicationHelper.executeLocalApplication(sc -> ApplicationHelper.replaceFileArgument(format, "FILE", file.toString()), true);
        }

        @Override
        public boolean isSelectable() {
            return true;
        }

        @Override
        public String getId() {
            return "app.custom";
        }
    };

    void launch(Path file) throws Exception;

    default boolean canOpenDirectory() {
        return false;
    }

    class LinuxPathType extends ExternalApplicationType.PathApplication implements ExternalEditorType {

        public LinuxPathType(String id, String command) {
            super(id, command);
        }

        @Override
        public void launch(Path file) throws IOException {
            new ProcessBuilder(List.of(executable, file.toString())).start();
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    }

    abstract class WindowsFullPathType extends ExternalApplicationType.WindowsFullPathType
            implements ExternalEditorType {

        public WindowsFullPathType(String id) {
            super(id);
        }

        public boolean detach() {
            return true;
        }

        @Override
        public void launch(Path file) throws Exception {
            var path = determinePath();
            if (path.isEmpty()) {
                throw new IOException("Unable to find installation of " + toTranslatedString());
            }

            ApplicationHelper.executeLocalApplication(
                    sc -> String.format(
                            "%s %s",
                            sc.getShellDialect().fileArgument(path.get().toString()),
                            sc.getShellDialect().fileArgument(file.toString())),
                    detach());
        }
    }

    List<ExternalEditorType> WINDOWS_EDITORS = List.of(VSCODE_WINDOWS, NOTEPADPLUSPLUS_WINDOWS, NOTEPAD);
    List<LinuxPathType> LINUX_EDITORS = List.of(VSCODE_LINUX, KATE, GEDIT, PLUMA, LEAFPAD, MOUSEPAD);
    List<ExternalEditorType> MACOS_EDITORS = List.of(BBEDIT, VSCODE_MACOS, SUBLIME_MACOS, TEXT_EDIT);

    List<ExternalEditorType> ALL = ((Supplier<List<ExternalEditorType>>) () -> {
                var all = new ArrayList<ExternalEditorType>();
                if (OsType.getLocal().equals(OsType.WINDOWS)) {
                    all.addAll(WINDOWS_EDITORS);
                }
                if (OsType.getLocal().equals(OsType.LINUX)) {
                    all.addAll(LINUX_EDITORS);
                }
                if (OsType.getLocal().equals(OsType.MACOS)) {
                    all.addAll(MACOS_EDITORS);
                }
                all.add(CUSTOM);
                return all;
            })
            .get();

    static void detectDefault() {
        var typeProperty = AppPrefs.get().externalEditor;
        var customProperty = AppPrefs.get().customEditorCommand;
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            typeProperty.set(WINDOWS_EDITORS.stream()
                    .filter(externalEditorType -> externalEditorType.isAvailable())
                    .findFirst()
                    .orElse(null));
        }

        if (OsType.getLocal().equals(OsType.LINUX)) {
            var env = System.getenv("VISUAL");
            if (env != null) {
                var found = LINUX_EDITORS.stream()
                        .filter(externalEditorType -> externalEditorType.executable.equalsIgnoreCase(env))
                        .findFirst()
                        .orElse(null);
                if (found == null) {
                    typeProperty.set(CUSTOM);
                    customProperty.set(env);
                } else {
                    typeProperty.set(found);
                }
            } else {
                typeProperty.set(LINUX_EDITORS.stream()
                        .filter(externalEditorType -> externalEditorType.isAvailable())
                        .findFirst()
                        .orElse(null));
            }
        }

        if (OsType.getLocal().equals(OsType.MACOS)) {
            typeProperty.set(MACOS_EDITORS.stream()
                    .filter(externalEditorType -> externalEditorType.isAvailable())
                    .findFirst()
                    .orElse(null));
        }
    }
}
