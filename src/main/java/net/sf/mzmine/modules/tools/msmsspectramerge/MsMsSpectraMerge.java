package net.sf.mzmine.modules.tools.msmsspectramerge;

import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.*;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.scans.ScanUtils;
import org.apache.commons.math3.special.Erf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class MsMsSpectraMerge implements MZmineModule {

    private final MsMsSpectraMergeParameters parameters;

    public MsMsSpectraMerge() {
        this.parameters = (MsMsSpectraMergeParameters) MZmineCore.getConfiguration().getModuleParameters(MsMsSpectraMerge.class);
    }

    public MsMsSpectraMerge(MsMsSpectraMergeParameters parameters) {
        this.parameters = parameters;
    }

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

    public List<MergedSpectrum> merge(PeakListRow row, String massList) {
        final MergeMode mode = parameters.getParameter(MsMsSpectraMergeParameters.MERGE_MODE).getValue();
        final double npeaksFilter = parameters.getParameter(MsMsSpectraMergeParameters.PEAK_COUNT_PARAMETER).getValue();
        switch (mode) {
            case CONSECUTIVE_SCANS:
                return Arrays.stream(row.getPeaks()).flatMap(x->mergeConsecutiveScans(x,massList).stream()).filter(x->x.data.length>0).map(x->x.filterByRelativeNumberOfScans(npeaksFilter)).collect(Collectors.toList());
            case SAME_SAMPLE:
                return Arrays.stream(row.getPeaks()).map(x->mergeFromSameSample(x,massList)).filter(x->x.data.length>0).map(x->x.filterByRelativeNumberOfScans(npeaksFilter)).collect(Collectors.toList());
            case ACROSS_SAMPLES:
                MergedSpectrum mergedSpectrum = mergeAcrossSamples(row,massList).filterByRelativeNumberOfScans(npeaksFilter);
                return mergedSpectrum.data.length==0 ? Collections.emptyList() : Collections.singletonList(mergedSpectrum);
            default:
                return Collections.emptyList();

        }

    }

    public MergedSpectrum mergeAcrossSamples(PeakListRow row, String massList) {
        return mergeAcrossFragmentSpectra(Arrays.stream(row.getPeaks()).map(r->mergeFromSameSample(r,massList)).filter(x->x.data.length>0).collect(Collectors.toList()));
    }

    public MergedSpectrum mergeFromSameSample(Feature feature, String massList) {
        List<MergedSpectrum> spectra = mergeConsecutiveScans(feature, massList);
        if (spectra.isEmpty()) return MergedSpectrum.empty();
        return mergeAcrossFragmentSpectra(spectra);
    }

    public List<MergedSpectrum> mergeConsecutiveScans(Feature feature, String massList) {
        final double ppm = parameters.getParameter(MsMsSpectraMergeParameters.MASS_ACCURACY).getValue().doubleValue();
        final double isolationWindowOffset = parameters.getParameter(MsMsSpectraMergeParameters.isolationWindowOffset).getValue();
        final double isolationWindowWidth = parameters.getParameter(MsMsSpectraMergeParameters.isolationWindowWidth).getValue();
        FragmentScan[] allFragmentScans = FragmentScan.getAllFragmentScansFor(feature, massList, Range.closed(isolationWindowOffset, isolationWindowOffset + isolationWindowWidth), ppm);
        final List<MergedSpectrum> mergedSpec = new ArrayList<>();
        for (FragmentScan scan : allFragmentScans) {
            MergedSpectrum e = mergeConsecutiveScans(scan, massList, Ms2QualityScoreModel.SelectByLowChimericIntensityRelativeToMs1Intensity);
            if (e.data.length>0)
                mergedSpec.add(e);
        }
        return mergedSpec;
    }


    protected MergedSpectrum mergeAcrossFragmentSpectra(List<MergedSpectrum> fragmentMergedSpectra) {
        if (fragmentMergedSpectra.isEmpty()) return MergedSpectrum.empty();
        int totalNumberOfScans = 0;
        for (MergedSpectrum s : fragmentMergedSpectra) totalNumberOfScans += s.totalNumberOfScans();
        final double[] scores = new double[fragmentMergedSpectra.size()];
        for (int k=0; k < fragmentMergedSpectra.size(); ++k) {
            scores[k] = fragmentMergedSpectra.get(k).bestFragmentScanScore;
        }
        final double bestScore = Arrays.stream(scores).max().getAsDouble();
        // only pick fragment scans with decent quality
        final List<MergedSpectrum> scansToMerge = new ArrayList<>();
        MergedSpectrum bestOne = null;
        for (int k=0; k < fragmentMergedSpectra.size(); ++k) {
            if (scores[k] >= bestScore/3) {
                if (scores[k]>=bestScore) bestOne = fragmentMergedSpectra.get(k);
                scansToMerge.add(fragmentMergedSpectra.get(k));
            }
        }
        Collections.sort(fragmentMergedSpectra, Comparator.comparingInt(u -> u.scanIds[0]));
        int bestIndex=0;
        for (int k=0; k < fragmentMergedSpectra.size(); ++k) {
            if (fragmentMergedSpectra.get(k)==bestOne)
                bestIndex = k;
        }
        final List<MergedSpectrum> toMerge = new ArrayList<>();
        toMerge.add(bestOne);
        for (int i=1; i < fragmentMergedSpectra.size(); ++i) {
            int k = bestIndex-i;
            if (k >= 0) toMerge.add(fragmentMergedSpectra.get(k));
            k = bestIndex+i;
            if (k < fragmentMergedSpectra.size()) toMerge.add(fragmentMergedSpectra.get(k));
        }
        /*
            merge every scan if its cosine is above the cosine threshold
         */
        final double cosineThreshold = parameters.getParameter(MsMsSpectraMergeParameters.COSINE_PARAMETER).getValue();
        final double ppm = parameters.getParameter(MsMsSpectraMergeParameters.MASS_ACCURACY).getValue().doubleValue();
        final MzMergeMode mzMergeMode = parameters.getParameter(MsMsSpectraMergeParameters.MZ_MERGE_MODE).getValue();
        final IntensityMergeMode intensityMergeMode = parameters.getParameter(MsMsSpectraMergeParameters.INTENSITY_MERGE_MODE).getValue();
        MergedSpectrum initial = bestOne;
        final double lowestMassToConsider = Math.min(50d, initial.precursorMz);

        final DataPoint[] initialMostIntensive = ScanUtils.extractMostIntensivePeaksAcrossMassRange(initial.data, Range.closed(lowestMassToConsider, 150d), 6);
        double lowestIntensityToConsider;
        {
            MergedDataPoint basePeak = Arrays.stream(initial.data).max((u, v) -> Double.compare(v.intensity, u.intensity)).get();
            lowestIntensityToConsider = basePeak.sources[0].getIntensity();
            for (DataPoint p : basePeak.sources) {
                lowestIntensityToConsider = Math.max(p.getIntensity(), lowestIntensityToConsider);
            }
            lowestIntensityToConsider = lowestMassToConsider*0.005;
        }
        Range<Double> cosineRange = Range.closed(lowestMassToConsider, initial.precursorMz - 20);
        final double initialCosine = ScanUtils.probabilityProductUnnormalized(initialMostIntensive,initialMostIntensive,ppm,lowestIntensityToConsider, cosineRange);
        for (int k=1; k < scansToMerge.size(); ++k) {
            MergedSpectrum scan = scansToMerge.get(k);
            DataPoint[] dataPoints = scan.data;
            final DataPoint[] mostIntensive = ScanUtils.extractMostIntensivePeaksAcrossMassRange(dataPoints, cosineRange, 6);
            final double norm = ScanUtils.probabilityProductUnnormalized(mostIntensive,mostIntensive,ppm,lowestIntensityToConsider,cosineRange);
            final double cosine = ScanUtils.probabilityProductUnnormalized(initialMostIntensive, mostIntensive, ppm, lowestIntensityToConsider,cosineRange) / Math.sqrt(norm*initialCosine);
            if (cosine >= cosineThreshold) {
                initial = merge(initial, scan, mzMergeMode, intensityMergeMode, ppm);
            } else {
                initial.removedScansByLowCosine += scan.totalNumberOfScans();
            }
        }
        initial.removedScansByLowQuality += totalNumberOfScans - initial.totalNumberOfScans();
        return initial;
    }

    protected MergedSpectrum mergeConsecutiveScans(FragmentScan scans, String massList, Ms2QualityScoreModel scoreModel) {
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
        final List<Scan> scansToMerge = new ArrayList<>();
        scansToMerge.add(scans.origin.getScan(scans.ms2ScanNumbers[best]));
        /*
            remove scans which are considerably worse than the best scan
         */
        final double scoreThreshold = best/3d;
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
        /*
            merge every scan if its cosine is above the cosine threshold
         */
        final double cosineThreshold = parameters.getParameter(MsMsSpectraMergeParameters.COSINE_PARAMETER).getValue();
        final double ppm = parameters.getParameter(MsMsSpectraMergeParameters.MASS_ACCURACY).getValue().doubleValue();
        final MzMergeMode mzMergeMode = parameters.getParameter(MsMsSpectraMergeParameters.MZ_MERGE_MODE).getValue();
        final IntensityMergeMode intensityMergeMode = parameters.getParameter(MsMsSpectraMergeParameters.INTENSITY_MERGE_MODE).getValue();

        MergedSpectrum initial = new MergedSpectrum(scansToMerge.get(0), massList);
        initial.bestFragmentScanScore =  best;
        final double lowestMassToConsider = Math.min(50d, scans.feature.getMZ()-50d);

        final DataPoint[] initialMostIntensive = ScanUtils.extractMostIntensivePeaksAcrossMassRange(initial.data, Range.closed(lowestMassToConsider, 150d), 6);
        final double lowestIntensityToConsider = 0.005d * initialMostIntensive[ScanUtils.findMostIntensivePeakWithin(initialMostIntensive, Range.closed(lowestMassToConsider,scans.feature.getMZ()))].getIntensity();
        Range<Double> cosineRange = Range.closed(lowestMassToConsider, scans.feature.getMZ() - 20);
        final double initialCosine = ScanUtils.probabilityProductUnnormalized(initialMostIntensive,initialMostIntensive,ppm,lowestIntensityToConsider, cosineRange);
        for (int k=1; k < scansToMerge.size(); ++k) {
            Scan scan = scansToMerge.get(k);
            DataPoint[] dataPoints = scan.getMassList(massList).getDataPoints();
            final DataPoint[] mostIntensive = ScanUtils.extractMostIntensivePeaksAcrossMassRange(dataPoints, cosineRange, 6);
            final double norm = ScanUtils.probabilityProductUnnormalized(mostIntensive,mostIntensive,ppm,lowestIntensityToConsider,cosineRange);
            final double cosine = ScanUtils.probabilityProductUnnormalized(initialMostIntensive, mostIntensive, ppm, lowestIntensityToConsider,cosineRange) / Math.sqrt(norm*initialCosine);
            if (cosine >= cosineThreshold) {
                initial = merge(initial, scan, dataPoints, mzMergeMode, intensityMergeMode, ppm);
            } else {
                initial.removedScansByLowCosine++;
            }
        }
        initial.removedScansByLowQuality += (totalNumberOfScans - scansToMerge.size());
        return initial;
    }

    private static MergedSpectrum merge(MergedSpectrum left, MergedSpectrum right, MzMergeMode mzMergeMode, IntensityMergeMode intensityMergeMode, double ppm) {
        DataPoint[] byInt = right.data.clone();
        Arrays.sort(byInt, (u,v)->Double.compare(v.getIntensity(),u.getIntensity()));
        MergedDataPoint[] merge = merge(left.data, byInt, mzMergeMode, intensityMergeMode, ppm);
        return left.merge(right,merge);
    }

    private static MergedSpectrum merge(MergedSpectrum left, Scan right, DataPoint[] rightData, MzMergeMode mzMergeMode, IntensityMergeMode intensityMergeMode, double ppm) {
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
    private static MergedDataPoint[] merge(MergedDataPoint[] orderedByMz, DataPoint[] orderedByInt, MzMergeMode mzMergeMode, IntensityMergeMode intensityMergeMode, double expectedPPM) {
        // we assume a rather large deviation as signal peaks should be contained in more than one
        // measurement
        final List<MergedDataPoint> append = new ArrayList<>();
        final double absoluteDeviation = 400 * expectedPPM * 1e-6;
        for (int k = 0; k < orderedByInt.length; ++k) {
            final DataPoint peak = orderedByInt[k];
            final double dev = Math.max(absoluteDeviation, peak.getMZ() * 4 * expectedPPM * 1e-6);
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
