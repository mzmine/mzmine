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

package io.github.mzmine.util.scans.similarity.impl.ms2deepscore;


import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bins spectra and returns two tensors, one for metadata and one for fragments
 */
public class MS2DeepscoreSpectrumTensorizer {

  private static final Logger logger = Logger.getLogger(
      MS2DeepscoreSpectrumTensorizer.class.getName());
  private final MS2DeepscoreSettings settings;
  private final int numBins;

  public MS2DeepscoreSpectrumTensorizer(MS2DeepscoreSettings settings) {
    this.settings = settings;
    this.numBins = (int) ((settings.maximumMZ() - settings.minimumMZ()) / settings.binWidth());
  }

  public float[] tensorizeFragments(MassSpectrum spectrum) {
    float[] vector = new float[numBins];

    int numberOfDataPoints = spectrum.getNumberOfDataPoints();
    for (int i = 0; i < numberOfDataPoints; i++) {
      float mz = (float) spectrum.getMzValue(i);
      float intensity = (float) spectrum.getIntensityValue(i);
      if (settings.minimumMZ() <= mz && mz < settings.maximumMZ()) {
        int binIndex = (int) ((mz - settings.minimumMZ()) / settings.binWidth());
        vector[binIndex] = (float) Math.max(vector[binIndex],
            Math.pow(intensity, settings.intensityScaling()));
      }
    }
    return vector;
  }

  private float binarizePolarity(@Nullable PolarityType polarity) {
    return polarity == PolarityType.NEGATIVE ? 0 : 1;
  }

  /**
   * @return scaled precursor mz or null if there was no precursor mz
   */
  @Nullable
  private Float scalePrecursorMZ(@Nullable Double precursorMZ, float mean,
      float standardDeviation) {
    if (precursorMZ == null) {
      return null;
    }
    return (precursorMZ.floatValue() - mean) / standardDeviation;
  }

  /**
   * @return vector or null if the scan has no precursor mz
   */
  public float @Nullable [] tensorizeMetadata(@NotNull Scan scan) {
    return tensorizeMetadata(scan.getPrecursorMz(), scan.getPolarity());
  }


  private float @Nullable [] tensorizeMetadata(@Nullable final Double precursorMz,
      @Nullable final PolarityType polarity) {
    Float scaledMz = scalePrecursorMZ(precursorMz, 0, 1000);
    if (scaledMz == null) {
      return null;
    }
    return new float[]{scaledMz, binarizePolarity(polarity)};
  }


  /**
   * Only works on scans or {@link SpectralLibraryEntry} with precursor mz
   */
  @NotNull
  public TensorizedSpectra tensorizeSpectra(@NotNull List<? extends MassSpectrum> scans) {
    int originalSize = scans.size();

    // requires precursor mz
    scans = scans.stream().filter(scan -> ScanUtils.getPrecursorMz(scan) != null).toList();

    if (originalSize > scans.size()) {
      logger.info(
          "List contained spectra without precursor m/z, those were filtered out. Remaining: %d; Total: %d".formatted(
              scans.stream(), originalSize));
    }

    float[][] metadataVectors = new float[scans.size()][numBins];
    float[][] fragmentVectors = new float[scans.size()][numBins];

    for (int i = 0; i < scans.size(); i++) {
      MassSpectrum spectrum = scans.get(i);

      Double precursorMz = ScanUtils.getPrecursorMz(spectrum);
      PolarityType polarity = ScanUtils.getPolarity(spectrum);
      metadataVectors[i] = tensorizeMetadata(precursorMz, polarity);

      // extract masslist if scan otherwise use fragments directly
      MassSpectrum fragments = spectrum;
      if (spectrum instanceof Scan scan) {
        fragments = scan.getMassList();
        if (fragments == null) {
          throw new MissingMassListException(scan);
        }
      }
      fragmentVectors[i] = tensorizeFragments(fragments);
    }

    return new TensorizedSpectra(fragmentVectors, metadataVectors);
  }

}