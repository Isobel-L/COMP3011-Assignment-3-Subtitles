package au.edu.adelaide.COMP3011.assignments;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST endpoint for generating timestamped subtitles.
 */
@RestController
public class SubtitleApiController {

    private static final double MAX_NO_SPEECH_PROB = 0.6;
    private static final double MIN_AVG_LOG_PROB = -1.0;

    private final TranscriptionService transcriptionService;

    public SubtitleApiController(
            TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @PostMapping(
        value = "/api/v1/subtitles",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public SubtitleResult generateSubtitles(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                "Audio file must not be empty."
            );
        }

        SubtitleResult result =
                transcriptionService.transcribeWithTimestamps(file);

        List<SubtitleSegment> filteredSegments =
                result.segments().stream()
                    .filter(segment ->
                        segment.noSpeechProb() < MAX_NO_SPEECH_PROB)
                    .filter(segment ->
                        segment.aveLogProb() > MIN_AVG_LOG_PROB)
                    .toList();

        List<SubtitleWord> filteredWords =
                result.words().stream()
                    .filter(word ->
                        filteredSegments.stream().anyMatch(segment ->
                            word.start() >= segment.start() - 0.05 &&
                            word.end() <= segment.end() + 0.05))
                    .toList();

        return new SubtitleResult(
                result.text(),
                result.duration(),
                filteredWords,
                filteredSegments
        );
    }
}
