package comp3011;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Coordinates audio extraction, subtitle generation and JSON parsing
 * asynchronously. Reuses completed or in-progress requests for unchanged files.
 */
public class SubtitleGenerationService {

    private final AudioExtractor audioExtractor = new AudioExtractor();
    private final SubtitleApiClient apiClient = new SubtitleApiClient();
    private final ConcurrentMap<VideoKey, CompletableFuture<List<SubtitleCue>>> cache =
            new ConcurrentHashMap<>();

    private record VideoKey(Path path, long size, long modifiedTime) { }

    public CompletableFuture<List<SubtitleCue>> generate(Path videoPath) {
        final VideoKey key;
        try {
            Path normalized = videoPath.toAbsolutePath().normalize();
            key = new VideoKey(normalized, Files.size(normalized),
                    Files.getLastModifiedTime(normalized).toMillis());
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }

        return cache.computeIfAbsent(key, ignored -> {
            CompletableFuture<List<SubtitleCue>> future = generateUncached(key.path());
            future.whenComplete((result, error) -> {
                if (error != null) {
                    // Schedule removal after computeIfAbsent has installed the entry.
                    CompletableFuture.runAsync(() -> cache.remove(key, future));
                }
            });
            return future;
        });
    }

    private CompletableFuture<List<SubtitleCue>> generateUncached(Path videoPath) {
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
                                return List.copyOf(SubtitleParser.parse(json));
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
                        }));
    }
}
