package org.taktik.mpegts.sinks.hls;

import java.util.List;

public class HLSVariant {
    int bandwidth;
    int bandwidth_bytes;
    long bandwidth_dts;

    HLSSegment[] segs;
    //int nsegs;
    int seg;
    long seg_dts;

    String m3u8_path;
}
