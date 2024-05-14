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

public sealed interface FxControllerBinding permits SelectedAbundanceMeasureBinding,
    SelectedMetadataColumnBinding, SelectedRowsBinding, SelectedFeaturesBinding,
    SelectedFilesBinding, SelectedFeatureListsBinding {

  public static void bindExposedProperties(Object master, Object child) {
    if (master instanceof FxControllerBinding && child instanceof FxControllerBinding) {
      bindExposedProperties(master, child);
    }
  }

  public static void bindExposedProperties(FxControllerBinding master, FxControllerBinding child) {
    if (master == null || child == null) {
      return;
    }

    if (master instanceof SelectedAbundanceMeasureBinding m
        && child instanceof SelectedAbundanceMeasureBinding c) {
      m.abundanceMeasureProperty().bindBidirectional(c.abundanceMeasureProperty());
    }
    if (master instanceof SelectedFeatureListsBinding m
        && child instanceof SelectedFeatureListsBinding c) {
      m.selectedFeatureListsProperty().bindBidirectional(c.selectedFeatureListsProperty());
    }
    if (master instanceof SelectedFeaturesBinding m && child instanceof SelectedFeaturesBinding c) {
      m.selectedFeaturesProperty().bindBidirectional(c.selectedFeaturesProperty());
    }
    if (master instanceof SelectedFilesBinding m && child instanceof SelectedFilesBinding c) {
      m.selectedRawFilesProperty().bindBidirectional(c.selectedRawFilesProperty());
    }
    if (master instanceof SelectedMetadataColumnBinding m
        && child instanceof SelectedMetadataColumnBinding c) {
      m.groupingColumnProperty().bindBidirectional(c.groupingColumnProperty());
    }
    if (master instanceof SelectedRowsBinding m && child instanceof SelectedRowsBinding c) {
      m.selectedRowsProperty().bindBidirectional(c.selectedRowsProperty());
    }
  }

  /**
   * // note: nothing to do here. This switch shall just remind you of adding new cases to the
   * {@link #bindExposedProperties} method.
   */
  private void reminderToKeepAllCasesCovered(FxControllerBinding b) {
    switch (b) {
      case SelectedRowsBinding _ -> {
      }
      case SelectedAbundanceMeasureBinding _ -> {
      }
      case SelectedFeatureListsBinding _ -> {
      }
      case SelectedFeaturesBinding _ -> {
      }
      case SelectedFilesBinding _ -> {
      }
      case SelectedMetadataColumnBinding _ -> {
      }
    }
  }
}
