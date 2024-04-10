package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionFormatter;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.ClipboardHelper;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileNames;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.List;
import java.util.stream.Collectors;

public class CopyPathAction implements BrowserAction, BranchAction {

    @Override
    public Category getCategory() {
        return Category.COPY_PASTE;
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("copyLocation");
    }

    @Override
    public List<LeafAction> getBranchingActions(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return List.of(
                new LeafAction() {
                    @Override
                    public KeyCombination getShortcut() {
                        return new KeyCodeCombination(KeyCode.C, KeyCombination.ALT_DOWN, KeyCombination.SHORTCUT_DOWN);
                    }

                    @Override
                    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>(BrowserActionFormatter.centerEllipsis(
                                    entries.getFirst().getRawFileEntry().getPath(), 50));
                        }

                        return AppI18n.observable("absolutePaths");
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath())
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new LeafAction() {
                    @Override
                    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>(BrowserActionFormatter.centerEllipsis(
                                    entries.getFirst().getRawFileEntry().getPath(), 50));
                        }

                        return AppI18n.observable("absoluteLinkPaths");
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return entries.stream()
                                .allMatch(browserEntry ->
                                        browserEntry.getRawFileEntry().getKind() == FileKind.LINK);
                    }

                    @Override
                    public boolean automaticallyResolveLinks() {
                        return false;
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath())
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new LeafAction() {
                    @Override
                    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>("\""
                                    + BrowserActionFormatter.centerEllipsis(
                                            entries.getFirst().getRawFileEntry().getPath(), 50)
                                    + "\"");
                        }

                        return AppI18n.observable("absolutePathsQuoted");
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return entries.stream()
                                .anyMatch(entry ->
                                        entry.getRawFileEntry().getPath().contains(" "));
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> "\"" + entry.getRawFileEntry().getPath() + "\"")
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new LeafAction() {
                    @Override
                    public KeyCombination getShortcut() {
                        return new KeyCodeCombination(
                                KeyCode.C, KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN);
                    }

                    @Override
                    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>(BrowserActionFormatter.centerEllipsis(
                                    FileNames.getFileName(
                                            entries.getFirst().getRawFileEntry().getPath()),
                                    50));
                        }

                        return AppI18n.observable("fileNames");
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> FileNames.getFileName(
                                        entry.getRawFileEntry().getPath()))
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new LeafAction() {
                    @Override
                    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>(BrowserActionFormatter.centerEllipsis(
                                    FileNames.getFileName(
                                            entries.getFirst().getRawFileEntry().getPath()),
                                    50));
                        }

                        return AppI18n.observable("linkFileNames");
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return entries.stream()
                                        .allMatch(browserEntry ->
                                                browserEntry.getRawFileEntry().getKind() == FileKind.LINK)
                                && entries.stream().anyMatch(browserEntry -> !browserEntry
                                        .getFileName()
                                        .equals(FileNames.getFileName(browserEntry
                                                .getRawFileEntry()
                                                .resolved()
                                                .getPath())));
                    }

                    @Override
                    public boolean automaticallyResolveLinks() {
                        return false;
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> FileNames.getFileName(
                                        entry.getRawFileEntry().getPath()))
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new LeafAction() {
                    @Override
                    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>("\""
                                    + BrowserActionFormatter.centerEllipsis(
                                            FileNames.getFileName(entries.getFirst()
                                                    .getRawFileEntry()
                                                    .getPath()),
                                            50)
                                    + "\"");
                        }

                        return AppI18n.observable("fileNamesQuoted");
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return entries.stream().anyMatch(entry -> FileNames.getFileName(
                                        entry.getRawFileEntry().getPath())
                                .contains(" "));
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> "\""
                                        + FileNames.getFileName(
                                                entry.getRawFileEntry().getPath()) + "\"")
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                });
    }
}
