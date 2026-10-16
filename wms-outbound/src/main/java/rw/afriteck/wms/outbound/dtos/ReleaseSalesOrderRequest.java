package rw.afriteck.wms.outbound.dtos;

import rw.afriteck.wms.operations.enums.TaskPriority;

import java.time.Instant;

/** All fields optional; the body itself may be empty. Releases every unreleased quantity of the order. */
public record ReleaseSalesOrderRequest(TaskPriority priority, Instant dueAt, String notes) {}   // priority null -> NORMAL

