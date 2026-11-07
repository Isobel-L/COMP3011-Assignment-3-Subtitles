
package au.edu.adelaide.COMP3011.assignments;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("fake-transcription")
class SubtitleApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void emptyAudioReturnsBadRequest() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "file", "empty.webm", "audio/webm", new byte[0]);

        mockMvc.perform(multipart("/api/v1/subtitles").file(audio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Audio file must not be empty."));
    }

    @Test
    void validAudioReturnsSubtitles() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "file", "sample.webm", "audio/webm",
                "fake audio".getBytes());

        mockMvc.perform(multipart("/api/v1/subtitles").file(audio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").exists())
                .andExpect(jsonPath("$.duration").isNumber())
                .andExpect(jsonPath("$.words").isArray())
                .andExpect(jsonPath("$.segments").isArray());
    }

    @Test
    void usageEndpointReturnsUsage() throws Exception {
        mockMvc.perform(get("/api/v1/global/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputTokens").isNumber())
                .andExpect(jsonPath("$.outputTokens").isNumber());
    }
    @Test
    void missingAudioReturnsClientError() throws Exception {
        mockMvc.perform(multipart("/api/v1/subtitles"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Required audio file is missing."));
    }
}
