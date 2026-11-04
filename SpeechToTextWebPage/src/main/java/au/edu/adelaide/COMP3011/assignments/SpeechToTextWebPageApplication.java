package au.edu.adelaide.COMP3011.assignments;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@SpringBootApplication
public class SpeechToTextWebPageApplication {

    @SuppressWarnings("null")
    public static void main(String[] args) {
        SpringApplication.run(SpeechToTextWebPageApplication.class, args);
    }

}

@Controller
class WebController {
    @GetMapping("/") // Tell Spring Web this for for handling the web site home page.
    public String recordPage() {
        return "forward:/record.html";
    }
}

@RestController
@RequestMapping("/api/v1/record") // Tell Spring Web this is for handling the internal /api/v1/record/* routes.
class RecordApiController {

    private final TranscriptionService transcriptionService;

    RecordApiController(TranscriptionService transcriptionService) {
        System.out.println("...constructing RecordApiController instance.");
        this.transcriptionService = transcriptionService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "ready", true,
                "message", "Recorder API is running");
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam MultipartFile file) throws IOException {
        String result_transcript;
        try {
            result_transcript = transcriptionService.transcribe(file);
            System.out.println("...success.");
        } catch (IOException e) {
            result_transcript = "<IOException>";
            e.printStackTrace();
        }
        System.out.println(String.format("transcription is '%s'", result_transcript));
        return Map.of(
                "success", true,
                "fileName", file.getOriginalFilename(),
                "size", file.getSize(),
                "transcript", result_transcript);
    }
}

@RestController
@RequestMapping("/api/v1/admin") // Tell Spring Web this for handling the public /api/v1/admin/* routes.
class AdminApiController {

    private final ConfigurableApplicationContext context; // Used in shutdown()
    private boolean inShutdown; // Used in shutdown()
    private final Instant serverStart;

    AdminApiController(ConfigurableApplicationContext context) {
        this.context = context;
        this.inShutdown = false;
        this.serverStart = Instant.now();
    }

    @GetMapping("/uptime")
    public Map<String, Object> uptime() {
        Instant now = Instant.now();
        Duration uptime = Duration.between(this.serverStart, now);
        double uptime_in_seconds = uptime.toMillis() / 1000.0;
        System.out.println("Server start: " + this.serverStart);
        System.out.println("         Now: " + now);
        System.out.println("      Uptime: " + uptime_in_seconds + "s");
        return Map.of(
                "utcServerStart", this.serverStart.toString(),
                "utcNow", now.toString(),
                "serverUptimeSeconds", uptime_in_seconds);
    }

    @SuppressWarnings("null")
    @PostMapping("/shutdown")
    public ResponseEntity<String> shutdown() {
        if (this.inShutdown) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Graceful shutdown is already in progress.");
        } else {
            System.out.println("Commencing graceful shutdown...");
            this.inShutdown = true;
            long shutdownNotBefore = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            // Launch a thread to run SpringApplication.exit, which shuts everything down
            // nicely.
            new Thread(() -> {
                waitUntil(shutdownNotBefore);
                SpringApplication.exit(context, this::returnZero);
            }).start();
            return ResponseEntity.accepted().body("Graceful shutdown accepted.");
        }
    }

    private int returnZero() {
        System.out.println("... graceful shutdown done.");
        return 0;
    }

    private static void waitUntil(long deadlineNanos) {
        while (true) {
            long remainingNanos = deadlineNanos - System.nanoTime();

            if (remainingNanos <= 0) {
                return;
            }

            try {
                TimeUnit.NANOSECONDS.sleep(remainingNanos);
            } catch (InterruptedException ignored) {
                // Continue waiting until the deadline.
            }
        }
    }

}

@RestController
@RequestMapping("/api/v1/global") // Tell Spring this is for handling the public /api/v1/global/* routes.
class GlobalApiController {

    private final TranscriptionService transcriptionService;

    GlobalApiController(TranscriptionService transcriptionService) {
        System.out.println("...constructing GlobalApiController instance.");
        this.transcriptionService = transcriptionService;
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return this.transcriptionService.getTokenUsage();
    }
}
