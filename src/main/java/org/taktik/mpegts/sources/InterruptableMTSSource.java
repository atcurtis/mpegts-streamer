package org.taktik.mpegts.sources;

import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.MTSPacket;

import java.util.Queue;

public class InterruptableMTSSource extends AbstractMTSSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterruptableMTSSource.class);
    private final MTSSource source;
    private MTSPacket nextSourcePacket;
    private MTSSource intermission;
    private final Queue<MTSSource> queue = Queues.newLinkedBlockingQueue();
    private boolean wantCancel;
    private boolean isCancelled;

    protected InterruptableMTSSource(MTSSource source) {
        this.source = source;
    }

    public void enqueueInterrupt(MTSSource source) {
        queue.add(source);
    }

    public boolean isInterrupted() {
        return intermission != null || !queue.isEmpty();
    }

    public void cancel() {
        wantCancel = true;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    protected MTSPacket nextPacketInternal() throws Exception {
        if (isCancelled) {
            return null;
        }
        MTSPacket packet = getNextSourcePacket();
        if (wantCancel && packet != null && packet.isAdaptationFieldExist()) {
            MTSPacket.AdaptationField adapt = packet.getAdaptationField();
            if (adapt != null && adapt.isRandomAccessIndicator()) {
                isCancelled = true;
                return null;
            }
        }
        return packet;
    }

    private MTSPacket getNextSourcePacket() throws Exception {
        if (intermission != null) {
            MTSPacket packet = intermission.nextPacket();
            if (packet != null) {
                return packet;
            }
            try {
                intermission.close();
            } catch (Exception e) {
                LOGGER.warn("close", e);
            }
            intermission = null;
        }
        if (nextSourcePacket == null) {
            nextSourcePacket = source.nextPacket();
        }
        if (nextSourcePacket != null && !queue.isEmpty() && nextSourcePacket.isAdaptationFieldExist()) {
            MTSPacket.AdaptationField field = nextSourcePacket.getAdaptationField();
            if (field != null && field.isPcrFlag() && field.isRandomAccessIndicator()) {
                while (!queue.isEmpty() && intermission == null) {
                    intermission = queue.poll();
                    if (intermission != null) {
                        MTSPacket packet = intermission.nextPacket();
                        if (packet != null) {
                            return packet;
                        }
                        try {
                            intermission.close();
                        } catch (Exception e) {
                            LOGGER.warn("intermission", e);
                        }
                    }
                }
            }
        }
        MTSPacket packet = nextSourcePacket;
        nextSourcePacket = source.nextPacket();
        return packet;
    }

    @Override
    protected void closeInternal() throws Exception {
        source.close();
        if (intermission != null) {
            intermission.close();
        }
        while (!queue.isEmpty()) {
            queue.poll().close();
        }
    }
}
