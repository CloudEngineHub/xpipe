package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public interface ExternalEditorType extends PrefsChoiceValue {

    ExternalEditorType NOTEPAD = new WindowsType("app.notepad", "notepad", false) {
        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("SystemRoot") + "\\System32\\notepad.exe"));
        }
    };

    ExternalEditorType VSCODIUM_WINDOWS = new WindowsType("app.vscodium", "codium.cmd", false) {

        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("VSCodium")
                    .resolve("bin")
                    .resolve("codium.cmd"));
        }
    };

    ExternalEditorType VSCODE_WINDOWS = new WindowsType("app.vscode", "code.cmd", false) {

        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("Microsoft VS Code")
                    .resolve("bin")
                    .resolve("code.cmd"));
        }
    };

    ExternalEditorType VSCODE_INSIDERS_WINDOWS = new WindowsType("app.vscodeInsiders", "code-insiders.cmd", false) {

        @Override
        protected Optional<Path> determineInstallation() {
            return Optional.of(Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("Microsoft VS Code Insiders")
                    .resolve("bin")
                    .resolve("code-insiders.cmd"));
        }
    };

    ExternalEditorType NOTEPADPLUSPLUS = new WindowsType("app.notepad++", "notepad++", false) {

        @Override
        protected Optional<Path> determineInstallation() {
            var found = WindowsRegistry.readString(WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Notepad++", null);

            // Check 32 bit install
            if (found.isEmpty()) {
                found = WindowsRegistry.readString(
                        WindowsRegistry.HKEY_LOCAL_MACHINE, "WOW6432Node\\SOFTWARE\\Notepad++", null);
            }
            return found.map(p -> p + "\\notepad++.exe").map(Path::of);
        }
    };

    LinuxPathType VSCODE_LINUX = new LinuxPathType("app.vscode", "code");

    LinuxPathType VSCODIUM_LINUX = new LinuxPathType("app.vscodium", "codium");

    LinuxPathType GNOME = new LinuxPathType("app.gnomeTextEditor", "gnome-text-editor");

    LinuxPathType KATE = new LinuxPathType("app.kate", "kate");

    LinuxPathType GEDIT = new LinuxPathType("app.gedit", "gedit");

    LinuxPathType LEAFPAD = new LinuxPathType("app.leafpad", "leafpad");

    LinuxPathType MOUSEPAD = new LinuxPathType("app.mousepad", "mousepad");

    LinuxPathType PLUMA = new LinuxPathType("app.pluma", "pluma");
    ExternalEditorType TEXT_EDIT = new MacOsEditor("app.textEdit", "TextEdit");
    ExternalEditorType BBEDIT = new MacOsEditor("app.bbedit", "BBEdit");
    ExternalEditorType SUBLIME_MACOS = new MacOsEditor("app.sublime", "Sublime Text");
    ExternalEditorType VSCODE_MACOS = new MacOsEditor("app.vscode", "Visual Studio Code");
    ExternalEditorType VSCODIUM_MACOS = new MacOsEditor("app.vscodium", "VSCodium");
    ExternalEditorType CUSTOM = new ExternalEditorType() {

        @Override
        public void launch(Path file) throws Exception {
            var customCommand = AppPrefs.get().customEditorCommand().getValue();
            if (customCommand == null || customCommand.isBlank()) {
                throw ErrorEvent.expected(new IllegalStateException("No custom editor command specified"));
            }

            var format =
                    customCommand.toLowerCase(Locale.ROOT).contains("$file") ? customCommand : customCommand + " $FILE";
            ExternalApplicationHelper.startAsync(CommandBuilder.of()
                    .add(ExternalApplicationHelper.replaceFileArgument(format, "FILE", file.toString())));
        }

        @Override
        public String getId() {
            return "app.custom";
        }
    };
    ExternalEditorType FLEET = new GenericPathType("app.fleet", "fleet", false);
    ExternalEditorType INTELLIJ = new GenericPathType("app.intellij", "idea", false);
    ExternalEditorType PYCHARM = new GenericPathType("app.pycharm", "pycharm", false);
    ExternalEditorType WEBSTORM = new GenericPathType("app.webstorm", "webstorm", false);
    ExternalEditorType CLION = new GenericPathType("app.clion", "clion", false);
    List<ExternalEditorType> WINDOWS_EDITORS =
            List.of(VSCODIUM_WINDOWS, VSCODE_INSIDERS_WINDOWS, VSCODE_WINDOWS, NOTEPADPLUSPLUS, NOTEPAD);
    List<LinuxPathType> LINUX_EDITORS =
            List.of(VSCODIUM_LINUX, VSCODE_LINUX, KATE, GEDIT, PLUMA, LEAFPAD, MOUSEPAD, GNOME);
    List<ExternalEditorType> MACOS_EDITORS = List.of(BBEDIT, VSCODIUM_MACOS, VSCODE_MACOS, SUBLIME_MACOS, TEXT_EDIT);
    List<ExternalEditorType> CROSS_PLATFORM_EDITORS = List.of(FLEET, INTELLIJ, PYCHARM, WEBSTORM, CLION);

    @SuppressWarnings("TrivialFunctionalExpressionUsage")
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
                all.addAll(CROSS_PLATFORM_EDITORS);
                all.add(CUSTOM);
                return all;
            })
            .get();

    static void detectDefault() {
        var typeProperty = AppPrefs.get().externalEditor;
        var customProperty = AppPrefs.get().customEditorCommand;
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            typeProperty.set(WINDOWS_EDITORS.stream()
                    .filter(PrefsChoiceValue::isAvailable)
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
                        .filter(ExternalApplicationType.PathApplication::isAvailable)
                        .findFirst()
                        .orElse(null));
            }
        }

        if (OsType.getLocal().equals(OsType.MACOS)) {
            typeProperty.set(MACOS_EDITORS.stream()
                    .filter(PrefsChoiceValue::isAvailable)
                    .findFirst()
                    .orElse(null));
        }
    }

    void launch(Path file) throws Exception;

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

            ExternalApplicationHelper.startAsync(CommandBuilder.of()
                    .add("open", "-a")
                    .addFile(execFile.orElseThrow().toString())
                    .addFile(file.toString()));
        }
    }

    class GenericPathType extends ExternalApplicationType.PathApplication implements ExternalEditorType {

        public GenericPathType(String id, String command, boolean explicityAsync) {
            super(id, command, explicityAsync);
        }

        @Override
        public void launch(Path file) throws Exception {
            var builder = CommandBuilder.of().addFile(executable).addFile(file.toString());
            if (explicityAsync) {
                ExternalApplicationHelper.startAsync(builder);
            } else {
                LocalShell.getShell().executeSimpleCommand(builder);
            }
        }
    }

    class LinuxPathType extends GenericPathType {

        public LinuxPathType(String id, String command) {
            super(id, command, true);
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal().equals(OsType.LINUX);
        }
    }

    abstract class WindowsType extends ExternalApplicationType.WindowsType implements ExternalEditorType {

        private final boolean detach;

        public WindowsType(String id, String executable, boolean detach) {
            super(id, executable);
            this.detach = true;
        }

        @Override
        public void launch(Path file) throws Exception {
            var location = determineFromPath();
            if (location.isEmpty()) {
                location = determineInstallation();
                if (location.isEmpty()) {
                    throw ErrorEvent.expected(new IOException("Unable to find installation of "
                            + toTranslatedString().getValue()));
                }
            }

            var builder = CommandBuilder.of().addFile(location.get().toString()).addFile(file.toString());
            if (detach) {
                ExternalApplicationHelper.startAsync(builder);
            } else {
                LocalShell.getShell().executeSimpleCommand(builder);
            }
        }
    }
}
