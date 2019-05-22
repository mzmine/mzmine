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
