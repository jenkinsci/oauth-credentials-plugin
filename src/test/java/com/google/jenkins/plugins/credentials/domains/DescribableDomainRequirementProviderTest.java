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

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.MockitoAnnotations;

import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.collect.ImmutableList;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;

/**
 * Tests for {@link DescribableDomainRequirementProvider}.
 */
public class DescribableDomainRequirementProviderTest {
  // Allow for testing using JUnit4, instead of JUnit3.
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  /**
   */
  public static class TestRequirement
      extends DomainRequirement {
  }

  /**
   * This is a trivial implementation of a {@link Builder} that
   * consumes {@code Credentials}.
   */
  @RequiresDomain(value = TestRequirement.class)
  public static class TestRobotBuilder extends Builder {
    public TestRobotBuilder() {
    }

    @Override
    public DescriptorImpl getDescriptor() {
      return (DescriptorImpl) Jenkins.get().getDescriptorOrDie(
        getClass());
    }

    /**
     * Descriptor for our trivial builder
     */
    @Extension
    public static final class DescriptorImpl
        extends Descriptor<Builder> {
      @Override
      public String getDisplayName() {
        return "Test Robot Builder";
      }
    }
  }

  private AutoCloseable openMocks;
  @Before
  public void setUp() throws Exception {
    openMocks = MockitoAnnotations.openMocks(this);
  }

  @After
  public void tearDown() throws Exception {
    openMocks.close();
  }

  @Test
  public void testDescribableRequirementDiscovery() throws Exception {
    List<TestRequirement> list =
        DomainRequirementProvider.lookupRequirements(TestRequirement.class);

    assertEquals(1, list.size());
  }

  @Test
  @WithoutJenkins
  public void testNoJenkinsInstance() throws Exception {
    // Make sure we don't crash if run on a slave, where no Jenkins
    // is present.
    List<TestRequirement> list =
        DomainRequirementProvider.lookupRequirements(TestRequirement.class);

    // However, we also shouldn't discover anything without Jenkins present.
    assertEquals(0, list.size());
  }

  /**
   */
  public static class BadTestRequirement
      extends DomainRequirement {
    public BadTestRequirement(String x) {
      // A trivial ctor is required, remove this
      // and things work fine.
    }
  }

  /**
   */
  @RequiresDomain(value = BadTestRequirement.class)
  public static class TestRobotBuilderBad extends Builder {
    public TestRobotBuilderBad() {
    }

    @Override
    public DescriptorImpl getDescriptor() {
      return (DescriptorImpl) Jenkins.get().getDescriptorOrDie(
        getClass());
    }

    /**
     * Descriptor for our trivial builder
     */
    @Extension
    public static final class DescriptorImpl
        extends Descriptor<Builder> {
      @Override
      public String getDisplayName() {
        return "Test Robot Builder (Bad)";
      }
    }
  }

  @Test
  public void testBadRequirement() throws Exception {
    List<BadTestRequirement> list =
        DomainRequirementProvider.lookupRequirements(BadTestRequirement.class);

    assertEquals(0, list.size());
  }

  /**
   */
  public static class AnotherBadTestRequirement
      extends DomainRequirement {
    private AnotherBadTestRequirement() {
      // A visible ctor is required, make this public
      // or protected and things work.
    }
  }

  /**
   */
  @RequiresDomain(value = AnotherBadTestRequirement.class)
  public static class TestRobotBuilderBadAgain extends Builder {
    public TestRobotBuilderBadAgain() {
    }

    @Override
    public DescriptorImpl getDescriptor() {
      return (DescriptorImpl) Jenkins.get().getDescriptorOrDie(
        getClass());
    }

    /**
     * Descriptor for our trivial builder
     */
    @Extension
    public static final class DescriptorImpl
        extends Descriptor<Builder> {
      @Override
      public String getDisplayName() {
        return "Test Robot Builder (Bad Again)";
      }
    }
  }

  @Test
  public void testBadRequirementProtection() throws Exception {
    List<AnotherBadTestRequirement> list =
        DomainRequirementProvider.lookupRequirements(
            AnotherBadTestRequirement.class);

    assertEquals(0, list.size());
  }

  private static String GOOD_SCOPE1 = "foo";
  private static String GOOD_SCOPE2 = "baz";
  private static String BAD_SCOPE = "bar";
  private static Collection<String> GOOD_SCOPES =
      ImmutableList.of(GOOD_SCOPE1, GOOD_SCOPE2);
  private static Collection<String> BAD_SCOPES =
      ImmutableList.of(GOOD_SCOPE1, BAD_SCOPE);
}
