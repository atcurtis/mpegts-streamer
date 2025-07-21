package org.taktik.mpegts.sources;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import com.google.common.base.Preconditions;
import org.taktik.mpegts.Constants;
import org.taktik.mpegts.MTSPacket;

public class SeekableByteChannelMTSSource extends AbstractByteChannelMTSSource<SeekableByteChannel> implements SeekableMTSSource {

	private long position;
	private long lastRandomPosition = -1;
	private long mark = -1;

	private SeekableByteChannelMTSSource(SeekableByteChannel byteChannel) throws IOException {
		super(byteChannel);
	}

	public static SeekableByteChannelMTSSourceBuilder builder() {
		return new SeekableByteChannelMTSSourceBuilder();
	}

	@Override
	public void reset() throws IOException {
		lastRandomPosition = -1;
		byteChannel.position(0);
		fillBuffer();
	}

	@Override
	public void mark() {
		mark = lastRandomPosition;
	}

	@Override
	public void rewind() throws IOException {
		if (mark >= 0L) {
			byteChannel.position(mark);
			fillBuffer();
		} else {
			throw new IllegalStateException();
		}
	}

	protected boolean fillBuffer(ByteBuffer buffer) throws IOException {
		position = byteChannel.position();
		return super.fillBuffer(buffer);
	}

	@Override
	protected MTSPacket nextPacketBlocking() throws IOException {
		MTSPacket packet = super.nextPacketBlocking();
		if (packet != null && packet.isAdaptationFieldExist()) {
			MTSPacket.AdaptationField adapt = packet.getAdaptationField();
			if (adapt != null && adapt.isRandomAccessIndicator()) {
				lastRandomPosition = position;
			}
		}
		return packet;
	}

	public void seek(long pcr) throws IOException {
		long minPos = 0L;
		long maxPos = byteChannel.size();
		long lastSeekPos = -1;
		while (minPos + Constants.MPEGTS_PACKET_SIZE < maxPos) {
			long seekPos = (maxPos + minPos) / 2;
			seekPos -= seekPos % Constants.MPEGTS_PACKET_SIZE;
			byteChannel.position(seekPos);
			fillBuffer();
			if (seekPos == lastSeekPos) {
				return;
			}
			lastSeekPos = seekPos;
			for (;;) {
				MTSPacket packet = nextPacketBlocking();
				if (packet == null) {
					reset();
					throw new IOException("Position not found");
				}
				if (!packet.isAdaptationFieldExist()) {
					continue;
				}
				MTSPacket.AdaptationField adapt = packet.getAdaptationField();
				if (adapt == null || !adapt.isRandomAccessIndicator() || !adapt.isPcrFlag()) {
					continue;
				}
				if (adapt.getPcr().getValue() > pcr) {
					maxPos = position;
					break;
				}
				if (adapt.getPcr().getValue() < pcr) {
					minPos = position + Constants.MPEGTS_PACKET_SIZE;
					break;
				}
				byteChannel.position(position);
				fillBuffer();
				return;
			}
		}
	}

	public static class SeekableByteChannelMTSSourceBuilder {
		private SeekableByteChannel byteChannel;

		private SeekableByteChannelMTSSourceBuilder(){}

		public SeekableByteChannelMTSSourceBuilder setByteChannel(SeekableByteChannel byteChannel) {
			this.byteChannel = byteChannel;
			return this;
		}

		public SeekableByteChannelMTSSource build() throws IOException {
			Preconditions.checkNotNull(byteChannel, "byteChannel cannot be null");
			return new SeekableByteChannelMTSSource(byteChannel);
		}
	}
}
