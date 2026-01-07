/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_masslynx;

import io.github.mzmine.modules.io.import_rawdata_imzml.Coordinates;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters.HorizontalStart;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters.ScanDirection;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters.VerticalStart;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ImagingMetadata {

  private static final Logger logger = Logger.getLogger(ImagingMetadata.class.getName());

  private final ImagingParameters imagingParameters;
  @NotNull
  private final Map<Float, Integer> xIndexMap = new HashMap<>();
  private final Map<Float, Integer> yIndexMap = new HashMap<>();
  MemorySegment scanInfoBuffer = Arena.ofAuto().allocate(ScanInfo.layout());

  public ImagingMetadata(@NotNull final MassLynxDataAccess ml) {
    HorizontalStart horizontalStart = null;
    VerticalStart verticalStart = null;
    ScanDirection scanDirection = null;
    Position previousPosition = null;

    final TreeMap<Float, TreeSet<Position>> coordinateMap = new TreeMap<>(Float::compare);

    // loop through all scans in all functions and get the laster coodrinates. Waters does not provide
    // an option to get the size/dimensions of the image, so we have to calculate that ourselves.
    for (int f = 0; f < ml.getNumberOfFunctions(); f++) {
      for (int s = 0; s < ml.getNumberOfScansInFunction(f); s++) {

        final ScanInfoWrapper scanInfo = ml.getScanInfo(f, s, scanInfoBuffer);
        if (Float.compare(scanInfo.laserYPos(), MassLynxConstants.NO_POSITION) == 0
            && Float.compare(scanInfo.laserXPos(), MassLynxConstants.NO_POSITION) == 0) {
          continue;
        }
        final TreeSet<Position> positions = coordinateMap.computeIfAbsent(scanInfo.laserXPos(),
            _ -> new TreeSet<>(Position::compareTo));

        if (previousPosition != null && (horizontalStart == null || verticalStart == null
            || scanDirection == null)) {
          final Position thisPosition = new Position(scanInfo.laserXPos(), scanInfo.laserYPos());

          // it's possible that the first pixel is a single one on top of the image, then we cannot
          // derive the correct start positions and directions
          if (thisPosition.differentCoordiniates(previousPosition) == 2) {
            previousPosition = thisPosition;
          } else if (thisPosition.differentCoordiniates(previousPosition) == 1) {

            if (Float.compare(thisPosition.x, previousPosition.x) != 0) {
              scanDirection = ScanDirection.HORIZONTAL;
            } else {
              scanDirection = ScanDirection.VERTICAL;
            }

            // assume the 0 point is on the top left of the stage
            if (thisPosition.x > previousPosition.x) {
              horizontalStart = HorizontalStart.LEFT;
            } else if (thisPosition.x < previousPosition.x) {
              horizontalStart = HorizontalStart.RIGHT;
            }

            // assume the 0 point is on the top left of the stage
            if (thisPosition.y < previousPosition.y) {
              verticalStart = VerticalStart.TOP;
            } else if (thisPosition.y > previousPosition.y) {
              verticalStart = VerticalStart.BOTTOM;
            }
            previousPosition = thisPosition;
          }
        }

        if (previousPosition == null) {
          previousPosition = new Position(scanInfo.laserXPos(), scanInfo.laserYPos());
        }

        positions.add(new Position(scanInfo.laserXPos(), scanInfo.laserYPos()));
      }
    }

    final List<Float> yValues = coordinateMap.values().stream().flatMap(Set::stream)
        .map(Position::y).distinct().sorted().toList();
    for (int i = 0; i < yValues.size(); i++) {
      final Float yValue = yValues.get(i);
      yIndexMap.put(yValue, i);
    }

    final DoubleSummaryStatistics summaryY = yValues.stream().mapToDouble(Float::doubleValue)
        .summaryStatistics();
    final int pixelInY = (int) summaryY.getCount();
    final float minY = (float) summaryY.getMin();
    final float maxY = (float) summaryY.getMax();


    final List<Float> xValues = coordinateMap.values().stream().flatMap(Set::stream)
        .map(Position::x).distinct().sorted().toList();
    for (int i = 0; i < xValues.size(); i++) {
      final Float xValue = xValues.get(i);
      xIndexMap.put(xValue, i);
    }

    final DoubleSummaryStatistics summaryX = xValues.stream().mapToDouble(Float::doubleValue)
        .summaryStatistics();
    final int pixelInX = (int) summaryX.getCount();
    final float minX = (float) summaryX.getMin();
    final float maxX = (float) summaryX.getMax();

    final float lateralWidth = maxX - minX;
    final float lateralHeight = maxY - minY;

    imagingParameters = new ImagingParameters(lateralWidth, lateralHeight, lateralWidth / pixelInX,
        lateralHeight / pixelInY, pixelInX, pixelInY);
  }

  @Nullable
  public Coordinates getCoordinates(@NotNull ScanInfoWrapper scanInfo) {
    final float x = scanInfo.laserXPos();
    final float y = scanInfo.laserYPos();

    if (Float.compare(x, MassLynxConstants.NO_POSITION) == 0
        && Float.compare(y, MassLynxConstants.NO_POSITION) == 0) {
      return null;
    }

    final Integer xIndex = xIndexMap.get(x * 1000);
    final Integer yIndex = yIndexMap.get(y * 1000);

    if (xIndex != null && yIndex != null) {
      return new Coordinates(xIndex, yIndex, 0);
    }

    logger.warning(() -> "No index position found for x %.2f -> %s, y %.2f -> %s".formatted(x,
        String.valueOf(xIndex), y, String.valueOf(yIndex)));

    return null;
  }

  static final class Position implements Comparable<Position> {

    private final float x;
    private final float y;

    Position(float xInMm, float y) {
      this.x = xInMm * 1000;
      this.y = y * 1000;
    }

    @Override
    public int compareTo(@NotNull ImagingMetadata.Position o) {
      final int xCompare = Float.compare(x, o.x);
      if (xCompare != 0) {
        return xCompare;
      }
      return Float.compare(y, o.y);
    }

    public int differentCoordiniates(@NotNull Position o) {
      int different = 0;
      different += Float.compare(x, o.x) != 0 ? 1 : 0;
      different += Float.compare(y, o.y) != 0 ? 1 : 0;
      return different;
    }

    public float x() {
      return x;
    }

    public float y() {
      return y;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (Position) obj;
      return Float.floatToIntBits(this.x) == Float.floatToIntBits(that.x)
          && Float.floatToIntBits(this.y) == Float.floatToIntBits(that.y);
    }

    @Override
    public int hashCode() {
      return Objects.hash(x, y);
    }

    @Override
    public String toString() {
      return "Position[" + "x=" + x + ", " + "y=" + y + ']';
    }

  }

  @Nullable
  public ImagingParameters getImagingParameters() {
    return imagingParameters;
  }

  boolean hasMoreThanOnePosition() {
    return xIndexMap.size() > 1 || yIndexMap.size() > 1;
  }
}
