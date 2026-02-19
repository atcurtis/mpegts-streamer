package org.taktik.mpegts.sinks.hls;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class HLS implements AutoCloseable {

    public final HLSConfig conf;

    public String m3u8_path;
    public String m3u8_tmp_path;
    public Path path;

    public HLSVariant[] vars;
    //public int nvars;

    public boolean done;

    private HLS(HLSBuilder elts) {
        conf = Objects.requireNonNull(elts.conf);
        path = conf.path.resolve(conf.path.toFile().getName() + elts.name);

    }

    @Override
    public void close() {
        for (HLSVariant var: vars) {
            //var.file...

            maxd = 0;
            for (int i = 0; i < var.prog.es.length; i++) {
                
            }
        }
    }

    public static class HLSBuilder {
        private HLSConfig conf;
        private TSStream stream;
        private String name;

        private HLSBuilder() {
        }

        public HLS build() {
            return new HLS(this);
        }
    }
}
