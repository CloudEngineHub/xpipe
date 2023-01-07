package io.xpipe.beacon.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class FocusExchange implements MessageExchange {

    @Override
    public String getId() {
        return "focus";
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
    }
}
