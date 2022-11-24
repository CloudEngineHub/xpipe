package io.xpipe.beacon.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.core.impl.FileStore;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Stores a stream of data in a storage.
 */
public class StoreStreamExchange implements MessageExchange {

    @Override
    public String getId() {
        return "storeStream";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {}

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        FileStore store;
    }
}
