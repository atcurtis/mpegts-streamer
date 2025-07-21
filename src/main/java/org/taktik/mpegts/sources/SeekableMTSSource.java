package org.taktik.mpegts.sources;

public interface SeekableMTSSource extends ResettableMTSSource {
	void seek(long pcr) throws Exception;
	void mark();
	void rewind() throws Exception;
}
