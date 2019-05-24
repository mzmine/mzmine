/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
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

package net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit;

import java.io.File;
import java.io.IOException;
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
import com.google.common.base.Charsets;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.formats.GnpsJsonGenerator;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.formats.MSPEntryGenerator;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.GnpsLibrarySubmitParameters;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibrarySubmitIonParameters;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibrarySubmitParameters;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.view.MSMSLibrarySubmissionWindow;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.view.ResultsTextPane;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.files.FileAndPathUtil;

/**
 * Exports all files needed for GNPS
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class LibrarySubmitTask extends AbstractTask {

  private enum Result {
    ERROR, SUCCED, INFO;
  }

  //
  public static final String GNPS_LIBRARY_SUBMIT_URL =
      "http://dorresteinappshub.ucsd.edu:5050/depostsinglespectrum";

  private Logger log = Logger.getLogger(this.getClass().getName());
  private Map<LibrarySubmitIonParameters, DataPoint[]> map;
  private int done = 0;
  private final String PASS;
  private final String USER;
  private final boolean saveLocal;
  private final boolean submitGNPS;
  private final boolean exportGNPSJsonFile;
  private final boolean exportMSPFile;
  private final File fileJson;
  private final File fileMSP;

  // window to show results
  private final MSMSLibrarySubmissionWindow window;

  public LibrarySubmitTask(MSMSLibrarySubmissionWindow window,
      Map<LibrarySubmitIonParameters, DataPoint[]> map) {
    this.window = window;
    this.map = map;
    // get file, user and pass
    Entry<LibrarySubmitIonParameters, DataPoint[]> e = map.entrySet().iterator().next();
    LibrarySubmitParameters paramSubmit = (LibrarySubmitParameters) e.getKey()
        .getParameter(LibrarySubmitIonParameters.SUBMIT_PARAM).getValue();

    submitGNPS = paramSubmit.getParameter(LibrarySubmitParameters.SUBMIT_GNPS).getValue();
    GnpsLibrarySubmitParameters gnpsParam =
        paramSubmit.getParameter(LibrarySubmitParameters.SUBMIT_GNPS).getEmbeddedParameters();
    PASS = gnpsParam.getParameter(GnpsLibrarySubmitParameters.pass).getValue();
    USER = gnpsParam.getParameter(GnpsLibrarySubmitParameters.user).getValue();
    saveLocal = paramSubmit.getParameter(LibrarySubmitParameters.LOCALFILE).getValue();
    exportGNPSJsonFile =
        paramSubmit.getParameter(LibrarySubmitParameters.EXPORT_GNPS_JSON).getValue();
    exportMSPFile = paramSubmit.getParameter(LibrarySubmitParameters.EXPORT_MSP).getValue();
    if (saveLocal) {
      File tmpfile = paramSubmit.getParameter(LibrarySubmitParameters.LOCALFILE)
          .getEmbeddedParameter().getValue();
      fileJson = exportGNPSJsonFile ? FileAndPathUtil.getRealFilePath(tmpfile, "json") : null;
      fileMSP = exportMSPFile ? FileAndPathUtil.getRealFilePath(tmpfile, "msp") : null;
    } else {
      fileJson = null;
      fileMSP = null;
    }
  }

  public LibrarySubmitTask(Map<LibrarySubmitIonParameters, DataPoint[]> map) {
    this(null, map);
  }



  @Override
  public double getFinishedPercentage() {
    return map.isEmpty() ? 0 : (done / map.size());
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
          String json = GnpsJsonGenerator.generateJSON(param, dps);
          log.info(json);
          if (saveLocal && fileJson != null) {
            if (writeToLocalGnpsJsonFile(fileJson, json))
              writeResults("GNPS json entry successfully writen" + fileJson.getAbsolutePath(),
                  Result.SUCCED);
            else
              writeResults("Error while writing GNPS json entry to " + fileJson.getAbsolutePath(),
                  Result.ERROR);
          }
          if (submitGNPS)
            submitGNPS(json);
        }
        // export msp?
        if (fileMSP != null) {
          if (writeToLocalMSPFIle(fileMSP, param, dps))
            writeResults("MSP entry successfully writen to " + fileMSP.getAbsolutePath(),
                Result.SUCCED);
          else
            writeResults("Error while writing msp entry to " + fileMSP.getAbsolutePath(),
                Result.ERROR);
        }
      }
      done++;
    }

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Show results in window
   * 
   * @param message
   * @param type
   * @param isLink
   */
  public void writeResults(final String message, final Result type) {
    writeResults(message, type, false);
  }

  public void writeResults(final String message, final Result type, boolean isLink) {
    writeResults(message, message, type, isLink);
  }

  public void writeResults(final String url, final String message, final Result type,
      boolean isLink) {
    if (window != null && window.getTxtResults() != null) {
      final ResultsTextPane pane = window.getTxtResults();
      SwingUtilities.invokeLater(() -> {
        switch (type) {
          case ERROR:
            if (isLink)
              pane.appendErrorLink(message, url);
            else
              pane.appendErrorText(message);
            break;
          case INFO:
            if (isLink)
              pane.appendInfoLink(message, url);
            else
              pane.appendInfoText(message);
            break;
          case SUCCED:
            if (isLink)
              pane.appendSuccedLink(message, url);
            else
              pane.appendSuccedText(message);
            break;
        }
      });
    }
  }


  /**
   * Append entry to msp file
   * 
   * @param file
   * @param json
   */
  private boolean writeToLocalMSPFIle(File file, LibrarySubmitIonParameters param,
      DataPoint[] dps) {
    try {
      if (!file.getParentFile().exists())
        file.getParentFile().mkdirs();
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
        entity.addPart("username", new StringBody(USER));
        entity.addPart("password", new StringBody(PASS));
        entity.addPart("spectrum", new StringBody(json));

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
      if (!file.getParentFile().exists())
        file.getParentFile().mkdirs();
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


  @Override
  public String getTaskDescription() {
    return "Exporting and submitting MS/MS library entries";
  }

}
