package io.xpipe.core.store;

import io.xpipe.core.process.ProcessControl;
import io.xpipe.core.process.ShellControl;

public interface ShellStore extends DataStore, FileSystemStore, ValidatableStore<ShellValidationContext> {

    @Override
    default FileSystem createFileSystem() {
        return new ConnectionFileSystem(control());
    }

    default ProcessControl prepareLaunchCommand() {
        return control();
    }

    ShellControl control();

    @Override
    default void validate(ShellValidationContext context) throws Exception {
        var c = context.get();
        if (!isInStorage()) {
            c.withoutLicenseCheck();
        }
        try (ShellControl pc = c.start()) {}
    }

    @Override
    default ShellValidationContext createContext() throws Exception {
        return new ShellValidationContext(control().start());
    }
}
