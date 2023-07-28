package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchange;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class ReadDrainExchange implements MessageExchange {

    @Override
    public String getId() {
        return "readDrain";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        String name;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {}
}
