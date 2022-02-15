package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.parameters.ParameterSet;
import java.io.File;
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
}
