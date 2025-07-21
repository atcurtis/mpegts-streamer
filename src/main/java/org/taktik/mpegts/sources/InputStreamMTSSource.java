package org.taktik.mpegts.sources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;
import org.taktik.mpegts.Constants;
import org.taktik.mpegts.MTSPacket;

public class InputStreamMTSSource extends AbstractBlockingMTSSource {

	private InputStream inputStream;

	protected InputStreamMTSSource(InputStream inputStream) throws IOException {
		this.inputStream = inputStream;
	}

	@Override
	protected MTSPacket nextPacketBlocking() throws IOException {
		byte[] barray = new byte[Constants.MPEGTS_PACKET_SIZE];
		if (inputStream.read(barray) != Constants.MPEGTS_PACKET_SIZE) {
			inputStream.close();
			return null;
		}

		// Parse the packet
		return new MTSPacket(ByteBuffer.wrap(barray));
	}

	@Override
	protected void closeInternal() throws Exception {
		try (InputStream toClose = inputStream) {
		}
		inputStream = null;
	}

	public static InputStreamMTSSourceBuilder builder() {
		return new InputStreamMTSSourceBuilder();
	}

	public static class InputStreamMTSSourceBuilder {
		private InputStream inputStream;

		private InputStreamMTSSourceBuilder() {
		}

		public InputStreamMTSSourceBuilder setInputStream(InputStream inputStream) {
			this.inputStream = inputStream;
			return this;
		}

		public InputStreamMTSSource build() throws IOException {
			Preconditions.checkNotNull(inputStream, "InputStream cannot be null");
			return new InputStreamMTSSource(inputStream);
		}
	}
}
