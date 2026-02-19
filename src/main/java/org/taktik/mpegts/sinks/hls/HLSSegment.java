package org.taktik.mpegts.sinks.hls;

public final class HLSSegment {
    public final int id;
    public final long duration;
    public final long size;

    public HLSSegment(int id, long duration, long size) {
        this.id = id;
        this.duration = duration;
        this.size = size;
    }
}
