package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.core.source.DataSourceReference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

/**
 * Output the data source contents.
 */
public class WriteExecuteExchange implements MessageExchange {

    @Override
    public String getId() {
        return "writeExecute";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        DataSourceReference ref;

        @NonNull
        UUID id;

        String mode;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        boolean hasBody;
    }
}
