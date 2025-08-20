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

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.sirius.ms.sdk.SiriusSDK;
import io.sirius.ms.sdk.SiriusSDK.ShutdownMode;
import io.sirius.ms.sdk.model.AlignedFeature;
import io.sirius.ms.sdk.model.AlignedFeatureOptField;
import io.sirius.ms.sdk.model.FeatureImport;
import io.sirius.ms.sdk.model.InstrumentProfile;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.JobSubmission;
import io.sirius.ms.sdk.model.ProjectInfo;
import io.sirius.ms.sdk.model.ProjectInfoOptField;
import io.sirius.ms.sdk.model.StructureCandidateFormula;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class Sirius implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(Sirius.class.getName());
  // one session id for this mzmine session
  private static final UUID uuid = UUID.randomUUID();
  private static final String sessionId = FileAndPathUtil.safePathEncode(
      "mzmine_%s".formatted(uuid.toString()));
  private final SiriusSDK sirius;
  private final ProjectInfo projectSpace;

  public Sirius() throws Exception {
    sirius = SiriusSDK.startAndConnectLocally(ShutdownMode.NEVER, true);
    checkLogin();
    logger.finest(sirius.infos().getInfo(null, null).toString());
    ProjectInfo proj = null;
    AtomicReference<ProjectInfo> projRef = new AtomicReference<>();
    File projectFile = new File(FileAndPathUtil.getTempDir(), sessionId + ".sirius");
    tryCatchSiriusException(() -> {
      projRef.set(sirius.projects().openProject(sessionId, projectFile.getAbsolutePath(),
          List.of(ProjectInfoOptField.NONE)));
    });
    proj = projRef.get();

    projectSpace = proj != null ? proj : sirius.projects()
        .createProject(sessionId, projectFile.getAbsolutePath(), List.of(ProjectInfoOptField.NONE));
    // project already opened after creating.
    //    sirius.projects().openProject(projectSpace.getProjectId(), projectSpace.getLocation(),
//        List.of(ProjectInfoOptField.NONE));
    sirius.gui().openGui(projectSpace.getProjectId());
  }

  public static void main(String[] args) {
    try (Sirius sirius = new Sirius()) {

      logger.info(sirius.api().jobs().getDefaultJobConfig(true, false, true).toString());

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

    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
    return;
  }

  /**
   * Exports rows to sirius. checks the current project. Only exports rows that are not already
   * contained in the sirius project.
   *
   * @param rows The rows to export
   * @return A mapping of mzmine feature id {@link FeatureListRow#getID()} to sirius unique id
   * {@link AlignedFeature#getAlignedFeatureId()}.
   */
  public Map<Integer, String> exportToSiriusUnique(List<? extends FeatureListRow> rows) {
    checkLogin();
    final List<FeatureImport> features = rows.stream().map(MzmineToSirius::feature).toList();
//    client.projects().openProjectSpace(project.getProjectId(), null, List.of());

    final Map<Integer, String> mzmineIdToSiriusId = sirius.features()
        .getAlignedFeatures(projectSpace.getProjectId(), null, List.of(AlignedFeatureOptField.NONE))
        .stream().collect(Collectors.toMap(
            alignedFeature -> Integer.valueOf(alignedFeature.getExternalFeatureId()),
            AlignedFeature::getAlignedFeatureId));

    final List<FeatureImport> notImportedFeatures = features.stream()
        .filter(f -> mzmineIdToSiriusId.get(Integer.valueOf(f.getExternalFeatureId())) == null)
        .toList();

    var imported = sirius.features()
        .addAlignedFeatures(projectSpace.getProjectId(), notImportedFeatures,
            InstrumentProfile.QTOF, List.of(AlignedFeatureOptField.NONE));
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
    final Map<Integer, String> mzmineIdToSiriusId = sirius.features()
        .getAlignedFeatures(projectSpace.getProjectId(), null, List.of(AlignedFeatureOptField.NONE))
        .stream().collect(Collectors.toMap(
            alignedFeature -> Integer.valueOf(alignedFeature.getExternalFeatureId()),
            AlignedFeature::getAlignedFeatureId));

    return rows.stream().collect(Collectors.toMap(r -> r, r -> mzmineIdToSiriusId.get(r.getID())));
  }

  public Job runFingerId(List<FeatureListRow> rows) {
    checkLogin();
    final Map<Integer, String> idsMap = exportToSiriusUnique(rows);
    final JobSubmission submission = sirius.jobs().getDefaultJobConfig(false, false, true);
    submission.setAlignedFeatureIds(idsMap.values().stream().toList());
    final Job job = sirius.jobs()
        .startJob(projectSpace.getProjectId(), submission, List.of(JobOptField.PROGRESS));
    return job;
  }

  public void importResultsForRows(List<FeatureListRow> rows) {
    checkLogin();
    final Map<FeatureListRow, String> rowToSiriusId = mapFeatureToSiriusId(rows);
    rowToSiriusId.forEach((row, id) -> {
      final List<StructureCandidateFormula> structureCandidates = api().features()
          .getStructureCandidates(projectSpace.getProjectId(), id, List.of());

//      AlignedFeature alignedFeature = api().features()
//          .getAlignedFeature(, , , List.of(AlignedFeatureOptField.TOPANNOTATIONS));
//      alignedFeature.getTopAnnotations().

      final List<CompoundDBAnnotation> siriusAnnotations = structureCandidates.stream()
          .sorted(Comparator.comparingInt(StructureCandidateFormula::getRank))
          .map(SiriusToMzmine::toMzmine).toList();
      final List<CompoundDBAnnotation> annotations = new ArrayList<>(row.getCompoundAnnotations());
      annotations.addAll(siriusAnnotations);
      row.setCompoundAnnotations(annotations);
    });
  }

  public void checkLogin() {
    if (!sirius.account().isLoggedIn()) {
      DesktopService.getDesktop().displayErrorMessageAndThrow(new SiriusNotLoggedInException());
    }
  }

  public ProjectInfo getProject() {
    return projectSpace;
  }

  @Override
  public void close() throws Exception {
    sirius.close();
  }

  public SiriusSDK api() {
    return sirius;
  }

  public void tryCatchSiriusException(Runnable runnable) {
    try {
      runnable.run();
    } catch (RuntimeException e) {
      switch (e) {
        case WebClientResponseException r ->
            logger.log(Level.SEVERE, r.getResponseBodyAsString(), e);
        default -> {
          logger.log(Level.SEVERE, e.getMessage(), e);
          throw e;
        }
      }
    }
  }
}
