/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.gui.preferences;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * When centroiding is requested for a native Agilent .d import (see "Try vendor centroiding"), this
 * selects how the AgilentReader produces the centroids. The general profile-vs-centroid decision
 * stays with the "Try vendor centroiding" option; this only chooses the centroid <i>source</i>.
 */
public enum AgilentCentroidingOption implements UniqueIdSupplier {
  /**
   * Reuse the vendor's stored centroids when the file carries them (fast, no recompute); the reader
   * recentroids the profile only for files that have no stored centroid representation.
   */
  PREFER_STORED,
  /**
   * Always read the profile and recompute the centroids; falls back to the stored centroids only
   * when the file has no profile representation.
   */
  PREFER_RECENTROIDED;

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case PREFER_STORED -> "prefer_stored";
      case PREFER_RECENTROIDED -> "prefer_recentroided";
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case PREFER_STORED -> "Prefer stored centroids";
      case PREFER_RECENTROIDED -> "Prefer recentroided";
    };
  }

  /**
   * The {@code centroidMode} value sent to the AgilentReader wire protocol when centroiding is
   * requested.
   */
  public String getWireCentroidMode() {
    return switch (this) {
      case PREFER_STORED -> "stored";
      case PREFER_RECENTROIDED -> "recentroid";
    };
  }

  public String getDescriptions() {
    return switch (this) {
      case PREFER_STORED -> """
          Use the vendor's stored centroids when present (fast); recentroid the profile only for
          files that carry no stored centroids.""";
      case PREFER_RECENTROIDED -> """
          Always read the profile data and recompute the centroids; falls back to stored centroids
          only when no profile data is available.""";
    };
  }

  public static String getTooltip() {
    return Arrays.stream(values()).map(opt -> opt.toString() + ": " + opt.getDescriptions())
        .collect(Collectors.joining("\n"));
  }
}
