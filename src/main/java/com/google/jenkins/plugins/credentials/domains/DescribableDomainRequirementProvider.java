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

import java.util.List;

import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.collect.Lists;

import hudson.Extension;
import hudson.ExtensionComponent;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * This implementation of {@link DomainRequirementProvider} implements
 * support for discovering {@link DomainRequirement}s annotated on
 * {@link hudson.model.Describable} classes by walking the {@link Descriptor}s
 * registered with {@link Jenkins}.
 *
 * TODO(mattmoor): should we allow the annotation on the descriptor itself?
 */
@Extension
public class DescribableDomainRequirementProvider
    extends DomainRequirementProvider {
  /**
   * {@inheritDoc}
   */
  @Override
  protected <T extends DomainRequirement> List<T> provide(Class<T> type) {
    ExtensionList<Descriptor> extensions =
        Jenkins.get().getExtensionList(Descriptor.class);

    List<T> result = Lists.newArrayList();
    for (ExtensionComponent<Descriptor> component :
             extensions.getComponents()) {
      Descriptor descriptor = component.getInstance();

      T element = of(descriptor.clazz, type);
      if (element != null) {
        // Add an instance of the annotated requirement
        result.add(element);
      }
    }
    return result;
  }
}
