package au.edu.adelaide.COMP3011.assignments;

import java.io.IOException;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for audio transcription and usage tracking.
 */
public interface TranscriptionService {

    // Returns only the recognised speech as text.
    String transcribe(MultipartFile file) throws IOException;

    // Returns the text, timestamps, words and segments.
    SubtitleResult transcribeWithTimestamps(MultipartFile file)
            throws IOException;

    // Returns cumulative transcription usage.
    Map<String, Long> getTokenUsage();
}