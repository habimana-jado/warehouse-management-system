package rw.afriteck.wms.foundation.enums;

public enum LocationType {

    ZONE(1),
    AISLE(2),
    RACK(3),
    BIN(4);

    private final int level;
    LocationType(int level) { this.level = level; }

    public boolean canBeChildOf(LocationType parent) { return this.level > parent.level; }
}
