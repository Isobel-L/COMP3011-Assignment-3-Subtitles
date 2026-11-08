
package comp3011;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class SubtitleApiClientTest {

    @Tag("integration")
    @Test
    void sendsAudioToSubtitleApi() throws Exception {

        Path audio = Files.createTempFile("subtitle-test-", ".mp3");

        try {
            Process process = new ProcessBuilder(
                    "ffmpeg",
                    "-nostdin",
                    "-y",
                    "-f", "lavfi",
                    "-i", "sine=frequency=440:sample_rate=16000",
                    "-t", "2",
                    "-ac", "1",
                    "-b:a", "64k",
                    audio.toString()
            ).redirectErrorStream(true)
             .redirectOutput(ProcessBuilder.Redirect.DISCARD)
             .start();

            assertTrue(process.waitFor() == 0,
                    "Test audio generation failed.");

            SubtitleApiClient client = new SubtitleApiClient();

            String response = client.generateSubtitles(audio)
                    .get(180, TimeUnit.SECONDS);

            assertNotNull(response);
            assertFalse(response.isBlank());

            System.out.println("Subtitle API response:");
            System.out.println(response);

        } finally {
            Files.deleteIfExists(audio);
        }
    }
}
