package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.UserReportComp;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.FileOpener;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.LocalStore;
import io.xpipe.core.util.XPipeInstallation;

public class TroubleshootCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "troubleshoot";
    }

    @Override
    protected Comp<?> create() {
        OptionsBuilder b = new OptionsBuilder()
                .addTitle("troubleshootingOptions")
                .spacer(30)
                .addComp(
                        new TileButtonComp("reportIssue", "reportIssueDescription", "mdal-bug_report", e -> {
                                    var event = ErrorEvent.fromMessage("User Report");
                                    if (AppLogs.get().isWriteToFile()) {
                                        event.attachment(AppLogs.get().getSessionLogsDirectory());
                                    }
                                    UserReportComp.show(event.build());
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .separator()
                .addComp(
                        new TileButtonComp("launchDebugMode", "launchDebugModeDescription", "mdmz-refresh", e -> {
                                    OperationMode.executeAfterShutdown(() -> {
                                        try (var sc = new LocalStore().control().start()) {
                                            var script = FileNames.join(
                                                    XPipeInstallation.getCurrentInstallationBasePath()
                                                            .toString(),
                                                    XPipeInstallation.getDaemonDebugScriptPath(OsType.getLocal()));
                                            var runScript = sc.getShellDialect().runScriptCommand(sc, script);
                                            TerminalLauncher.openDirect("XPipe Debug", sc, runScript);
                                        }
                                    });
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .separator();

        if (AppLogs.get().isWriteToFile()) {
            b.addComp(
                            new TileButtonComp(
                                            "openCurrentLogFile",
                                            "openCurrentLogFileDescription",
                                            "mdmz-text_snippet",
                                            e -> {
                                                FileOpener.openInTextEditor(AppLogs.get()
                                                        .getSessionLogsDirectory()
                                                        .resolve("xpipe.log")
                                                        .toString());
                                                e.consume();
                                            })
                                    .grow(true, false),
                            null)
                    .separator();
        }

        b.addComp(
                        new TileButtonComp(
                                        "openInstallationDirectory",
                                        "openInstallationDirectoryDescription",
                                        "mdomz-snippet_folder",
                                        e -> {
                                            DesktopHelper.browsePath(
                                                    XPipeInstallation.getCurrentInstallationBasePath());
                                            e.consume();
                                        })
                                .grow(true, false),
                        null)
                .separator()
                .addComp(
                        new TileButtonComp("clearCaches", "clearCachesDescription", "mdi2t-trash-can-outline", e -> {
                                    ClearCacheAlert.show();
                                    e.consume();
                                })
                                .grow(true, false),
                        null);
        return b.buildComp();
    }
}
