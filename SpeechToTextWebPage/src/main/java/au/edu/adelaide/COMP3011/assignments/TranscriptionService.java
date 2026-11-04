package au.edu.adelaide.COMP3011.assignments;

import java.io.IOException;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

/**
 * Simple interface for getting transcription text back from a multipart audio
 * file and keeping track of the cumulative token usage that arises.
 */
public interface TranscriptionService {

    String transcribe(MultipartFile file) throws IOException;
    // Input file.bytes() is assumed to contain the raw audio data.

    Map<String, Long> getTokenUsage();
    // Return format: { "inputTokens": long, "outputTokens": long }
}
