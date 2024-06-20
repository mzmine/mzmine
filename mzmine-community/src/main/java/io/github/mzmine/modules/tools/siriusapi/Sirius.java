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
import de.unijena.bioinf.ms.nightsky.sdk.model.AlignedFeatureOptField;
import de.unijena.bioinf.ms.nightsky.sdk.model.FeatureImport;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobSubmission;
import de.unijena.bioinf.ms.nightsky.sdk.model.ProjectInfo;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

public class Sirius {

  private static final Logger logger = Logger.getLogger(Sirius.class.getName());


  private static NightSkyClient client;

  private static Sirius instance;

  // one session id for this mzmine session
  private static final String sessionId = FileAndPathUtil.safePathEncode(
      "mzmine_%s".formatted(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)));

  private static ProjectInfo project;

  private Sirius() {
    client = launchOrGetClient();
    instance = this;
    if (project == null) {
      project = client.projects().getProjectSpaces().getFirst();
    }
  }

  public static synchronized Sirius getInstance() {
    if (instance == null) {
      instance = new Sirius();
    }
    return instance;
  }

  public static synchronized NightSkyClient launchOrGetClient() throws RuntimeException {
    final File siriusDir = new File(FileUtils.getUserDirectory(), ".sirius-6.0");
    final File portFile = new File(siriusDir, "sirius.port");
    final File pidFile = new File(siriusDir, "sirius.pid");

    final Integer port = getPort(portFile);

    if (port == null) {
      logger.info("Sirius not running yet, launching.");
      return new NightSkyClient();
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

  public NightSkyClient api() {
    return client;
  }

  public void exportToSirius(List<? extends FeatureListRow> rows) {
    checkLogin();
    final List<FeatureImport> features = rows.stream().map(MzmineToSirius::feature).toList();
//    client.projects().openProjectSpace(project.getProjectId(), null, List.of());
    var imported = client.features()
        .addAlignedFeatures(project.getProjectId(), features, List.of(AlignedFeatureOptField.NONE));
//    imported.stream().collect(Collectors.toMap(f -> f.getAlignedFeatureId(), f -> ))
    logger.info(() -> "Added features " + imported.stream()
        .map(f -> f.getName() + "->" + f.getAlignedFeatureId()).collect(Collectors.joining(";")));
  }

  public void runFingerId(List<FeatureImport> features) {
    checkLogin();
    client.projects().openProjectSpace(project.getProjectId(), null, List.of());
    client.features().addAlignedFeatures(project.getProjectId(), features, List.of());

    final JobSubmission submission = client.jobs().getDefaultJobConfig(true);
    submission.setAlignedFeatureIds(features.stream().map(FeatureImport::getFeatureId).toList());

//    client.jobs().startJob(project.getProjectId(), )
  }

  public static void main(String[] args) {
    final Sirius sirius = getInstance();

    final List<ProjectInfo> projectSpaces = sirius.client.projects().getProjectSpaces();
    projectSpaces.forEach(p -> logger.info(p.toString()));
    final ProjectInfo project = projectSpaces.getFirst();

    logger.info(client.jobs().getDefaultJobConfig(true).toString());

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

  public void checkLogin() {
    if (!client.account().isLoggedIn()) {
      DesktopService.getDesktop().displayErrorMessageAndThrow(new SiriusNotLoggedInException());
    }
  }
}
