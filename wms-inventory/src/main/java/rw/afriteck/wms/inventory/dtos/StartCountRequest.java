package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.operations.enums.TaskPriority;

import java.time.Instant;

public record StartCountRequest(TaskPriority priority, Instant dueAt) {}                    // both optional (task defaults)

