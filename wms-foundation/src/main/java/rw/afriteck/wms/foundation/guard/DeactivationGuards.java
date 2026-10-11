package rw.afriteck.wms.foundation.guard;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeactivationGuards {

    private final ObjectProvider<DeactivationGuard> guards;

    public void check(DeactivationGuard.Target target, UUID id) {
        guards.orderedStream()
                .filter(g -> g.target() == target)
                .forEach(g -> g.assertCanDeactivate(id));
    }
}
