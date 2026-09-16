package comp3011.assignment1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment1.dto.ShutdownResponse;
import comp3011.assignment1.dto.UptimeResponse;
import comp3011.assignment1.service.ServerLifecycleService;

/** Administration endpoints defined under the "Administration" tag of the YAML specification. */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final ServerLifecycleService lifecycleService;

    public AdminController(ServerLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    /** {@code GET /api/v1/admin/uptime} - server start, current time and uptime in seconds. */
    @GetMapping("/uptime")
    public UptimeResponse uptime() {
        return lifecycleService.uptime();
    }

    /**
     * {@code POST /api/v1/admin/shutdown} - 202 when accepted; 409 (via the exception handler)
     * when a shutdown is already in progress.
     */
    @PostMapping("/shutdown")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ShutdownResponse shutdown() {
        lifecycleService.requestShutdown();
        return new ShutdownResponse("Graceful shutdown requested.");
    }
}
