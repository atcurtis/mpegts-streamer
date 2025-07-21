package org.taktik.mpegts.sinks;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.MTSPacket;

import java.nio.ByteBuffer;
import java.util.List;

public class HLSSink implements MTSSink {

    private static final Logger LOGGER = LoggerFactory.getLogger(HLSSink.class);

    List<TSProg> progs = Lists.newArrayList();

    @Override
    public void send(MTSPacket packet) throws Exception {
        if (packet.getPid() == 0) {
            readPat(packet);
            return;
        }
        for (TSProg prog : progs) {
            if (packet.getPid() == prog.getPid()) {
                readPmt(prog, packet);
                return;
            }
            for (TSES es : prog.es) {
                if (packet.getPid() == es.getPid()) {
                    readPes(prog, es, packet);
                }
            }
        }

        LOGGER.info("dropping unexpected TS packet pid:{}", packet.getPid());
    }

    private void readPes(TSProg prog, TSES es, MTSPacket packet) {

    }

    private void readPmt(TSProg prog, MTSPacket packet) {

    }

    private void readPat(MTSPacket packet) {
        ByteBuffer buf = packet.getPayload().asReadOnlyBuffer();
        byte ptr = buf.get(); // pointer_field
        buf.position(buf.position() + (0xff & ptr) + 1); // skipped + table_id
        short len = buf.getShort(); // section_length
        len &= 0x0fff;
        if (len < 9) {
            LOGGER.error("malformed PAT");
            return;
        }
        if (len > 0x03fd) {
            LOGGER.error("too big PAT: {}", len);
            return;
        }
        if (buf.remaining() < len) {
            return;
        }
        // PAT fully available
        buf.position(buf.position() + 5); // transport_stream_id .. last_section_number

        int nprogs = (len - 9) / 4;
        for (int i = 0; i < nprogs; i++) {
            short number = buf.getShort(); // program_number
            short pid = buf.getShort(); // network_PID / program_map_PID
            progs.add(new TSProg(number, pid));
            LOGGER.atDebug().log("ts program {}, pid:{}", number, pid);
        }

       // tsRunHandler(TS_PAT);
    }

    @Override
    public void close() throws Exception {

    }

    static class TSProg {
        final short number;
        final short pid;
        final List<TSES> es = Lists.newArrayList();

        TSProg(short number, short pid) {
            this.number = number;
            this.pid = pid;
        }

        short getPid() {
            return pid;
        }
    }

    static class TSES {

        short getPid() {
            return 0;
        }
    }
}
