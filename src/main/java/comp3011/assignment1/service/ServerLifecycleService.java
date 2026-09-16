package comp3011.assignment1.service;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import comp3011.assignment1.dto.UptimeResponse;
import comp3011.assignment1.exception.ShutdownInProgressException;

/**
 * Tracks server start time and coordinates graceful shutdown.
 *
 * <p>The start time is taken from the JVM runtime so it reflects the actual process start rather
 * than when this bean happened to be created.
 */
@Service
public class ServerLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(ServerLifecycleService.class);

    /** Small delay so the HTTP 202 response is flushed before the context starts closing. */
    private static final long SHUTDOWN_DELAY_MS = 250;

    private final Instant serverStart;
    private final Runnable shutdownAction;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    /** Production constructor used by Spring. */
    @Autowired
    public ServerLifecycleService(ConfigurableApplicationContext context) {
        this(Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()),
                () -> System.exit(SpringApplication.exit(context, () -> 0)));
    }

    /** Test-friendly constructor allowing a fixed start time and a fake shutdown action. */
    public ServerLifecycleService(Instant serverStart, Runnable shutdownAction) {
        this.serverStart = serverStart;
        this.shutdownAction = shutdownAction;
    }

    /** Computes uptime as the difference between now and the UTC server start. */
    public UptimeResponse uptime() {
        Instant now = Instant.now();
        double seconds = Math.max(0, Duration.between(serverStart, now).toNanos() / 1_000_000_000.0);
        return new UptimeResponse(serverStart.toString(), now.toString(), seconds);
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
