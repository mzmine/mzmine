/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 */

package net.sf.mzmine.modules.tools.msmsspectramerge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.math3.special.Erf;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.scans.ScanUtils;

/**
 * Module for merging MS/MS spectra. Merging is performed by:
 * 1. first selecting all consecutive MS/MS and the surrounding MS1 scans
 * 2. scoring each such list of consecutive MS/MS by the intensity of the precursor peak in the surrounding MS1 divided
 * by the intensity of chimeric peaks around them
 * 3. removing MS/MS with considerably low scoring (<20% of best score)
 * 4. start with MS/MS with best score. Merge iteratively all spectra into this MS/MS which have a cosine score
 * above the user given threshold.
 *
 * The reasoning behind 4. is that the method will first select a MS/MS with, hopefully, low chimeric contamination.
 * Afterwards it only merges MS/MS into this spectra which do not deviate too much (e.g. have low chimerics, too)
 *
 * 5. Merging all merged spectra within a sample belonging to the same feature using the same routine
 * 6. Merging all merged spectra across samples belonging to the same feature using the same routine
 * 7. removing peaks from merged spectra which are not consistent across the merged spectra
 */
public class MsMsSpectraMergeModule implements MZmineModule {

    @Nonnull
    @Override
    public String getName() {
        return "MS/MS Spectra Merge";
    }

    @Nullable
    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {
        return MsMsSpectraMergeParameters.class;
    }

    /**
     * Merge MS/MS spectra belonging to the given feature row according to the parameter setting.
     * The method returns a list of merged spectra. If MERGE_MODE is set to 'across samples', this list
     * contains only one merged spectrum. Otherwise it will contain one spectrum per sample or even multiple spectra
     * per sample.
     * @param row the feature which MS/MS should be merged
     * @param massList name of the mass list to use when extracting peaks
     * @return list of merged MS/MS spectra belonging to this feature
     */
    public List<MergedSpectrum> getMergedSpectra(ParameterSet parameters, PeakListRow row, String massList) {
        final MergeMode mode = parameters.getParameter(MsMsSpectraMergeParameters.MERGE_MODE).getValue();
        final double npeaksFilter = parameters.getParameter(MsMsSpectraMergeParameters.PEAK_COUNT_PARAMETER).getValue();
        switch (mode) {
            case CONSECUTIVE_SCANS:
                // merge all consecutive MS/MS, remove peaks if they do not occur consistently in merged spectra, return the list of merged spectra
                return Arrays.stream(row.getPeaks()).flatMap(x->mergeConsecutiveScans(parameters, x,massList).stream()).filter(x->x.data.length>0).map(x->x.filterByRelativeNumberOfScans(npeaksFilter)).collect(Collectors.toList());
            case SAME_SAMPLE:
                // first merge all consecutive scans. Afterwards, merge all MS/MS within the same sample.
                // Remove peaks if they do not occur consistently in merged spectra. Returns a merged MS/MS for each sample
                return Arrays.stream(row.getPeaks()).map(x->mergeFromSameSample(parameters, x,massList)).filter(x->x.data.length>0).map(x->x.filterByRelativeNumberOfScans(npeaksFilter)).collect(Collectors.toList());
            case ACROSS_SAMPLES:
                // first merge all consecutive scans. Afterwards merge all MS/MS within the same sample. Finally, merge
                // all MS/MS across samples that belong to the same feature. Remove peaks if they do not occur consistently in merged spectra.
                // returns a single merged spectrum
                MergedSpectrum mergedSpectrum = mergeAcrossSamples(parameters, row,massList).filterByRelativeNumberOfScans(npeaksFilter);
                return mergedSpectrum.data.length==0 ? Collections.emptyList() : Collections.singletonList(mergedSpectrum);
            default:
                // should never be called
                return Collections.emptyList();
        }
    }

    /**
     * Merge MS/MS spectra belonging to the given feature row according to the parameter setting.
     * The method returns either the completely merged spectrum if MERGE_MODE is set to 'across samples'. Otherwise,
     * it will return the best merged spectrum across all merged spectra..
     * @param row the feature which MS/MS should be merged
     * @param massList name of the mass list to use when extracting peaks
     * @return merged spectrum or null, if none exist
     */
    public MergedSpectrum getBestMergedSpectrum(ParameterSet parameters, PeakListRow row, String massList) {
        return getMergedSpectra(parameters, row,massList).stream().max(Comparator.comparingDouble(MergedSpectrum::getBestFragmentScanScore)).orElse(null);
    }

