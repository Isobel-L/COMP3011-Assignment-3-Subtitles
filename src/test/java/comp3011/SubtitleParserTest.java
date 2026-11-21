
package comp3011;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class SubtitleParserTest {

    @Test
    void parsesSubtitleSegments() throws Exception {

        String json = """
            {
              "text": "Hello everyone",
              "duration": 3.0,
              "segments": [
                {
                  "text": "Hello",
                  "start": 0.0,
                  "end": 1.0
                },
                {
                  "text": "everyone",
                  "start": 1.0,
                  "end": 3.0
                }
              ]
            }
            """;

        List<SubtitleCue> cues = SubtitleParser.parse(json);

        assertEquals(2, cues.size());

        assertEquals("Hello", cues.get(0).text());
        assertEquals(0.0, cues.get(0).start());
        assertEquals(1.0, cues.get(0).end());

        assertEquals("everyone", cues.get(1).text());

        assertTrue(cues.get(0).isActiveAt(0.5));
        assertFalse(cues.get(0).isActiveAt(1.5));

        assertTrue(cues.get(1).isActiveAt(2.0));
    }

    @Test
    void skipsInvalidSubtitleSegments() throws Exception {

        String json = """
            {
              "segments": [
                {"text": "", "start": 0.0, "end": 1.0},
                {"text": "Invalid", "start": 3.0, "end": 2.0},
                {"text": "Valid", "start": 1.0, "end": 2.0}
              ]
            }
            """;

        List<SubtitleCue> cues = SubtitleParser.parse(json);

        assertEquals(1, cues.size());
        assertEquals("Valid", cues.get(0).text());
    }
}
