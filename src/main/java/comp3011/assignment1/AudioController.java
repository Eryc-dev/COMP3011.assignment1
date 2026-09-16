package comp3011.assignment1;
 
import comp3011.assignment1.service.OpenAiSttService;
import comp3011.assignment1.service.TranscriptionResult;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
 
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
 
@RestController
@RequestMapping("/api/v1")
public class AudioController {
 
    private final OpenAiSttService sttService;
    private final ConfigurableApplicationContext context;
    private final Instant serverStart;
    private final AtomicLong inputTokens = new AtomicLong(0);
    private final AtomicLong outputTokens = new AtomicLong(0);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
 
    public AudioController(OpenAiSttService sttService, ConfigurableApplicationContext context) {
        this.serverStart = Instant.now();
        this.sttService = sttService;
        this.context = context;
    }
 
    @PostMapping("/transcribe")
    public ResponseEntity<Map<String, String>> transcribeAudio(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please upload a valid audio file."));
        }
 
        try {
            TranscriptionResult result = sttService.transcribe(file);
 
            inputTokens.addAndGet(result.inputTokens());
            outputTokens.addAndGet(result.outputTokens());
 
            return ResponseEntity.ok(Map.of("text", result.text()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Transcription error: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/uptime")
    public ResponseEntity<Map<String, Object>> getUptime() {
        Instant now = Instant.now();
        double uptimeSeconds = (now.toEpochMilli() - serverStart.toEpochMilli()) / 1000.0;
 
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("utcServerStart", serverStart.toString());
        body.put("utcNow", now.toString());
        body.put("serverUptimeSeconds", uptimeSeconds);
 
        return ResponseEntity.ok(body);
    }

    @GetMapping("/global/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputTokens", inputTokens.get());
        body.put("outputTokens", outputTokens.get());
 
        return ResponseEntity.ok(body);
    }

    @PostMapping("/admin/shutdown")
    public ResponseEntity<Map<String, Object>> shutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            Map<String, Object> conflict = errorResponse(
                    HttpStatus.CONFLICT,
                    "Graceful shutdown is already in progress.",
                    "/api/v1/admin/shutdown");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflict);
        }
 
        new Thread(() -> {
            try {
                Thread.sleep(500);
                context.close();
                System.exit(0);
            } catch (InterruptedException e) {
                // ignore
            }
        }).start();
 
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Graceful shutdown requested.");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }
 
    private Map<String, Object> errorResponse(HttpStatus status, String message, String path) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", Instant.now().toString());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        error.put("path", path);
        return error;
    }
}