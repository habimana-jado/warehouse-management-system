package rw.afriteck.wms.common.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Explicit "any logged-in user may call this", so read endpoints are a visible decision. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("isAuthenticated()")
public @interface AnyAuthenticatedUser {}
