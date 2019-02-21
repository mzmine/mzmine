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

package net.sf.mzmine.modules.datapointprocessing.datamodel.results;

/**
 * This interface is used to store data point processing results in a {@link net.sf.mzmine.modules.datapointprocessing.datamodel.ProcessedDataPoint}
 * When adding a new result type, also add it to the ResultType enum.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public abstract class DPPResult<T> {

  public enum ResultType {SUMFORMULA, ISOTOPEPATTERN, ADDUCT, FRAGMENT};
  
  final T value;
  
  public DPPResult (T value) {
    this.value = value;
  }
  
  public T getValue() {
    return value;
  }

//  public String toString();
  
  public abstract String generateLabel();

  public abstract ResultType getResultType();
}
