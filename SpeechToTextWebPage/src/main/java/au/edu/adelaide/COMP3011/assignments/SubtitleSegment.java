package au.edu.adelaide.COMP3011.assignments;

/**
 * Represents a transcribed speech segment.
 *
 * Times are measured in seconds from the beginning
 * of the audio recording.
 *
 * Confidence values help identify unreliable speech
 * or segments containing silence.
 */
public record SubtitleSegment(
        String text,
        double start,
        double end,
        double noSpeechProb,
        double aveLogProb
) {
}