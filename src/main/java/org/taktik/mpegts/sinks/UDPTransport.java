package org.taktik.mpegts.sinks;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.MTSPacket;

public class UDPTransport implements MTSSink {

	private final InetSocketAddress inetSocketAddress;
	private final MulticastSocket multicastSocket;
	private CompletableFuture<Void> queue = CompletableFuture.completedFuture(null);
	private final Thread.Builder builder = Thread.ofVirtual().name("HTTPMTSSource");



	private UDPTransport(String address, int port, int ttl, int soTimeout) throws IOException {
		// InetSocketAddress
		inetSocketAddress = new InetSocketAddress(address, port);

		// Create the socket but we don't bind it as we are only going to send data
		// Note that we don't have to join the multicast group if we are only sending data and not receiving
		multicastSocket = new MulticastSocket();
		multicastSocket.setReuseAddress(true);
		multicastSocket.setSoTimeout(soTimeout);
		multicastSocket.setTimeToLive(ttl);
	}

	@Override
	public void send(MTSPacket packet) throws Exception {
		ByteBuffer buffer = packet.getBuffer();
		Preconditions.checkArgument(buffer.hasArray());
		DatagramPacket datagramPacket = new DatagramPacket(buffer.array(), buffer.arrayOffset(), buffer.limit(), inetSocketAddress);
		CompletableFuture<Void> oldFuture = queue;
		queue = queue.thenRunAsync(() -> {
            try {
                multicastSocket.send(datagramPacket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, builder::start);
		if (queue.isDone()) {
			queue.get();
		} else {
			oldFuture.join();
		}
	}

	public void close() {
		multicastSocket.close();
	}

	public static UDPTransport.UDPTransportBuilder builder() {
		return new UDPTransportBuilder();
	}

	public static class UDPTransportBuilder {
		private String address;
		private int port;
		private int ttl;
		private int soTimeout;

		public UDPTransportBuilder setAddress(String address) {
			this.address = address;
			return this;
		}

		public UDPTransportBuilder setPort(int port) {
			this.port = port;
			return this;
		}

		public UDPTransportBuilder setTtl(int ttl) {
			this.ttl = ttl;
			return this;
		}

		public UDPTransportBuilder setSoTimeout(int timeout) {
			this.soTimeout = timeout;
			return this;
		}

		public UDPTransport build() throws IOException {
			return new UDPTransport(address, port, ttl, soTimeout);
		}
	}
}
