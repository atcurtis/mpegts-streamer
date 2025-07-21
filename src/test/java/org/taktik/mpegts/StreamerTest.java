package org.taktik.mpegts;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.sinks.MTSSink;
import org.taktik.mpegts.sinks.UDPTransport;
import org.taktik.mpegts.sources.*;

public class StreamerTest {

	static final Logger LOGGER = LoggerFactory.getLogger(StreamerTest.class);

	@Test
	public void testBasic() throws Exception {

		final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		final File tsFile = new File(tmpDir, "file_example_MP4_480_1_5MG.ts");
		if (!tsFile.canRead() || tsFile.length() < 65536) {
			try (BufferedInputStream is = new BufferedInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/file_example_MP4_480_1_5MG.ts")));
                 FileOutputStream os = new FileOutputStream(tsFile, false)) {

				LOGGER.info("Starting download");
				long len = is.transferTo(os);
				LOGGER.info("Transferred {} bytes", len);
			}
		}
		LOGGER.info("Found file {} of {} bytes", tsFile.getAbsolutePath(), tsFile.length());

		// Set up mts sink
		MTSSink transport = UDPTransport.builder()
				//.setAddress("239.222.1.1")
				.setAddress("127.0.0.1")
				.setPort(1234)
				.setSoTimeout(5000)
				.setTtl(1)
				.build();


		//ResettableMTSSource ts1 = MTSSources.from(new File("/Users/abaudoux/Downloads/EBSrecording.mpg"));
		ResettableMTSSource ts1 = MTSSources.from(tsFile);
		ResettableMTSSource ts2 = MTSSources.from(tsFile);

		((SeekableByteChannelMTSSource) ts1).seek(202500000L);

		// media132, media133 --> ok
		// media133, media132 --> ok
		// media123, media132 --> ko


		// Build source

		MTSSource source = ProgMTSSource.builder()
				.addSource(ts1)
				.addSource(ts2)
				.loops(2)
				.setFixContinuity(true)
				.build();

		// build streamer
		Streamer streamer = Streamer.builder()
				.setSource(source)
				//.setSink(ByteChannelSink.builder().setByteChannel(fc).build())
				.setSink(transport)
				.build();

		// Start streaming
		streamer.stream();
		streamer.join();
	}
}
