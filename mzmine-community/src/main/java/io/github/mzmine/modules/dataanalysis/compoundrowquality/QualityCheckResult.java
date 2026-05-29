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

package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.List;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Single quality-check outcome for a CompoundRow. Carries the shared chrome data (check type,
 * status, involved member rows) used by the wrapper to render the status icon and route
 * click-to-navigate, and exposes two hooks that let each subclass own the visual content of its
 * card: {@link #buildMainPane()} for the always-visible title-bar content next to the status icon,
 * and {@link #buildSubPane()} for the expandable detail content.
 * <p>
 * Subclass directly when a check needs custom rendering (e.g. a mirror plot, a structure
 * thumbnail). For plain text summaries with bullet detail lines, use
 * {@link DefaultQualityCheckResult}.
 */
public abstract class QualityCheckResult {

  protected final @NotNull QualityCheckType type;
  protected final @NotNull QualityCheckStatus status;
  protected final @NotNull List<@NotNull FeatureListRow> involvedRows;

  protected QualityCheckResult(@NotNull QualityCheckType type, @NotNull QualityCheckStatus status,
      @NotNull List<@NotNull FeatureListRow> involvedRows) {
    this.type = type;
    this.status = status;
    this.involvedRows = List.copyOf(involvedRows);
  }

  public final @NotNull QualityCheckType type() {
    return type;
  }

  public final @NotNull QualityCheckStatus status() {
    return status;
  }

  public final @NotNull List<@NotNull FeatureListRow> involvedRows() {
    return involvedRows;
  }

  /// Builds the always-visible content shown to the right of the status icon inside the card's
  /// title bar. The result owns the type label, summary text, and any extra inline controls.
  /// Called on the FX thread.
  public abstract @NotNull Region buildMainPane();

  /// Builds the expandable detail content shown below the title bar when the user expands the
  /// card. Return {@code null} to mark the card as non-collapsible. Called on the FX thread.
  public abstract @Nullable Region buildSubPane();

  /// When true the wrapper drops the icon-column indent on the sub pane so it can use the full card
  /// width (handy for grid-heavy sub panes like the annotation-agreement structure grid). Other
  /// checks should leave this {@code false} — their indented sub panes line up with the status-icon
  /// column for a consistent look.
  public boolean wantsFullWidthSubPane() {
    return false;
  }
}
