/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.visualization.rawdataoverviewims.providers;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.impl.SimpleMobilityScan;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.project.impl.StorableFrame;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Loads a frame and it's subscans into ram.
 *
 * @author https://github.com/SteffenHeu
 */
public class CachedFrame extends StorableFrame {

  private final StorableFrame frame;
  private List<MobilityScan> sortedMobilityScans;
  private DataPoint[] summedDataPoints;

  public CachedFrame(StorableFrame frame) throws IOException {
    super(frame, (RawDataFileImpl) frame.getDataFile(), frame.getNumberOfDataPoints(),
        frame.getStorageID());
    this.frame = frame;
    summedDataPoints = new DataPoint[0];
    sortedMobilityScans = new ArrayList<>();

    summedDataPoints = frame.getDataPoints();

    for (MobilityScan scan : frame.getMobilityScans()) {
      DataPoint[] dataPoints = scan.getDataPoints();
      mobilitySubScans.put(scan.getMobilityScamNumber(),
          new SimpleMobilityScan(scan.getMobilityScamNumber(), this, dataPoints));
    }
    sortedMobilityScans = mobilitySubScans.values().stream()
        .sorted(Comparator.comparingDouble(MobilityScan::getMobility)).collect(Collectors.toList());
  }

  @Nonnull
  @Override
  public DataPoint[] getDataPoints() {
    return summedDataPoints;
  }

  public List<MobilityScan> getSortedMobilityScans() {
    return sortedMobilityScans;
  }

  @Nonnull
  @Override
  public Collection<MobilityScan> getMobilityScans() {
    return getSortedMobilityScans();
  }
}
