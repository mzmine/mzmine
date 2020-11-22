package io.github.mzmine.parameters.parametertypes.ranges;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import javafx.scene.control.Button;

public class MobilityRangeComponent extends DoubleRangeComponent {

  private final Button setAutoButton;

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
          System.out.println("no IMS data");
        }
      }
      setValue(mobilityRange);
    });
    RawDataFile currentFiles[] = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
    setAutoButton.setDisable(currentFiles.length == 0);

    getChildren().add(setAutoButton);
  }

}
