package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchange;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class StatusExchange implements MessageExchange {

    @Override
    public String getId() {
        return "status";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        String mode;
    }
}
