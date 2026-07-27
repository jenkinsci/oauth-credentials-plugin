package com.google.jenkins.plugins.credentials.oauth;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;

/**
 * A logical capability an OAuth2 consumer needs.
 * <p>
 * NOTE: A small set of common capabilities is predefined here. Additional
 * capabilities can be created with {@link #of(String)} when both caller and
 * provider agree on the id.
 */
public final class OAuth2ScopeCapability {

  public static final OAuth2ScopeCapability SEND_EMAIL =
      new OAuth2ScopeCapability("SEND_EMAIL");

  private final String id;

  private OAuth2ScopeCapability(@NonNull String id) {
    this.id = Objects.requireNonNull(id, "id");
  }

  /**
   * Creates a capability not covered by predefined constants.
   *
   * @param id unique capability id, e.g. {@code "SEND_EMAIL"}
   * @return capability instance for the given id
   */
  @NonNull
  public static OAuth2ScopeCapability of(@NonNull String id) {
    return new OAuth2ScopeCapability(id);
  }

  @NonNull
  public String getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OAuth2ScopeCapability)) {
      return false;
    }
    return id.equals(((OAuth2ScopeCapability) o).id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return id;
  }
}