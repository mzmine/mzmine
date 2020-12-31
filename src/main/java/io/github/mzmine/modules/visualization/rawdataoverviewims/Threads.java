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

package io.github.mzmine.modules.visualization.rawdataoverviewims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.impl.SimpleMobilogram;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing.PreviewMobilogram;
import io.github.mzmine.util.MobilogramUtils;
import java.util.Set;
import javafx.application.Platform;
import javax.annotation.Nonnull;

class Threads {

  private Threads() {
  }

  public static class BuildMobilogram implements Runnable {

    private final Range<Double> mzRange;
    private final Set<Frame> frames;
    private final IMSRawDataOverviewPane pane;
    private final boolean isSelectedMobilogram;

    protected BuildMobilogram(@Nonnull Range<Double> mzRange, @Nonnull Set<Frame> frames,
        @Nonnull IMSRawDataOverviewPane pane, boolean isSelectedMobilogram) {
      this.mzRange = mzRange;
      this.frames = frames;
      this.pane = pane;
      this.isSelectedMobilogram = isSelectedMobilogram;
    }

    @Override
    public void run() {
      SimpleMobilogram mobilogram = MobilogramUtils.buildMobilogramForMzRange(frames, mzRange);
      PreviewMobilogram prev = new PreviewMobilogram(mobilogram,
          "m/z " + MZmineCore.getConfiguration().getMZFormat()
              .format((mzRange.upperEndpoint() + mzRange
                  .lowerEndpoint()) / 2));
      Platform.runLater(() -> pane.addMobilogramToChart(prev, isSelectedMobilogram));
    }
  }
}
