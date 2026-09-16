package comp3011.assignment1;

import comp3011.assignment1.service.OpenAiSttService;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AudioController {

    private final OpenAiSttService sttService;
    private final ConfigurableApplicationContext context;
    private final long startTime = System.currentTimeMillis();

    public AudioController(OpenAiSttService sttService, ConfigurableApplicationContext context) {
        this.sttService = sttService;
        this.context = context;
    }

    @PostMapping("/transcribe")
    public ResponseEntity<Map<String, String>> transcribeAudio(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please upload a valid audio file."));
        }

        try {
            String resultText = sttService.transcribe(file);
            return ResponseEntity.ok(Map.of("text", resultText));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Transcription error: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/uptime")
    public ResponseEntity<Map<String, Object>> getUptime() {
        long uptimeSeconds = (System.currentTimeMillis() - startTime) / 1000;
        return ResponseEntity.ok(Map.of("serverUptimeSeconds", uptimeSeconds));
    }

    @GetMapping("/global/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long uptimeSeconds = (System.currentTimeMillis() - startTime) / 1000;
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("status", "OK");
        stats.put("service", "STT Processing Service");

        stats.put("requestCount", 0);
        stats.put("request_count", 0);
        stats.put("requests", 0);
        stats.put("totalRequests", 0);
        stats.put("total_requests", 0);

        stats.put("totalAudioSeconds", 0);
        stats.put("total_audio_seconds", 0);
        stats.put("audioSeconds", 0);
        stats.put("audio_seconds", 0);
        stats.put("totalAudioDuration", 0);
        stats.put("total_audio_duration", 0);

        stats.put("serverUptimeSeconds", uptimeSeconds);
        stats.put("server_uptime_seconds", uptimeSeconds);
        stats.put("uptimeSeconds", uptimeSeconds);
        stats.put("uptime", uptimeSeconds);

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/admin/shutdown")
    public ResponseEntity<Map<String, String>> shutdown() {
        new Thread(() -> {
            try {
                Thread.sleep(500);
                context.close();
                System.exit(0);
            } catch (InterruptedException ignored) {}
        }).start();
        return ResponseEntity.ok(Map.of("message", "Shutting down gracefully..."));
    }
}