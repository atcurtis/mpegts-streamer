package org.taktik.mpegts.sources;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.MTSPacket;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public class RandMTSSource extends AbstractMTSSource implements ResettableMTSSource {
    static final Logger log = LoggerFactory.getLogger("randsource");
    private final List<MTSSource> sources;
    private MTSSource currentSource;
    private final Random rand = new Random();

    protected RandMTSSource(Collection<MTSSource> sources) {
        this.sources = Lists.newArrayList(sources);
    }

    @Override
    protected MTSPacket nextPacketInternal() throws Exception {
        if (currentSource == null) {
            if (sources.isEmpty()) {
                return null;
            }

            currentSource = sources.remove(rand.nextInt(sources.size()));
        }

        return currentSource.nextPacket();
    }

    @Override
    protected synchronized void closeInternal() throws Exception {
        for (MTSSource source : sources) {
            source.close();
        }
        if (currentSource != null && !sources.contains(currentSource)) {
            currentSource.close();
        }
    }

    @Override
    public void reset() throws Exception {
        if (currentSource != null) {
            try {
                currentSource.close();
                ((ResettableMTSSource) currentSource).reset();
                sources.add(currentSource);
            } catch (Exception e) {
                log.warn("Error resetting source", e);
            }
            currentSource = null;
        }
    }
}
