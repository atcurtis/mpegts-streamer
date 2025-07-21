package org.taktik.mpegts.sources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.MTSPacket;

import java.io.BufferedReader;
import java.io.IOException;

public class ProcessMTSSource extends AbstractMTSSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessMTSSource.class);
    private final Process process;
    private final MTSSource source;

    protected ProcessMTSSource(ProcessBuilder processBuilder) throws IOException {
        process = processBuilder
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start();process.info()
        LOGGER.atInfo().setMessage("Starting process PID={}").addArgument(process.pid()).log();
        source = InputStreamMTSSource.builder().setInputStream(process.getInputStream()).build();
        Thread.ofVirtual().name("stderr").start(this::stderrHandler);
    }

    private void stderrHandler() {
        try (BufferedReader reader = process.errorReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.atWarn().setMessage("[{}] {}")
                        .addArgument(process.pid()).addArgument(line)
                        .log();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected MTSPacket nextPacketInternal() throws Exception {
        return source.nextPacket();
    }

    @Override
    protected void closeInternal() throws Exception {
        source.close();
        if (process.isAlive()) {
            process.destroy();
        }
        process.waitFor();
    }

    public static ProcessMTSSourceBuilder builder() {
        return new ProcessMTSSourceBuilder();
    }

    public static class ProcessMTSSourceBuilder {
        private final ProcessBuilder builder = new ProcessBuilder();
        private ProcessMTSSourceBuilder() {

        }
    }
}