    /**
     * Merge across samples. It is recommended to use #merge(PeakListRow,String) instead. Note, that this method will not remove noise peaks
     * from the merged spectra.
     * @param row the feature which MS/MS should be merged
     * @param massList name of the mass list to use when extracting peaks
     * @return the merged MS/MS of all fragment spectra belonging to the feature row
     */
    public MergedSpectrum mergeAcrossSamples(ParameterSet parameters, PeakListRow row, String massList) {
        return mergeAcrossFragmentSpectra(parameters, Arrays.stream(row.getPeaks()).map(r->mergeFromSameSample(parameters, r,massList)).filter(x->x.data.length>0).collect(Collectors.toList()));
    }

    /**
     * Merge all MS/MS belonging to the same sample. It is recommended to use #merge(PeakListRow,String) instead. Note, that this method will not remove noise peaks from the merged spectra.
     * @param feature the feature which MS/MS should be merged
     * @param massList name of the mass list to use when extracting peaks
     * @return the merged MS/MS of all fragment spectra belonging to the feature
     */
    public MergedSpectrum mergeFromSameSample(ParameterSet parameters, Feature feature, String massList) {
        List<MergedSpectrum> spectra = mergeConsecutiveScans(parameters, feature, massList);
        if (spectra.isEmpty()) return MergedSpectrum.empty();
        return mergeAcrossFragmentSpectra(parameters, spectra);
    }

    /**
     * Merge all consecutive MS/MS scans of the same feature within the same sample. Two scans are consecutive, if there is
     * no other MS/MS or MS in between. It is recommended to use #merge(PeakListRow,String) instead.
     * Note, that this method will not remove noise peaks from the merged spectra.
     * @param feature the feature which MS/MS should be merged
     * @param massList name of the mass list to use when extracting peaks
     * @return all merged spectra of consecutive MS/MS scans of the given feature
     */
    public List<MergedSpectrum> mergeConsecutiveScans(ParameterSet parameters, Feature feature, String massList) {
        MZTolerance ppm = parameters.getParameter(MsMsSpectraMergeParameters.MASS_ACCURACY).getValue();
        final double isolationWindowOffset = parameters.getParameter(MsMsSpectraMergeParameters.ISOLATION_WINDOW_OFFSET).getValue();
        final double isolationWindowWidth = parameters.getParameter(MsMsSpectraMergeParameters.ISOLATION_WINDOW_WIDTH).getValue();
        FragmentScan[] allFragmentScans = FragmentScan.getAllFragmentScansFor(feature, massList, Range.closed(isolationWindowOffset-isolationWindowWidth, isolationWindowOffset + isolationWindowWidth), ppm);
        final List<MergedSpectrum> mergedSpec = new ArrayList<>();
        for (FragmentScan scan : allFragmentScans) {
            MergedSpectrum e = mergeConsecutiveScans(parameters, scan, massList, Ms2QualityScoreModel.SelectByLowChimericIntensityRelativeToMs1Intensity);
            if (e.data.length>0)
                mergedSpec.add(e);
        }
        return mergedSpec;
    }

