package io.github.mzmine.modules.tools.timstofmaldiacq.imaging;

import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.MaldiTimsPrecursor;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.TopNSelectionModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ImagingSpot {

  private final MaldiSpotInfo spotInfo;
  private final List<MaldiTimsPrecursor>[][] precursorLists = new List[2][2];

  public ImagingSpot(MaldiSpotInfo spotInfo) {
    this.spotInfo = spotInfo;

    precursorLists[0][1] = new ArrayList<>();
    precursorLists[1][1] = new ArrayList<>();
    precursorLists[1][0] = new ArrayList<>();
  }

  public List<MaldiTimsPrecursor> getPrecursorList(int xOffset, int yOffset) {
    assert xOffset < 2 && yOffset < 2;
    assert !(xOffset == 0 && yOffset == 0);

    return precursorLists[xOffset][yOffset];
  }

  /**
   * @return True if the precursor was added successfully somewhere adjacent to this spot.
   */
  public boolean addPrecursor(MaldiTimsPrecursor precursor) {
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
