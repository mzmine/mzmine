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

import io.github.mzmine.modules.io.export_features_gnps.GNPSUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import java.io.IOException;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class GNPSTest {

  private static final Logger logger = Logger.getLogger(GNPSTest.class.getName());

  @Test
  @Disabled
  void testLibraryAccess() throws IOException {
    String libraryID = "https://gnps.ucsd.edu/ProteoSAFe/SpectrumCommentServlet?SpectrumID=CCMSLIB00005463737";
    SpectralDBEntry spec = GNPSUtils.accessLibrarySpectrum("CCMSLIB00005463737");
    Assertions.assertNotNull(spec);
    Assertions.assertTrue(spec.getDataPoints().length > 0);
    Assertions.assertTrue(spec.getPrecursorMZ() > 0);
  }
}
