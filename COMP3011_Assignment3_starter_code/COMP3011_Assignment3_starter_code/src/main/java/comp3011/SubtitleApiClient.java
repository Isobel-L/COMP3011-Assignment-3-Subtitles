
package comp3011;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SubtitleApiClient {

    private static final URI API_URL =
            URI.create("http://localhost:8080/api/v1/subtitles");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CompletableFuture<String> generateSubtitles(Path audioPath) {

        try {
            byte[] audio = Files.readAllBytes(audioPath);

            String boundary = "Boundary-" + UUID.randomUUID();

            byte[] header = (
                    "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"audio.mp3\"\r\n" +
                    "Content-Type: audio/mpeg\r\n\r\n"
            ).getBytes(StandardCharsets.UTF_8);

            byte[] footer = (
                    "\r\n--" + boundary + "--\r\n"
            ).getBytes(StandardCharsets.UTF_8);

            byte[] body = new byte[header.length + audio.length + footer.length];

            System.arraycopy(header, 0, body, 0, header.length);
            System.arraycopy(audio, 0, body, header.length, audio.length);
            System.arraycopy(footer, 0, body,
                    header.length + audio.length, footer.length);

            HttpRequest request = HttpRequest.newBuilder(API_URL)
                    .timeout(Duration.ofMinutes(3))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            return client.sendAsync(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            ).thenApply(response -> {

                if (response.statusCode() != 200) {
                    throw new RuntimeException(
                            "Subtitle API failed with HTTP " +
                            response.statusCode() + ": " + response.body()
                    );
                }

                return response.body();
            });

        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}
