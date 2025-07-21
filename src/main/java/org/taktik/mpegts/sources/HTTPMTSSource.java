package org.taktik.mpegts.sources;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.Constants;
import org.taktik.mpegts.MTSPacket;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class HTTPMTSSource extends AbstractBlockingMTSSource implements ResettableMTSSource {

    private static final Logger log = LoggerFactory.getLogger(HTTPMTSSource.class);
    private HttpURLConnection connection;
    private InputStream inputStream;
    private final long contentLength;
    private long bytesRead;

    protected HTTPMTSSource(URI source) throws IOException {
        connection = (HttpURLConnection) source.toURL().openConnection();
        connection.setRequestMethod("HEAD");
        if (connection.getResponseCode() != 200) {
            throw new IOException("bad response code: " + connection.getResponseCode());
        }
        contentLength = connection.getContentLengthLong();
        String accepts = connection.getHeaderField("Accept-Ranges");
        if (accepts == null || !Lists.newArrayList(accepts.split(",")).contains("bytes")) {
            log.warn("Source doesn't support bytes range: {}", accepts);
        }
        connection = (HttpURLConnection) connection.getURL().openConnection();
    }

    private InputStream getInputStream() throws IOException {
        return new BufferedInputStream(connection.getInputStream(), 65536);
    }

    @Override
    protected MTSPacket nextPacketBlocking() throws Exception {
        if (contentLength >= 0 && bytesRead >= contentLength) {
            return null;
        }

        byte[] barray = new byte[Constants.MPEGTS_PACKET_SIZE];

        if (inputStream == null) {
            inputStream = getInputStream();
        }
        for (;;) {
            int read = inputStream.readNBytes(barray, 0, barray.length);

            if (read == Constants.MPEGTS_PACKET_SIZE) {
                bytesRead += Constants.MPEGTS_PACKET_SIZE;

                // Parse the packet
                return new MTSPacket(ByteBuffer.wrap(barray));
            }
            if (read <= 0 && contentLength == -1) {
                return null;
            }

            connection.disconnect();
            connection = (HttpURLConnection) connection.getURL().openConnection();
            connection.setRequestProperty("Range", "bytes=" + bytesRead + "-");
            if (connection.getResponseCode() == 206) {
                inputStream = getInputStream();
            } else if (connection.getResponseCode() != 200) {
                log.warn("response code = {}", connection.getResponseCode());
                return null;
            } else {
                inputStream = getInputStream();
                inputStream.skipNBytes(bytesRead);
            }
        }
    }

    @Override
    protected void closeInternal() throws Exception {
        connection.disconnect();
    }

    @Override
    public void reset() throws Exception {
        connection.disconnect();
        inputStream = null;
        bytesRead = 0;
    }

    public static HTTPMTSSourceBuilder builder() {
        return new HTTPMTSSourceBuilder();
    }

    public static class HTTPMTSSourceBuilder {
        private URI uri;

        private HTTPMTSSourceBuilder() {
        }

        public HTTPMTSSourceBuilder setURI(URI uri) {
            this.uri = uri;
            return this;
        }

        public HTTPMTSSourceBuilder setURI(String uri) {
            return setURI(URI.create(uri));
        }


        public HTTPMTSSource build() throws IOException {
            return new HTTPMTSSource(uri);
        }
    }
}
