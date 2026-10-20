package rw.afriteck.wms.web.support;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@Component("uiLinks")
public class UiLinks {

    /** Current URL with another page number: every filter is preserved. */
    public String page(int page) {
        return base().replaceQueryParam("page", page).build().toUriString();
    }

    public String sort(String field) {
        String dir = (field + ",asc").equals(param("sort")) ? "desc" : "asc";
        return base().replaceQueryParam("sort", field + "," + dir).replaceQueryParam("page", 0).build().toUriString();
    }

    public String sortClass(String field) {
        String s = param("sort");
        if ((field + ",asc").equals(s))  return "sorted-asc";
        if ((field + ",desc").equals(s)) return "sorted-desc";
        return "";
    }

    public Map<String, SortLink> sorts(String... fields) {
        Map<String, SortLink> map = new LinkedHashMap<>();
        for (String f : fields) {
            map.put(f, new SortLink(sort(f), sortClass(f)));
        }
        return map;
    }
    private UriComponentsBuilder base() { return ServletUriComponentsBuilder.fromCurrentRequest(); }

    private String param(String name) {
        var attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attrs.getRequest().getParameter(name);
    }
}

