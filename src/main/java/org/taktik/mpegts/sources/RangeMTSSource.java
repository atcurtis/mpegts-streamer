package org.taktik.mpegts.sources;

import org.taktik.mpegts.MTSPacket;

public class RangeMTSSource extends AbstractMTSSource implements ResettableMTSSource {

    private final SeekableMTSSource source;
    private final long startPCR;
    private final long endPCR;
    private boolean end;

    public RangeMTSSource(SeekableMTSSource source, long startPCR, long endPCR) {
        this.source = source;
        this.startPCR = startPCR;
        this.endPCR = endPCR;
    }

    @Override
    protected MTSPacket nextPacketInternal() throws Exception {
        if (end) {
            return null;
        }
        MTSPacket packet = source.nextPacket();

        if (packet.isAdaptationFieldExist()) {
            MTSPacket.AdaptationField adapt = packet.getAdaptationField();
            if (adapt != null && adapt.isPcrFlag() && adapt.isRandomAccessIndicator()) {
                if (adapt.getPcr().getValue() >= endPCR) {
                    end = true;
                    return null;
                }
            }
        }

        return packet;
    }

    @Override
    protected void closeInternal() throws Exception {
        end = true;
        source.close();
    }

    @Override
    public void reset() throws Exception {
        source.reset();
        source.seek(startPCR);
        end = false;
    }
}
