package io.github.mzmine.modules.tools.timstofmaldiacq.imaging.acquisitionwriters;

import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.tools.timstofmaldiacq.TimsTOFAcquisitionUtils;
import io.github.mzmine.modules.tools.timstofmaldiacq.imaging.ImagingSpot;
import io.github.mzmine.parameters.ParameterSet;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SingleSpotMs2Writer implements MaldiMs2AcqusitionWriter {

  private static final Logger logger = Logger.getLogger(SingleSpotMs2Writer.class.getName());

  @Override
  public @NotNull String getName() {
    return "Single spot";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return SingleSpotMs2Parameters.class;
  }

  @Override
  public boolean writeAcqusitionFile(File acquisitionFile, ParameterSet parameters,
      List<ImagingSpot> spots, File savePathDir, BooleanSupplier isCanceled) {

    final int laserOffsetX = parameters.getValue(SingleSpotMs2Parameters.laserOffsetX);
    final int laserOffsetY = parameters.getValue(SingleSpotMs2Parameters.laserOffsetY);
    for (int i = 0; i < spots.size(); i++) {
      final ImagingSpot spot = spots.get(i);

//      progress = 0.2 + 0.8 * i / spots.size();
      if (isCanceled.getAsBoolean()) {
        return false;
      }

      final MaldiSpotInfo spotInfo = spot.spotInfo();

      int counter = 1;
      try {
        TimsTOFAcquisitionUtils.appendToCommandFile(acquisitionFile, spotInfo.spotName(),
            spot.getPrecursorList(0, 0), null, null, laserOffsetX, laserOffsetY, counter++, 0,
            savePathDir, spotInfo.spotName() + "_" + counter, null, false);
      } catch (IOException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
        return false;
      }
    }
    return true;
  }
}
