package au.edu.adelaide.COMP3011.assignments;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * This implementation of TranscriptionService is for load testing. It simulates
 * the the blocking behaviour of calling the real cloud transcription service
 * with a configurable sleep time. The profile "fake-transcription" can be
 * activated in unit tests to inject this in place of the
 * OpenAiTranscriptionService.
 */
@Service
@Profile("fake-transcription")
public class FakeTranscriptionService implements TranscriptionService {

    private final Duration delay;

    FakeTranscriptionService(@Value("${fake-transcription.delay:PT0S}") Duration delay) {
        this.delay = delay;
    }

    @Override
    public String transcribe(MultipartFile file) throws IOException {
        try {
            Thread.sleep(this.delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while simulating transcription latency", e);
        }

        return "Fake transcript for " + file.getOriginalFilename();
    }

    @Override
    public synchronized Map<String, Long> getTokenUsage() {
        return Map.of(
                "inputTokens", 0L,
                "outputTokens", 0L);
    }

}
