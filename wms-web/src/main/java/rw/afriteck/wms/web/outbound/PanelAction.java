package rw.afriteck.wms.web.outbound;

/**
 * Run a service action and re-render the panel. This package keeps its own copy (the inbound one is package-private).
 */
@FunctionalInterface
interface PanelAction<T> {
    T execute();
}
