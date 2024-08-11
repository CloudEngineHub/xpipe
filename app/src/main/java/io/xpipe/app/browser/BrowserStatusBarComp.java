package io.xpipe.app.browser;

import io.xpipe.app.browser.file.BrowserContextMenu;
import io.xpipe.app.browser.file.BrowserFileListCompEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.util.HumanReadableFormat;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class BrowserStatusBarComp extends SimpleComp {

    OpenFileSystemModel model;

    @Override
    protected Region createSimple() {
        var bar = new HorizontalComp(List.of(
                createProgressNameStatus(),
                createProgressStatus(),
                createProgressEstimateStatus(),
                Comp.hspacer(),
                createClipboardStatus(),
                createSelectionStatus()));
        bar.spacing(15);
        bar.styleClass("status-bar");

        var r = bar.createRegion();
        r.setOnDragDetected(event -> {
            event.consume();
            r.startFullDrag();
        });
        AppFont.small(r);
        simulateEmptyCell(r);
        return r;
    }

    private Comp<?> createProgressEstimateStatus() {
        var text = BindingsHelper.map(model.getProgress(), p -> {
            if (p == null || p.done()) {
                return null;
            } else {
                var expected = p.expectedTimeRemaining();
                var show = p.elapsedTime().compareTo(Duration.of(200, ChronoUnit.MILLIS)) > 0
                        && (p.getTotal() > 50_000_000 || expected.toMillis() > 5000);
                var time = show ? HumanReadableFormat.duration(p.expectedTimeRemaining()) : "";
                return time;
            }
        });
        var progressComp = new LabelComp(text)
                .styleClass("progress")
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .prefWidth(90);
        return progressComp;
    }

    private Comp<?> createProgressStatus() {
        var text = BindingsHelper.map(model.getProgress(), p -> {
            if (p == null || p.done()) {
                return null;
            } else {
                var transferred = HumanReadableFormat.progressByteCount(p.getTransferred());
                var all = HumanReadableFormat.byteCount(p.getTotal());
                return transferred + " / " + all;
            }
        });
        var progressComp = new LabelComp(text)
                .styleClass("progress")
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .prefWidth(150);
        return progressComp;
    }

    private Comp<?> createProgressNameStatus() {
        var text = BindingsHelper.map(model.getProgress(), p -> {
            if (p == null || p.done()) {
                return null;
            } else {
                return p.getName();
            }
        });
        var progressComp = new LabelComp(text)
                .styleClass("progress")
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .prefWidth(180);
        return progressComp;
    }

    private Comp<?> createClipboardStatus() {
        var cc = BrowserClipboard.currentCopyClipboard;
        var ccCount = Bindings.createStringBinding(
                () -> {
                    if (cc.getValue() != null && cc.getValue().getEntries().size() > 0) {
                        return cc.getValue().getEntries().size() + " file"
                                + (cc.getValue().getEntries().size() > 1 ? "s" : "") + " in clipboard";
                    } else {
                        return null;
                    }
                },
                cc);
        return new LabelComp(ccCount);
    }

    private Comp<?> createSelectionStatus() {
        var selectedCount = Bindings.createIntegerBinding(
                () -> {
                    return model.getFileList().getSelection().size();
                },
                model.getFileList().getSelection());

        var allCount = Bindings.createIntegerBinding(
                () -> {
                    return model.getFileList().getAll().getValue().size();
                },
                model.getFileList().getAll());
        var selectedComp = new LabelComp(Bindings.createStringBinding(
                () -> {
                    if (selectedCount.getValue() == 0) {
                        return null;
                    } else {
                        return selectedCount.getValue() + " / " + allCount.getValue() + " selected";
                    }
                },
                selectedCount,
                allCount));
        return selectedComp;
    }

    private void simulateEmptyCell(Region r) {
        var emptyEntry = new BrowserFileListCompEntry(null, r, null, model.getFileList());
        r.setOnMouseClicked(e -> {
            emptyEntry.onMouseClick(e);
        });
        r.setOnMouseDragEntered(event -> {
            emptyEntry.onMouseDragEntered(event);
        });
        r.setOnDragOver(event -> {
            emptyEntry.onDragOver(event);
        });
        r.setOnDragEntered(event -> {
            emptyEntry.onDragEntered(event);
        });
        r.setOnDragDetected(event -> {
            emptyEntry.startDrag(event);
        });
        r.setOnDragExited(event -> {
            emptyEntry.onDragExited(event);
        });
        r.setOnDragDropped(event -> {
            emptyEntry.onDragDrop(event);
        });
        r.setOnDragDone(event -> {
            emptyEntry.onDragDone(event);
        });

        // Use status bar as an extension of file list
        new ContextMenuAugment<>(
                        mouseEvent -> mouseEvent.getButton() == MouseButton.SECONDARY,
                        null,
                        () -> new BrowserContextMenu(model, null, false))
                .augment(new SimpleCompStructure<>(r));
    }
}
