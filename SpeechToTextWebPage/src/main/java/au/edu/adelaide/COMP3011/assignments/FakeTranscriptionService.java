
package au.edu.adelaide.COMP3011.assignments;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Fake transcription service used for load testing.
 *
 * Simulates cloud transcription latency without
 * making real API requests.
 */
@Service
@Profile("fake-transcription")
public class FakeTranscriptionService implements TranscriptionService {

    private final Duration delay;

    FakeTranscriptionService(
            @Value("${fake-transcription.delay:PT0S}") Duration delay) {
        this.delay = delay;
    }

    @Override
    public String transcribe(MultipartFile file) throws IOException {
        return transcribeWithTimestamps(file).text();
    }

    /**
     * Simulates a timestamped transcription response.
     */
    @Override
    public SubtitleResult transcribeWithTimestamps(MultipartFile file)
            throws IOException {

        try {
            Thread.sleep(this.delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Interrupted while simulating transcription latency", e);
        }

        String text = "Fake transcript for " + file.getOriginalFilename();

        return new SubtitleResult(
                text,
                2.0,
                List.of(
                        new SubtitleWord("Fake", 0.0, 0.5),
                        new SubtitleWord("transcript", 0.5, 1.2)
                ),
                List.of(
                        new SubtitleSegment(
                                text,
                                0.0,
                                2.0,
                                0.0,
                                -0.1
                        )
                )
        );
    }

    @Override
    public synchronized Map<String, Long> getTokenUsage() {
        return Map.of(
                "inputTokens", 0L,
                "outputTokens", 0L);
    }
}
