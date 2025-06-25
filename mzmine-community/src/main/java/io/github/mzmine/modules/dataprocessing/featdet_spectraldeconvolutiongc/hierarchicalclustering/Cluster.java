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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.hierarchicalclustering;

import io.github.mzmine.datamodel.features.ModularFeature;
import java.util.ArrayList;
import java.util.List;

public class Cluster {

  List<ModularFeature> features;
  float minRT;
  float maxRT;
  ModularFeature representativeFeature;

  Cluster(ModularFeature feature) {
    this.features = new ArrayList<>();
    this.features.add(feature);
    this.minRT = feature.getRT();
    this.maxRT = feature.getRT();
    this.representativeFeature = feature;
  }

  float getRTDistance(Cluster other) {
    return Math.abs(this.representativeFeature.getRT() - other.representativeFeature.getRT());
  }

  void addFeatures(Cluster other) {
    this.features.addAll(other.features);
    this.minRT = Math.min(this.minRT, other.minRT);
    this.maxRT = Math.max(this.maxRT, other.maxRT);
    if (other.representativeFeature.getHeight() > this.representativeFeature.getHeight()) {
      this.representativeFeature = other.representativeFeature;
    }
  }

  ModularFeature getRepresentativeFeature() {
    return representativeFeature;
  }

}
