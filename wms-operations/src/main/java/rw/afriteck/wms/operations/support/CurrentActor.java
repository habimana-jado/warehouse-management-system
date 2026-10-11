package rw.afriteck.wms.operations.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.security.UserContext;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentActor {

    private final UserContext userContext;

    /** Current user id, or null when there is no authenticated user (jobs, system calls). */
    public UUID idOrNull() {
        try {
            return userContext.currentUserId();
        } catch (IllegalStateException e) {
            return null;
        }
    }
}