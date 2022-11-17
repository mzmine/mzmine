/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.spectraldbsubmit;

import com.google.common.base.Charsets;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MGFEntryGenerator;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MSPEntryGenerator;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MZmineJsonGenerator;
import io.github.mzmine.modules.io.spectraldbsubmit.param.GnpsLibrarySubmitParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibrarySubmitIonParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibrarySubmitParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.view.MSMSLibrarySubmissionWindow;
import io.github.mzmine.modules.io.spectraldbsubmit.view.ResultsTextPane;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Exports all files needed for GNPS
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class LibrarySubmitTask extends AbstractTask {

  public static final String SOURCE_DESCRIPTION = "MZmine library entry submission";
  public static final String GNPS_LIBRARY_SUBMIT_URL = "http://dorresteinappshub.ucsd.edu:5050/depostsinglespectrum";
  private static final Logger log = Logger.getLogger(LibrarySubmitTask.class.getName());
  private final Map<LibrarySubmitIonParameters, DataPoint[]> map;
  private final String PASS;
  private final String USER;
  private final boolean saveLocal;
  private final boolean submitGNPS;
  private final File fileJson;
  private final File fileMSP;
  private final File fileMGF;
  // window to show results
  private final MSMSLibrarySubmissionWindow window;
  private int done = 0;

  public LibrarySubmitTask(MSMSLibrarySubmissionWindow window,
      Map<LibrarySubmitIonParameters, DataPoint[]> map, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.window = window;
    this.map = map;
    // get file, user and pass
    Entry<LibrarySubmitIonParameters, DataPoint[]> e = map.entrySet().iterator().next();
    LibrarySubmitParameters paramSubmit = (LibrarySubmitParameters) e.getKey()
        .getParameter(LibrarySubmitIonParameters.SUBMIT_PARAM).getValue();

    submitGNPS = paramSubmit.getParameter(LibrarySubmitParameters.SUBMIT_GNPS).getValue();
    GnpsLibrarySubmitParameters gnpsParam = paramSubmit.getParameter(
        LibrarySubmitParameters.SUBMIT_GNPS).getEmbeddedParameters();
    PASS = gnpsParam.getParameter(GnpsLibrarySubmitParameters.pass).getValue();
    USER = gnpsParam.getParameter(GnpsLibrarySubmitParameters.user).getValue();
    saveLocal = paramSubmit.getParameter(LibrarySubmitParameters.LOCALFILE).getValue();
    boolean exportGNPSJsonFile = paramSubmit.getParameter(LibrarySubmitParameters.EXPORT_GNPS_JSON)
        .getValue();
    boolean exportMSPFile = paramSubmit.getParameter(LibrarySubmitParameters.EXPORT_MSP).getValue();
    boolean exportMGFFile = paramSubmit.getParameter(LibrarySubmitParameters.EXPORT_MGF).getValue();
    if (saveLocal) {
      File tmpfile = paramSubmit.getParameter(LibrarySubmitParameters.LOCALFILE)
          .getEmbeddedParameter().getValue();
      fileJson = exportGNPSJsonFile ? FileAndPathUtil.getRealFilePath(tmpfile, "json") : null;
      fileMSP = exportMSPFile ? FileAndPathUtil.getRealFilePath(tmpfile, "msp") : null;
      fileMGF = exportMGFFile ? FileAndPathUtil.getRealFilePath(tmpfile, "mgf") : null;
    } else {
      fileJson = null;
      fileMSP = null;
      fileMGF = null;
    }
  }

  public LibrarySubmitTask(Map<LibrarySubmitIonParameters, DataPoint[]> map,
      @NotNull Instant moduleCallDate) {
    this(null, map, moduleCallDate);
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    for (Entry<LibrarySubmitIonParameters, DataPoint[]> e : map.entrySet()) {
      LibrarySubmitIonParameters param = e.getKey();
      DataPoint[] dps = e.getValue();

      // final check
      // at least 2 data points
      if (dps != null && dps.length > 2) {
        // export / submit json?
        if (fileJson != null || submitGNPS) {
          String json = MZmineJsonGenerator.generateJSON(param, dps);
          log.info(json);
          if (saveLocal && fileJson != null) {
            if (writeToLocalGnpsJsonFile(fileJson, json)) {
              writeResults("GNPS json entry successfully writen" + fileJson.getAbsolutePath(),
                  Result.SUCCED);
            } else {
              writeResults("Error while writing GNPS json entry to " + fileJson.getAbsolutePath(),
                  Result.ERROR);
            }
          }
          if (submitGNPS) {
            submitGNPS(json);
          }
        }
        // export msp?
        if (fileMSP != null) {
          if (writeToLocalMSPFIle(fileMSP, param, dps)) {
            writeResults("MSP entry successfully writen to " + fileMSP.getAbsolutePath(),
                Result.SUCCED);
          } else {
            writeResults("Error while writing msp entry to " + fileMSP.getAbsolutePath(),
                Result.ERROR);
          }
        }
        if (fileMGF != null) {
          if (writeToLocalMGFFIle(fileMGF, param, dps)) {
            writeResults("MSP entry successfully writen to " + fileMGF.getAbsolutePath(),
                Result.SUCCED);
          } else {
            writeResults("Error while writing msp entry to " + fileMGF.getAbsolutePath(),
                Result.ERROR);
          }
        }
      }
      done++;
    }

    setStatus(TaskStatus.FINISHED);
  }

  @Override
  public double getFinishedPercentage() {
    return map.isEmpty() ? 0 : (done / map.size());
  }

  /**
   * Show results in window
   */
  public void writeResults(final String message, final Result type) {
    writeResults(message, type, false);
  }

  public void writeResults(final String url, final String message, final Result type,
      boolean isLink) {
    if (window != null && window.getTxtResults() != null) {
//      final ResultsTextPane pane = window.getTxtResults();
      final ResultsTextPane pane = window.getTxtResults();
      SwingUtilities.invokeLater(() -> {
        switch (type) {
          case ERROR:
            if (isLink) {
              pane.appendErrorLink(message, url);
            } else {
              pane.appendErrorText(message);
            }
            break;
          case INFO:
            if (isLink) {
              pane.appendInfoLink(message, url);
            } else {
              pane.appendInfoText(message);
            }
            break;
          case SUCCED:
            if (isLink) {
              pane.appendSuccedLink(message, url);
            } else {
              pane.appendSuccedText(message);
            }
            break;
        }
      });
    }
  }

  public void writeResults(final String message, final Result type, boolean isLink) {
    writeResults(message, message, type, isLink);
  }

  /**
   * Append entry to msp file
   */
  private boolean writeToLocalMSPFIle(File file, LibrarySubmitIonParameters param,
      DataPoint[] dps) {
    try {
      if (!file.getParentFile().exists()) {
        file.getParentFile().mkdirs();
      }
    } catch (Exception e) {
      log.log(Level.SEVERE, "Cannot create folder " + file.getParent() + " ", e);
    }

    // export json
    try {
      CharSink chs = Files.asCharSink(file, Charsets.UTF_8, FileWriteMode.APPEND);
      String msp = MSPEntryGenerator.createMSPEntry(param, dps);
      chs.write(msp + "\n");
      return true;
    } catch (Exception e) {
      log.log(Level.SEVERE, "Cannot save to msp file " + file.getAbsolutePath(), e);
      return false;
    }
  }

  /**
   * Append entry to mgf file
   */
  private boolean writeToLocalMGFFIle(File file, LibrarySubmitIonParameters param,
      DataPoint[] dps) {
    try {
      if (!file.getParentFile().exists()) {
        file.getParentFile().mkdirs();
      }
    } catch (Exception e) {
      log.log(Level.SEVERE, "Cannot create folder " + file.getParent() + " ", e);
    }

    // export json
    try {
      CharSink chs = Files.asCharSink(file, Charsets.UTF_8, FileWriteMode.APPEND);
      String entry = MGFEntryGenerator.createMGFEntry(param, dps);
      chs.write(entry + "\n");
      return true;
    } catch (Exception e) {
      log.log(Level.SEVERE, "Cannot save to mgf file " + file.getAbsolutePath(), e);
      return false;
    }
  }

  /**
   * Submit json library entry to GNPS webserver
   *
   * @param json
   */
  private void submitGNPS(String json) {
    try {
      CloseableHttpClient httpclient = HttpClients.createDefault();
      try {
        MultipartEntity entity = new MultipartEntity();

        // ######################################################
        // NEEDED
        // user pass and json entry
        //
        entity.addPart("username", new StringBody(USER));
        entity.addPart("password", new StringBody(PASS));
        entity.addPart("spectrum", new StringBody(json));
        // job description is not entry description
        entity.addPart("description", new StringBody(SOURCE_DESCRIPTION));

        HttpPost httppost = new HttpPost(GNPS_LIBRARY_SUBMIT_URL);
        httppost.setEntity(entity);

        log.info("Submitting GNPS library entry " + httppost.getRequestLine());
        CloseableHttpResponse response = httpclient.execute(httppost);
        try {
          writeResults("GNPS submit entry response status: " + response.getStatusLine(),
              Result.INFO);
          log.info("GNPS submit entry response status: " + response.getStatusLine());
          HttpEntity resEntity = response.getEntity();
          if (resEntity != null) {
            log.info("GNPS submit entry response content length: " + resEntity.getContentLength());
            writeResults(
                "GNPS submit entry response content length: " + resEntity.getContentLength(),
                Result.SUCCED);

            String body = IOUtils.toString(resEntity.getContent());
            String url = "https://gnps.ucsd.edu/ProteoSAFe/status.jsp?task=" + body;
            log.log(Level.INFO, "Submission task: " + url);
            writeResults(url, Result.SUCCED, true);
            EntityUtils.consume(resEntity);
          } else {
            log.warning("Not submitted to GNPS:\n" + json);
            writeResults("Not submitted to GNPS\n" + json, Result.ERROR);
          }
        } finally {
          response.close();
        }
      } finally {
        httpclient.close();
      }
    } catch (IOException e) {
      log.log(Level.SEVERE, "Error while submitting GNPS job", e);
      throw new MSDKRuntimeException(e);
    }
  }

  /**
   * Append json to file
   *
   * @param file
   * @param json
   */
  private boolean writeToLocalGnpsJsonFile(File file, String json) {
    try {
      if (!file.getParentFile().exists()) {
        file.getParentFile().mkdirs();
      }
    } catch (Exception e) {
      log.log(Level.SEVERE, "Cannot create folder " + file.getParent(), e);
    }

    // export json
    try {
      CharSink chs = Files.asCharSink(file, Charsets.UTF_8, FileWriteMode.APPEND);
      chs.write(json + "\n");
      return true;
    } catch (Exception e) {
      log.log(Level.SEVERE, "Cannot create or write to file " + file.getAbsolutePath(), e);
      return false;
    }
  }

  private enum Result {
    ERROR, SUCCED, INFO
  }

  @Override
  public String getTaskDescription() {
    return "Exporting and submitting MS/MS library entries";
  }

}
