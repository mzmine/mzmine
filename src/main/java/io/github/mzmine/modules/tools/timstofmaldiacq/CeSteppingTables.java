package io.github.mzmine.modules.tools.timstofmaldiacq;

import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class CeSteppingTables {

  private static final Logger logger = Logger.getLogger(CeSteppingTables.class.getName());
  private final float[] ces;

  public CeSteppingTables(String ceString) {
    final String[] ceStrings = ceString.split(",");
    final int num = ceStrings.length;

    ces = new float[num];
    for(int i = 0; i < num; i++) {
      ces[i] = Float.parseFloat(ceStrings[i]);
    }
  }

  public int getNumberOfCEs() {
    return ces.length;
  }

  public boolean writeCETable(int index, @NotNull final File file) {
    final String[] header = {"mass", "iso_width", "ce", "charge", "type"};
    final String[] line1 = {"100.0", "1.5", String.valueOf(ces[index]), "1", "0"};
    final String[] line2 = {"500.0", "1.5", String.valueOf(ces[index]), "1", "0"};
    final String[] line3 = {"1000.0", "1.5", String.valueOf(ces[index]), "1", "0"};

    try {
      final ICSVWriter writer = new CSVWriterBuilder(new FileWriter(file)).withSeparator(',')
          .withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER).build();

      writer.writeNext(header);
      writer.writeNext(line1);
      writer.writeNext(line2);
      writer.writeNext(line3);
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
