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
package com.google.jenkins.plugins.credentials.domains;

import static java.util.logging.Level.SEVERE;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

/**
 * This {@link ExtensionPoint} serves as a means for plugins to augment the
 * domain-requirement discovery process.  The intended usage is:
 * <code>
 *   List&lt;T&gt; list = DomainRequirementProvider.lookupRequirements(
 *      FooRequirement.class);
 * </code>
 * This will delegate to the various extension implementations to
 * {@link #provide(Class)} a {@link List} of requirements from things it
 * understands how to discover.  The expectation is that it will call:
 * <code>
 *   of(discoveredClass, type /* parameter to provide *{@literal /});
 * </code>
 * in order to perform the {@link RequiresDomain} resolution.
 *
 * @see RequiresDomain
 * @see DescribableDomainRequirementProvider
 */
public abstract class DomainRequirementProvider implements ExtensionPoint {
  private static final Logger logger =
      Logger.getLogger(DomainRequirementProvider.class.getName());

  /**
   * This hook is intended for providers to implement such that they can
   * surface custom class-discovery logic, on which they will call {@code of()}
   * to instantiate the elements returned.
   */
  protected abstract <T extends DomainRequirement> List<T> provide(
      Class<T> type);

  /**
   * The the entrypoint for requirement gathering, this static method delegates
   * to any registered providers to provide their set of discoverable
   * requirements.
   */
  public static <T extends DomainRequirement> List<T> lookupRequirements(
      Class<T> type) {
    ExtensionList<DomainRequirementProvider> providers;
    try {
      providers = Jenkins.get().getExtensionList(
          DomainRequirementProvider.class);
    } catch (Exception e) {
      logger.log(SEVERE, e.getMessage(), e);
      return ImmutableList.of();
    }

    List<T> result = Lists.newArrayList();
    for (DomainRequirementProvider provider : providers) {
      result.addAll(provider.provide(type));
    }
    return result;
  }

  /**
   * This is called by implementations of {@code provide()} to instantiate
   * the class specified by an actual attribute, if present.  It returns null
   * otherwise.
   */
  @Nullable
  public static <T extends DomainRequirement> T of(Class<?> type,
      Class<T> requirementType) {
    RequiresDomain requiresDomain = type.getAnnotation(RequiresDomain.class);
    if (requiresDomain == null) {
      // Not annotated with @RequiresDomain
      return null;
    }

    if (!requirementType.isAssignableFrom(requiresDomain.value())) {
      // The "type" filter excludes the annotation
      return null;
    }

    try {
      // Add an instance of the annotated requirement
      return (T) requiresDomain.value().getDeclaredConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
      logger.log(SEVERE, e.getMessage(), e);
      return null;
    }
  }
}
