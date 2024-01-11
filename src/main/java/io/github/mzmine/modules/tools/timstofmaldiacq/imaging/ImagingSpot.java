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

package io.github.mzmine.modules.tools.timstofmaldiacq.imaging;

import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.MaldiTimsPrecursor;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.TopNSelectionModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ImagingSpot {

  /**
   * Maximum number of 25 precursors in a tims ramp when using the simple ESI-PASEF collision
   * energies.
   */
  private static final int MAX_PRECURSORS = 25;

  private final MaldiSpotInfo spotInfo;
  private final List<MaldiTimsPrecursor>[][] precursorLists;

  private final double collisionEnergy;

  private final Ms2ImagingMode imagingMode;

  public ImagingSpot(MaldiSpotInfo spotInfo, Ms2ImagingMode mode, double collisionEnergy) {
    this.spotInfo = spotInfo;

    this.imagingMode = mode;
    this.collisionEnergy = collisionEnergy;

    switch (imagingMode) {
      case SINGLE -> {
        precursorLists = new List[2][2];
        precursorLists[0][0] = new ArrayList<>();
        precursorLists[0][1] = List.of();
        precursorLists[1][1] = List.of();
        precursorLists[1][0] = List.of();
      }
      case TRIPLE -> {
        precursorLists = new List[2][2];
        precursorLists[0][0] = List.of();
        precursorLists[0][1] = new ArrayList<>();
        precursorLists[1][1] = new ArrayList<>();
        precursorLists[1][0] = new ArrayList<>();
      }
      default -> throw new IllegalStateException("Illegal value for Ms2ImagingMode");
    }
  }

  public List<MaldiTimsPrecursor> getPrecursorList(int xOffset, int yOffset) {
    assert xOffset < 2 && yOffset < 2;
    return precursorLists[xOffset][yOffset];
  }

  public List<List<MaldiTimsPrecursor>> getPrecursorLists() {
    return switch (imagingMode) {
      case SINGLE -> List.of(precursorLists[0][0]);
      case TRIPLE -> List.of(precursorLists[0][1], precursorLists[1][0], precursorLists[1][1]);
    };
  }

  /**
   * @return True if the precursor was added successfully somewhere adjacent to this spot. Increases
   * the CE counter in the feature for this spot's collision energy if the precursor was added.
   */
  public boolean addPrecursor(MaldiTimsPrecursor precursor, double minMobilityDistance) {
    switch (imagingMode) {
      case TRIPLE -> {
        for (int x = 0; x < 2; x++) {
          for (int y = 0; y < 2; y++) {
            if (x == 0 && y == 0) {
              continue;
            }
            if (addPrecursorToList(precursor, x, y, minMobilityDistance)) {
              return true;
            }
          }
        }
        return false;
      }
      case SINGLE -> {
        return addPrecursorToList(precursor, 0, 0, minMobilityDistance);
      }
    }
    return false;
  }

  public boolean checkPrecursor(MaldiTimsPrecursor precursor, double minMobilityDistance) {
    switch (imagingMode) {
      case TRIPLE -> {
        for (int x = 0; x < 2; x++) {
          for (int y = 0; y < 2; y++) {
            if (x == 0 && y == 0) {
              continue;
            }
            if (checkPrecursor(precursor, x, y, minMobilityDistance)) {
              return true;
            }
          }
        }
        return false;
      }
      case SINGLE -> {
        return checkPrecursor(precursor, 0, 0, minMobilityDistance);
      }
    }
    return false;
  }

  /**
   * Checks if the precursor can be added to this spot. Also increments the spot msms counter for
   * the CE of this spot.
   *
   * @param xOffset integer offset in x direction from this spot
   * @param yOffset integer offset in y direction from this spot
   * @return True if the precursor was added successfully, false if it would overlap
   */
  private boolean addPrecursorToList(MaldiTimsPrecursor precursor, int xOffset, int yOffset,
      double minMobilityDistance) {
    var list = getPrecursorList(xOffset, yOffset);
    if (list.size() == MAX_PRECURSORS) {
      return false;
    }
    for (MaldiTimsPrecursor p : list) {
      if (TopNSelectionModule.overlaps(p, precursor, minMobilityDistance)) {
        return false;
      }
    }

    list.add(precursor);
    precursor.incrementSpotCounterForCollisionEnergy(collisionEnergy);
    return true;
  }

  private boolean checkPrecursor(MaldiTimsPrecursor precursor, int xOffset, int yOffset,
      double minMobilityDistance) {
    var list = getPrecursorList(xOffset, yOffset);
    if (list.size() == MAX_PRECURSORS) {
      return false;
    }
    for (MaldiTimsPrecursor p : list) {
      if (TopNSelectionModule.overlaps(p, precursor, minMobilityDistance)) {
        return false;
      }
    }

    return true;
  }

  public MaldiSpotInfo spotInfo() {
    return spotInfo;
  }

  public Double getCollisionEnergy() {
    return collisionEnergy;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (ImagingSpot) obj;
    return Objects.equals(this.spotInfo, that.spotInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(spotInfo);
  }

  @Override
  public String toString() {
    return "ImagingSpot[" + "spotInfo=" + spotInfo + ']';
  }

}
