package org.taktik.mpegts.sources;

import org.taktik.mpegts.MTSPacket;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public abstract class AbstractBlockingMTSSource extends AbstractMTSSource {

    private Future<MTSPacket> nextPacket;
    private final Thread.Builder builder = Thread.ofVirtual().name(Objects.toIdentityString(this));

    @Override
    protected MTSPacket nextPacketInternal() throws Exception {
        MTSPacket packet;
        if (nextPacket == null) {
            packet = nextPacketBlocking();
        } else {
            packet = nextPacket.get();
        }
        if (packet == null) {
            nextPacket = null;
        } else {
            FutureTask<MTSPacket> task = new FutureTask<>(this::nextPacketBlocking);
            builder.start(task);
            nextPacket = task;
        }
        return packet;
    }

    protected abstract MTSPacket nextPacketBlocking() throws Exception;
}
