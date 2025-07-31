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

package io.github.mzmine.parameters.parametertypes;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeIntComponent;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupSelection;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupSelectionComponent;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MinimumSamplesInOneMetadataGroupComponent extends FlowPane {

  private final AbsoluteAndRelativeIntComponent minSamples;
  private final MetadataGroupSelectionComponent metadata;

  public MinimumSamplesInOneMetadataGroupComponent(AbsoluteAndRelativeIntComponent minSamples,
      MetadataGroupSelectionComponent metadata, MinimumSamplesFilterConfig value) {
    final HBox metadataBox = FxLayout.newHBox(Insets.EMPTY, new Label("in column"),
        metadata.getColumnField(), new Label("in"), FxLabels.newBoldLabel("THIS"),
        new Label("group"), metadata.getGroupField());
    super(minSamples, metadataBox);
    this.minSamples = minSamples;
    this.metadata = metadata;
    setVgap(FxLayout.DEFAULT_SPACE);
    setHgap(FxLayout.DEFAULT_SPACE);

    setValue(value);
  }

  public void setValue(@Nullable MinimumSamplesFilterConfig value) {
    if (value == null) {
      metadata.setValue(MetadataGroupSelection.NONE);
      minSamples.setValue(MinimumSamplesFilterConfig.DEFAULT.minSamples());
      return;
    }

    final String column = requireNonNullElse(value.columnName(), "");
    final String groupStr = requireNonNullElse(value.group(), "");
    metadata.setValue(new MetadataGroupSelection(column, groupStr));
    minSamples.setValue(value.minSamples());
  }

  @NotNull
  public MinimumSamplesFilterConfig getValue() {
    final MetadataGroupSelection selection = metadata.getValue();
    return new MinimumSamplesFilterConfig(minSamples.getValue(), selection.columnName(),
        selection.groupStr());
  }
}
