/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.masscalibration.standardslist;

import com.google.common.collect.Range;

import java.util.ArrayList;
import java.util.List;


/**
 * Class for maintaining a list of standard molecules
 * expected to appear in the dataset
 */
public class StandardsList {
  protected ArrayList<StandardsListItem> standardMolecules;

  public StandardsList(List<StandardsListItem> standardMolecules) {
    this.standardMolecules = new ArrayList<StandardsListItem>(standardMolecules);
    this.standardMolecules.sort(StandardsListItem.mzComparator);
  }
  
  public ArrayList<StandardsListItem> getInRanges(Range<Double> mzRange, Range<Double> rtSecRange){
    ArrayList<StandardsListItem> withinRange = new ArrayList<>();
    for(StandardsListItem molecule: standardMolecules){
      if(mzRange.contains(molecule.getMzRatio()) && rtSecRange.contains(molecule.getRetentionTimeSec())){
        withinRange.add(molecule);
      }
    }
    return withinRange;
  }

  public ArrayList<StandardsListItem> getStandardsMolecules() {
    return standardMolecules;
  }
}
