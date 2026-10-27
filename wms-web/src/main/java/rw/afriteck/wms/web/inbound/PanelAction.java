package rw.afriteck.wms.web.inbound;

@FunctionalInterface
interface PanelAction<T> {
    T execute();
}