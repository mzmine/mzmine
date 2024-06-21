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

package io.github.mzmine.modules.tools.siriusapi;

import de.unijena.bioinf.ms.nightsky.sdk.NightSkyClient;
import de.unijena.bioinf.ms.nightsky.sdk.model.AlignedFeature;
import de.unijena.bioinf.ms.nightsky.sdk.model.AlignedFeatureOptField;
import de.unijena.bioinf.ms.nightsky.sdk.model.FeatureImport;
import de.unijena.bioinf.ms.nightsky.sdk.model.Job;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobOptField;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobSubmission;
import de.unijena.bioinf.ms.nightsky.sdk.model.ProjectInfo;
import de.unijena.bioinf.ms.nightsky.sdk.model.StructureCandidateFormula;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

public class Sirius {

  private static final Logger logger = Logger.getLogger(Sirius.class.getName());
  // one session id for this mzmine session
  private static final String sessionId = FileAndPathUtil.safePathEncode(
      "mzmine_%s".formatted(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)));
  private NightSkyClient client;
  private ProjectInfo project;

  public Sirius() {
    client = launchOrGetClient();
    if (project == null) {
      project = client.projects().getProjectSpaces().getFirst();
    }
  }

  public static synchronized NightSkyClient launchOrGetClient() throws RuntimeException {
    final File siriusDir = new File(FileUtils.getUserDirectory(), ".sirius-6.0");
    final File portFile = new File(siriusDir, "sirius.port");

    Integer port = getPort(portFile);

    if (port == null) {
      logger.info("Sirius not running yet, launching.");
      final Runtime runtime = Runtime.getRuntime();
      try {
        runtime.exec(new String[]{"sirius", "rest", "--gui"});
        while ((port = getPort(portFile)) == null) {
          Thread.sleep(100);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return new NightSkyClient(port);
    }
    logger.info("Sirius already running at port: " + port);
    return new NightSkyClient(port.intValue());
  }

  private static @Nullable Integer getPort(File portFile) {
    String portString = null;
    if (!portFile.exists() || !portFile.canRead()) {
      return null;
    }

    try (var reader = Files.newBufferedReader(portFile.toPath(), StandardCharsets.UTF_8)) {
      portString = reader.readLine();
    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot read sirius port file.", e);
    }

    Integer port = null;
    if (portString != null && !portString.isBlank()) {
      port = Integer.parseInt(portString);
    }
    return port;
  }

  public static void main(String[] args) {
    final Sirius sirius = new Sirius();

    final List<ProjectInfo> projectSpaces = sirius.client.projects().getProjectSpaces();
    projectSpaces.forEach(p -> logger.info(p.toString()));
    final ProjectInfo project = projectSpaces.getFirst();

    logger.info(sirius.api().jobs().getDefaultJobConfig(true).toString());

//    sirius.client.features().addAlignedFeatures(project.getProjectId(), )

    /*logger.info(sirius.client.features()
        .getAlignedFeatures(project.getProjectId(), List.of(AlignedFeatureOptField.TOPANNOTATIONS))
        .toString());

    final List<StructureCandidateFormula> candidates = sirius.client.features()
        .getStructureCandidates(project.getProjectId(), "591533384235718370",
            List.of(StructureCandidateOptField.DBLINKS));
    logger.info(candidates.getFirst().toString());

    final StructureCandidateFormula first = candidates.getFirst();

    final SearchableDatabaseParameters searchableDatabaseParameters = new SearchableDatabaseParameters().displayName(
        "Custom database");
    final SearchableDatabase db = sirius.client.databases()
        .createDatabase(sessionId + "_" + Instant.now().toEpochMilli(),
            searchableDatabaseParameters);*/

//    final AlignedFeature alignedFeature = sirius.client.features()
//        .getAlignedFeature(project.getProjectId(), "1002", List.of());
//
//    logger.info(alignedFeature.toString());

    return;
  }

  public NightSkyClient api() {
    return client;
  }

  /**
   * Exports rows to sirius. checks the current project
   *
   * @param rows The rows to export
   * @return A mapping of mzmine feature id {@link FeatureListRow#getID()} to sirius unique id
   * {@link AlignedFeature#getAlignedFeatureId()}.
   */
  public Map<Integer, String> exportToSiriusUnique(List<? extends FeatureListRow> rows) {
    checkLogin();
    final List<FeatureImport> features = rows.stream().map(MzmineToSirius::feature).toList();
//    client.projects().openProjectSpace(project.getProjectId(), null, List.of());

    final Map<Integer, String> mzmineIdToSiriusId = client.features()
        .getAlignedFeatures(project.getProjectId(), List.of(AlignedFeatureOptField.NONE)).stream()
        .collect(Collectors.toMap(
            alignedFeature -> Integer.valueOf(alignedFeature.getExternalFeatureId()),
            AlignedFeature::getAlignedFeatureId));

    final List<FeatureImport> notImportedFeatures = features.stream()
        .filter(f -> mzmineIdToSiriusId.get(Integer.valueOf(f.getExternalFeatureId())) == null)
        .toList();

    var imported = client.features().addAlignedFeatures(project.getProjectId(), notImportedFeatures,
        List.of(AlignedFeatureOptField.NONE));
    var mzmineIdToSiriusId2 = imported.stream().collect(
        Collectors.toMap(AlignedFeature::getExternalFeatureId,
            AlignedFeature::getAlignedFeatureId));

    final HashMap<Integer, String> idMap = new HashMap<>(mzmineIdToSiriusId);
    mzmineIdToSiriusId2.forEach(
        (mzmineId, siriusId) -> idMap.put(Integer.valueOf(mzmineId), siriusId));

    logger.info(() -> "Added features " + idMap.entrySet().stream()
        .map(e -> "%d->%s".formatted(e.getKey(), e.getValue())).collect(Collectors.joining("; ")));
    return idMap;
  }

  /**
   * Maps the rows that are already imported to sirius to their aligned feature id.
   */
  public Map<FeatureListRow, String> mapFeatureToSiriusId(List<? extends FeatureListRow> rows) {
    checkLogin();
    final Map<Integer, String> mzmineIdToSiriusId = client.features()
        .getAlignedFeatures(project.getProjectId(), List.of(AlignedFeatureOptField.NONE)).stream()
        .collect(Collectors.toMap(
            alignedFeature -> Integer.valueOf(alignedFeature.getExternalFeatureId()),
            AlignedFeature::getAlignedFeatureId));

    return rows.stream().collect(Collectors.toMap(r -> r, r -> mzmineIdToSiriusId.get(r.getID())));
  }

  public Job runFingerId(List<FeatureListRow> rows) {
    checkLogin();
    final Map<Integer, String> idsMap = exportToSiriusUnique(rows);
    final JobSubmission submission = client.jobs().getDefaultJobConfig(false);
    submission.setAlignedFeatureIds(idsMap.values().stream().toList());
    final Job job = client.jobs()
        .startJob(project.getProjectId(), submission, List.of(JobOptField.PROGRESS));
    return job;
  }

  public void importResultsForRows(List<FeatureListRow> rows) {
    checkLogin();
    final Map<FeatureListRow, String> rowToSiriusId = mapFeatureToSiriusId(rows);
    rowToSiriusId.forEach((row, id) -> {
      final List<StructureCandidateFormula> structureCandidates = api().features()
          .getStructureCandidates(project.getProjectId(), id, List.of());
      final List<CompoundDBAnnotation> siriusAnnotations = structureCandidates.stream()
          .sorted(Comparator.comparingInt(StructureCandidateFormula::getRank))
          .map(SiriusToMzmine::toMzmine).toList();
      final List<CompoundDBAnnotation> annotations = new ArrayList<>(row.getCompoundAnnotations());
      annotations.addAll(siriusAnnotations);
      row.setCompoundAnnotations(annotations);
    });
  }

  public void checkLogin() {
    if (!client.account().isLoggedIn()) {
      DesktopService.getDesktop().displayErrorMessageAndThrow(new SiriusNotLoggedInException());
    }
  }

}
