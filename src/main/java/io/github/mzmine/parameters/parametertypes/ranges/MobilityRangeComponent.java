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
