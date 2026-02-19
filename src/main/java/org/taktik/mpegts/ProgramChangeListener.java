package org.taktik.mpegts;

import java.util.TreeMap;

public interface ProgramChangeListener {
    void notify(PATSection patSection, TreeMap<Integer, PMTSection> pmtSection);
}
