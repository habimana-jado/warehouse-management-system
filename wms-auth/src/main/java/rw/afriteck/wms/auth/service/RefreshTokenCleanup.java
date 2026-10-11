package rw.afriteck.wms.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanup {
    private final RefreshTokenService service;

    @Scheduled(cron = "0 30 3 * * *")
    public void purge() {
        log.info("Purged {} expired refresh tokens", service.purgeExpired());
    }
}
