package org.taktik.mpegts.sinks.hls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.MTSPacket;
import org.taktik.mpegts.sinks.MTSSink;

import java.nio.ByteBuffer;

public class TSStream implements MTSSink {

    private static final Logger LOGGER = LoggerFactory.getLogger(TSStream.class);

    enum Event {
        TS_PAT,
        TS_PMT,
        TS_PES,
    }

    static class ES {
        byte type;
        byte sid;
        byte cont;
        short pid;
        short pts;
        short dts;
        boolean ptsf;
        boolean rand;
        boolean video;
        ByteBuffer bufs; // ES

        public int getPid() {
            return 0xffff & pid;
        }
    }

    static class Program {
        short number;
        short pid;
        short pcr_pid;
        long pcr;
        boolean video;
        int nes;
        ES[] es;
        ByteBuffer bufs; // PMT

        public int getPid() {
            return 0xffff & pid;
        }
    }

    public static class Handlers {
        Event event;
        TSStream ts;
        Program prog;
        ES es;
        ByteBuffer bufs;
        ByteBuffer data;
    }

    Program[] progs;
    Handlers handlers;


    @Override
    public void send(MTSPacket packet) throws Exception {
        if (packet.getPid() == 0) {
            handlePat(packet);
            return;
        }

        for (Program prog : this.progs) {
            if (packet.getPid() == prog.getPid()) {
                handlePmt(prog, packet);
            }
            for (ES es : prog.es) {
                if (packet.getPid() == es.getPid()) {
                    handlePes(prog, es, packet);
                }
            }
        }

        LOGGER.atError().setMessage("dropping unexpected TS packet pid:{}")
                .addArgument(() -> String.format("0x%04x", packet.getPid()))
                .log();
    }

    private void handlePat(MTSPacket packet) {
        if (progs != null) {
            LOGGER.atDebug().log("ts dropping successive pat");
            return;
        }
        //packet.get
        //byte ptr =
    }

    @Override
    public void close() throws Exception {

    }
}
