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
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bundle of thresholds, optional row coloring, and optional callbacks passed into each
 * {@link QualityCheck}. Immutable snapshot taken before dispatching to a background thread. The
 * coloring + callbacks are nullable so the quality pane can run standalone (without a host
 * dashboard); checks that want to render colored / clickable member labels should fall back to
 * plain text when the coloring is absent, and skip dispatching when the callbacks are absent.
 * <p>
 * {@code selectedMemberRow} is the live selection property (not a snapshot of its value): chips
 * built by a check read it for their bold-label style and write to it on click. Hosts typically
 * bidirectionally bind this property to their own row-selection so the quality pane and the
 * dashboard share one selection.
 */
public record QualityCheckContext(@NotNull RTTolerance rtTolerance,
                                  @NotNull MZTolerance mzTolerance,
                                  @NotNull MZTolerance ms2Tolerance,
                                  @Nullable ColorAssignment colorAssignment,
                                  @Nullable ObjectProperty<@Nullable FeatureListRow> selectedMemberRow,
                                  @Nullable Consumer<@NotNull QualityCheckEvent> onEvent,
                                  @Nullable ParameterSet checkParameters,
                                  @Nullable Consumer<@NotNull ParameterSet> onCheckParametersUpdate) {

}
