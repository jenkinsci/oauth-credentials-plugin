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
import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.Secret;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.DomainSpecification.Result;
import com.cloudbees.plugins.credentials.domains.SchemeSpecification;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.domains.DomainRequirementProvider;

import hudson.Extension;
import hudson.security.ACL;
import jenkins.model.Jenkins;

/**
 * Tests for {@link OAuth2ScopeSpecification}.
 */
public class OAuth2ScopeSpecificationTest {
  // Allow for testing using JUnit4, instead of JUnit3.
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  /**
   */
  public static class TestRequirement
      extends OAuth2ScopeRequirement {
    public TestRequirement(Collection<String> scopes) {
      this.scopes = scopes;
    }

    @Override
    public Collection<String> getScopes() {
      return scopes;
    }
    private final Collection<String> scopes;
  }

  /**
   */
  public static class TestGoodRequirement
      extends TestRequirement {
    public TestGoodRequirement() {
      super(GOOD_SCOPES);
    }
  }

  /**
   */
  public static class TestBadRequirement
      extends TestRequirement {
    public TestBadRequirement() {
      super(BAD_SCOPES);
    }
  }

  /**
   */
  public static class TestSpec
      extends OAuth2ScopeSpecification<TestRequirement> {
    public TestSpec(Collection<String> scopes) {
      super(scopes);
    }

    /**
     */
    @Extension
    public static class DescriptorImpl
        extends OAuth2ScopeSpecification.Descriptor<TestRequirement> {
      public DescriptorImpl() {
        super(TestRequirement.class);
      }

      @Override
      public String getDisplayName() {
        return "blah";
      }
    }
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @WithoutJenkins
  public void testBasics() throws Exception {
    TestSpec spec = new TestSpec(GOOD_SCOPES);

    assertThat(spec.getSpecifiedScopes(),
        hasItems(GOOD_SCOPE1, GOOD_SCOPE2));
  }

  @Test
  public void testUnknownRequirement() throws Exception {
    TestSpec spec = new TestSpec(GOOD_SCOPES);

    OAuth2ScopeRequirement requirement = new OAuth2ScopeRequirement() {
        @Override
        public Collection<String> getScopes() {
          return GOOD_SCOPES;
        }
      };

    // Verify that even with the right scopes the type kind excludes
    // the specification from matching this requirement
    assertEquals(Result.UNKNOWN, spec.test(requirement));
  }

  @Test
  public void testKnownRequirements() throws Exception {
    TestSpec spec = new TestSpec(GOOD_SCOPES);

    TestRequirement goodReq = new TestGoodRequirement();
    TestRequirement badReq = new TestBadRequirement();

    // Verify that with the right type of requirement that
    // good scopes match POSITIVEly and bad scopes match NEGATIVEly
    assertEquals(Result.POSITIVE, spec.test(goodReq));
    assertEquals(Result.NEGATIVE, spec.test(badReq));
  }

  @Test
  public void testWithCustom_test() throws Exception {
    TestRequirement badReq = new TestBadRequirement();

    final TestSpec forDescriptor = new TestSpec(GOOD_SCOPES);

    TestSpec spec = new TestSpec(GOOD_SCOPES) {
        @Override
        protected Result _test(TestRequirement foo) {
          return Result.POSITIVE;
        }

        @Override
        public Descriptor<TestRequirement> getDescriptor() {
          return forDescriptor.getDescriptor();
        }
      };

    // Verify that if we *override* the '_test' method that we can
    // even let bad scopes through.
    assertEquals(Result.POSITIVE, spec.test(badReq));
  }

  /**
   */
  @TestExtension
  public static class CustomProvider extends DomainRequirementProvider {
    @Override
    protected <T extends DomainRequirement> List<T> provide(Class<T> type) {
      if (type.isAssignableFrom(TestRequirement.class)) {
        return ImmutableList.<T>of((T) new TestGoodRequirement());
      }
      return ImmutableList.of();
    }
  }

  @Test
  public void testGetScopeItems() throws Exception {
    TestSpec forDescriptor = new TestSpec(ImmutableList.<String>of());

    Collection<String> discovered =
        forDescriptor.getDescriptor().getScopeItems();

    assertEquals(2, discovered.size());
    assertThat(discovered, hasItems(GOOD_SCOPE1, GOOD_SCOPE2));
    assertThat(discovered, not(hasItems(BAD_SCOPE)));
  }

  private OAuth2Credentials mockCredentials = new MockOAuthCredentials();

  /**
   * Verify that credentials that appear outside of a domain with
   * an OAuth2 specification can be matched
   */
  @Test
  public void testUnscopedLookup() throws Exception {
    SystemCredentialsProvider.getInstance().getCredentials()
        .add(mockCredentials);

    List<OAuth2Credentials> matchingCredentials =
        CredentialsProvider.lookupCredentials(OAuth2Credentials.class,
            Jenkins.get(), ACL.SYSTEM, new TestGoodRequirement());

    assertThat(matchingCredentials, hasItems(mockCredentials));
  }

  /**
   * Verify that credentials that appear inside of a domain where the oauth
   * specification provides no scopes is not matched.
   */
  @Test
  public void testBadDomainScopedLookupEmptySpec() throws Exception {
    TestSpec spec = new TestSpec(ImmutableList.<String>of());
    Domain domain = new Domain("domain name", "domain description",
        ImmutableList.<DomainSpecification>of(spec));

    SystemCredentialsProvider.getInstance().getDomainCredentialsMap()
        .put(domain, ImmutableList.<Credentials>of(mockCredentials));

    List<OAuth2Credentials> matchingCredentials =
        CredentialsProvider.lookupCredentials(OAuth2Credentials.class,
            Jenkins.get(), ACL.SYSTEM, new TestGoodRequirement());

    assertThat(matchingCredentials, not(hasItems(mockCredentials)));
  }

  /**
   * Verify that credentials that appear inside of a domain without any
   * specification are found and returned.
   */
  @Test
  public void testGoodDomainScopedLookupUnspecifiedDomain() throws Exception {
    Domain domain = new Domain("domain name", "domain description",
        ImmutableList.<DomainSpecification>of());

    SystemCredentialsProvider.getInstance().getDomainCredentialsMap()
        .put(domain, ImmutableList.<Credentials>of(mockCredentials));

    List<OAuth2Credentials> matchingCredentials =
        CredentialsProvider.lookupCredentials(OAuth2Credentials.class,
            Jenkins.get(), ACL.SYSTEM, new TestGoodRequirement());

    assertThat(matchingCredentials, hasItems(mockCredentials));
  }

  /**
   * Verify that credentials that appear inside of a domain with a single
   * unrelated specification (inapplicable) are found and returned.
   */
  @Test
  public void testGoodDomainScopedLookupUnrelatedSpecification()
      throws Exception {
    SchemeSpecification spec = new SchemeSpecification("http");
    Domain domain = new Domain("domain name", "domain description",
        ImmutableList.<DomainSpecification>of(spec));

    SystemCredentialsProvider.getInstance().getDomainCredentialsMap()
        .put(domain, ImmutableList.<Credentials>of(mockCredentials));

    List<OAuth2Credentials> matchingCredentials =
        CredentialsProvider.lookupCredentials(OAuth2Credentials.class,
            Jenkins.get(), ACL.SYSTEM, new TestGoodRequirement());

    assertThat(matchingCredentials, hasItems(mockCredentials));
  }

  /**
   * Verify that credentials that appear inside of a domain with a matching
   * specification are found and returned.
   */
  @Test
  public void testGoodDomainScopedLookup() throws Exception {
    TestSpec spec = new TestSpec(GOOD_SCOPES);
    Domain domain = new Domain("domain name", "domain description",
        ImmutableList.<DomainSpecification>of(spec));

    SystemCredentialsProvider.getInstance().getDomainCredentialsMap()
        .put(domain, ImmutableList.<Credentials>of(mockCredentials));

    List<OAuth2Credentials> matchingCredentials =
        CredentialsProvider.lookupCredentials(OAuth2Credentials.class,
            Jenkins.get(), ACL.SYSTEM, new TestGoodRequirement());

    assertThat(matchingCredentials, hasItems(mockCredentials));
  }

  /**
   * Verify that credentials that appear inside of a domain with a matching
   * superset specification are found and returned.
   */
  @Test
  public void testGoodDomainScopedLookupSubset() throws Exception {
    TestSpec spec = new TestSpec(GOOD_SCOPES);
    Domain domain = new Domain("domain name", "domain description",
        ImmutableList.<DomainSpecification>of(spec));

    SystemCredentialsProvider.getInstance().getDomainCredentialsMap()
        .put(domain, ImmutableList.<Credentials>of(mockCredentials));

    List<OAuth2Credentials> matchingCredentials =
        CredentialsProvider.lookupCredentials(OAuth2Credentials.class,
            Jenkins.get(), ACL.SYSTEM, new TestRequirement(
                ImmutableList.of(GOOD_SCOPE1)));

    assertThat(matchingCredentials, hasItems(mockCredentials));
  }

  /**
   * Verify that credentials that appear inside of a domain with a mismatched
   * specification are NOT returned
   */
  @Test
  public void testBadDomainScopedLookup() throws Exception {
    TestSpec spec = new TestSpec(GOOD_SCOPES);
    Domain domain = new Domain("domain name", "domain description",
        ImmutableList.<DomainSpecification>of(spec));

    SystemCredentialsProvider.getInstance().getDomainCredentialsMap()
        .put(domain, ImmutableList.<Credentials>of(mockCredentials));

    List<OAuth2Credentials> matchingCredentials =
        CredentialsProvider.lookupCredentials(OAuth2Credentials.class,
            Jenkins.get(), ACL.SYSTEM, new TestBadRequirement());

    assertThat(matchingCredentials, not(hasItems(mockCredentials)));
  }

  /**
   * Verify that credentials that appear inside of a domain with a subset of the
   * scopes required are NOT returned
   */
  @Test
  public void testBadDomainScopedLookupSuperset() throws Exception {
    TestSpec spec = new TestSpec(ImmutableList.of(GOOD_SCOPE1));
    Domain domain = new Domain("domain name", "domain description",
        ImmutableList.<DomainSpecification>of(spec));

    SystemCredentialsProvider.getInstance().getDomainCredentialsMap()
        .put(domain, ImmutableList.<Credentials>of(mockCredentials));

    List<OAuth2Credentials> matchingCredentials =
        CredentialsProvider.lookupCredentials(OAuth2Credentials.class,
            Jenkins.get(), ACL.SYSTEM, new TestGoodRequirement());

    assertThat(matchingCredentials, not(hasItems(mockCredentials)));
  }

  /**
   * Verify that credentials that appear inside of a domain with a partially
   * overlapping set of scopes are NOT matched.
   */
  @Test
  public void testBadDomainScopedLookupOverlap() throws Exception {
    TestSpec spec = new TestSpec(GOOD_SCOPES);
    Domain domain = new Domain("domain name", "domain description",
        ImmutableList.<DomainSpecification>of(spec));

    SystemCredentialsProvider.getInstance().getDomainCredentialsMap()
        .put(domain, ImmutableList.<Credentials>of(mockCredentials));

    List<OAuth2Credentials> matchingCredentials =
        CredentialsProvider.lookupCredentials(OAuth2Credentials.class,
            Jenkins.get(), ACL.SYSTEM, new TestBadRequirement());

    assertThat(matchingCredentials, not(hasItems(mockCredentials)));
  }

  public static final class MockOAuthCredentials implements OAuth2Credentials {

    @Override
    public Secret getAccessToken(OAuth2ScopeRequirement requirement) {
      return null;
    }

    @Override
    public CredentialsScope getScope() {
      return null;
    }

    @NonNull
    @Override
    public CredentialsDescriptor getDescriptor() {
      return (DescriptorImpl) Jenkins.get().getDescriptorOrDie(getClass());
    }

    @TestExtension
    public static class DescriptorImpl extends CredentialsDescriptor {
      @Override
      @NonNull
      public String getDisplayName() {
        return "OAuth Mock Credentials";
      }
    }
  }


  private static String GOOD_SCOPE1 = "foo";
  private static String GOOD_SCOPE2 = "baz";
  private static String BAD_SCOPE = "bar";
  private static Collection<String> GOOD_SCOPES =
      ImmutableList.of(GOOD_SCOPE1, GOOD_SCOPE2);
  private static Collection<String> BAD_SCOPES =
      ImmutableList.of(GOOD_SCOPE1, BAD_SCOPE);
}
