package au.edu.adelaide.COMP3011.assignments;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * This implementation of TranscriptionService goes out to OpenAI and keeps a
 * threadsafe (but not persistent) tally of token usage. It expects
 * openai.api.key to be set as a property when the constructor is called. It is
 * constructed and injected when the "fake-transcription" profile is active -
 * i.e. whenever we are out of a test environment.
 */
@Service
@Profile("!fake-transcription")
public class OpenAiTranscriptionService implements TranscriptionService {

    private final RestClient restClient;

    // Just use simple transient storage for our token count tallies.
    private long cumulativeInputTokenCount = 0;
    private long cumulativeOutputTokenCount = 0;
    // WARNING: Not intrinsically thread safe, so we only access them through the
    // synchronized methods below to ensure an atomic update/view of both.

    OpenAiTranscriptionService(@Value("${openai.api.key}") String apiKey) {
        System.out.println("Constructing OpenAiTranscriptionService...");
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        System.out.println("...done.");
    }

    @Override
    public String transcribe(MultipartFile file) throws IOException {
        ByteArrayResource audio = new ByteArrayResource(file.getBytes()) {
            @SuppressWarnings("null")
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("model", "gpt-4o-mini-transcribe");
        body.add("file", audio);

        TranscriptionResponse response = restClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(TranscriptionResponse.class);
        System.out.println("... response text:" + response.text());
        System.out.println("... input token count = " + response.usage().inputTokens());
        System.out.println("... output token count = " + response.usage().outputTokens());

        // Keeps stats up to date
        this.addTokenUsage(response.usage().inputTokens(), response.usage().outputTokens());

        return response.text();
    }

    @Override
    public synchronized Map<String, Long> getTokenUsage() {
        return Map.of(
                "inputTokens", this.cumulativeInputTokenCount,
                "outputTokens", this.cumulativeOutputTokenCount);
    }

    private synchronized void addTokenUsage(long inputTokens, long outputTokens) {
        this.cumulativeInputTokenCount += inputTokens;
        this.cumulativeOutputTokenCount += outputTokens;
    }

    private record TranscriptionResponse(
            String text,
            Usage usage) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private record Usage(
            long inputTokens,
            long outputTokens) {
    }
}
