package rw.afriteck.wms.web.support;

import rw.afriteck.wms.reporting.dtos.AuditEventResponse;

public record AuditRow(AuditEventResponse event, String link) {}      // link null = no screen for this document

