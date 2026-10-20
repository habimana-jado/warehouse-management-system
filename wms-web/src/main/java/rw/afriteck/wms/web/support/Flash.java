package rw.afriteck.wms.web.support;

public record Flash(String type, String key, Object[] args) {
    public static Flash success(String key, Object... args) { return new Flash("success", key, args); }
    public static Flash error(String key, Object... args)   { return new Flash("danger", key, args); }
}
