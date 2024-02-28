/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class FeaturePreviewComboRenderer extends BorderPane {

  FeaturePreviewComboRenderer(FeatureListRow row) {

    Feature peak = row.getFeatures().get(0);
//    if(peak instanceof ModularFeature && peak.getRawDataFile() instanceof IMSRawDataFile) {
//      peak = FeatureConvertorIonMobility.collapseMobilityDimensionOfModularFeature((ModularFeature) peak);
//    }

    String labelText = "#" + row.getID() + " "
        + MZmineCore.getConfiguration().getMZFormat().format(row.getAverageMZ()) + " m/z ";
    Label textComponent = new Label(labelText);
    //
    //PeakXICComponent shapeComponent = new PeakXICComponent(peak);

    setLeft(textComponent);
    //setCenter(shapeComponent);

  }

}
