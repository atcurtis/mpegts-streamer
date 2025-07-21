package org.taktik.mpegts.sinks;

import com.google.common.base.Preconditions;
import org.jcodec.common.io.IOUtils;
import org.taktik.mpegts.MTSPacket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class HTTPTransport implements MTSSink {

    private HttpURLConnection connection;
    private OutputStream outputStream;
    private boolean closed;
    private CompletableFuture<Void> queue = CompletableFuture.completedFuture(null);
    private final Thread.Builder builder = Thread.ofVirtual().name("HTTPMTSSource");


    protected HTTPTransport(URI uri) throws IOException {
        connection = (HttpURLConnection) uri.toURL().openConnection();
        initConnection();
    }

    protected void initConnection() throws ProtocolException {
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "video/mp2t");
    }

    @Override
    public void send(MTSPacket packet) throws Exception {
        ByteBuffer buffer = packet.getBuffer();
        Preconditions.checkArgument(buffer.hasArray());

        queue = queue.thenRunAsync(() -> {
            try {
                for (; ; ) {
                    while (outputStream == null) {
                        if (closed) {
                            throw new IOException("closed");
                        }
                        try {
                            outputStream = connection.getOutputStream();
                        } catch (IOException ex) {
                            if (connection instanceof HttpURLConnection x) {
                                x.disconnect();
                            }
                            connection = (HttpURLConnection) connection.getURL().openConnection();
                            initConnection();
                        }
                    }

                    try {
                        outputStream.write(buffer.array(), buffer.arrayOffset(), buffer.limit());
                        break;
                    } catch (IOException ex) {
                        IOUtils.closeQuietly(outputStream);
                        outputStream = null;
                    }
                }
                outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, builder::start);
        if (queue.isDone()) {
            queue.get();
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;

        if (outputStream != null) {
            IOUtils.closeQuietly(outputStream);
        }
        if (connection instanceof HttpURLConnection x) {
            x.disconnect();
        }
    }

    public static HTTPTransportBuilder builder() {
        return new HTTPTransportBuilder();
    }

    public static class HTTPTransportBuilder {
        private URI uri;

        private HTTPTransportBuilder() {
        }

        public HTTPTransportBuilder setURI(URI uri) {
            this.uri = uri;
            return this;
        }

        public HTTPTransportBuilder setURI(String uri) {
            return setURI(URI.create(uri));
        }

        public HTTPTransport build() throws IOException {
            if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
                return new HTTPTransport(uri);
            }
            throw new IllegalArgumentException("Bad URI scheme");
        }
    }
}
