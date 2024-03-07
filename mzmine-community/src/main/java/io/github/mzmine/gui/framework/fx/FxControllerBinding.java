/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.gui.framework.fx;

public sealed interface FxControllerBinding permits SelectedAbundanceMeasureController,
    SelectedMetadataColumnController, SelectedRowsController, SelectedFeaturesController,
    SelectedFilesController, SelectedFeatureListsController {

  public static void bindExposedProperties(FxControllerBinding master, FxControllerBinding child) {
    if (master == null || child == null) {
      return;
    }

    if (master instanceof SelectedAbundanceMeasureController m
        && child instanceof SelectedAbundanceMeasureController c) {
      m.abundanceMeasureProperty().bindBidirectional(c.abundanceMeasureProperty());
    }
    if (master instanceof SelectedFeatureListsController m
        && child instanceof SelectedFeatureListsController c) {
      m.selectedFeatureListsProperty().bindBidirectional(c.selectedFeatureListsProperty());
    }
    if (master instanceof SelectedFeaturesController m
        && child instanceof SelectedFeaturesController c) {
      m.selectedFeaturesProperty().bindBidirectional(c.selectedFeaturesProperty());
    }
    if (master instanceof SelectedFilesController m && child instanceof SelectedFilesController c) {
      m.selectedRawFilesProperty().bindBidirectional(c.selectedRawFilesProperty());
    }
    if (master instanceof SelectedMetadataColumnController m
        && child instanceof SelectedMetadataColumnController c) {
      m.groupingColumnProperty().bindBidirectional(c.groupingColumnProperty());
    }
    if (master instanceof SelectedRowsController m && child instanceof SelectedRowsController c) {
      m.selectedRowsProperty().bindBidirectional(c.selectedRowsProperty());
    }
  }
}
