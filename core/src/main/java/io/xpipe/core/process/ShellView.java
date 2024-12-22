package io.xpipe.core.process;

import io.xpipe.core.store.FilePath;

import java.io.InputStream;

public class ShellView {

    protected final ShellControl shellControl;

    public ShellView(ShellControl shellControl) {this.shellControl = shellControl;}

    protected ShellDialect getDialect() {
        return shellControl.getShellDialect();
    }

    public void writeTextFile(FilePath path, String text) throws Exception {
        var cc = getDialect().createTextFileWriteCommand(shellControl,text,path.toString());
        cc.execute();
    }

    public void writeScriptFile(FilePath path, String text) throws Exception {
        var cc = getDialect().createScriptTextFileWriteCommand(shellControl,text,path.toString());
        cc.execute();
    }

    public void writeStreamFile(FilePath path, InputStream inputStream, long size) throws Exception {
        try (var out = getDialect().createStreamFileWriteCommand(shellControl,path.toString(), size).startExternalStdin()) {
            inputStream.transferTo(out);
        }
    }
}
