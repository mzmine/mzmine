package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import com.Ostermiller.util.CSVParser;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.parameters.ParameterSet;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.Nullable;

public class BioTransformerUtil {

  @Nullable
  public static String buildCommandLine(CompoundDBAnnotation annotation, ParameterSet param,
      File outputFile) {

    if (annotation.getSmiles() == null) {
      return null;
    }

    final StringBuilder b = new StringBuilder("java -jar ");
    final String path = param.getValue(BioTransformerParameters.bioPath).getAbsolutePath();
    b.append(path);
    b.append(" -k pred");

    final String transformation = param.getValue(BioTransformerParameters.transformationType);
    b.append(" -b ").append(transformation);

    final Integer steps = param.getValue(BioTransformerParameters.steps);
    b.append(" -s ").append(steps);

    b.append(" -ismi \"").append(annotation.getSmiles()).append("\"");

    b.append(" -ocsv ").append(outputFile.getAbsolutePath());

    final String cmdOptions = param.getValue(BioTransformerParameters.cmdOptions);
    b.append(" ").append(cmdOptions);

    return b.toString().trim();
  }

  public static List<CompoundDBAnnotation> parseLibrary(final File file, final IonType[] ionTypes,
      final AtomicBoolean canceled, final AtomicInteger parsedLines) throws IOException {

    final FileReader dbFileReader = new FileReader(file);
    final CSVParser parser = new CSVParser(dbFileReader, ',');

    final List<CompoundDBAnnotation> annotations = new ArrayList<>();

    parser.getLine();
    String[] line = null;
    while ((line = parser.getLine()) != null && !canceled.get()) {
      for (final IonType ionType : ionTypes) {
        annotations.add(BioTransformerAnnotation.fromCsvLine(line, ionType));
      }
      parsedLines.getAndIncrement();
    }

    return annotations;
  }
}
