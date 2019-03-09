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
 */

package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Used to keep track of the isotope composition of a peak. This result type contains an ArrayList
 * of Strings. A ProcessedDataPoint should only contain one result of this type. If there is more
 * than one isotope involved, they should all be added to this list.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPIsotopeCompositionResult extends DPPResult<List<String>> {

  public DPPIsotopeCompositionResult(List<String> value) {
    super(value);
  }

  public DPPIsotopeCompositionResult(String value) {
    super(new ArrayList<String>());
    
    if(value != null && !value.equals("") && !value.equals(" "))
      getValue().add(value);
  }
  
  public void add(String value) {
    if(value == null || value.equals("") || value.equals(" "))
      return;
    this.getValue().add(value);
  }
  
  public void addAll(Collection<String> values) {
    for(String value : values)
      add(value);
  }

  @Override
  public String generateLabel() {
    String label = "";
    for (String s : value)
      label += s + ", ";
    label = label.substring(0, label.length() - 2);
    return label;
  }

  @Override
  public ResultType getResultType() {
    return ResultType.ISOTOPECOMPOSITION;
  }


}
