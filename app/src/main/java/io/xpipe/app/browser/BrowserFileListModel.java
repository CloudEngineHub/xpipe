package io.xpipe.app.browser;

import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.FileOpener;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.store.FileSystem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Getter
public final class BrowserFileListModel {

    static final Comparator<BrowserEntry> FILE_TYPE_COMPARATOR =
            Comparator.comparing(path -> !path.getRawFileEntry().isDirectory());
    static final Predicate<BrowserEntry> PREDICATE_ANY = path -> true;
    static final Predicate<BrowserEntry> PREDICATE_NOT_HIDDEN = path -> true;

    private final OpenFileSystemModel fileSystemModel;
    private final Property<Comparator<BrowserEntry>> comparatorProperty =
            new SimpleObjectProperty<>(FILE_TYPE_COMPARATOR);
    private final Property<List<BrowserEntry>> all = new SimpleObjectProperty<>(new ArrayList<>());
    private final Property<List<BrowserEntry>> shown = new SimpleObjectProperty<>(new ArrayList<>());
    private final ObjectProperty<Predicate<BrowserEntry>> predicateProperty =
            new SimpleObjectProperty<>(path -> true);
    private final ObservableList<BrowserEntry> selection = FXCollections.observableArrayList();
    private final ObservableList<FileSystem.FileEntry> selectedRaw =
            BindingsHelper.mappedContentBinding(selection, entry -> entry.getRawFileEntry());

    private final Property<BrowserEntry> draggedOverDirectory = new SimpleObjectProperty<BrowserEntry>();
    private final Property<Boolean> draggedOverEmpty = new SimpleBooleanProperty();
    private final Property<BrowserEntry> editing = new SimpleObjectProperty<>();

    public BrowserFileListModel(OpenFileSystemModel fileSystemModel) {
        this.fileSystemModel = fileSystemModel;

        fileSystemModel.getFilter().addListener((observable, oldValue, newValue) -> {
            refreshShown();
        });
    }

    public BrowserModel.Mode getMode() {
        return fileSystemModel.getBrowserModel().getMode();
    }

    public void setAll(Stream<FileSystem.FileEntry> newFiles) {
        try (var s = newFiles) {
            var parent = fileSystemModel.getCurrentParentDirectory();
            var l = Stream.concat(
                            parent != null ? Stream.of(new BrowserEntry(parent, this, true)) : Stream.of(),
                            s.filter(entry -> entry != null)
                                    .limit(5000)
                                    .map(entry -> new BrowserEntry(entry, this, false)))
                    .toList();
            all.setValue(l);
            refreshShown();
        }
    }

    public void setComparator(Comparator<BrowserEntry> comparator) {
        comparatorProperty.setValue(comparator);
        refreshShown();
    }

    private void refreshShown() {
        List<BrowserEntry> filtered = fileSystemModel.getFilter().getValue() != null
                ? all.getValue().stream()
                        .filter(entry -> {
                            var name = FileNames.getFileName(
                                            entry.getRawFileEntry().getPath())
                                    .toLowerCase(Locale.ROOT);
                            var filterString =
                                    fileSystemModel.getFilter().getValue().toLowerCase(Locale.ROOT);
                            return name.contains(filterString);
                        })
                        .toList()
                : all.getValue();

        Comparator<BrowserEntry> tableComparator = comparatorProperty.getValue();
        var comparator =
                tableComparator != null ? FILE_TYPE_COMPARATOR.thenComparing(tableComparator) : FILE_TYPE_COMPARATOR;
        var listCopy = new ArrayList<>(filtered);
        listCopy.sort(comparator);
        shown.setValue(listCopy);
    }

    public boolean rename(String filename, String newName) {
        var fullPath = FileNames.join(fileSystemModel.getCurrentPath().get(), filename);
        var newFullPath = FileNames.join(fileSystemModel.getCurrentPath().get(), newName);
        try {
            fileSystemModel.getFileSystem().move(fullPath, newFullPath);
            fileSystemModel.refresh();
            return true;
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            return false;
        }
    }

    public void onDoubleClick(BrowserEntry entry) {
        if (!entry.getRawFileEntry().isDirectory() && getMode().equals(BrowserModel.Mode.SINGLE_FILE_CHOOSER)) {
            getFileSystemModel().getBrowserModel().finishChooser();
            return;
        }

        if (entry.getRawFileEntry().isDirectory()) {
            var dir = fileSystemModel.cd(entry.getRawFileEntry().getPath());
            if (dir.isPresent()) {
                fileSystemModel.cd(dir.get());
            }
        } else {
            FileOpener.openInTextEditor(entry.getRawFileEntry());
        }
    }

    public ObjectProperty<Predicate<BrowserEntry>> predicateProperty() {
        return predicateProperty;
    }
}
