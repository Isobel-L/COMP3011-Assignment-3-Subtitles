
package comp3011;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AudioExtractorTest {

    @Test
    void extractsAudioFromVideo() throws Exception {

        Path video = Files.createTempFile("test-video-", ".mp4");

        try {
            Process process = new ProcessBuilder(
                    "ffmpeg",
                    "-nostdin",
                    "-y",
                    "-f", "lavfi",
                    "-i", "color=c=black:s=320x240:r=25",
                    "-f", "lavfi",
                    "-i", "sine=frequency=440:sample_rate=16000",
                    "-t", "2",
                    "-c:v", "mpeg4",
                    "-c:a", "aac",
                    video.toString()
            ).redirectErrorStream(true)
             .redirectOutput(ProcessBuilder.Redirect.DISCARD)
             .start();

            assertTrue(process.waitFor() == 0,
                    "Test video generation failed.");

            Path audio = AudioExtractor.extractAudio(video);

            try {
                assertTrue(Files.exists(audio));
                assertTrue(Files.size(audio) > 0);

                System.out.println("Audio extraction successful!");
                System.out.println("Size: " + Files.size(audio) + " bytes");

            } finally {
                Files.deleteIfExists(audio);
            }

        } finally {
            Files.deleteIfExists(video);
        }
    }
}
