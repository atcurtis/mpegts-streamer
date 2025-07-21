package org.taktik.mpegts;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.sinks.MTSSink;
import org.taktik.mpegts.sinks.PCRDebugTransportFilter;
import org.taktik.mpegts.sinks.UDPTransport;
import org.taktik.mpegts.sources.*;

public class Streamer2Test {

	static final Logger LOGGER = LoggerFactory.getLogger(Streamer2Test.class);

	@Test
	public void testBasic() throws Exception {
		// Set up mts sink
		try (MTSSink transport = UDPTransport.builder()
				//.setAddress("239.222.1.1")
				.setAddress("127.0.0.1")
				.setPort(1234)
				.setSoTimeout(5000)
				.setTtl(1)
				.build()) {

			MTSSource ts1 = HTTPMTSSource.builder().setURI("https://ott.dolby.com/OnDelKits/DDP/Dolby_Digital_Plus_Online_Delivery_Kit_v1.4.1/Test_Signals/muxed_streams/MPEG2TS/Example/ChID_voices_1280x720p_25fps_h264_6ch_256kbps_ddp.ts").build();
			//MTSSource ts1 = HTTPMTSSource.builder().setURI("https://filesamples.com/samples/video/ts/sample_640x360.ts").build();
			//MTSSource ts2 = HTTPMTSSource.builder().setURI("https://filesamples.com/samples/video/ts/sample_640x360.ts").build();
			MTSSource ts2 = HTTPMTSSource.builder().setURI("https://ott.dolby.com/OnDelKits/DDP/Dolby_Digital_Plus_Online_Delivery_Kit_v1.4.1/Test_Signals/muxed_streams/MPEG2TS/Example/ChID_voices_1280x720p_25fps_h264_6ch_256kbps_ddp.ts").build();

			// media132, media133 --> ok
			// media133, media132 --> ok
			// media123, media132 --> ko


			// Build source

			MTSSource source = ProgMTSSource.builder()
					.addSource(ts1)
					.addSource(ts2)
					.setFixContinuity(true)
					.build();

			// build streamer
			Streamer streamer = Streamer.builder()
					.setSource(source)
					//.setSink(ByteChannelSink.builder().setByteChannel(fc).build())
					.setSink(PCRDebugTransportFilter.wrap(transport))
					.build();

			// Start streaming
			streamer.stream();
			streamer.join();
		}
	}
}
