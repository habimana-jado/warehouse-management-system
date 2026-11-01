package rw.afriteck.wms.web.support;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.security.UserContext;

import java.util.*;

@Component("navRegistry")
public class NavRegistry {

    private record ModuleDef(String key, String icon) {
    }

    private record Match(String module, NavItemDef item) {
    }

    // launcher order
    private static final List<ModuleDef> MODULES = List.of(
            new ModuleDef("admin", "zmdi zmdi-settings"),
            new ModuleDef("inbound", "zmdi zmdi-download"),
            new ModuleDef("inventory", "zmdi zmdi-storage"),
            new ModuleDef("outbound", "zmdi zmdi-upload"),
            new ModuleDef("operations", "zmdi zmdi-assignment-check"),
            new ModuleDef("reporting", "zmdi zmdi-chart"));

    private final UserContext user;
    private final Map<String, List<NavSectionDef>> sectionsByModule = new HashMap<>();

    public NavRegistry(ObjectProvider<NavContributor> contributors, UserContext user) {
        this.user = user;
        contributors.stream()
                .sorted(Comparator.comparingInt(NavContributor::order))
                .forEach(c -> sectionsByModule.computeIfAbsent(c.module(), k -> new ArrayList<>()).addAll(c.sections()));
    }

    /**
     * Navigation for a request path; empty when nobody is signed in.
     */
    public Optional<NavView> view(String path) {
        if (!authenticated()) return Optional.empty();

        List<NavModuleView> tiles = new ArrayList<>();
        for (ModuleDef m : MODULES) {
            List<NavItemDef> visible = visibleItems(m.key());
            if (!visible.isEmpty()) tiles.add(tile(m, visible.get(0).url()));
        }

        NavModuleView current = null;
        List<NavSectionView> sections = List.of();
        Match match = match(path);
        if (match != null) {
            ModuleDef m = MODULES.stream().filter(d -> d.key().equals(match.module())).findFirst().orElseThrow();
            List<NavItemDef> visible = visibleItems(m.key());
            current = tile(m, visible.isEmpty() ? "/" : visible.get(0).url());
            sections = sectionViews(m.key(), match.item().key());
        }
        return Optional.of(new NavView(tiles, current, sections));
    }

    /**
     * Where "/" should send the user, or empty to show the launcher.
     */
    public Optional<String> landing() {
        List<NavModuleView> tiles = view("/").map(NavView::modules).orElse(List.of());
        if (tiles.size() == 1) return Optional.of(tiles.get(0).homeUrl());
        boolean staff = user.hasRole("ADMIN") || user.hasRole("WAREHOUSE_MANAGER") || user.hasRole("VIEWER");
        if (!staff && tiles.stream().anyMatch(t -> t.key().equals("operations"))) {
            return Optional.ofNullable(urlOf("my-tasks"));
        }
        return Optional.empty();
    }

    // ---------- internals ----------

    private Match match(String path) {                       // longest entry URL wins, visible or not
        Match best = null;
        for (var e : sectionsByModule.entrySet()) {
            for (NavSectionDef s : e.getValue()) {
                for (NavItemDef it : s.items()) {
                    boolean hit = path.equals(it.url()) || path.startsWith(it.url() + "/");
                    if (hit && (best == null || it.url().length() > best.item().url().length())) {
                        best = new Match(e.getKey(), it);
                    }
                }
            }
        }
        return best;
    }

    private List<NavItemDef> visibleItems(String module) {
        return sectionsByModule.getOrDefault(module, List.of()).stream()
                .flatMap(s -> s.items().stream())
                .filter(this::canSee)
                .toList();
    }

    private List<NavSectionView> sectionViews(String module, String activeKey) {
        List<NavSectionView> out = new ArrayList<>();
        for (NavSectionDef s : sectionsByModule.getOrDefault(module, List.of())) {
            List<NavLink> links = s.items().stream().filter(this::canSee)
                    .map(i -> new NavLink(i.key(), "menu." + i.key(), i.icon(), i.url(), i.key().equals(activeKey)))
                    .toList();
            if (!links.isEmpty()) out.add(new NavSectionView(s.titleKey(), links));
        }
        return out;
    }

    private NavModuleView tile(ModuleDef m, String homeUrl) {
        return new NavModuleView(m.key(), "nav.module." + m.key(), "nav.module." + m.key() + ".desc", m.icon(), homeUrl);
    }

    private String urlOf(String key) {
        return sectionsByModule.values().stream().flatMap(List::stream).flatMap(s -> s.items().stream())
                .filter(i -> i.key().equals(key)).map(NavItemDef::url).findFirst().orElse(null);
    }

    private boolean canSee(NavItemDef item) {
        return item.roles().stream().anyMatch(user::hasRole);
    }

    private boolean authenticated() {
        try {
            user.currentUserId();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
