/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
