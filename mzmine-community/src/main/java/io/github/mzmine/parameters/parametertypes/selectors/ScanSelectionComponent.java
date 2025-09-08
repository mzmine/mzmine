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

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.EmbeddedComponentOptions;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleComponent;
import javafx.scene.control.Button;
import javafx.scene.text.Text;

public class ScanSelectionComponent extends OptionalModuleComponent {

  private final Text textDescription = new Text();

  public ScanSelectionComponent(final ScanSelectionFiltersParameters embeddedParameters,
      final EmbeddedComponentOptions viewOption, final String title, final boolean active) {
    super(embeddedParameters, viewOption, title, false, active);

    var clearbtn = new Button("Clear");
    topPane.getChildren().add(clearbtn);
    clearbtn.setOnAction(event -> setSelection(ScanSelection.ALL_SCANS));

    var selection = createSelection(embeddedParameters);
    textDescription.setText(selection.toShortDescription());
    textDescription.setWrappingWidth(350);
    if (active) {
      topPane.getChildren().add(textDescription);
    }
  }

  private ScanSelectionFiltersParameters getSelectionParameters() {
    return (ScanSelectionFiltersParameters) getEmbeddedParameters();
  }

  private void setSelection(final ScanSelection selection) {
    getSelectionParameters().setFilter(selection);
    setParameterValuesToComponents(getEmbeddedParameters());
  }

  public ScanSelection createSelection(ParameterSet embeddedParameters) {
    return ((ScanSelectionFiltersParameters) embeddedParameters).createFilter();
  }

  @Override
  protected void applyCheckBoxState() {
    super.applyCheckBoxState();
    if (textDescription == null || topPane == null) {
      return;
    }
    topPane.getChildren().remove(textDescription);
    if (getCheckbox().isSelected()) {
      topPane.getChildren().add(textDescription);
    }
  }

  @Override
  public void onViewStateChange(final boolean hidden) {
    super.onViewStateChange(hidden);
    updateParameterSetFromComponents(getEmbeddedParameters());
  }

  @Override
  public void updateParameterSetFromComponents(ParameterSet embeddedParameters) {
    // not yet initialized
    if (textDescription == null) {
      return;
    }
    super.updateParameterSetFromComponents(embeddedParameters);
    var selection = createSelection(embeddedParameters);
    textDescription.setText(selection.toShortDescription());
  }

  @Override
  public void setParameterValuesToComponents(ParameterSet embeddedParameters) {
    super.setParameterValuesToComponents(embeddedParameters);
    var selection = createSelection(embeddedParameters);
    textDescription.setText(selection != null ? selection.toShortDescription() : "");
  }

}
