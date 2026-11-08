
package comp3011;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Converts the subtitle API's JSON response
 * into Java subtitle cues.
 */
public class SubtitleParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static List<SubtitleCue> parse(String json) throws Exception {

        JsonNode root = MAPPER.readTree(json);
        JsonNode segments = root.path("segments");

        if (!segments.isArray()) {
            throw new IllegalArgumentException(
                    "Subtitle response does not contain a segments array.");
        }

        List<SubtitleCue> cues = new ArrayList<>();

        for (JsonNode segment : segments) {

            String text = segment.path("text").asText("").trim();
            double start = segment.path("start").asDouble();
            double end = segment.path("end").asDouble();

            if (text.isBlank() || start < 0 || end <= start) {
                continue;
            }

            cues.add(new SubtitleCue(text, start, end));
        }

        return List.copyOf(cues);
    }
}
