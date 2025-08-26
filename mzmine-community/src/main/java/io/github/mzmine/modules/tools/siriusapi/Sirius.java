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

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.modules.tools.siriusapi.modules.fingerid.SiriusFingerIdModule;
import io.github.mzmine.modules.tools.siriusapi.modules.fingerid.SiriusFingerIdParameters;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.sirius.ms.sdk.SiriusSDK;
import io.sirius.ms.sdk.SiriusSDK.ShutdownMode;
import io.sirius.ms.sdk.SiriusSDKUtils;
import io.sirius.ms.sdk.model.AlignedFeature;
import io.sirius.ms.sdk.model.AllowedFeatures;
import io.sirius.ms.sdk.model.ConfidenceMode;
import io.sirius.ms.sdk.model.GuiInfo;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.JobSubmission;
import io.sirius.ms.sdk.model.ProjectInfo;
import io.sirius.ms.sdk.model.ProjectInfoOptField;
import io.sirius.ms.sdk.model.SearchableDatabase;
import io.sirius.ms.sdk.model.StructureCandidateFormula;
import io.sirius.ms.sdk.model.StructureDbSearch;
import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class Sirius implements AutoCloseable {

  /**
   * default custom database for mzmine compounds exported to sirius.
   */
  public static final String mzmineCustomDbId = "mzmine_default_export";
  private static final Logger logger = Logger.getLogger(Sirius.class.getName());
  // one session id for this mzmine session
  private static final LocalDateTime creationDate = LocalDateTime.now();
  /**
   * in case a temporary project is created, we use this ID.
   */
  private static final String sessionId = FileAndPathUtil.safePathEncode(
      "mzmine_%s".formatted(creationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))));
  @NotNull
  private final SiriusSDK sirius;
  @NotNull
  private final ProjectInfo projectSpace;

  /**
   * Default constructor to initialise a temporary project.
   */
  public Sirius() throws Exception {
    final File projectFile = new File(FileAndPathUtil.getTempDir(), sessionId + ".sirius");
    this(projectFile);
  }

  /**
   * @param project project file to open
   * @throws Exception
   */
  public Sirius(File project) throws Exception {
    sirius = SiriusSDK.startAndConnectLocally(ShutdownMode.NEVER, true);

    int tries = 0;
    final int maxTries = 10;
    while (!SiriusSDKUtils.restHealthCheck(sirius.getApiClient()) && tries < maxTries) {
      tries++;
      Thread.sleep(200);
      logger.finest("Waiting for Sirius API startup. Try %d/%d".formatted(tries, maxTries));
    }

    if (!SiriusSDKUtils.restHealthCheck(sirius.getApiClient())) {
      throw new RuntimeException("Could not connect to Sirius API.");
    }

    checkLogin();
    logger.finest(sirius.infos().getInfo(null, null).toString());
    projectSpace = activateSiriusProject(project);
    showGuiForProject();
  }

  public static String getDefaultSessionId() {
    return sessionId;
  }

  public static @NotNull List<CompoundDBAnnotation> getSiriusAnnotations(Sirius sirius,
      String siriusFeatureId, FeatureListRow row) {

    final AlignedFeature alignedFeature = SiriusToMzmine.getSiriusFeatureOrThrow(sirius,
        siriusFeatureId, row);

    final List<StructureCandidateFormula> structureCandidates = sirius.api().features()
        .getStructureCandidates(sirius.projectSpace.getProjectId(),
            alignedFeature.getAlignedFeatureId(), List.of()).stream()
        .sorted(Comparator.comparingInt(s -> s.getRank() != null ? s.getRank() : 100)).limit(10)
        .toList();

    final List<CompoundDBAnnotation> siriusAnnotations = structureCandidates.stream()
        .sorted(Comparator.comparingInt(StructureCandidateFormula::getRank)).map(
            s -> SiriusToMzmine.structureCandidateToMzmine(s, sirius.sirius, sirius.projectSpace,
                siriusFeatureId)).filter(Objects::nonNull).toList();
    final List<CompoundDBAnnotation> annotations = new ArrayList<>();
    annotations.addAll(siriusAnnotations);
    return annotations;
  }

  private void showGuiForProject() {
    for (GuiInfo gui : sirius.gui().getGuis()) {
      if (gui.getProjectId().equals(projectSpace.getProjectId())) {
        return;
      }
    }
    sirius.gui().openGui(projectSpace.getProjectId());
  }

  /**
   * Utility methods to load, open, or create a project from the given file.
   */
  private @NotNull ProjectInfo activateSiriusProject(@NotNull File projectFile) {
    ProjectInfo projectSpace;

    final String projectName = FileAndPathUtil.eraseFormat(projectFile.getName());

    try {
      projectSpace = sirius.projects().getProject(projectName, List.of(ProjectInfoOptField.NONE));
      if (projectSpace != null) {
        return projectSpace;
      }
    } catch (WebClientResponseException e) {
      logger.log(Level.FINEST, "Sirius project not opened yet, trying to open.");
    }

    try {
      projectSpace = sirius.projects().openProject(projectName, projectFile.getAbsolutePath(),
          List.of(ProjectInfoOptField.NONE));
      if (projectSpace != null) {
        return projectSpace;
      }
    } catch (WebClientResponseException e) {
      logger.finest(() -> "Sirius project not created yet, trying to create.");
    }

    try {
      projectSpace = sirius.projects().createProject(projectName, projectFile.getAbsolutePath(),
          List.of(ProjectInfoOptField.NONE));
      if (projectSpace != null) {
        return projectSpace;
      }
    } catch (WebClientResponseException e) {
      logger.log(Level.SEVERE,
          "Error while creating sirius project: " + e.getResponseBodyAsString(), e);
    }

    throw new RuntimeException("Could not create or open project space for this mzmine session.");
  }

  public Job createFingerIdJob(List<FeatureListRow> rows) {
    checkLogin();
    final Map<Integer, String> idsMap = MzmineToSirius.exportToSiriusUnique(this, rows);
    final JobSubmission submission = sirius.jobs().getDefaultJobConfig(false, false, true);

    // polarity for fallback adducts
    // todo: set in a sirius preferences or so
    final PolarityType polarity = rows.stream()
        .map(r -> FeatureUtils.extractBestPolarity(r).orElse(null)).filter(Objects::nonNull)
        .findFirst().orElse(PolarityType.POSITIVE);
    switch (polarity) {
      case NEGATIVE -> submission.setFallbackAdducts(List.of("[M-H]-"));
      default -> submission.setFallbackAdducts(List.of("[M+H]+", "[M+NH4]+", "[M+Na]+"));
    }

    submission.setAlignedFeatureIds(idsMap.values().stream().toList());
    final Job job = sirius.jobs()
        .startJob(projectSpace.getProjectId(), submission, List.of(JobOptField.PROGRESS));
    return job;
  }

  /**
   * checks if the user is logged in (=has valid license) and if his license allows api usage.
   */
  public void checkLogin() {
    if (!sirius.account().isLoggedIn()) {
      DesktopService.getDesktop().displayErrorMessageAndThrow(new SiriusNotLoggedInException());
    }

    final String activeSubscriptionId = sirius.account().getAccountInfo(true)
        .getActiveSubscriptionId();
    if (activeSubscriptionId == null) {
      DesktopService.getDesktop().displayErrorMessageAndThrow(new SiriusNotLoggedInException());
    }

    if (!sirius.account().getAccountInfo(true).getSubscriptions().stream().filter(s -> {
      String sid = s.getSid();
      if (sid != null) {
        return sid.equals(activeSubscriptionId);
      }
      return false;
    }).findFirst().map(s -> {
      AllowedFeatures allowedFeatures = s.getAllowedFeatures();
      if (allowedFeatures == null) {
        return false;
      }
      return allowedFeatures.isApi();
    }).orElse(false)) {
      DesktopService.getDesktop().displayErrorMessageAndThrow(new SiriusNotLoggedInException());
    }
  }

  @NotNull
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


  /**
   * Ranks the current {@link CompoundDBAnnotation}s by exporting to a custom database and using only that
   * database for possible structures.
   *
   * @param row
   */
  public void rankCurrentCompoundAnnotations(@NotNull final FeatureListRow row) {

    List<CompoundDBAnnotation> annotations = row.getCompoundAnnotations();

    if (annotations.isEmpty()) {
      return;
    }

    // inchi may be there but not the smiles
    annotations.forEach(CompoundDBAnnotation::enrichMetadata);
    final SearchableDatabase customDatabase = MzmineToSirius.toCustomDatabase(annotations, this);

    final String siriusId = MzmineToSirius.exportToSiriusUnique(this, List.of(row))
        .get(row.getID());
    JobSubmission config = sirius.jobs().getDefaultJobConfig(false, false, true);
    StructureDbSearch structureDbParams = config.getStructureDbSearchParams();
    structureDbParams.setStructureSearchDBs(List.of(customDatabase.getDatabaseId()));
    structureDbParams.setExpansiveSearchConfidenceMode(
        ConfidenceMode.OFF); // no fallback to pubchem for this application

    final PolarityType polarity = FeatureUtils.extractBestPolarity(row)
        .orElse(PolarityType.POSITIVE);

    switch (polarity) {
      case NEGATIVE -> config.setFallbackAdducts(List.of("[M-H]-"));
      default -> config.setFallbackAdducts(List.of("[M+H]+", "[M+NH4]+", "[M+Na]+"));
    }

    config.setAlignedFeatureIds(List.of(siriusId));
    final Job job = sirius.jobs()
        .startJob(projectSpace.getProjectId(), config, List.of(JobOptField.PROGRESS));
    JobWaiterTask task = new JobWaiterTask(SiriusFingerIdModule.class, Instant.now(),
        SiriusFingerIdParameters.of(List.of(row)), () -> sirius.jobs()
        .getJob(projectSpace.getProjectId(), job.getId(), List.of(JobOptField.PROGRESS)),
        () -> SiriusToMzmine.importResultsForRows(this, List.of(row)));

    TaskService.getController().addTask(task);
  }
}
