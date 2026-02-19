package org.taktik.mpegts.sinks.hls;

import java.nio.file.Path;
import java.time.Duration;

public class HLSConfig {

    public final Path path;
    public final long min_seg;
    public final long max_seg;
    public final long analyse;
    public final long max_size;
    public final int nsegs;
    public final boolean clean;

    public HLSConfig(HLSConfigBuilder elts) {
        Path path = null;
        long min_seg = 5000;
        long max_seg = 10000;
        long analyse = 0;
        long max_size = 16 * 1024 * 1024;
        int nsegs = 6;
        boolean clean = true;

        if (elts.path != null) {
            if (!elts.path.toFile().isDirectory()) {
                throw new IllegalArgumentException();
            }
            path = elts.path;
        }
        if (elts.min_seg != null) {
            if (elts.min_seg.toMillis() < 1) {
                throw new IllegalArgumentException();
            }
            min_seg = elts.min_seg.toMillis();
        }
        if (elts.max_seg != null) {
            if (elts.max_seg.toMillis() < 1) {
                throw new IllegalArgumentException();
            }
            max_seg = elts.max_seg.toMillis();
        }
        if (elts.analyse != null) {
            if (elts.analyse.toMillis() < 1) {
                throw new IllegalArgumentException();
            }
            analyse = elts.analyse.toMillis();
        }
        if (elts.max_size != null) {
            if (elts.max_size < 1) {
                throw new IllegalArgumentException();
            }
            max_size = elts.max_size;
        }
        if (elts.segments != null) {
            if (elts.segments < 1) {
                throw new IllegalArgumentException();
            }
            nsegs = elts.segments;
        }
        if (elts.no_clean) {
            clean = false;
        }

        if (path == null) {
            throw new IllegalArgumentException();
        }

        this.path = path;
        this.min_seg = min_seg;
        this.max_seg = max_seg;
        this.analyse = analyse != 0 ? analyse : min_seg;
        this.max_size = max_size;
        this.nsegs = nsegs;
        this.clean = clean;
    }

    public static HLSConfigBuilder builder() {
        return new HLSConfigBuilder();
    }

    public static class HLSConfigBuilder {
        private Path path;
        private Duration min_seg;
        private Duration max_seg;
        private Duration analyse;
        private Long max_size;
        private Integer segments;
        private boolean no_clean;

        private HLSConfigBuilder() {
        }

        public HLSConfigBuilder no_clean() {
            no_clean = true;
            return this;
        }

        public HLSConfig build() {
            return new HLSConfig(this);
        }
    }

}
