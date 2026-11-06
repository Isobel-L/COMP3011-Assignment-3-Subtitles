package au.edu.adelaide.COMP3011.assignments;

import java.io.IOException;

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

        return transcriptionService.transcribeWithTimestamps(file);
    }
}

