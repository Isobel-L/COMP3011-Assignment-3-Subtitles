package au.edu.adelaide.COMP3011.assignments;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "fake-transcription.delay=PT3S",
        "logging.level.root=WARN",
        "spring.main.banner-mode=off",
        "debug=false"
}, classes = SpeechToTextWebPageApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("fake-transcription")
class SpeechToTextWebPageApplicationTests {

    private static final Duration THROUGHPUT_WARM_UP_DURATION = Duration.ofSeconds(5);
    private static final Duration THROUGHPUT_MEASUREMENT_DURATION = Duration.ofSeconds(20);
    private static final int CONCURRENT_CLIENTS = 250;

    @Autowired
    private MockMvc mockMvc;

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
        // This just is a smoke test for Spring. At runtime, it answers the questions:
        // 1) Can Spring create all the required Controllers and Services (aka Beans)?
        // 2) Can it inject everything unambiguously?
        // 3) Are there any missing configuration properties.
    }

    @Test
    void uploadUsesFakeTranscriptionService() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "file",
                "sample.webm",
                "audio/webm",
                "fake audio".getBytes());

        this.mockMvc.perform(multipart("/api/v1/record/upload").file(audio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.transcript", containsString("Fake transcript for sample.webm")));
    }

    /**
     * This test is a load test. Its performance (and the performance of the
     * production system) can be controlled through application.properties. Under
     * default settings, output from this test on my 32GB Microsoft Windows 11 Pro
     * laptop Processor Intel(R) Core(TM) i7-10870H CPU @ 2.20GHz, 2208 Mhz, 8
     * Core(s), 16 Logical Processor(s) I was consistently getting:
     * measuredCompleted = 1400
     * measuredFailed = 0
     * totalCompleted = 1650
     * totalFailed = 0
     * clients = 250
     * warmup = 5.00s
     * measurement = 20.00s
     * throughput = 70.00 requests/s
     *
     * 70 requests per second can get through 350 clients in a 5 second window,
     * which is probably okay to meet the 200+ non functional requirement, but this
     * may not be the case on TITAN. So for safety, either enabling virtual threads
     * or extending the default thread pool size from 200 to 300 as shown in
     * application.properties increases the safety to:
     * measuredCompleted = 1750
     * measuredFailed = 0
     * totalCompleted = 1650
     * totalFailed = 0
     * clients = 250
     * warmup = 5.00s
     * measurement = 20.00s
     * throughput = 87.50 requests/s
     */
    @Test
    void uploadThroughputWithFakeTranscriptionService() throws Exception {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + this.port)
                .build();
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_CLIENTS);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicLong measurementStartNanos = new AtomicLong();
        AtomicLong measurementEndNanos = new AtomicLong();
        AtomicLong completedRequestsDuringMeasurement = new AtomicLong();
        AtomicLong failedRequestsDuringMeasurement = new AtomicLong();
        AtomicLong totalCompletedRequests = new AtomicLong();
        AtomicLong totalFailedRequests = new AtomicLong();
        PrintStream originalOut = System.out;

        try {
            Future<?>[] workers = new Future<?>[CONCURRENT_CLIENTS];
            for (int i = 0; i < CONCURRENT_CLIENTS; i++) {
                int clientNumber = i;
                workers[i] = executor.submit(() -> {
                    startGate.await();
                    while (System.nanoTime() < measurementEndNanos.get()) {
                        try {
                            uploadFakeAudio(client, clientNumber);
                            totalCompletedRequests.incrementAndGet();
                            long completedAtNanos = System.nanoTime();
                            if (isInsideMeasurementWindow(
                                    completedAtNanos,
                                    measurementStartNanos.get(),
                                    measurementEndNanos.get())) {
                                completedRequestsDuringMeasurement.incrementAndGet();
                            }
                        } catch (Exception e) {
                            totalFailedRequests.incrementAndGet();
                            long failedAtNanos = System.nanoTime();
                            if (isInsideMeasurementWindow(
                                    failedAtNanos,
                                    measurementStartNanos.get(),
                                    measurementEndNanos.get())) {
                                failedRequestsDuringMeasurement.incrementAndGet();
                            }
                        }
                    }
                    return null;
                });
            }

            long startNanos = System.nanoTime();
            measurementStartNanos.set(startNanos + THROUGHPUT_WARM_UP_DURATION.toNanos());
            measurementEndNanos.set(measurementStartNanos.get() + THROUGHPUT_MEASUREMENT_DURATION.toNanos());
            System.setOut(new PrintStream(OutputStream.nullOutputStream()));
            startGate.countDown();

            for (Future<?> worker : workers) {
                worker.get(
                        THROUGHPUT_WARM_UP_DURATION.plus(THROUGHPUT_MEASUREMENT_DURATION).plusSeconds(10).toMillis(),
                        TimeUnit.MILLISECONDS);
            }

            double measurementSeconds = THROUGHPUT_MEASUREMENT_DURATION.toNanos() / 1_000_000_000.0;
            double throughput = completedRequestsDuringMeasurement.get() / measurementSeconds;
            System.setOut(originalOut);
            System.out.printf("%n" +
                    "Fake transcription steady-state throughput stats:%n" +
                    "  measuredCompleted = %d%n" +
                    "  measuredFailed    = %d%n" +
                    "  totalCompleted    = %d%n" +
                    "  totalFailed       = %d%n" +
                    "  clients           = %d%n" +
                    "  warmup            = %.2fs%n" +
                    "  measurement       = %.2fs%n" +
                    "  throughput        = %.2f requests/s%n" +
                    "%n",
                    completedRequestsDuringMeasurement.get(),
                    failedRequestsDuringMeasurement.get(),
                    totalCompletedRequests.get(),
                    totalFailedRequests.get(),
                    CONCURRENT_CLIENTS,
                    THROUGHPUT_WARM_UP_DURATION.toNanos() / 1_000_000_000.0,
                    measurementSeconds,
                    throughput);
            assertEquals(0, totalFailedRequests.get(), "Every fake transcription request should complete successfully");
            assertTrue(
                    completedRequestsDuringMeasurement.get() > 0,
                    "Throughput test should complete at least one request during the measurement window");
        } finally {
            System.setOut(originalOut);
            executor.shutdownNow();
        }
    }

    private static boolean isInsideMeasurementWindow(long timestampNanos, long measurementStartNanos,
            long measurementEndNanos) {
        return timestampNanos >= measurementStartNanos && timestampNanos < measurementEndNanos;
    }

    private static void uploadFakeAudio(RestClient client, int clientNumber) {
        ByteArrayResource audio = new ByteArrayResource("fake audio".getBytes()) {
            @Override
            public String getFilename() {
                return "throughput-" + clientNumber + ".webm";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", audio);

        client.post()
                .uri("/api/v1/record/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(Map.class);
    }

}
