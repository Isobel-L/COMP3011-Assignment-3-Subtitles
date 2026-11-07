
package au.edu.adelaide.COMP3011.assignments;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsageApiController {

    private final TranscriptionService transcriptionService;

    public UsageApiController(
            TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @GetMapping("/api/v1/global/usage")
    public Map<String, Long> getGlobalUsage() {
        return transcriptionService.getTokenUsage();
    }
}
