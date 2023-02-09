package io.xpipe.beacon.exchange.api;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.core.source.DataSourceReference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class QueryTextDataExchange implements MessageExchange {

    @Override
    public String getId() {
        return "queryTextData";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        DataSourceReference ref;

        @Builder.Default
        int maxLines = -1;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {}
}
