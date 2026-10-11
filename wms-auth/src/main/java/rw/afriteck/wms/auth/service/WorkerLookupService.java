package rw.afriteck.wms.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.auth.entity.User;
import rw.afriteck.wms.auth.enums.RoleName;
import rw.afriteck.wms.auth.repository.UserRepository;
import rw.afriteck.wms.common.security.WorkerLookup;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkerLookupService implements WorkerLookup {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isAssignableToWarehouse(UUID userId, UUID warehouseId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .map(u -> u.getRoles().contains(RoleName.ADMIN)
                        || u.getWarehouseIds().contains(warehouseId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> findDisplayNames(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> u.getFullName() != null && !u.getFullName().isBlank()
                                ? u.getFullName() : u.getUsername()));
    }
}
