package org.taktik.mpegts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class PESSection {
    private static final Logger LOGGER = LoggerFactory.getLogger(PESSection.class);

    private byte sid;


    public static PESSection parse(ByteBuffer data) {
        byte[] pfx = new byte[3];
        data.get(pfx);
        if (pfx[0] != 0 || pfx[1] != 0 || pfx[2] != 1) {
            LOGGER.atError().log("missing PES start prefix");
            return null;
        }
        int sid = 0xff & data.get();
        // PES packet length
        int len = 0xffff & data.getShort();
        if (data.remaining() < len) {
            return null;
        }

        boolean ptsf;
        long pts = 0;

        switch (sid) {
            case 0xbe:  // padding stream
                return null;
            case 0xbc:  // program stream map
            case 0xbf:  // private stream 2
            case 0xf0:  // ECM stream
            case 0xf1:  // EMM stream
            case 0xff:  // program stream directory
            case 0xf2:  // DSMMC stream
            case 0xf8:  // ITU-T Rec. H.222.1 type E stream
                break;
            default:
                int flags = 0xffff & data.getInt();
                int hlen = 0xff & data.get();
                if (len > 0) {
                    if (len < 3 + hlen) {
                        LOGGER.atError().log("malformed PES");
                        return null;
                    }
                    len -= 3 + hlen;
                }
                if (data.remaining() < hlen) {
                    return null;
                }
                if ((flags & 0x00c0) == 0x0080) {   // PTS_DTS flag == '10'
                    ptsf = true;
                    int v8 = 0xff & data.get();
                    pts = (v8 & 0x0eL) << 29;
                    int v16 = 0xffff & data.getShort();
                    pts |= (v16 & 0xfffeL) << 14;
                    v16 = 0xffff & data.getShort();
                    pts |= v16 >> 1;
                    LOGGER.atDebug().setMessage("ts pes pts:{}").addArgument(pts).log();
                }

        }

        return null;
    }
}
