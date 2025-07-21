package org.taktik.mpegts.sinks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.MTSPacket;

import java.time.Duration;
import java.util.Objects;

public class PCRDebugTransportFilter implements MTSSink {
    private static final Logger log = LoggerFactory.getLogger(PCRDebugTransportFilter.class);

    private final MTSSink sink;

    public static MTSSink wrap(MTSSink sink) {
        return new PCRDebugTransportFilter(Objects.requireNonNull(sink));
    }

    protected PCRDebugTransportFilter(MTSSink sink) {
        this.sink = sink;
    }

    @Override
    public void send(MTSPacket packet) throws Exception {
        sink.send(packet);

        if (packet.isAdaptationFieldExist() && packet.getAdaptationField() != null /*&& packet.getAdaptationField().isRandomAccessIndicator()*/) {
            if (packet.getAdaptationField().isPcrFlag()) {
                MTSPacket.AdaptationField.PCR pcr = packet.getAdaptationField().getPcr();
                Duration duration = Duration.ofMillis(pcr.getValue() / 27000L);
                log.atInfo().setMessage("point - {}")
                        .addArgument(() -> String
                                .format("%d:%02d:%02d.%03d",
                                        duration.toHours(),
                                        duration.toMinutesPart(),
                                        duration.toSecondsPart(), duration.toMillisPart()))
                        .log();
            }
        }
    }

    @Override
    public void close() throws Exception {
        sink.close();
    }
}
