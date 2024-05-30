package io.xpipe.app.browser.session;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.fxcomps.util.ListBindingsHelper;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.FileReference;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.util.FailableFunction;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
public class BrowserFileChooserModel extends BrowserAbstractSessionModel<OpenFileSystemModel> {

    private final OpenFileSystemModel.SelectionMode selectionMode;
    private final ObservableList<BrowserEntry> fileSelection = FXCollections.observableArrayList();

    @Setter
    private Consumer<List<FileReference>> onFinish;

    public BrowserFileChooserModel(OpenFileSystemModel.SelectionMode selectionMode) {
        this.selectionMode = selectionMode;
        selectedEntry.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                fileSelection.clear();
                return;
            }

            ListBindingsHelper.bindContent(fileSelection, newValue.getFileList().getSelection());
        });
    }

    public void finishChooser() {
        var chosen = new ArrayList<>(fileSelection);

        synchronized (BrowserFileChooserModel.this) {
            var open = selectedEntry.getValue();
            if (open != null) {
                ThreadHelper.runAsync(() -> {
                    open.close();
                });
            }
        }

        var stores = chosen.stream()
                .map(entry -> new FileReference(
                        selectedEntry.getValue().getEntry(),
                        entry.getRawFileEntry().getPath()))
                .toList();
        onFinish.accept(stores);
    }

    public void openFileSystemAsync(
            DataStoreEntryRef<? extends FileSystemStore> store,
            FailableFunction<OpenFileSystemModel, String, Exception> path,
            BooleanProperty externalBusy) {
        if (store == null) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            OpenFileSystemModel model;

            try (var b = new BooleanScope(externalBusy != null ? externalBusy : new SimpleBooleanProperty()).start()) {
                model = new OpenFileSystemModel(this, store, selectionMode);
                model.init();
                // Prevent multiple calls from interfering with each other
                synchronized (BrowserFileChooserModel.this) {
                    selectedEntry.setValue(model);
                    sessionEntries.add(model);
                }
                if (path != null) {
                    model.initWithGivenDirectory(FileNames.toDirectory(path.apply(model)));
                } else {
                    model.initWithDefaultDirectory();
                }
            }
        });
    }
}
