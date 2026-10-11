package rw.afriteck.wms.operations.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.security.WorkerLookup;
import rw.afriteck.wms.operations.dtos.ProductivityFilter;
import rw.afriteck.wms.operations.dtos.WorkerProductivityResponse;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.operations.repository.TaskProductivityRepository;
import rw.afriteck.wms.operations.repository.WorkerLineStats;
import rw.afriteck.wms.operations.repository.WorkerTaskStats;
import rw.afriteck.wms.operations.service.TaskProductivityService;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TaskProductivityServiceImpl implements TaskProductivityService {

    private final TaskProductivityRepository repository;
    private final WorkerLookup workerLookup;

    @Override
    @Transactional(readOnly = true)
    public List<WorkerProductivityResponse> byWorker(ProductivityFilter f) {
        if (!f.from().isBefore(f.to())) {
            throw new BusinessException("INVALID_DATE_RANGE", "'from' must be before 'to'");
        }
        List<String> types = (f.type() == null
                ? Arrays.stream(TaskType.values()) : Stream.of(f.type())).map(Enum::name).toList();

        Map<UUID, WorkerTaskStats> taskStats = repository
                .taskStatsByWorker(f.warehouseId(), types, f.from(), f.to()).stream()
                .collect(Collectors.toMap(WorkerTaskStats::getUserId, Function.identity()));
        Map<UUID, WorkerLineStats> lineStats = repository
                .lineStatsByWorker(f.warehouseId(), types, f.from(), f.to()).stream()
                .collect(Collectors.toMap(WorkerLineStats::getUserId, Function.identity()));

        Set<UUID> ids = new HashSet<>(taskStats.keySet());
        ids.addAll(lineStats.keySet());
        Map<UUID, String> names = ids.isEmpty() ? Map.of() : workerLookup.findDisplayNames(ids);

        return ids.stream().map(id -> {
                    WorkerTaskStats ts = taskStats.get(id);
                    WorkerLineStats ls = lineStats.get(id);
                    Long avg = (ts == null || ts.getAvgTaskSeconds() == null)
                            ? null : Math.round(ts.getAvgTaskSeconds());
                    return new WorkerProductivityResponse(
                            id, names.get(id),
                            ts == null ? 0 : ts.getTasksCompleted(),
                            ls == null ? 0 : ls.getLinesCompleted(),
                            ls == null ? BigDecimal.ZERO : ls.getTotalQtyDone(),
                            ls == null ? 0 : ls.getShortLines(),
                            avg);
                })
                .sorted(Comparator.comparingLong(WorkerProductivityResponse::tasksCompleted).reversed()
                        .thenComparing(r -> r.userName() == null ? "" : r.userName()))
                .toList();
    }
}