    /**
     * Internal method that merges a list of spectra into one.
     * @param fragmentMergedSpectra list of spectra with meta information
     * @return merged spectrum
     */
    protected MergedSpectrum mergeAcrossFragmentSpectra(ParameterSet parameters, List<MergedSpectrum> fragmentMergedSpectra) {
        if (fragmentMergedSpectra.isEmpty()) return MergedSpectrum.empty();
        int totalNumberOfScans = 0;
        for (MergedSpectrum s : fragmentMergedSpectra) totalNumberOfScans += s.totalNumberOfScans();
        final double[] scores = new double[fragmentMergedSpectra.size()];
        for (int k=0; k < fragmentMergedSpectra.size(); ++k) {
            scores[k] = fragmentMergedSpectra.get(k).bestFragmentScanScore;
        }
        int scansRemovedDueToLowQuality = 0;
        final double bestScore = Arrays.stream(scores).max().getAsDouble();
        // only pick fragment scans with decent quality
        final List<MergedSpectrum> selectedScans = new ArrayList<>();
        MergedSpectrum bestOne = null;
        for (int k=0; k < fragmentMergedSpectra.size(); ++k) {
            if (scores[k] >= bestScore/5d) {
                if (scores[k]>=bestScore) bestOne = fragmentMergedSpectra.get(k);
                selectedScans.add(fragmentMergedSpectra.get(k));
            } else {
                scansRemovedDueToLowQuality += fragmentMergedSpectra.get(k).totalNumberOfScans();
            }
        }

        Collections.sort(selectedScans, Comparator.comparingInt(u -> u.scanIds[0]));
        int bestIndex=0;
        for (int k=0; k < selectedScans.size(); ++k) {
            if (selectedScans.get(k)==bestOne)
                bestIndex = k;
        }
        final List<MergedSpectrum> toMerge = new ArrayList<>();
        toMerge.add(bestOne);
        for (int i=1; i < selectedScans.size(); ++i) {
            int k = bestIndex-i;
            if (k >= 0) toMerge.add(selectedScans.get(k));
            k = bestIndex+i;
            if (k < selectedScans.size()) toMerge.add(selectedScans.get(k));
        }

        if (toMerge.size()==1) {
            toMerge.get(0).removedScansByLowQuality = scansRemovedDueToLowQuality;
            return toMerge.get(0);
        }

        /*
            merge every scan if its cosine is above the cosine threshold
         */
        final double cosineThreshold = parameters.getParameter(MsMsSpectraMergeParameters.COSINE_PARAMETER).getValue();
        final MZTolerance massTolerance = parameters.getParameter(MsMsSpectraMergeParameters.MASS_ACCURACY).getValue();
        final MzMergeMode mzMergeMode = parameters.getParameter(MsMsSpectraMergeParameters.MZ_MERGE_MODE).getValue();
        final IntensityMergeMode intensityMergeMode = parameters.getParameter(MsMsSpectraMergeParameters.INTENSITY_MERGE_MODE).getValue();
        MergedSpectrum initial = bestOne;
        final double lowestMassToConsider = Math.min(50d, initial.precursorMz-50d);

        final DataPoint[] initialMostIntensive = ScanUtils.extractMostIntensePeaksAcrossMassRange(initial.data, Range.closed(lowestMassToConsider, lowestMassToConsider+100), 6);
        final Range<Double> cosineRange = Range.closed(lowestMassToConsider, initial.precursorMz - 20);
        double lowestIntensityToConsider;
        {
            Optional<MergedDataPoint> max = Arrays.stream(initial.data).filter(x -> cosineRange.contains(x.getMZ())).max(Comparator.comparingDouble(u -> u.intensity));
            if (!max.isPresent()) {
                // no peak beside precursor ion
                return MergedSpectrum.empty(totalNumberOfScans);
            }
            MergedDataPoint basePeak = max.get();
            lowestIntensityToConsider = basePeak.sources[0].getIntensity();
            for (DataPoint p : basePeak.sources) {
                lowestIntensityToConsider = Math.max(p.getIntensity(), lowestIntensityToConsider);
            }
            lowestIntensityToConsider = lowestIntensityToConsider*0.01;
        }
        final double initialCosine = ScanUtils.probabilityProductUnnormalized(initialMostIntensive,initialMostIntensive,massTolerance,lowestIntensityToConsider, cosineRange);
        for (int k=1; k < toMerge.size(); ++k) {
            MergedSpectrum scan = toMerge.get(k);
            DataPoint[] dataPoints = scan.data;
            final DataPoint[] mostIntensive = ScanUtils.extractMostIntensePeaksAcrossMassRange(dataPoints, Range.closed(50d,150d), 6);
            final double norm = ScanUtils.probabilityProductUnnormalized(mostIntensive,mostIntensive,massTolerance,lowestIntensityToConsider,cosineRange);
            final double cosine = ScanUtils.probabilityProductUnnormalized(initialMostIntensive, mostIntensive, massTolerance, lowestIntensityToConsider,cosineRange) / Math.sqrt(norm*initialCosine);
            if (cosine >= cosineThreshold) {
                initial = merge(initial, scan, mzMergeMode, intensityMergeMode, massTolerance);
            } else {
                initial.removedScansByLowCosine += scan.totalNumberOfScans();
            }
        }
        initial.removedScansByLowQuality = scansRemovedDueToLowQuality;
        return initial;
    }

