package io.xpipe.app.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchanges;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class MessageExchangeImpls {

    private static Set<MessageExchangeImpl<?, ?>> ALL;

    public static void loadAll() {
        ALL = ServiceLoader.load(MessageExchangeImpl.class).stream()
                .map(s -> {
                    var ex = (MessageExchangeImpl<?, ?>) s.get();
                    // TrackEvent.trace("init", "Loaded exchange implementation " + ex.getId());
                    return ex;
                })
                .collect(Collectors.toSet());

        ALL.forEach(messageExchange -> {
            if (MessageExchanges.byId(messageExchange.getId()).isEmpty()) {
                throw new AssertionError("Missing base exchange: " + messageExchange.getId());
            }
        });

        MessageExchanges.getAll().forEach(messageExchange -> {
            if (MessageExchangeImpls.byId(messageExchange.getId()).isEmpty()) {
                throw new AssertionError("Missing exchange implementation: " + messageExchange.getId());
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <RQ extends RequestMessage, RS extends ResponseMessage> Optional<MessageExchangeImpl<RQ, RS>> byId(
            String name) {
        var r = ALL.stream().filter(d -> d.getId().equals(name)).findAny();
        return Optional.ofNullable((MessageExchangeImpl<RQ, RS>) r.orElse(null));
    }

    @SuppressWarnings("unchecked")
    public static <RQ extends RequestMessage, RS extends ResponseMessage>
            Optional<MessageExchangeImpl<RQ, RS>> byRequest(RQ req) {
        var r = ALL.stream()
                .filter(d -> d.getRequestClass().equals(req.getClass()))
                .findAny();
        return Optional.ofNullable((MessageExchangeImpl<RQ, RS>) r.orElse(null));
    }

    public static Set<MessageExchangeImpl<?, ?>> getAll() {
        return ALL;
    }
}
