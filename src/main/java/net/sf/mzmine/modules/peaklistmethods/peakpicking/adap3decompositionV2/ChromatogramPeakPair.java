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

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.ParameterSet;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author Du-Lab Team dulab.binf@gmail.com
 */
public class ChromatogramPeakPair
{
    public final PeakList chromatograms;
    public final PeakList peaks;

    private ChromatogramPeakPair(@Nonnull PeakList chromatograms, @Nonnull PeakList peaks) {
        this.chromatograms = chromatograms;
        this.peaks = peaks;
    }

    @Override
    public String toString() {
        return chromatograms.getName() + " / " + peaks.getName();
    }

    public static Map<RawDataFile, ChromatogramPeakPair> fromParameterSet(@Nonnull ParameterSet parameterSet)
    {
        Map<RawDataFile, ChromatogramPeakPair> pairs = new HashMap<>();

        PeakList[] chromatograms = parameterSet.getParameter(ADAP3DecompositionV2Parameters.CHROMATOGRAM_LISTS)
                .getValue().getMatchingPeakLists();
        PeakList[] peaks = parameterSet.getParameter(ADAP3DecompositionV2Parameters.PEAK_LISTS)
                .getValue().getMatchingPeakLists();
        if (chromatograms == null || chromatograms.length == 0 || peaks == null || peaks.length == 0)
            return pairs;

        Set<RawDataFile> dataFiles = new HashSet<>();
        for (PeakList peakList : chromatograms)
            dataFiles.add(peakList.getRawDataFile(0));
        for (PeakList peakList : peaks)
            dataFiles.add(peakList.getRawDataFile(0));

        for (RawDataFile dataFile : dataFiles) {
            PeakList chromatogram = Arrays.stream(chromatograms)
                    .filter(c -> c.getRawDataFile(0) == dataFile).findFirst().orElse(null);
            PeakList peak = Arrays.stream(peaks)
                    .filter(c -> c.getRawDataFile(0) == dataFile).findFirst().orElse(null);
            if (chromatogram != null && peak != null)
                pairs.put(dataFile, new ChromatogramPeakPair(chromatogram, peak));
        }

        return pairs;
    }
}