    /**
     * Internal method for merging a list of consecutive MS/MS scans.
     * @param scans MS/MS scans with their precursor information
     * @param massList name of the mass list to use when extracting peaks
     * @param scoreModel scoring model to use when removing low quality MS/MS and selecting the best quality MS/MS
     * @return merged spectrum
     */
    protected MergedSpectrum mergeConsecutiveScans(ParameterSet parameters, FragmentScan scans, String massList, Ms2QualityScoreModel scoreModel) {
        int totalNumberOfScans = scans.ms2ScanNumbers.length;
        /*
         * find scan with best quality
         */
        final double[] scores = scoreModel.calculateQualityScore(scans);
        int best=0;
        for (int k=1;k<scores.length; ++k) {
            if (scores[k]>scores[best]) {
                best=k;
            }
        }
        if (scores[best]<=0) return MergedSpectrum.empty(totalNumberOfScans);
        final List<Scan> scansToMerge = new ArrayList<>();
        scansToMerge.add(scans.origin.getScan(scans.ms2ScanNumbers[best]));
        if (scansToMerge.get(0).getMassList(massList).getDataPoints().length <= 1)
            return MergedSpectrum.empty(totalNumberOfScans);
        /*
            remove scans which are considerably worse than the best scan
         */
        final double scoreThreshold = best/5d;
        for (int i=1; i < scores.length; ++i) {
            int k = best-i;
            if (k >= 0 && scores[k] > scoreThreshold) {
                scansToMerge.add(scans.origin.getScan(scans.ms2ScanNumbers[k]));
            }
            k = best+i;
            if (k < scores.length && scores[k] > scoreThreshold) {
                scansToMerge.add(scans.origin.getScan(scans.ms2ScanNumbers[k]));
            }
        }
        if (scansToMerge.size()==1) {
            final MergedSpectrum single = new MergedSpectrum(scansToMerge.get(0), massList);
            single.bestFragmentScanScore = scores[best];
            single.removedScansByLowQuality = scores.length-1;
            return single;
        }
        /*
            merge every scan if its cosine is above the cosine threshold
         */
        final double cosineThreshold = parameters.getParameter(MsMsSpectraMergeParameters.COSINE_PARAMETER).getValue();
        final MZTolerance mzTolerance = parameters.getParameter(MsMsSpectraMergeParameters.MASS_ACCURACY).getValue();
        final MzMergeMode mzMergeMode = parameters.getParameter(MsMsSpectraMergeParameters.MZ_MERGE_MODE).getValue();
        final IntensityMergeMode intensityMergeMode = parameters.getParameter(MsMsSpectraMergeParameters.INTENSITY_MERGE_MODE).getValue();

        MergedSpectrum initial = new MergedSpectrum(scansToMerge.get(0), massList);
        initial.bestFragmentScanScore =  best;
        final double lowestMassToConsider = Math.min(50d, scans.feature.getMZ()-50d);

        final DataPoint[] initialMostIntensive = ScanUtils.extractMostIntensePeaksAcrossMassRange(initial.data, Range.closed(lowestMassToConsider, 150d), 6);
        final double lowestIntensityToConsider = 0.005d * initialMostIntensive[ScanUtils.findMostIntensePeakWithin(initialMostIntensive, Range.closed(lowestMassToConsider,scans.feature.getMZ()))].getIntensity();
        Range<Double> cosineRange = Range.closed(lowestMassToConsider, scans.feature.getMZ() - 20);
        final double initialCosine = ScanUtils.probabilityProductUnnormalized(initialMostIntensive,initialMostIntensive,mzTolerance,lowestIntensityToConsider, cosineRange);
        for (int k=1; k < scansToMerge.size(); ++k) {
            Scan scan = scansToMerge.get(k);
            if (!(scan.getPolarity().equals(initial.polarity) && scan.getPrecursorCharge()==initial.precursorCharge && mzTolerance.checkWithinTolerance(scan.getPrecursorMZ(), initial.precursorMz))) {
                LoggerFactory.getLogger(MsMsSpectraMergeModule.class).warn("Scan " + scan.getScanNumber() + " cannot be merged: it seems to belong to a different feature.");
                continue;
            }
            DataPoint[] dataPoints = scan.getMassList(massList).getDataPoints();
            final DataPoint[] mostIntensive = ScanUtils.extractMostIntensePeaksAcrossMassRange(dataPoints, cosineRange, 6);
            final double norm = ScanUtils.probabilityProductUnnormalized(mostIntensive,mostIntensive,mzTolerance,lowestIntensityToConsider,cosineRange);
            final double cosine = ScanUtils.probabilityProductUnnormalized(initialMostIntensive, mostIntensive, mzTolerance, lowestIntensityToConsider,cosineRange) / Math.sqrt(norm*initialCosine);
            if (cosine >= cosineThreshold) {
                initial = merge(initial, scan, dataPoints, mzMergeMode, intensityMergeMode, mzTolerance);
            } else {
                initial.removedScansByLowCosine++;
            }
        }
        initial.removedScansByLowQuality += (totalNumberOfScans - scansToMerge.size());
        return initial;
    }

