/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jenkins.plugins.credentials.oauth;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.DomainSpecificationDescriptor;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.jenkins.plugins.credentials.domains.DomainRequirementProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * The base class for provider-specific specifications, instantiated with the
 * provider-specific requirement type to which the specification should apply.
 *
 * NOTE: The reason for provider-specific paired implementations of scope /
 * requirement is due to the fact that an OAuth2 credential is per-provider
 * (e.g. Google, Facebook, GitHub).
 *
 * This base implementation, returns {@code UNKNOWN} from {@link #test}
 * if the passed requirement doesn't match our descriptor's provider
 * requirement.  It then delegates to {@link #_test}, a hook that by default
 * returns {@code POSITIVE}/{@code NEGATIVE} depending on whether
 * {@code specifiedScopes} is a superset of the required scopes.
 *
 * @param <T> The type of requirements to which this specification may apply
 */
public abstract class OAuth2ScopeSpecification<T extends OAuth2ScopeRequirement>
    extends DomainSpecification {
  protected OAuth2ScopeSpecification(Collection<String> specifiedScopes) {
    this.specifiedScopes = checkNotNull(specifiedScopes);
  }

  /**
   * Tests the scope against this specification.
   *
   * @param requirement The set of requirements to validate against this
   * specification
   * @return the result of the test.
   */
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  public final Result test(DomainRequirement requirement) {
    Class<T> providerRequirement = getDescriptor().getProviderRequirement();

    if (!providerRequirement.isInstance(requirement)) {
      return Result.UNKNOWN;
    }

    // NOTE: This cast is checked by the isInstance above
    return _test((T) requirement);
  }

  /**
   * Surfaces a hook for implementations to override or extend the
   * default functionality of simply matching the set of scopes.
   */
  protected Result _test(T requirement) {
    for (String scope : requirement.getScopes()) {
      if (!specifiedScopes.contains(scope)) {
        // We are missing this required scope
        return Result.NEGATIVE;
      }
    }
    // We matched all the scopes
    return Result.POSITIVE;
  }

  /**
   * Surfaces the set of scopes specified by this requirement for
   * jelly roundtripping.
   */
  public Collection<String> getSpecifiedScopes() {
    return Collections.unmodifiableCollection(specifiedScopes);
  }
  private final Collection<String> specifiedScopes;

  /**
   * {@inheritDoc}
   */
  @Override
  public Descriptor<T> getDescriptor() {
    return (Descriptor<T>) super.getDescriptor();
  }

  /**
   * The base descriptor for specification extensions.  This carries the
   * class of requirements to which this specification should apply.
   */
  public abstract static class Descriptor<T extends OAuth2ScopeRequirement>
      extends DomainSpecificationDescriptor {
    public Descriptor(Class<T> providerRequirement) {
      this.providerRequirement = checkNotNull(providerRequirement);
    }

    /**
     * Fetches the names and values of the set of scopes consumed by clients of
     * this plugin.
     */
    public Collection<String> getScopeItems() {
      List<T> requirements = DomainRequirementProvider.lookupRequirements(
          getProviderRequirement());

      Set<String> result = Sets.newHashSet();
      for (T required : requirements) {
        Iterables.addAll(result, required.getScopes());
      }
      return result;
    }

    /**
     * Retrieve the class of {@link DomainRequirement}s to which our associated
     * specifications should apply.
     */
    public Class<T> getProviderRequirement() {
      return providerRequirement;
    }
    private final Class<T> providerRequirement;
  }
}
