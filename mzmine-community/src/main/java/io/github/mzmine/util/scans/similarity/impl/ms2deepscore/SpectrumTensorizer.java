/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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


import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import org.jetbrains.annotations.NotNull;

/**
 * Bins spectra and returns two tensors, one for metadata and one for fragments
 */
public class SpectrumTensorizer {

  private final SettingsMS2Deepscore settings;
  private final int numBins;

  public SpectrumTensorizer(SettingsMS2Deepscore settings) {
    this.settings = settings;
    this.numBins = (int) ((settings.maximumMZ() - settings.minimumMZ()) / settings.binWidth());
  }

  public float[] tensorizeFragments(Scan spectrum) {
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

  private float binarizePolarity(Scan scan) {
    @NotNull PolarityType polarity = scan.getPolarity();
    if (!(polarity == PolarityType.POSITIVE || polarity == PolarityType.NEGATIVE)) {
      throw new RuntimeException("The polarity has to be positive or negative");
    }
    if (polarity == PolarityType.POSITIVE) {
      return 1;
    } else {
      return 0;
    }
  }

  private float scalePrecursorMZ(Scan scan, float mean, float standardDeviation) {
//    @robin what happens if this is null, does that throw an exception?
    Double precursorMZ = scan.getPrecursorMz();
    if (precursorMZ == null) {
      throw new RuntimeException("The precursor mz cannot be null to run ms2deepscore");
    }
    return (float) (precursorMZ - mean) / standardDeviation;
  }


  public float[] tensorizeMetadata(Scan scan) {
    return new float[]{scalePrecursorMZ(scan, 0, 1000), binarizePolarity(scan)};
  }

  public TensorizedSpectra tensorizeSpectra(Scan[] scans) {

    float[][] metadataVectors = new float[scans.length][numBins];
    float[][] fragmentVectors = new float[scans.length][numBins];

    for (int i = 0; i < scans.length; i++) {
      metadataVectors[i] = tensorizeMetadata(scans[i]);
      fragmentVectors[i] = tensorizeFragments(scans[i]);
    }

    return new TensorizedSpectra(fragmentVectors, metadataVectors);

  }
}