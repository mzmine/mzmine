/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */package net.sf.mzmine.datamodel.identities.ms2;

import java.util.ArrayList;
import java.util.List;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.identities.iontype.IonType;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

/**
 * One MSMS signal identified by several x-mers (M , 2M, 3M ...)
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class MSMSMultimerIdentity extends MSMSIonIdentity {

  // the identified x-mer
  private List<MSMSMultimerIdentity> links;

  public MSMSMultimerIdentity(MZTolerance mzTolerance, DataPoint dp, IonType b) {
    super(mzTolerance, dp, b);
  }

  public List<MSMSMultimerIdentity> getLinks() {
    return links;
  }

  public void addLink(MSMSMultimerIdentity l) {
    if (links == null)
      links = new ArrayList<>();
    links.add(l);
  }

  public int getLinksCount() {
    return links == null ? 0 : links.size();
  }

  public int getMCount() {
    return getType().getMolecules();
  }

}
