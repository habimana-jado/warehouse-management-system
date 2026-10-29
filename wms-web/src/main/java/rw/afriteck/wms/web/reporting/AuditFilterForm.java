package rw.afriteck.wms.web.reporting;

import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.reporting.enums.AuditSource;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Getter @Setter
public class AuditFilterForm extends DateRangeFilterForm {
    private Set<AuditSource> sources = EnumSet.allOf(AuditSource.class);
    private UUID actorId;                       // blank = everyone
    private int page = 0;
    private int size = 50;
}
