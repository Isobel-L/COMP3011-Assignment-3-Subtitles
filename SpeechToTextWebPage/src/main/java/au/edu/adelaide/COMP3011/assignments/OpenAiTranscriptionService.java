package au.edu.adelaide.COMP3011.assignments;

import java.io.IOException;
import java.util.List;
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Transcription service that communicates with OpenAI's Whisper API.
 *
 * Requests word-level and segment-level timestamps for subtitle generation.
 * Tracks cumulative audio usage in seconds.
 */
@Service
@Profile("!fake-transcription")
public class OpenAiTranscriptionService implements TranscriptionService {

    private final RestClient restClient;

    // Total duration of audio successfully transcribed.
    private long cumulativeAudioSeconds = 0;

    public OpenAiTranscriptionService(
            @Value("${openai.api.key}") String apiKey) {

        System.out.println("Constructing OpenAiTranscriptionService...");

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        System.out.println("...done.");
    }

    /**
     * Sends an audio file to Whisper and returns its transcription text.
     *
     * The timestamp information is also parsed, ready for use when
     * implementing the subtitle endpoint.
     */
    @Override
    public String transcribe(MultipartFile file) throws IOException {

        ByteArrayResource audio = new ByteArrayResource(file.getBytes()) {

            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("model", "whisper-1");
        body.add("response_format", "verbose_json");
        body.add("timestamp_granularities[]", "word");
        body.add("timestamp_granularities[]", "segment");
        body.add("file", audio);

        TranscriptionResponse response = restClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(TranscriptionResponse.class);

        if (response == null) {
            throw new IOException("OpenAI returned an empty response.");
        }

        System.out.println("Transcription: " + response.text());

        if (response.words() != null) {
            System.out.println("Word timestamps received: "
                    + response.words().size());
        }

        if (response.segments() != null) {
            System.out.println("Segments received: "
                    + response.segments().size());
        }

        if (response.duration() != null) {
            addAudioUsage((long) Math.ceil(response.duration()));
        }

        return response.text();
    }

    /**
     * Returns cumulative audio usage in seconds.
     *
     * This method retains the existing interface method name
     * until we update the rest of the server.
     */
    @Override
    public synchronized Map<String, Long> getTokenUsage() {
        return Map.of("usage", cumulativeAudioSeconds);
    }

    private synchronized void addAudioUsage(long seconds) {
        cumulativeAudioSeconds += seconds;
    }

    /**
     * Represents Whisper's verbose JSON response.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TranscriptionResponse(
            String text,
            Double duration,
            List<Word> words,
            List<Segment> segments) {
    }

    /**
     * Individual word and its timestamps.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Word(
            String word,
            double start,
            double end) {
    }

    /**
     * Larger speech segment with timestamps and
     * confidence information.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Segment(
            double start,
            double end,
            String text,

            @JsonProperty("no_speech_prob")
            double noSpeechProb,

            @JsonProperty("avg_logprob")
            double aveLogProb) {
    }
}