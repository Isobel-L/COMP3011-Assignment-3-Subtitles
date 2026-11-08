
package comp3011;

/**
 * Represents a subtitle displayed during a specific
 * time interval in the video.
 *
 * All timestamps are measured in seconds.
 */
public record SubtitleCue(
        String text,
        double start,
        double end) {

    /**
     * Checks whether this subtitle should be visible
     * at the given playback time.
     */
    public boolean isActiveAt(double timeSeconds) {
        return timeSeconds >= start && timeSeconds < end;
    }
}
