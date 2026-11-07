
package comp3011;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class AudioExtractor {

    public static Path extractAudio(Path videoPath)
            throws IOException, InterruptedException {

        if (!Files.isRegularFile(videoPath)) {
            throw new IOException("Video file does not exist: " + videoPath);
        }

        Path audioPath = Files.createTempFile(
                "comp3011-audio-", ".mp3");

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "ffmpeg",
                    "-nostdin",
                    "-y",
                    "-i", videoPath.toAbsolutePath().toString(),
                    "-vn",
                    "-ac", "1",
                    "-ar", "16000",
                    "-b:a", "64k",
                    audioPath.toAbsolutePath().toString()
            );

            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

            Process process = builder.start();

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new IOException("FFmpeg audio extraction timed out.");
            }

            if (process.exitValue() != 0) {
                throw new IOException("FFmpeg failed to extract audio.");
            }

            if (Files.size(audioPath) == 0) {
                throw new IOException("Extracted audio file is empty.");
            }

            return audioPath;

        } catch (IOException | InterruptedException exception) {
            Files.deleteIfExists(audioPath);
            throw exception;
        }
    }
}
