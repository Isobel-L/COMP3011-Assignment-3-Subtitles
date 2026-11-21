
package comp3011;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Generates and caches subtitles asynchronously.
 *
 * Uses a content hash to reuse subtitles for identical videos,
 * including renamed files and subsequent application launches.
 */
public class SubtitleGenerationService {

    private static final Path CACHE_DIRECTORY =
            Path.of(System.getProperty("user.home"),
                    ".comp3011-subtitle-cache");

    private final SubtitleApiClient apiClient =
            new SubtitleApiClient();

    private final ConcurrentMap<VideoKey,
            CompletableFuture<List<SubtitleCue>>> cache =
            new ConcurrentHashMap<>();

    private record VideoKey(
            Path path,
            long size,
            long modifiedTime) {
    }

    public CompletableFuture<List<SubtitleCue>> generate(
            Path videoPath) {

        final VideoKey key;

        try {
            Path normalized =
                    videoPath.toAbsolutePath().normalize();

            key = new VideoKey(
                    normalized,
                    Files.size(normalized),
                    Files.getLastModifiedTime(normalized).toMillis());

        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }

        return cache.computeIfAbsent(key, ignored -> {

            CompletableFuture<List<SubtitleCue>> future =
                    loadOrGenerate(key.path());

            future.whenComplete((result, error) -> {
                if (error != null) {
                    CompletableFuture.runAsync(
                            () -> cache.remove(key, future));
                }
            });

            return future;
        });
    }

    /**
     * Checks the persistent cache before generating subtitles.
     */
    private CompletableFuture<List<SubtitleCue>> loadOrGenerate(
            Path videoPath) {

        return CompletableFuture.supplyAsync(
                () -> getCachePath(videoPath)
        ).thenCompose(cachePath -> {

            if (Files.isRegularFile(cachePath)) {
                try {
                    String json = Files.readString(
                            cachePath,
                            StandardCharsets.UTF_8);

                    List<SubtitleCue> cues =
                            List.copyOf(SubtitleParser.parse(json));

                    System.out.println(
                            "Loaded subtitles from persistent cache.");

                    return CompletableFuture.completedFuture(cues);

                } catch (Exception exception) {
                    System.err.println(
                            "Subtitle cache could not be read; regenerating.");
                }
            }

            return generateUncached(videoPath, cachePath);
        });
    }

    /**
     * Computes a SHA-256 hash of the video contents.
     * Identical files receive the same cache key,
     * regardless of filename.
     */
    private Path getCachePath(Path videoPath) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            try (InputStream input =
                         Files.newInputStream(videoPath)) {

                byte[] buffer = new byte[65536];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            String hash =
                    HexFormat.of().formatHex(digest.digest());

            return CACHE_DIRECTORY.resolve(hash + ".json");

        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new CompletionException(exception);
        }
    }

    /**
     * Extracts audio, requests timestamps and caches the
     * successful API response.
     */
    private CompletableFuture<List<SubtitleCue>> generateUncached(
            Path videoPath,
            Path cachePath) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return AudioExtractor.extractAudio(videoPath);
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }

        }).thenCompose(audioPath ->

                apiClient.generateSubtitles(audioPath)
                        .thenApply(json -> {

                            try {
                                List<SubtitleCue> cues =
                                        List.copyOf(
                                                SubtitleParser.parse(json));

                                saveCache(cachePath, json);

                                return cues;

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

    /**
     * Writes the cache through a temporary file to avoid
     * leaving incomplete JSON after an interrupted write.
     */
    private void saveCache(Path cachePath, String json) {

        Path temporaryFile = null;

        try {
            Files.createDirectories(CACHE_DIRECTORY);

            temporaryFile = Files.createTempFile(
                    CACHE_DIRECTORY,
                    "subtitles-",
                    ".tmp");

            Files.writeString(
                    temporaryFile,
                    json,
                    StandardCharsets.UTF_8);

            try {
                Files.move(
                        temporaryFile,
                        cachePath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);

            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(
                        temporaryFile,
                        cachePath,
                        StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println(
                    "Saved subtitles to persistent cache.");

        } catch (IOException exception) {
            System.err.println(
                    "Could not save subtitle cache: "
                            + exception.getMessage());

        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // Temporary cleanup failure is non-fatal.
                }
            }
        }
    }
}
