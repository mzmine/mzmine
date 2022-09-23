package io.github.mzmine.modules.tools.timstofmaldiacq.imaging;

import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.MaldiTimsPrecursor;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.TopNSelectionModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ImagingSpot {

  private final MaldiSpotInfo spotInfo;
  private final List<MaldiTimsPrecursor>[][] precursorLists;

  private final Ms2ImagingMode imagingMode;

  public ImagingSpot(MaldiSpotInfo spotInfo, Ms2ImagingMode mode) {
    this.spotInfo = spotInfo;

    this.imagingMode = mode;
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
      case default -> {
        throw new IllegalStateException("Illegal value for Ms2ImagingMode");
      }
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
   * @return True if the precursor was added successfully somewhere adjacent to this spot.
   */
  public boolean addPrecursor(MaldiTimsPrecursor precursor) {
    switch (imagingMode) {
      case TRIPLE -> {
        for (int x = 0; x < 2; x++) {
          for (int y = 0; y < 2; y++) {
            if (x == 0 && y == 0) {
              continue;
            }
            if (addPrecursorToList(precursor, x, y)) {
              return true;
            }
          }
        }
        return false;
      }
      case SINGLE -> {
        return addPrecursorToList(precursor, 0, 0);
      }
    }
    return false;
  }

  /**
   * @param xOffset integer offset in x direction from this spot
   * @param yOffset integer offset in y direction from this spot
   * @return True if the precursor was added successfully, false if it would overlap
   */
  public boolean addPrecursorToList(MaldiTimsPrecursor precursor, int xOffset, int yOffset) {
    var list = getPrecursorList(xOffset, yOffset);

    for (MaldiTimsPrecursor p : list) {
      if (TopNSelectionModule.overlaps(p, precursor)) {
        return false;
      }
    }

    list.add(precursor);
    return true;
  }

  public MaldiSpotInfo spotInfo() {
    return spotInfo;
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
