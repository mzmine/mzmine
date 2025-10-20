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

import static io.github.mzmine.util.StringUtils.inQuotes;

import com.google.common.io.Files;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.dialogs.NotificationService;
import io.github.mzmine.javafx.dialogs.NotificationService.NotificationType;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.sirius.ms.sdk.SiriusSDK;
import io.sirius.ms.sdk.SiriusSDK.ShutdownMode;
import io.sirius.ms.sdk.SiriusSDKUtils;
import io.sirius.ms.sdk.model.AllowedFeatures;
import io.sirius.ms.sdk.model.GuiInfo;
import io.sirius.ms.sdk.model.ProjectInfo;
import io.sirius.ms.sdk.model.ProjectInfoOptField;
import java.io.IOException;
import javafx.scene.control.Alert.AlertType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  private static final CloseableReentrantReadWriteLock projectCreationLock = new CloseableReentrantReadWriteLock();
  private static volatile @Nullable File sessionSpecificTempProject = null;

  @NotNull
  private final SiriusSDK sirius;
  @NotNull
  private final ProjectInfo projectSpace;

  /**
   * Default constructor to initialise a temporary project. This is a time-consuming operation.
   * Don't run on the GUI.
   */
  public Sirius() throws Exception {

    if (sessionSpecificTempProject == null) {
      // only set once and use the same file for default projects.
      try (var _ = projectCreationLock.lockWrite()) {
        if (sessionSpecificTempProject == null) {
          sessionSpecificTempProject = new File(FileAndPathUtil.getTempDir(),
              sessionId + ".sirius");
        }
      }
    }

    if (sessionSpecificTempProject == null) {
      throw new IllegalStateException(
          "Failed to initialize session specific temp project for SIRIUS.");
    }
    this(sessionSpecificTempProject);
  }

  /**
   * This is a time-consuming operation. Don't run on the GUI.
   *
   * @param project project file to open.
   * @throws Exception
   */
  public Sirius(File project) throws Exception {

    try {
      // try to connect to running instance
      final SiriusSDK alreadyRunning = SiriusSDK.findAndConnectLocally(ShutdownMode.NEVER, true);
      if (alreadyRunning != null) {
        sirius = alreadyRunning;
      } else {
        NotificationService.show(NotificationType.INFO, "Starting SIRIUS",
            "Trying to start Sirius. This may take a moment.");

        sirius = SiriusSDK.startAndConnectLocally(ShutdownMode.NEVER, true);
      }
    } catch (Exception e) {
      // this catches situations where SIRIUS is not available or has not been installed properly.
      DialogLoggerUtil.showDialog(AlertType.ERROR, "Error when Starting SIRIUS", """
          No SIRIUS installation found.
          Make sure SIRIUS executable is in your PATH or that SIRIUS_EXE environment variable has been set.
          Re-installation of SIRIUS should fix this issue.
          
          SIRIUS error message:
          
          """ + e.getMessage());
      throw e;
    }

    try {
      int tries = 0;
      final int maxTries = 10;
      while (!SiriusSDKUtils.restHealthCheck(sirius.getApiClient()) && tries < maxTries) {
        tries++;
        Thread.sleep(50);
        logger.finest("Waiting for SIRIUS API startup. Try %d/%d".formatted(tries, maxTries));
      }

      if (!SiriusSDKUtils.restHealthCheck(sirius.getApiClient())) {
        throw new RuntimeException("Could not connect to SIRIUS API.");
      }

      checkLogin();
      logger.finest(sirius.infos().getInfo(null, null).toString());
      projectSpace = activateSiriusProject(project);
      showGuiForProject();
    } catch (Exception e) {
      // this catches, e.g., situations where the user's license does not allow API usage.
      DesktopService.getDesktop().displayMessage("Error when connecting to SIRIUS",
          "SIRIUS error message: " + e.getMessage(), null);
      throw e;
    }
  }

  public static String getDefaultSessionId() {
    return sessionId;
  }

  /**
   * @return The file or null if no project was initialized.
   */
  public static @Nullable File getSessionSpecificTempProject() {
    try (var _ = projectCreationLock.lockRead()) {
      return sessionSpecificTempProject;
    }
  }

  /**
   * Copies the current state of the default project to the specific file. Does not change the
   * default project. Any further changes will have to be applied to the "to" file will have to call
   * this method again to copy the respective file.
   *
   * @param to Full file path including file name and suffix.
   */
  public static void copyDefaultProject(@NotNull final File to) {
    if (getSessionSpecificTempProject() == null) {
      return;
    }

    final SiriusSDK alreadyRunning = SiriusSDK.findAndConnectLocally(ShutdownMode.AUTO, true);
    if(alreadyRunning != null) {
      try {
        final String projectId = getProjectIdFromFile(Sirius.getSessionSpecificTempProject());
        alreadyRunning.projects().closeProject(projectId, false);
      } catch (Exception e) {
        logger.log(Level.INFO, e.getMessage(), e);
      }
    }

    try {
      Files.copy(Sirius.getSessionSpecificTempProject(), to);
    } catch (Exception e) {
      logger.log(Level.INFO, e.getMessage(), e);
      DialogLoggerUtil.showDialog(AlertType.ERROR, "Cannot save SIRIUS project",
          "An error occurred while copying the SIRIUS project %s to %s. Please save it manually.".formatted(
              inQuotes(Sirius.getSessionSpecificTempProject().getAbsolutePath()),
              inQuotes(to.getAbsolutePath())));
    }
  }

  private void showGuiForProject() {
    if (DesktopService.isHeadLess()) {
      return;
    }
    for (GuiInfo gui : sirius.gui().getGuis()) {
      if (Objects.equals(gui.getProjectId(), projectSpace.getProjectId())) {
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

    final String projectName = getProjectIdFromFile(projectFile);

    try {
      projectSpace = sirius.projects().getProject(projectName, List.of(ProjectInfoOptField.NONE));
      if (projectSpace != null) {
        return projectSpace;
      }
    } catch (WebClientResponseException e) {
      logger.log(Level.FINEST, "SIRIUS project not opened yet, trying to open.");
    }

    try {
      projectSpace = sirius.projects().openProject(projectName, projectFile.getAbsolutePath(),
          List.of(ProjectInfoOptField.NONE));
      if (projectSpace != null) {
        return projectSpace;
      }
    } catch (WebClientResponseException e) {
      logger.finest(() -> "SIRIUS project not created yet, trying to create.");
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

  private static @NotNull String getProjectIdFromFile(@NotNull File projectFile) {
    return FileAndPathUtil.eraseFormat(projectFile.getName());
  }

  /**
   * checks if the user is logged in (=has valid license) and if his license allows api usage.
   */
  public void checkLogin() {
    if (!sirius.account().isLoggedIn()) {
      sirius.shutdown();
      SiriusSDKUtils.restShutdown(sirius.getApiClient());
      DesktopService.getDesktop().displayErrorMessageAndThrow(new SiriusNotLoggedInException());
    }

    final String activeSubscriptionId = sirius.account().getAccountInfo(true)
        .getActiveSubscriptionId();
    if (activeSubscriptionId == null) {
      SiriusSDKUtils.restShutdown(sirius.getApiClient());
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
      if (!Boolean.TRUE.equals(allowedFeatures.isApi())) {
        throw new SiriusLicenseHasNoApiException();
      }
      return true;
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

  @NotNull
  public SiriusSDK api() {
    return sirius;
  }
}
