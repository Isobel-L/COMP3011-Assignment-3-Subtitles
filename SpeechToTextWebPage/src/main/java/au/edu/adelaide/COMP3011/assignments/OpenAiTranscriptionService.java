
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
 * Transcription service using OpenAI Whisper.
 *
 * Supports plain text transcription and timestamped
 * transcription for subtitle generation.
 */
@Service
@Profile("!fake-transcription")
public class OpenAiTranscriptionService implements TranscriptionService {

    private final RestClient restClient;

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
     * Returns only the transcription text.
     * Preserves compatibility with the existing recording page.
     */
    @Override
    public String transcribe(MultipartFile file) throws IOException {
        return transcribeWithTimestamps(file).text();
    }

    /**
     * Returns text, duration, words and segments.
     */
    @Override
    public SubtitleResult transcribeWithTimestamps(MultipartFile file)
            throws IOException {

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

        // Convert Whisper words into our public subtitle records.
        List<SubtitleWord> words = response.words() == null
                ? List.of()
                : response.words().stream()
                    .map(word -> new SubtitleWord(
                            word.word(),
                            word.start(),
                            word.end()))
                    .toList();

        // Convert Whisper segments into our public subtitle records.
        List<SubtitleSegment> segments = response.segments() == null
                ? List.of()
                : response.segments().stream()
                    .map(segment -> new SubtitleSegment(
                            segment.text(),
                            segment.start(),
                            segment.end(),
                            segment.noSpeechProb(),
                            segment.aveLogProb()))
                    .toList();

        double duration = response.duration() == null
                ? 0.0
                : response.duration();

        if (response.duration() != null) {
            addAudioUsage((long) Math.ceil(duration));
        }

        System.out.println("Transcription: " + response.text());
        System.out.println("Word timestamps received: " + words.size());
        System.out.println("Segments received: " + segments.size());

        return new SubtitleResult(
                response.text(),
                duration,
                words,
                segments);
    }

    /**
     * Returns cumulative transcription usage in seconds.
     */
    @Override
    public synchronized Map<String, Long> getTokenUsage() {
        return Map.of("usage", cumulativeAudioSeconds);
    }

    private synchronized void addAudioUsage(long seconds) {
        cumulativeAudioSeconds += seconds;
    }

    /**
     * Internal representation of Whisper's JSON response.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TranscriptionResponse(
            String text,
            Double duration,
            List<Word> words,
            List<Segment> segments) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Word(
            String word,
            double start,
            double end) {
    }

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
