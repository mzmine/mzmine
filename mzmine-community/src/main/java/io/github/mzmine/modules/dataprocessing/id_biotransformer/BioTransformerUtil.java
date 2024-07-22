/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ALogPType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.EnzymeType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ReactionType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.modules.dataprocessing.id_biotransformer.BioTransformerParameters.TransformationTypes;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.CSVParsingUtils.CompoundDbLoadResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class BioTransformerUtil {

  private static final List<ImportType> types = List.of(
      new ImportType(true, "Molecular formula", DataTypes.get(FormulaType.class)),
      new ImportType(true, "SMILES", DataTypes.get(SmilesStructureType.class)),
      new ImportType(true, "InChI", DataTypes.get(InChIStructureType.class)),
      new ImportType(true, "InChIKey", DataTypes.get(InChIKeyStructureType.class)),
      new ImportType(true, "Reaction", DataTypes.get(ReactionType.class)),
      new ImportType(true, "Enzyme(s)", DataTypes.get(EnzymeType.class)),
      new ImportType(true, "ALogP", DataTypes.get(ALogPType.class)),
      new ImportType(true, "Metabolite ID", DataTypes.get(CompoundNameType.class)));
  private static final Logger logger = Logger.getLogger(BioTransformerUtil.class.getName());

  private BioTransformerUtil() {
  }

  @NotNull
  public static List<String> buildCommandLineArguments(@NotNull String smiles,
      @NotNull ParameterSet param, @NotNull File outputFile) {

    final List<String> cmdList = new ArrayList<>();
    cmdList.add("java");

    final String path = param.getValue(BioTransformerParameters.bioPath).getAbsolutePath();
    final String name = new File(path).getName();
    cmdList.add("-jar");
    cmdList.add(name);
    cmdList.add("-k");
    cmdList.add("pred");

    final TransformationTypes transformation = param.getValue(
        BioTransformerParameters.transformationType);
    cmdList.add("-b");
    cmdList.add(transformation.transformationName());

    final Integer steps = param.getValue(BioTransformerParameters.steps);
    cmdList.add("-s");
    cmdList.add(String.valueOf(steps));

    cmdList.add("-ismi");
    cmdList.add("\"" + smiles + "\"");
    cmdList.add("-ocsv");
    cmdList.add("\"" + outputFile.getAbsolutePath() + "\"");

    return cmdList;
  }


  public static List<CompoundDBAnnotation> parseLibrary(final File file,
      IonNetworkLibrary library) {
    if (!file.exists() && file.canRead()) {
      logger.info(
          () -> "BioTransformer result file does not exist, this means that no metabolites were predicted.");
      return List.of();
    }
    final CompoundDbLoadResult annotationResults = CSVParsingUtils.getAnnotationsFromCsvFile(file,
        ",", types, library);
    if (annotationResults.status() != TaskStatus.ERROR) {
      return annotationResults.annotations();
    } else {
      logger.info(annotationResults.errorMessage());
      return List.of();
    }
  }

  public static boolean runCommandAndWait(File dir, List<String> cmd) {
    try {
      ProcessBuilder b = new ProcessBuilder();
      b.directory(dir);
      b.command(cmd);

      final String command = cmd.stream().collect(Collectors.joining(" "));
      logger.finest(() -> "Running biotransformer with cmd: " + command);

      Process process = b.start();
      StringBuilder output = new StringBuilder();
      BufferedReader errorReader = new BufferedReader(
          new InputStreamReader(process.getErrorStream()));
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

      String line = null;
      String error = null;
      while ((line = reader.readLine()) != null || (error = errorReader.readLine()) != null) {
//        output.append(line).append("\n");
        logger.finest(line);
        if (error != null) {
          logger.severe(error);
//          output.append("ERROR: ").append(error).append("\n");
        }
      }

      int exitVal = process.waitFor();
      errorReader.close();
      reader.close();
      process.getOutputStream().close();
//      logger.info(() -> output.toString());
      if (exitVal != 0) {
        logger.warning(() -> "Error " + exitVal + " while running bio transformer command " + cmd);
        return false;
      }
    } catch (IOException | InterruptedException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      return false;
    }
    return true;
  }
}
