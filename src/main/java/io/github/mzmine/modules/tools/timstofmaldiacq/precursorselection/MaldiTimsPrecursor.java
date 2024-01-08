/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.Feature;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

public final class MaldiTimsPrecursor {

  private final Feature feature;
  private final double mz;
  private final Range<Float> mobility;
  private final Map<Double, Integer> collisionEnergies;

  public MaldiTimsPrecursor(Feature feature, double mz, Range<Float> mobility,
      Collection<Double> collisionEnergies) {
    this.feature = feature;
    this.mz = mz;
    this.mobility = mobility;
    this.collisionEnergies = new HashMap<>();
    if (collisionEnergies != null) {
      collisionEnergies.forEach(d -> this.collisionEnergies.put(d, 0));
    }
  }

  public Feature feature() {
    return feature;
  }

  public double mz() {
    return mz;
  }

  public Range<Float> mobility() {
    return mobility;
  }

  public Map<Double, Integer> collisionEnergies() {
    return collisionEnergies;
  }

  public int getTotalMsMs() {
    return collisionEnergies.values().stream().mapToInt(Integer::intValue).sum();
  }

  public int getLowestMsMsCountForCollisionEnergies() {
    return collisionEnergies.values().stream().mapToInt(Integer::intValue).min().orElse(0);
  }

  public int getMsMsSpotsForCollisionEnergy(double c) {
    return collisionEnergies.getOrDefault(c, 0);
  }

  public Double getCollisionEnergyWithFewestSpots() {
    final Optional<Entry<Double, Integer>> first = collisionEnergies.entrySet().stream()
        .sorted(Comparator.comparingInt(Entry::getValue)).findFirst();

    return first.map(Entry::getKey)
        .orElseThrow(() -> new RuntimeException("No collision energy available."));
  }

  public int incrementSpotCounterForCollisionEnergy(double energy) {
    Integer ceCounter = collisionEnergies.get(energy);
    if (ceCounter == null) {
      throw new IllegalArgumentException(
          String.format("Cannot increment counter for collision energy %.2f, invalid CE.", energy));
    }
    ceCounter++;
    collisionEnergies.put(energy, ceCounter);
    return ceCounter;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (MaldiTimsPrecursor) obj;
    return Objects.equals(this.feature, that.feature)
        && Double.doubleToLongBits(this.mz) == Double.doubleToLongBits(that.mz) && Objects.equals(
        this.mobility, that.mobility) && Objects.equals(this.collisionEnergies,
        that.collisionEnergies);
  }

  @Override
  public int hashCode() {
    return Objects.hash(feature, mz, mobility, collisionEnergies);
  }

  @Override
  public String toString() {
    return "MaldiTimsPrecursor[" + "feature=" + feature + ", " + "mz=" + mz + ", " + "oneOverK0="
        + mobility + ", " + ", " + "collisionEnergies=" + collisionEnergies + ']';
  }


}
