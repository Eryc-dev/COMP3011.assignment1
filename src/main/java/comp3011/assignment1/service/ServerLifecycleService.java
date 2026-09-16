package comp3011.assignment1.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import comp3011.assignment1.dto.UptimeResponse;
import comp3011.assignment1.exception.ShutdownInProgressException;

/**
 * Tracks server start time and coordinates graceful shutdown.
 *
 * <p>The server start time is the moment the Spring application (including the embedded web
 * server) finished starting. Until that event fires, the time this bean was created is used.
 */
@Service
public class ServerLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(ServerLifecycleService.class);

    /** Small delay so the HTTP 202 response is flushed before the context starts closing. */
    private static final long SHUTDOWN_DELAY_MS = 250;

    private final AtomicReference<Instant> serverStart;
    private final Runnable shutdownAction;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    /** Production constructor used by Spring. */
    @Autowired
    public ServerLifecycleService(ConfigurableApplicationContext context,
            @Value("${app.shutdown-exit-code:0}") int exitCode) {
        this(Instant.now(), () -> System.exit(SpringApplication.exit(context, () -> exitCode)));
    }

    /** Test-friendly constructor allowing a fixed start time and a fake shutdown action. */
    public ServerLifecycleService(Instant serverStart, Runnable shutdownAction) {
        this.serverStart = new AtomicReference<>(serverStart);
        this.shutdownAction = shutdownAction;
    }

    /** Records the UTC server start once the application and web server have started. */
    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted() {
        serverStart.set(Instant.now());
    }

    /** Computes uptime as the difference between now and the UTC server start. */
    public UptimeResponse uptime() {
        Instant start = serverStart.get();
        Instant now = Instant.now();
        double seconds = Math.max(0, Duration.between(start, now).toNanos() / 1_000_000_000.0);
        return new UptimeResponse(start.toString(), now.toString(), seconds);
    }

    /**
     * Starts a graceful shutdown on a separate thread and returns immediately.
     *
     * <p>Closing the Spring context with {@code server.shutdown=graceful} lets in-flight requests
     * finish before the web server stops.
     *
     * @throws ShutdownInProgressException if a shutdown was already requested
     */
    public void requestShutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            throw new ShutdownInProgressException();
        }
        log.info("Graceful shutdown requested via API.");
        Thread.ofPlatform().name("graceful-shutdown").daemon(false).start(() -> {
            try {
                Thread.sleep(SHUTDOWN_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            shutdownAction.run();
        });
    }

    /** Whether a shutdown has already been requested. */
    public boolean isShuttingDown() {
        return shuttingDown.get();
    }
}
