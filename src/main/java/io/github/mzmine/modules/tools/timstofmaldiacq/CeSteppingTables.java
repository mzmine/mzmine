package io.github.mzmine.modules.tools.timstofmaldiacq;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class CeSteppingTables {

  private static final Logger logger = Logger.getLogger(CeSteppingTables.class.getName());
  private final float[] ces;
  private final double isolationWidth;
  private final String isoString;

  public CeSteppingTables(String ceString, Double isolationWidth) {
    final String[] ceStrings = ceString.split(",");
    final int num = ceStrings.length;

    this.isolationWidth = Objects.requireNonNullElse(isolationWidth, 1.5d);
    isoString = String.format("%.1f", this.isolationWidth);

    ces = new float[num];
    for (int i = 0; i < num; i++) {
      ces[i] = Float.parseFloat(ceStrings[i]);
    }
  }

  public int getNumberOfCEs() {
    return ces.length;
  }

  public boolean writeCETable(int index, @NotNull final File file) {
    final String header = "mass,iso_width,ce,charge,type";
    final String line1 = "100.0," + isoString + "," + String.valueOf(ces[index]) + ",1,0";
    final String line2 = "500.0," + isoString + "," + String.valueOf(ces[index]) + ",1,0";
    final String line3 = "1000.0," + isoString + "," + String.valueOf(ces[index]) + ",1,0";

    try {
      final BufferedWriter writer = new BufferedWriter(new FileWriter(file));

      writer.write(header);
      writer.newLine();
      writer.write(line1);
      writer.newLine();
      writer.write(line2);
      writer.newLine();
      writer.write(line3);
      writer.newLine();
      writer.flush();
      writer.close();
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      return false;
    }
    return true;
  }

  public float getCE(int index) {
    return ces[index];
  }
}
