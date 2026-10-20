package rw.afriteck.wms.web.users;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.afriteck.wms.auth.dtos.AssignRolesRequest;
import rw.afriteck.wms.auth.dtos.AssignWarehousesRequest;
import rw.afriteck.wms.auth.dtos.ResetPasswordRequest;
import rw.afriteck.wms.auth.dtos.UserResponse;
import rw.afriteck.wms.auth.enums.RoleName;
import rw.afriteck.wms.auth.service.UserService;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize(WebAccess.ADMIN)
public class UserPageController {

    private static final Set<String> SORTS = Set.of("username", "fullName", "lastLoginAt");

    private final UserService users;
    private final WarehouseService warehouses;
    private final UiLinks uiLinks;

    @ModelAttribute("roles")
    List<RoleName> roles()     { return List.of(RoleName.values()); }
    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() { return List.of(15, 25, 50, 100); }

    @GetMapping
    String list(@RequestParam(required = false) String q,
                @PageableDefault(size = 15) Pageable pageable,
                HttpServletRequest request, Model model) {

        Pageable safe = PageRules.sanitize(pageable, SORTS, Sort.by("username"));   // adjust to the real signature
        PageResponse<UserResponse> result = PageResponse.of(users.search(q, safe));
        Map<UUID, String> names = warehouseNames();

        model.addAttribute("page", PageView.of(result, u -> toRow(u, names)));
        model.addAttribute("q", q);
        model.addAttribute("sortFullNameUrl", uiLinks.sort("fullName"));
        model.addAttribute("sortFullNameClass", uiLinks.sortClass("fullName"));
        return Htmx.isFragmentRequest(request) ? "users/list :: results" : "users/list";
    }

    @GetMapping("/new")
    String newForm(Model model) {
        model.addAttribute("form", new UserCreateForm());
        model.addAttribute("warehouseOptions", warehouses.findAllActive());
        return "users/create";
    }

    @PostMapping
    String create(@Valid @ModelAttribute("form") UserCreateForm form, BindingResult result,
                  Model model, RedirectAttributes redirect) {
        if (!form.getRoles().contains(RoleName.ADMIN) && form.getWarehouseIds().isEmpty()) {
            result.rejectValue("warehouseIds", "users.warehouse.required", "Select at least one warehouse");
        }
        if (!result.hasErrors()) {
            try {
                UserResponse created = users.create(form.toRequest());
                redirect.addFlashAttribute("flash", Flash.success("users.created", created.username()));
                return "redirect:/users";
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("USERNAME_EXISTS", "username",
                        "WEAK_PASSWORD", "password",
                        "UNKNOWN_WAREHOUSE", "warehouseIds"));
            }
        }
        model.addAttribute("warehouseOptions", warehouses.findAllActive());
        return "users/create";                                          // status 200 on purpose
    }

    @GetMapping("/{id}/edit")
    String edit(@PathVariable UUID id, Model model) {
        UserResponse u = users.get(id);
        model.addAttribute("userId", id);
        model.addAttribute("username", u.username());
        model.addAttribute("active", u.active());
        model.addAttribute("profileForm", UserProfileForm.from(u));
        model.addAttribute("accessForm", UserAccessForm.from(u));
        model.addAttribute("resetForm", new ResetPasswordForm());
        model.addAttribute("warehouseOptions", warehouses.findAllActive());
        return "users/edit";
    }

    @PostMapping("/{id}/profile")
    String updateProfile(@PathVariable UUID id,
                         @Valid @ModelAttribute("profileForm") UserProfileForm form,
                         BindingResult result, Model model) {
        model.addAttribute("userId", id);
        if (!result.hasErrors()) {
            try {
                users.update(id, form.toRequest());
                model.addAttribute("saved", true);
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of());
            }
        }
        return "users/edit :: profileCard";
    }

    @PostMapping("/{id}/access")
    String updateAccess(@PathVariable UUID id,
                        @Valid @ModelAttribute("accessForm") UserAccessForm form,
                        BindingResult result, Model model) {
        model.addAttribute("userId", id);
        model.addAttribute("warehouseOptions", warehouses.findAllActive());
        if (!form.getRoles().contains(RoleName.ADMIN) && form.getWarehouseIds().isEmpty()) {
            result.rejectValue("warehouseIds", "users.warehouse.required", "Select at least one warehouse");
        }
        if (!result.hasErrors()) {
            try {
                users.assignWarehouses(id, new AssignWarehousesRequest(form.getWarehouseIds()));
                users.assignRoles(id, new AssignRolesRequest(Set.copyOf(form.getRoles())));
                model.addAttribute("saved", true);
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("LAST_ADMIN", "roles", "UNKNOWN_WAREHOUSE", "warehouseIds"));
            }
        }
        return "users/edit :: accessCard";
    }

    @PostMapping("/{id}/account/status")
    String changeStatus(@PathVariable UUID id, @RequestParam boolean active, Model model) {
        try {
            model.addAttribute("active", users.setActive(id, active).active());
            model.addAttribute("statusSaved", true);
        } catch (BusinessException e) {                       // SELF_DISABLE, LAST_ADMIN
            model.addAttribute("active", users.get(id).active());
            model.addAttribute("statusError", e.getMessage());
        }
        model.addAttribute("userId", id);
        model.addAttribute("resetForm", new ResetPasswordForm());
        return "users/edit :: accountCard";
    }

    @PostMapping("/{id}/reset-password")
    String resetPassword(@PathVariable UUID id,
                         @Valid @ModelAttribute("resetForm") ResetPasswordForm form,
                         BindingResult result, Model model) {
        model.addAttribute("userId", id);
        model.addAttribute("active", users.get(id).active());
        if (!result.hasErrors()) {
            try {
                users.resetPassword(id, new ResetPasswordRequest(form.getNewPassword()));
                model.addAttribute("passwordReset", true);
                model.addAttribute("resetForm", new ResetPasswordForm());     // empty the field
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("WEAK_PASSWORD", "newPassword"));
            }
        }
        return "users/edit :: accountCard";
    }

    private String row(UserResponse u, Model model) {
        model.addAttribute("u", toRow(u, warehouseNames()));
        return "users/list :: row";
    }

    private Map<UUID, String> warehouseNames() {
        return warehouses.findAllActive().stream()
                .collect(Collectors.toMap(WarehouseResponse::id, WarehouseResponse::name));
    }

    private UserRow toRow(UserResponse u, Map<UUID, String> names) {
        List<String> labels = u.warehouseIds().stream().map(id -> names.getOrDefault(id, "?")).sorted().toList();
        return new UserRow(u.id(), u.fullName(), u.email(), u.username(), u.active(),
                List.copyOf(u.roles()), u.roles().contains(RoleName.ADMIN), labels, u.lastLoginAt());
    }
}
