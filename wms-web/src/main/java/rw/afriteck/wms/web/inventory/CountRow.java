package rw.afriteck.wms.web.inventory;

import rw.afriteck.wms.inventory.dtos.CountResponse;

public record CountRow(CountResponse count, String zoneName) {}

