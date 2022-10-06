/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.identities.iontype.networks;


import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.Comparator;

/**
 * Sort ion identity networks
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class IonNetworkSorter implements Comparator<IonNetwork> {

  private final SortingProperty property;
  private final SortingDirection direction;

  public IonNetworkSorter(SortingProperty property, SortingDirection direction) {
    this.property = property;
    this.direction = direction;
  }

  @Override
  public int compare(IonNetwork a, IonNetwork b) {
    Double va = getValue(a);
    Double vb = getValue(b);

    if (direction == SortingDirection.Ascending) {
      return va.compareTo(vb);
    } else {
      return vb.compareTo(va);
    }
  }

  private Double getValue(IonNetwork net) {
    switch (property) {
      case Height:
        return net.getHeightSum();
      case MZ:
        return net.getNeutralMass();
      case RT:
        return net.getAvgRT();
      case ID:
        return (double) net.getID();
    }

    // We should never get here, so throw exception
    throw (new IllegalStateException());
  }

}
