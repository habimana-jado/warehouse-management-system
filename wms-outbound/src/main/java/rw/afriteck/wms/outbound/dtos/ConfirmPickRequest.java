package rw.afriteck.wms.outbound.dtos;

import java.util.List;

/** An empty `allocations` list with a `shortReason` closes the line as picked 0 (SHORT). */
public record ConfirmPickRequest(List<PickAllocationRequest> allocations, String shortReason, boolean expiryOverride) {}
//   expiryOverride is honored only for privileged callers (the controller passes `privileged`)

