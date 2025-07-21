package org.taktik.mpegts.sources;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.MTSPacket;

import java.util.Collection;
import java.util.List;
import java.util.function.IntPredicate;

public class ProgMTSSource extends AbstractMTSSource {
    static final Logger log = LoggerFactory.getLogger("progsource");
    private List<Prog> sources;
    private Prog currentSource;
    private int idx;
    private boolean fixContinuity;

    private ContinuityFixer continuityFixer;
    private boolean closeCurrentSource;

    protected ProgMTSSource(boolean fixContinuity, Collection<Prog> sources) {
        Preconditions.checkArgument(sources.size() > 0, "Multisource must at least contain one source");
        this.sources = Lists.newArrayList(sources);
        this.fixContinuity = fixContinuity;
        idx = 0;
        currentSource = this.sources.get(0);
        if (fixContinuity) {
            continuityFixer = new ContinuityFixer();
        }
        this.closeCurrentSource = false;
    }

    private static void checkLoopingPossible(MTSSource source) {
        if (!(source instanceof ResettableMTSSource)) {
            throw new IllegalStateException("Sources must be resettable for looping");
        }
    }

    @Override
    protected MTSPacket nextPacketInternal() throws Exception {
        if (currentSource == null) {
            return null;
        }
        MTSPacket tsPacket = currentSource.source().nextPacket();
        if (tsPacket != null) {
            if (fixContinuity) {
                continuityFixer.fixContinuity(tsPacket);
            }
            return tsPacket;
        } else {
            nextSource();
            return nextPacket();
        }
    }

    @Override
    protected synchronized void closeInternal() throws Exception {
        for (Prog source : sources) {
            source.source().close();
        }
        if (closeCurrentSource && currentSource != null && !sources.contains(currentSource)) {
            currentSource.source().close();
        }
    }


    private synchronized void nextSource() {
        if (closeCurrentSource) {
            try {
                currentSource.source().close();
            } catch (Exception e) {
                log.error("Error closing source", e);
            } finally {
                closeCurrentSource = false;
            }
        }
        if (fixContinuity) {
            continuityFixer.nextSource();
        }
        if (currentSource.testLoop()) {
            final int oldIdx = idx;
            idx = currentSource.loopTo();
            for (int i = idx; i <= oldIdx; i++) {
                try {
                    ((ResettableMTSSource) sources.get(i).source()).reset();
                } catch (Exception e) {
                    log.warn("Error resetting source #{}", i, e);
                }
            }
            currentSource = sources.get(idx);
        } else {
            idx++;
            if (idx < sources.size()) {
                currentSource = sources.get(idx);
            } else {
                currentSource = null;
            }
        }
        if (idx < sources.size()) {
            log.info("Switched to source #{}", idx);
        }
    }

    public static ProgMTSSourceBuilder builder() {
        return new ProgMTSSourceBuilder();
    }

    public static class ProgMTSSourceBuilder {
        private final List<MTSSource> sources = Lists.newArrayList();
        private final List<String> marks = Lists.newArrayList();
        private final List<String> dests = Lists.newArrayList();
        private final List<IntPredicate> tests = Lists.newArrayList();
        boolean fixContinuity = false;
        private String nextMark = "start";
        private final List<String> lastMark = Lists.newArrayList();

        private ProgMTSSourceBuilder() {
        }

        public ProgMTSSourceBuilder mark(String name) {
            if (name != null && marks.contains(name)) {
                throw new IllegalArgumentException("Duplicate mark: " + name);
            }
            nextMark = name;
            return this;
        }

        public ProgMTSSourceBuilder addSource(MTSSource source) {
            sources.add(source);
            marks.add(nextMark);
            dests.add(null);
            tests.add(null);
            if (nextMark != null) {
                lastMark.add(nextMark);
            }
            nextMark = null;
            return this;
        }

        public ProgMTSSourceBuilder addSources(Collection<MTSSource> sources) {
            sources.forEach(this::addSource);
            return this;
        }

        public ProgMTSSourceBuilder setFixContinuity(boolean fixContinuity) {
            this.fixContinuity = fixContinuity;
            return this;
        }

        public ProgMTSSourceBuilder loopIf(IntPredicate test) {
            return loopIf(popLastMark(), test);
        }

        public ProgMTSSourceBuilder loopIf(String mark, IntPredicate test) {
            if (mark == null || !marks.contains(mark)) {
                throw new IllegalArgumentException("Mark not found: " + mark);
            }
            if (test == null) {
                throw new IllegalArgumentException("null loop test");
            }
            dests.set(dests.size()-1, mark);
            tests.set(tests.size()-1, test);
            return this;
        }

        public ProgMTSSourceBuilder loop() {
            return loopIf(popLastMark(), loop -> true);
        }

        public ProgMTSSourceBuilder loop(String mark) {
            return loopIf(mark, loop -> true);
        }

        public ProgMTSSourceBuilder loops(int count) {
            return loops(popLastMark(), count);
        }

        public ProgMTSSourceBuilder loops(String mark, int count) {
            return loopIf(mark, loop -> loop >= count);
        }

        private String popLastMark() {
            return lastMark.remove(lastMark.size() - 1);
        }

        public ProgMTSSourceBuilder noLoop() {
            dests.set(dests.size(), null);
            tests.set(tests.size(), null);
            return this;
        }

        public ProgMTSSource build() {
            List<Prog> progList = Lists.newArrayList();

            for (int index = 0; index < sources.size(); index++) {
                int mark = -1;
                if (tests.get(index) != null) {
                    String name;
                    mark = (name = dests.get(index)) != null ? marks.indexOf(name) : -1;
                    if (mark < 0) {
                        throw new IllegalStateException();
                    }
                    for (int i = mark; i <= index; i++) {
                        checkLoopingPossible(sources.get(i));
                    }
                }

                progList.add(new Prog(sources.get(index), tests.get(index), mark));
            }

            return new ProgMTSSource(fixContinuity, progList);
        }
    }

    private static class Prog {
        private final MTSSource source;
        private final IntPredicate loopTest;
        private final int loopTo;
        private int loopCount = 1;

        private Prog(MTSSource source, IntPredicate loopTest, int loopTo) {
            this.source = source;
            this.loopTest = loopTest;
            this.loopTo = loopTo;
        }

        MTSSource source() {
            return source;
        }

        boolean testLoop() {
            if (loopTest != null && loopTest.test(loopCount++)) {
                loopCount = 1;
                return true;
            }
            return false;
        }

        int loopTo() {
            return loopTo;
        }
    }
}