    private static MergedSpectrum merge(MergedSpectrum left, MergedSpectrum right, MzMergeMode mzMergeMode, IntensityMergeMode intensityMergeMode, MZTolerance ppm) {
        DataPoint[] byInt = right.data.clone();
        Arrays.sort(byInt, (u,v)->Double.compare(v.getIntensity(),u.getIntensity()));
        MergedDataPoint[] merge = merge(left.data, byInt, mzMergeMode, intensityMergeMode, ppm);
        return left.merge(right,merge);
    }

    private static MergedSpectrum merge(MergedSpectrum left, Scan right, DataPoint[] rightData, MzMergeMode mzMergeMode, IntensityMergeMode intensityMergeMode, MZTolerance ppm) {
        DataPoint[] byInt = rightData.clone();
        Arrays.sort(byInt, (u,v)->Double.compare(v.getIntensity(),u.getIntensity()));
        MergedDataPoint[] merge = merge(left.data, byInt, mzMergeMode, intensityMergeMode, ppm);
        RawDataFile f = left.origins[0];
        RawDataFile[] fm;
        if (right.getDataFile().equals(left.origins[0])) {
            fm = left.origins;
        } else {
            HashSet<RawDataFile> rawDataFiles = new HashSet<>(Arrays.asList(left.origins));
            rawDataFiles.add(right.getDataFile());
            fm = rawDataFiles.toArray(new RawDataFile[0]);
        }
        int[] scanIds = Arrays.copyOf(left.scanIds, left.scanIds.length+1);
        scanIds[scanIds.length-1] = right.getScanNumber();
        return new MergedSpectrum(
                merge,fm, scanIds, left.precursorMz, left.polarity, left.precursorCharge, left.removedScansByLowQuality, left.removedScansByLowCosine, left.bestFragmentScanScore
        );
    }


    /**
     * Merge a scan into a merged spectrum.
     * @param orderedByMz peaks from merged spectrum, sorted by ascending m/z
     * @param orderedByInt peaks from scan, sorted by descending intensity
     * @return a merged spectrum. Might be the original one if no new peaks were added.
     */
    private static MergedDataPoint[] merge(MergedDataPoint[] orderedByMz, DataPoint[] orderedByInt, MzMergeMode mzMergeMode, IntensityMergeMode intensityMergeMode, MZTolerance expectedPPM) {
        // we assume a rather large deviation as signal peaks should be contained in more than one
        // measurement
        expectedPPM = new MZTolerance(expectedPPM.getMzTolerance()*4, expectedPPM.getPpmTolerance()*4);
        final List<MergedDataPoint> append = new ArrayList<>();
        for (int k = 0; k < orderedByInt.length; ++k) {
            final DataPoint peak = orderedByInt[k];
            final double dev = expectedPPM.getMzToleranceForMass(peak.getMZ());
            final double lb = peak.getMZ() - dev, ub = peak.getMZ() + dev;
            int mz1 = Arrays.binarySearch(orderedByMz, peak, Comparator.comparingDouble(DataPoint::getMZ));
            if (mz1 < 0) {
                mz1 = -(mz1 + 1);
            }
            int mz0 = mz1 - 1;
            while (mz1 < orderedByMz.length && orderedByMz[mz1].getMZ() <= ub)
                ++mz1;
            --mz1;
            while (mz0 >= 0 && orderedByMz[mz0].getMZ() >= lb)
                --mz0;
            ++mz0;
            if (mz0 <= mz1) {
                // merge!
                int mostIntensive = mz0;
                double bestScore = Double.NEGATIVE_INFINITY;
                for (int i = mz0; i <= mz1; ++i) {
                    final double massDiff = orderedByMz[i].getMZ() - peak.getMZ();
                    final double score =
                            Erf.erfc(3 * massDiff) / (dev * Math.sqrt(2)) * orderedByMz[i].getIntensity();
                    if (score > bestScore) {
                        bestScore = score;
                        mostIntensive = i;
                    }
                }

                orderedByMz[mostIntensive] = orderedByMz[mostIntensive].merge(peak, mzMergeMode, intensityMergeMode);

            } else {
                // append
                append.add(new MergedDataPoint(mzMergeMode, intensityMergeMode, peak));
            }
        }
        if (append.size() > 0) {
            int offset = orderedByMz.length;
            orderedByMz = Arrays.copyOf(orderedByMz, orderedByMz.length + append.size());
            for (MergedDataPoint p : append) {
                orderedByMz[offset++] = p;
            }
            ScanUtils.sortDataPointsByMz(orderedByMz);
        }
        return orderedByMz;
    }

}
