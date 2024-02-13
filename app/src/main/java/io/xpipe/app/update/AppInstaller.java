package io.xpipe.app.update;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.LocalStore;
import io.xpipe.core.util.XPipeInstallation;
import lombok.Getter;

import java.nio.file.Files;
import java.nio.file.Path;

public class AppInstaller {

    public static void installFileLocal(InstallerAssetType asset, Path localFile) throws Exception {
        asset.installLocal(localFile.toString());
    }

    public static InstallerAssetType getSuitablePlatformAsset() {
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return new InstallerAssetType.Msi();
        }

        if (OsType.getLocal().equals(OsType.LINUX)) {
            return Files.exists(Path.of("/etc/debian_version"))
                    ? new InstallerAssetType.Debian()
                    : new InstallerAssetType.Rpm();
        }

        if (OsType.getLocal().equals(OsType.MACOS)) {
            return new InstallerAssetType.Pkg();
        }

        throw new AssertionError();
    }

    @Getter
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = InstallerAssetType.Msi.class),
        @JsonSubTypes.Type(value = InstallerAssetType.Debian.class),
        @JsonSubTypes.Type(value = InstallerAssetType.Rpm.class),
        @JsonSubTypes.Type(value = InstallerAssetType.Pkg.class)
    })
    public abstract static class InstallerAssetType {

        public abstract void installLocal(String file) throws Exception;

        public boolean isCorrectAsset(String name) {
            return name.endsWith(getExtension())
                    && name.contains(AppProperties.get().getArch());
        }

        public abstract String getExtension();

        @JsonTypeName("msi")
        public static final class Msi extends InstallerAssetType {

            @Override
            public String getExtension() {
                return ".msi";
            }

            @Override
            public void installLocal(String file) throws Exception {
                var shellProcessControl = new LocalStore().control().start();
                var exec = XPipeInstallation.getCurrentInstallationBasePath().resolve(XPipeInstallation.getDaemonExecutablePath(OsType.getLocal()));
                var logsDir = FileNames.join(XPipeInstallation.getDataDir().toString(), "logs");
                var logFile = FileNames.join(logsDir, "installer_" + FileNames.getFileName(file) + ".log");
                var script = ScriptHelper.createExecScript(
                        shellProcessControl,
                        String.format(
                                """
                                cd /D "%%HOMEDRIVE%%%%HOMEPATH%%"
                                start "" /wait msiexec /i "%s" /l* "%s" /qb
                                start "" "%s"
                                """,
                                file, logFile, exec));
                shellProcessControl.executeSimpleCommand("start \"\" /min \"" + script + "\"");
            }
        }

        @JsonTypeName("debian")
        public static final class Debian extends InstallerAssetType {

            @Override
            public String getExtension() {
                return ".deb";
            }

            @Override
            public void installLocal(String file) throws Exception {
                var name = AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
                var command = new LocalStore()
                        .control()
                        .subShell(ShellDialects.BASH)
                        .command(String.format(
                                """
                                        function exec {
                                            echo "+ sudo apt install \\"%s\\""
                                            DEBIAN_FRONTEND=noninteractive sudo apt-get install -qy "%s" || return 1
                                            %s open || return 1
                                        }

                                        cd ~
                                        exec || read -rsp "Update failed ..."$'\\n' -n 1 key
                                        """,
                                file, file, name));
                TerminalLauncher.open("XPipe Updater", command);
            }
        }

        @JsonTypeName("rpm")
        public static final class Rpm extends InstallerAssetType {
            @Override
            public String getExtension() {
                return ".rpm";
            }

            @Override
            public void installLocal(String file) throws Exception {
                var name = AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
                var command = new LocalStore()
                        .control()
                        .subShell(ShellDialects.BASH)
                        .command(String.format(
                                """
                                        function exec {
                                            echo "+ sudo rpm -U -v --force \\"%s\\""
                                            sudo rpm -U -v --force "%s" || return 1
                                            %s open || return 1
                                        }

                                        cd ~
                                        exec || read -rsp "Update failed ..."$'\\n' -n 1 key
                                        """,
                                file, file, name));
                TerminalLauncher.open("XPipe Updater", command);
            }
        }

        @JsonTypeName("pkg")
        public static final class Pkg extends InstallerAssetType {
            @Override
            public String getExtension() {
                return ".pkg";
            }

            @Override
            public void installLocal(String file) throws Exception {
                var name = AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
                var command = new LocalStore()
                        .control()
                        .command(String.format(
                                """
                                        function exec {
                                            echo "+ sudo installer -verboseR -allowUntrusted -pkg \\"%s\\" -target /"
                                            sudo installer -verboseR -allowUntrusted -pkg "%s" -target / || return 1
                                            %s open || return 1
                                        }

                                        cd ~
                                        exec || echo "Update failed ..." && read -rs -k 1 key
                                        """,
                                file, file, name));
                TerminalLauncher.open("XPipe Updater", command);
            }
        }
    }
}
