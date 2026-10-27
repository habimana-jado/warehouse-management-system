package rw.afriteck.wms.web.inbound;

/** What the receive form needs to know about one return line's item. tracking is NONE, LOT or SERIAL. */
public record ReturnLineInfo(String tracking, boolean expiryTracked) {}