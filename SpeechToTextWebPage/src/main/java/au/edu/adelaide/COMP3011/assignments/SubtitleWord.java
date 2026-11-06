package au.edu.adelaide.COMP3011.assignments;

/**
 * Represents one spoken word and its position in the audio.
 *
 * Times are measured in seconds from the beginning
 * of the audio recording.
 */
public record SubtitleWord(
        String word,
        double start,
        double end
) {
}