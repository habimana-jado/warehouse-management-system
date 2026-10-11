package rw.afriteck.wms.operations.mapper;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.operations.dtos.TaskEventResponse;
import rw.afriteck.wms.operations.dtos.TaskLineResponse;
import rw.afriteck.wms.operations.dtos.TaskResponse;
import rw.afriteck.wms.operations.dtos.TaskSummaryResponse;
import rw.afriteck.wms.operations.entity.Task;
import rw.afriteck.wms.operations.entity.TaskEvent;
import rw.afriteck.wms.operations.entity.TaskLine;

import java.util.Map;
import java.util.UUID;

public final class TaskMapper {

    private TaskMapper() {}

    public static TaskResponse toResponse(Task t,
                                          Map<UUID, String> userNames,
                                          Map<UUID, ItemResponse> items,
                                          Map<UUID, LocationResponse> locations) {
        return new TaskResponse(
                t.getId(),
                t.getVersion(),
                t.getTaskNumber(),
                t.getType(), t.getStatus(), t.getPriority(),
                t.getWarehouseId(), t.getZoneLocationId(),
                t.getReferenceType(), t.getReferenceId(), t.getReferenceNumber(),
                t.getAssigneeId(), userName(userNames, t.getAssigneeId()),
                t.getAssignedAt(), t.getStartedAt(), t.getCompletedAt(), t.getDueAt(),
                t.getCancelReason(), t.getNotes(), t.getCreatedAt(),
                t.getLines().stream().map(l -> toLineResponse(l, items, locations)).toList());
    }

    public static TaskLineResponse toLineResponse(TaskLine l,
                                                  Map<UUID, ItemResponse> items,
                                                  Map<UUID, LocationResponse> locations) {
        ItemResponse item = l.getItemId() == null ? null : items.get(l.getItemId());
        LocationResponse from = l.getFromLocationId() == null ? null : locations.get(l.getFromLocationId());
        LocationResponse to = l.getToLocationId() == null ? null : locations.get(l.getToLocationId());
        return new TaskLineResponse(
                l.getId(), l.getLineNo(),
                l.getItemId(),
                item == null ? null : item.sku(),
                item == null ? null : item.name(),
                l.getLotNumber(), l.getSerialNumber(),
                l.getFromLocationId(), from == null ? null : from.code(),
                l.getToLocationId(), to == null ? null : to.code(),
                l.getPlannedQty(), l.getDoneQty(),
                l.getStatus(), l.getReferenceLineId(), l.getShortReason());
    }

    public static TaskSummaryResponse toSummary(Task t, int lineCount, int doneLineCount,
                                                Map<UUID, String> userNames) {
        return new TaskSummaryResponse(
                t.getId(), t.getTaskNumber(),
                t.getType(), t.getStatus(), t.getPriority(),
                t.getWarehouseId(),
                t.getReferenceType(), t.getReferenceId(), t.getReferenceNumber(),
                t.getAssigneeId(), userName(userNames, t.getAssigneeId()),
                t.getDueAt(), t.getCreatedAt(),
                lineCount, doneLineCount);
    }

    public static TaskEventResponse toResponse(TaskEvent e, Map<UUID, String> userNames) {
        return new TaskEventResponse(
                e.getId(), e.getEventType(),
                e.getFromStatus(), e.getToStatus(),
                e.getActorUserId(), userName(userNames, e.getActorUserId()),
                e.getAssigneeId(), userName(userNames, e.getAssigneeId()),
                e.getOccurredAt(), e.getNote());
    }

    private static String userName(Map<UUID, String> names, UUID id) {
        return id == null ? null : names.get(id);       // null check: Map.of().get(null) throws
    }
}
