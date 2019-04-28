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

package net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.parser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.SpectralMatchTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;

public interface SpectralDBParser {

  /**
   * Parses the file and creates spectral db entries
   * 
   * @param task
   * 
   * @param dataBaseFile
   * @return the list or an empty list if something went wrong (e.g., wrong format)
   * @throws IOException
   */
  @Nonnull
  public List<SpectralMatchTask> parse(AbstractTask mainTask, PeakList peakList,
      ParameterSet parameters, File dataBaseFile) throws IOException;
}
