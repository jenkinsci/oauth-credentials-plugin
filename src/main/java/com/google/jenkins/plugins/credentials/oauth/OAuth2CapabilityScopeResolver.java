package com.google.jenkins.plugins.credentials.oauth;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import java.util.Collection;

/**
 * Resolves a set of {@link OAuth2ScopeCapability} into a concrete, provider-specific
 * {@link OAuth2ScopeRequirement} populated with that provider's scope strings.
 */
public abstract class OAuth2CapabilityScopeResolver implements ExtensionPoint {

    /**
     * Whether this resolver knows how to handle the given credentials type.
     */
    public abstract boolean isApplicable(@NonNull Class<? extends OAuth2Credentials<?>> credentialsType);

    /**
     * Resolves the given capabilities into a requirement object for this provider.
     *
     * @return the populated requirement, or {@code null} if this provider does not
     *     implement one or more of the requested capabilities
     */
    @CheckForNull
    public abstract OAuth2ScopeRequirement resolveRequirement(@NonNull Collection<OAuth2ScopeCapability> capabilities);

    /**
     * Finds the applicable resolver for a credentials type and resolves it.
     *
     * @return the requirement, or {@code null} if no resolver is registered for the
     *     credentials type, or the applicable resolver couldn't satisfy the capabilities
     */
    @CheckForNull
    public static OAuth2ScopeRequirement resolve(
            @NonNull Class<? extends OAuth2Credentials<?>> credentialsType,
            @NonNull Collection<OAuth2ScopeCapability> capabilities) {
        for (OAuth2CapabilityScopeResolver resolver : ExtensionList.lookup(OAuth2CapabilityScopeResolver.class)) {
            if (resolver.isApplicable(credentialsType)) {
                return resolver.resolveRequirement(capabilities);
            }
        }
        return null;
    }
}