/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.visualization.otherdetectors.integrationplot.IntegrationPlotController;
import org.jetbrains.annotations.NotNull;

public enum IntegrationTransfer implements UniqueIdSupplier {
  ALL, ONLY_MISSING, NONE;

  @Override
  public String toString() {
    return switch (this) {
      case ALL -> "All";
      case ONLY_MISSING -> "Only missing";
      case NONE -> "None";
    };
  }

  public String getToolTip() {
    return switch (this) {
      case ALL ->
          "%s: Transfers a single re-integration to to all other files, overriding current integrations.".formatted(
              this.toString());
      case ONLY_MISSING ->
          "%s: Transfers a re-integration to all files in which no feature was detected.".formatted(
              this.toString());
      case NONE ->
          "%s: Does not transfer re-integration across files. All files must be re-integrated manually.".formatted(
              this.toString());
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case ALL -> "all";
      case ONLY_MISSING -> "only_missing";
      case NONE -> "none";
    };
  }

  boolean appliesTo(FeatureListRow r, RawDataFile f, IntegrationPlotController c) {
    return switch (this) {
      case ALL -> true;
      case NONE -> false;
      case ONLY_MISSING -> r.getFeature(f) == null || c.getIntegratedFeatures().isEmpty();
    };
  }
}
