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

package io.github.mzmine.modules.tools.timstofmaldiacq.imaging.acquisitionwriters;

import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.tools.timstofmaldiacq.CeSteppingTables;
import io.github.mzmine.modules.tools.timstofmaldiacq.CeSteppingTables.CeSteppingTable;
import io.github.mzmine.modules.tools.timstofmaldiacq.TimsTOFAcquisitionUtils;
import io.github.mzmine.modules.tools.timstofmaldiacq.imaging.ImagingSpot;
import io.github.mzmine.parameters.ParameterSet;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TripleSpotMs2Writer implements MaldiMs2AcquisitionWriter {

  private static final Logger logger = Logger.getLogger(TripleSpotMs2Writer.class.getName());

  private final int stageOffsetX;
  private final int stageOffsetY;

  public TripleSpotMs2Writer() {
    stageOffsetX = 50;
    stageOffsetY = 0;
  }

  public TripleSpotMs2Writer(final ParameterSet parameters) {
    stageOffsetX = parameters.getValue(TripleSpotMs2Parameters.stageOffsetX);
    stageOffsetY = parameters.getValue(TripleSpotMs2Parameters.stageOffsetY);
  }

  @Override
  public @NotNull String getName() {
    return "Triple spot";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return TripleSpotMs2Parameters.class;
  }

  @Override
  public boolean writeAcqusitionFile(File acquisitionFile, List<ImagingSpot> spots,
      CeSteppingTables tables, BooleanSupplier isCanceled, File savePathDir) {

    final Map<Double, File> ceTableMap = new HashMap<>();
    for (CeSteppingTable table : tables.asList()) {
      var file = new File(acquisitionFile.getParentFile(),
          "ce_table_" + table.collisionEnergy() + "eV.csv");
      table.writeCETable(file);
      ceTableMap.put(table.collisionEnergy(), file);
    }

    for (int i = 0; i < spots.size(); i++) {
      final ImagingSpot spot = spots.get(i);

//      progress = 0.2 + 0.8 * i / spots.size();
      if (isCanceled.getAsBoolean()) {
        return false;
      }

      final MaldiSpotInfo spotInfo = spot.spotInfo();

      int counter = 1;
      for (int x = 0; x < 2; x++) {
        for (int y = 0; y < 2; y++) {
          if (x == 0 && y == 0 || spot.getPrecursorList(x, y).isEmpty()) {
            continue;
          }
          try {
            TimsTOFAcquisitionUtils.appendToCommandFile(acquisitionFile, spotInfo.spotName(),
                spot.getPrecursorList(x, y), x * stageOffsetX, y * stageOffsetY, null, null,
                counter++, savePathDir, spotInfo.spotName() + "_" + counter,
                ceTableMap.get(spot.getCollisionEnergy()), true, null);
          } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
          }
        }
      }
    }
    return true;
  }
}
