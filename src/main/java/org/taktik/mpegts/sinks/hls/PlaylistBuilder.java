package org.taktik.mpegts.sinks.hls;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class PlaylistBuilder {

    HLS hls;
    HLSVariant var;
    String mediaext = ".ts\n";

    public byte[] build() {

        int ms = var.seg <= var.segs.length ? 0 : var.seg - var.segs.length;
        long td = (hls.conf.max_seg + 999) / 1000;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.US_ASCII);
        writer.print("#EXTM3U\n"
                + "#EXT-X-VERSION:3\n"
                + "#EXT-X-MEDIA-SEQUENCE:");
        writer.print(ms);
        writer.print("\n"
                + "#EXT-X-TARGETDURATION:");
        writer.print(td);
        writer.print("\n\n");

        for (int i = 0; i < var.segs.length; i++) {
            HLSSegment seg = var.segs[(var.seg + i) % var.segs.length];

            if (seg != null && seg.duration > 0) {
                writer.format("#EXTINF:%.3f,\n", seg.duration / 90000.);

                if (hls.vars.length > 1) {
                    writer.print(Integer.toUnsignedString(var.prog.number));
                    writer.print(".");
                    writer.print(Integer.toUnsignedString(seg.id));
                    writer.print(mediaext);
                } else {
                    writer.print(Integer.toUnsignedString(seg.id));
                    writer.print(mediaext);
                }
            }
        }

        if (hls.done) {
            writer.print("\n#EXT-X-ENDLIST\n");
        }

        return baos.toByteArray();
    }
}
