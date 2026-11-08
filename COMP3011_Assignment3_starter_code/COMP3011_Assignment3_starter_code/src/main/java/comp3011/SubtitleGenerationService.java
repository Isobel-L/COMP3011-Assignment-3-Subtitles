
package comp3011;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Coordinates audio extraction, subtitle generation,
 * and JSON parsing without blocking the JavaFX thread.
 */
public class SubtitleGenerationService {

    private final AudioExtractor audioExtractor = new AudioExtractor();
    private final SubtitleApiClient apiClient = new SubtitleApiClient();

    public CompletableFuture<List<SubtitleCue>> generate(Path videoPath) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return audioExtractor.extractAudio(videoPath);
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }).thenCompose(audioPath ->
            apiClient.generateSubtitles(audioPath)
                .thenApply(json -> {
                    try {
                        return SubtitleParser.parse(json);
                    } catch (Exception exception) {
                        throw new CompletionException(exception);
                    }
                })
                .whenComplete((result, error) -> {
                    try {
                        Files.deleteIfExists(audioPath);
                    } catch (IOException exception) {
                        System.err.println(
                            "Could not delete temporary audio: "
                            + exception.getMessage());
                    }
                })
        );
    }
}
