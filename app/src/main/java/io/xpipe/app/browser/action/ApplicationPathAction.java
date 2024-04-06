package io.xpipe.app.browser.action;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;

import java.util.List;

public interface ApplicationPathAction extends BrowserAction {

    String getExecutable();

    @Override
    default boolean isActive(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return model.getCache().isApplicationInPath(getExecutable());
    }
}
