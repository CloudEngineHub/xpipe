package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.impl.TextSource;
import io.xpipe.core.impl.XpbtSource;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.JacksonizedValue;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Represents a formal description on what exactly makes up the
 * actual data source and how to access/locate it for a given data store.
 * <p>
 * This instance is only valid in combination with its associated data store instance.
 */
@SuperBuilder
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
public abstract class DataSource<DS extends DataStore> extends JacksonizedValue {

    protected DS store;

    public static DataSource<?> createInternalDataSource(DataSourceType t, DataStore store) {
        try {
            return switch (t) {
                case TABLE -> XpbtSource.builder().store(store.asNeeded()).build();
                case STRUCTURE -> null;
                case TEXT -> TextSource.builder()
                        .store(store.asNeeded())
                        .newLine(NewLine.LF)
                        .charset(StreamCharset.UTF8)
                        .build();
                case RAW -> null;
                // TODO
                case COLLECTION -> null;
            };
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    public boolean isComplete() {
        try {
            checkComplete();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public void checkComplete() throws Exception {
        if (store == null) {
            throw new IllegalStateException("Store cannot be null");
        }

        store.checkComplete();
    }

    public void validate() throws Exception {
        store.validate();
    }

    public List<WriteMode> getAvailableWriteModes() {
        if (getFlow() == null) {
            return List.of();
        }

        if (getFlow() != null && (getFlow() == DataFlow.TRANSFORMER || getFlow() == DataFlow.INPUT)) {
            return List.of();
        }

        if (getFlow() != null && (getFlow() == DataFlow.OUTPUT || getFlow() == DataFlow.INPUT_OR_OUTPUT)) {
            return List.of(WriteMode.REPLACE);
        }

        return List.of(WriteMode.REPLACE, WriteMode.APPEND, WriteMode.PREPEND);
    }

    public DataFlow getFlow() {
        if (store == null) {
            return null;
        }

        return store.getFlow();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T extends DataSource<DS>> T copy() {
        var mapper = JacksonMapper.newMapper();
        TokenBuffer tb = new TokenBuffer(mapper, false);
        mapper.writeValue(tb, this);
        return (T) mapper.readValue(tb.asParser(), getClass());
    }

    public DataSource<DS> withStore(DS store) {
        var c = copy();
        c.store = store;
        return c;
    }

    /**
     * Casts this instance to the required type without checking whether a cast is possible.
     */
    @SuppressWarnings("unchecked")
    public final <DSD extends DataSource<?>> DSD asNeeded() {
        return (DSD) this;
    }

    /**
     * Determines on optional default name for this data store that is
     * used when determining a suitable default name for a data source.
     */
    public Optional<String> determineDefaultName() {
        return Optional.empty();
    }

    public DataSourceReadConnection openReadConnection() throws Exception {
        throw new UnsupportedOperationException();
    }

    public DataSourceConnection openWriteConnection(WriteMode mode) throws Exception {
        throw new UnsupportedOperationException();
    }

    public DS getStore() {
        return store;
    }

    public abstract DataSourceType getType();
}
