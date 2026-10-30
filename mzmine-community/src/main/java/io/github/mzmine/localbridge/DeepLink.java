/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.localbridge;

import java.util.Optional;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/** An opaque, validated target received through the mzmine URL scheme. */
public record DeepLink(@Nullable UUID instanceId, @Nullable UUID projectId,
                       @NotNull Destination destination, @NotNull Optional<UUID> resourceId) {

  public DeepLink {
    Objects.requireNonNull(destination);
    Objects.requireNonNull(resourceId);
    if (destination == Destination.APP_CONNECTION) {
      if (instanceId != null || projectId != null || resourceId.isPresent()) {
        throw new IllegalArgumentException("Application links cannot carry resource identifiers");
      }
    } else {
      Objects.requireNonNull(instanceId);
      Objects.requireNonNull(projectId);
      if ((destination == Destination.WIZARD) != resourceId.isEmpty()) {
        throw new IllegalArgumentException("Only wizard destinations may omit a resource identifier");
      }
    }
  }

  public enum Destination {
    WIZARD("wizard"), PROPOSAL("proposal"), FEATURE_TABLE("feature-table"),
    APP_CONNECTION("app-connection");

    private final String pathSegment;

    Destination(final @NotNull String pathSegment) {
      this.pathSegment = pathSegment;
    }

    static @NotNull Destination fromPathSegment(final @NotNull String pathSegment) {
      for (final Destination value : values()) {
        if (value != APP_CONNECTION && value.pathSegment.equals(pathSegment)) {
          return value;
        }
      }
      throw new IllegalArgumentException("Unknown mzmine deep-link destination: " + pathSegment);
    }
  }
}
