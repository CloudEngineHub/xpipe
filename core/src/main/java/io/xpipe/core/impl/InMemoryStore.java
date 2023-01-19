package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.core.util.JacksonizedValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.*;

/**
 * A store whose contents are stored in memory.
 */
@JsonTypeName("inMemory")
@SuperBuilder
@Jacksonized
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InMemoryStore extends JacksonizedValue implements StreamDataStore {

    private byte[] value;

    @Override
    public String toString() {
        return value != null && value.length > 100 ? "<memory>" : (value != null ? new String(value) : "null");
    }

    @Override
    public boolean isContentExclusivelyAccessible() {
        return true;
    }

    @Override
    public InputStream openInput() throws Exception {
        return value != null ? new ByteArrayInputStream(value) : InputStream.nullInputStream();
    }

    @Override
    public OutputStream openOutput() throws Exception {
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                InMemoryStore.this.value = this.toByteArray();
            }
        };
    }
}
