package rw.afriteck.wms.reporting.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SqlArraysTest {

    @Test
    void uuids_empty() {
        assertEquals("{}", SqlArrays.uuids(List.of()));
    }

    @Test
    void uuids_nonEmpty() {
        UUID u1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID u2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertEquals("{00000000-0000-0000-0000-000000000001,00000000-0000-0000-0000-000000000002}",
                SqlArrays.uuids(List.of(u1, u2)));
    }
}
