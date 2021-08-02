/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
