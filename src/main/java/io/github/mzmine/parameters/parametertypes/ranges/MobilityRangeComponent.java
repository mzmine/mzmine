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

package io.github.mzmine.parameters.parametertypes.ranges;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import javafx.scene.control.Button;

public class MobilityRangeComponent extends DoubleRangeComponent {

  private final Button setAutoButton;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public MobilityRangeComponent() {

    super(MZmineCore.getConfiguration().getMobilityFormat());

    setAutoButton = new Button("Auto range");
    setAutoButton.setOnAction(e -> {
      RawDataFile currentFiles[] =
          MZmineCore.getProjectManager().getCurrentProject().getDataFiles();

      Range<Double> mobilityRange = null;
      for (RawDataFile file : currentFiles) {
        if (file instanceof IMSRawDataFile) {
          IMSRawDataFile imsFile = (IMSRawDataFile) file;
          Range<Double> fileRange = imsFile.getDataMobilityRange();
          if (mobilityRange == null)
            mobilityRange = fileRange;
          else
            mobilityRange = mobilityRange.span(fileRange);
        } else {
          mobilityRange = Range.singleton(0.0d);
          logger.log(Level.WARNING, "The selected raw data has no ion mobility dimension");
        }
      }
      setValue(mobilityRange);
    });
    RawDataFile currentFiles[] = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
    setAutoButton.setDisable(currentFiles.length == 0);

    getChildren().add(setAutoButton);
  }

}
