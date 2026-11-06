package au.edu.adelaide.COMP3011.assignments;

import java.util.List;

/**
 * Holds the complete result of a timestamped transcription.
 */
public record SubtitleResult(
        String text,
        double duration,
        List<SubtitleWord> words,
        List<SubtitleSegment> segments
) {
}