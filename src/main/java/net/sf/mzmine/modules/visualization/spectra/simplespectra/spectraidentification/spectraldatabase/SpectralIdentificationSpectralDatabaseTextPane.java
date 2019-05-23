/*
 * Copyright 2006-2019 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase;

import java.awt.Dimension;
import javax.swing.JTextPane;

public class SpectralIdentificationSpectralDatabaseTextPane extends JTextPane {

  /**
   * TextPane with line break for spectra DB reulst frame
   * 
   * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
   */
  private static final long serialVersionUID = 1L;

  public SpectralIdentificationSpectralDatabaseTextPane(String databaseEntry) {

    // perform line wrapping for long Strings without breaks, such as SMILES strings
    String parsedStr = null;
    int ctr = 0;
    char[] string = databaseEntry.toCharArray();
    for (int i = 0; i < string.length; i++) {
      if (string[i] == ' ') {
        ctr++;
      }
    }
    if (databaseEntry.length() > 34 && ctr <= 1) {
      parsedStr = databaseEntry.replaceAll("(.{35})", "$1\n");
    } else
      parsedStr = databaseEntry;
    this.setText(parsedStr);
    this.setToolTipText(databaseEntry);
    this.setEditable(false);
    this.setPreferredSize(new Dimension(250, 100));
  }

}
