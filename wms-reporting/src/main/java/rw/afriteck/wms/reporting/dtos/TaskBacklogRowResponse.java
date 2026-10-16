package rw.afriteck.wms.reporting.dtos;

public record TaskBacklogRowResponse(String type, long open, long assigned, long inProgress, long overdue) {}
