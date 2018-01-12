/*
 * Copyright (C) 2018 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;

import com.google.common.collect.Range;
import dulab.adap.datamodel.Chromatogram;
import dulab.adap.workflow.decomposition.PeakDetector;
import dulab.adap.workflow.decomposition.RetTimeClusterer;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Du-Lab Team dulab.binf@gmail.com
 */
public class DataProvider
{
    private final ParameterSet parameters;

    private RetTimeClusterer.Item[] ranges = new RetTimeClusterer.Item[0];

    private List<RetTimeClusterer.Cluster> windows = new ArrayList<>(0);

    public DataProvider(@Nonnull ParameterSet parameters) {
        this.parameters = parameters;
    }

    public ParameterSet getParameterSet() {return parameters;}

    public RetTimeClusterer.Item[] getRanges(boolean recalculate)
    {
        if (ranges.length == 0 || recalculate)
        {
            Integer numSmoothPoints = parameters.getParameter(PeakDetectionSupplier.NUM_SMOOTH_POINTS).getValue();
            Double minPeakHeight = parameters.getParameter(PeakDetectionSupplier.MIN_PEAK_HEIGHT).getValue();
            Range<Double> durationRange = parameters.getParameter(PeakDetectionSupplier.DURATION_RANGE).getValue();

            if (numSmoothPoints == null || minPeakHeight == null || durationRange == null)
                return ranges;

            PeakDetector peakDetector = new PeakDetector(numSmoothPoints, minPeakHeight, durationRange);

            PeakList chromatograms = MZmineCore.getDesktop().getSelectedPeakLists()[0];

            ranges = Arrays.stream(chromatograms.getRows())
                    .parallel()
                    .map(PeakListRow::getBestPeak)
                    .flatMap(c -> peakDetector.run(new Chromatogram(
                            Arrays.stream(c.getScanNumbers())
                                    .mapToDouble(s -> c.getDataFile().getScan(s).getRetentionTime()).toArray(),
                            Arrays.stream(c.getScanNumbers())
                                    .mapToObj(c::getDataPoint)
                                    .mapToDouble(p -> p != null ? p.getIntensity() : 0.0).toArray()), c.getMZ())
                            .stream())
                    .toArray(RetTimeClusterer.Item[]::new);
        }
        return ranges;
    }

    public List<RetTimeClusterer.Cluster> getWindows(boolean recalculate)
    {
        if (windows.isEmpty() || recalculate)
        {
            Double prefWindowWidth = parameters.getParameter(WindowDetectionSupplier.PREF_WINDOW_WIDTH).getValue();
            Integer minNumPeaks = parameters.getParameter(WindowDetectionSupplier.MIN_NUM_PEAKS).getValue();

            if (prefWindowWidth == null || minNumPeaks == null)
                return windows;

            windows = new RetTimeClusterer(prefWindowWidth, minNumPeaks).execute(getRanges(recalculate));
        }
        return windows;
    }
}
