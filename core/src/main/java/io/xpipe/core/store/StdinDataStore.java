package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.util.JacksonizedValue;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.FilterInputStream;
import java.io.InputStream;

@JsonTypeName("stdin")
@SuperBuilder
@Jacksonized
public class StdinDataStore extends JacksonizedValue implements StreamDataStore {

    @Override
    public boolean isContentExclusivelyAccessible() {
        return true;
    }

    @Override
    public InputStream openInput() {
        var in = System.in;
        // Prevent closing the standard in when the returned input stream is closed
        return new FilterInputStream(in) {
            @Override
            public void close() {}
        };
    }
}
