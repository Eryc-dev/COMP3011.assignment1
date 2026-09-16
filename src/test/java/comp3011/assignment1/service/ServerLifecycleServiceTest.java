package comp3011.assignment1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import comp3011.assignment1.dto.UptimeResponse;
import comp3011.assignment1.exception.ShutdownInProgressException;

class ServerLifecycleServiceTest {

    @Test
    void uptimeIsDifferenceBetweenStartAndNow() {
        Instant start = Instant.now().minusSeconds(90);
        ServerLifecycleService service = new ServerLifecycleService(start, () -> { });

        UptimeResponse uptime = service.uptime();

        assertThat(uptime.utcServerStart()).isEqualTo(start.toString()).endsWith("Z");
        assertThat(uptime.utcNow()).endsWith("Z");
        assertThat(uptime.serverUptimeSeconds()).isBetween(90.0, 95.0);
        double expected = (Instant.parse(uptime.utcNow()).toEpochMilli() - start.toEpochMilli()) / 1000.0;
        assertThat(uptime.serverUptimeSeconds()).isCloseTo(expected, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void firstShutdownRunsActionAndSecondIsRejected() throws InterruptedException {
        CountDownLatch actionRan = new CountDownLatch(1);
        ServerLifecycleService service = new ServerLifecycleService(Instant.now(), actionRan::countDown);

        service.requestShutdown();

        assertThat(service.isShuttingDown()).isTrue();
        assertThat(actionRan.await(5, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(service::requestShutdown).isInstanceOf(ShutdownInProgressException.class);
    }
}
