package rw.afriteck.wms.web.inventory;

@FunctionalInterface
interface PanelAction<T> {
    T execute();
}
