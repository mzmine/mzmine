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
import dulab.adap.datamodel.BetterPeak;
import dulab.adap.datamodel.Chromatogram;
import dulab.adap.datamodel.Peak;
import dulab.adap.datamodel.PeakInfo;
import dulab.adap.workflow.decomposition.PeakDetector;
import dulab.adap.workflow.decomposition.RetTimeClusterer;
import net.sf.mzmine.datamodel.*;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;

import javax.annotation.Nonnull;
import java.awt.event.ComponentAdapter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Du-Lab Team dulab.binf@gmail.com
 */
public class DataProvider
{
    private final Logger log = Logger.getLogger(DataProvider.class.getName());

    private final ParameterSet parameters;

    private List<BetterPeak> chromatograms = new ArrayList<>();

    private RetTimeClusterer.Item[] ranges = new RetTimeClusterer.Item[0];

    private List<RetTimeClusterer.Cluster> windows = new ArrayList<>(0);

    public DataProvider(@Nonnull ParameterSet parameters) {
        this.parameters = parameters;
    }

    public ParameterSet getParameterSet() {return parameters;}

    public PeakList getPeakList() {
        return MZmineCore.getDesktop().getSelectedPeakLists()[0];
    }

    public List<BetterPeak> getChromatograms(boolean recalculate)
    {
        PeakList peakList = getPeakList();

        if (chromatograms.isEmpty() || recalculate)
        {
            chromatograms.clear();
            for (PeakListRow row : peakList.getRows())
            {
                Feature peak = row.getBestPeak();
                int[] scanNumbers = peak.getScanNumbers();

                double[] retTimes = Arrays.stream(scanNumbers)
                        .mapToDouble(s -> peak.getDataFile().getScan(s).getRetentionTime()).toArray();
                double[] intensities = Arrays.stream(scanNumbers)
                        .mapToObj(peak::getDataPoint)
                        .mapToDouble(p -> p != null ? p.getIntensity() : 0.0).toArray();
                Chromatogram chromatogram = new Chromatogram(retTimes, intensities);

                if (chromatogram.length <= 1) continue;

                // Fill out PeakInfo
                PeakInfo info = new PeakInfo();
                try {
                    // Note: info.peakID is the index of PeakListRow in PeakList.peakListRows (starts from 0)
                    //       row.getID is row.myID (starts from 1)
                    info.peakID = row.getID() - 1;
                    info.peakIndex = Arrays.stream(scanNumbers)
                            .filter(s -> peak.getDataPoint(s) != null)
                            .boxed()
                            .max(Comparator.comparing(s -> peak.getDataPoint(s).getIntensity()))
                            .orElseThrow(Exception::new);
                    info.leftApexIndex = scanNumbers[0];
                    info.rightApexIndex = scanNumbers[scanNumbers.length - 1];
                    info.retTime = peak.getRT();
                    info.mzValue = peak.getMZ();
                    info.intensity = peak.getHeight();
                    info.leftPeakIndex = info.leftApexIndex;
                    info.rightPeakIndex = info.rightApexIndex;
                }
                catch (Exception e) {
                    log.info("Skipping " + row + ": " + e.getMessage());
                    continue;
                }

                chromatograms.add(new BetterPeak(chromatogram, info));
            }
        }
        return chromatograms;
    }

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

            PeakList peakList = getPeakList();

            ranges = Arrays.stream(peakList.getRows())
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
            Double prefWindowWidth = parameters.getParameter(WindowSelectionSupplier.PREF_WINDOW_WIDTH).getValue();
            Integer minNumPeaks = parameters.getParameter(WindowSelectionSupplier.MIN_NUM_PEAKS).getValue();

            if (prefWindowWidth == null || minNumPeaks == null)
                return windows;

            windows = new RetTimeClusterer(prefWindowWidth, minNumPeaks).execute(getRanges(recalculate));
        }
        return windows;
    }
}
