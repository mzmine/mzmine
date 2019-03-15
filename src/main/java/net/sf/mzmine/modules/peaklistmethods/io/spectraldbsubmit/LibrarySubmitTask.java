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
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
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
import net.sf.mzmine.parameters.Parameter;
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
  //
  private final String s = "http://dorresteinappshub.ucsd.edu:5050/";

  private Logger log = Logger.getLogger(this.getClass().getName());
  private Map<LibrarySubmitIonParameters, DataPoint[]> map;
  private int done = 0;
  private final String PASS;
  private final String USER;
  private final boolean saveLocal;
  private final boolean submitGNPS;
  private final File file;

  private boolean exportRT;


  public LibrarySubmitTask(Map<LibrarySubmitIonParameters, DataPoint[]> map) {
    this.map = map;
    // get file, user and pass
    Entry<LibrarySubmitIonParameters, DataPoint[]> e = map.entrySet().iterator().next();
    LibrarySubmitParameters meta = (LibrarySubmitParameters) e.getKey()
        .getParameter(LibrarySubmitIonParameters.META_PARAM).getValue();

    exportRT = meta.getParameter(LibrarySubmitParameters.EXPORT_RT).getValue();
    PASS = meta.getParameter(LibrarySubmitParameters.PASSWORD).getValue();
    USER = meta.getParameter(LibrarySubmitParameters.USERNAME).getValue();
    submitGNPS = meta.getParameter(LibrarySubmitParameters.SUBMIT_GNPS).getValue();
    saveLocal = meta.getParameter(LibrarySubmitParameters.LOCALFILE).getValue();
    if (saveLocal) {
      File tmpfile =
          meta.getParameter(LibrarySubmitParameters.LOCALFILE).getEmbeddedParameter().getValue();
      file = FileAndPathUtil.getRealFilePath(tmpfile, "json");
    } else {
      file = null;
    }
  }

  @Override
  public double getFinishedPercentage() {
    return (done / map.size());
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

        String json = generateJSON(param, dps);
        log.info(json);
        if (saveLocal && file != null)
          writeToLocalFile(file, json);

        if (submitGNPS)
          submitGNPS(json);
      }
      done++;
    }

    setStatus(TaskStatus.FINISHED);
  }


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

        HttpPost httppost =
            new HttpPost("http://dorresteinappshub.ucsd.edu:5050/depostsinglespectrum");
        httppost.setEntity(entity);

        log.info("Submitting GNPS library entry " + httppost.getRequestLine());
        CloseableHttpResponse response = httpclient.execute(httppost);
        try {
          log.info("GNPS submit entry response status: " + response.getStatusLine());
          HttpEntity resEntity = response.getEntity();
          if (resEntity != null) {
            log.info("GNPS submit entry response content length: " + resEntity.getContentLength());

            String body = IOUtils.toString(resEntity.getContent());
            String url =
                "Submission task: https://gnps.ucsd.edu/ProteoSAFe/status.jsp?task=" + body;
            log.log(Level.INFO, url);
            EntityUtils.consume(resEntity);
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
  private void writeToLocalFile(File file, String json) {
    try {
      if (!file.getParentFile().exists())
        file.getParentFile().mkdirs();
    } catch (Exception e) {
      log.log(Level.SEVERE, "Cannot create folder " + file.getParent(), e);
    }

    try {
      CharSink chs = Files.asCharSink(file, Charsets.UTF_8, FileWriteMode.APPEND);
      chs.write(json + "\n");
    } catch (Exception e) {
      log.log(Level.SEVERE, "Cannot create folder " + file.getParent(), e);
    }
  }

  /**
   * Whole JSON entry
   * 
   * @param param
   * @param dps
   * @return
   */
  private String generateJSON(LibrarySubmitIonParameters param, DataPoint[] dps) {
    LibrarySubmitParameters meta = (LibrarySubmitParameters) param
        .getParameter(LibrarySubmitIonParameters.META_PARAM).getValue();

    JsonObjectBuilder json = Json.createObjectBuilder();
    // tag spectrum from mzmine2
    json.add("softwaresource", "mzmine2");
    // ion specific
    json.add("MZ", param.getParameter(LibrarySubmitIonParameters.MZ).getValue());
    json.add("CHARGE", param.getParameter(LibrarySubmitIonParameters.CHARGE).getValue());
    json.add("ADDUCT", param.getParameter(LibrarySubmitIonParameters.ADDUCT).getValue());

    if (exportRT) {
      Double rt =
          meta.getParameter(LibrarySubmitParameters.EXPORT_RT).getEmbeddedParameter().getValue();
      if (rt != null)
        json.add("RT", rt);
    }

    // add data points array
    json.add("peaks", genJSONData(dps));

    // add meta data
    for (Parameter<?> p : meta.getParameters()) {
      if (!p.getName().equals("username") && !p.getName().equals("password")
          && !p.getName().equals(LibrarySubmitParameters.LOCALFILE.getName())
          && !p.getName().equals(LibrarySubmitParameters.SUBMIT_GNPS.getName())
          && !p.getName().equals(LibrarySubmitParameters.EXPORT_RT.getName())) {
        String key = p.getName();
        Object value = p.getValue();
        if (value instanceof Double) {
          if (Double.compare(0d, (Double) value) == 0)
            json.add(key, 0);
          else
            json.add(key, (Double) value);
        } else if (value instanceof Float) {
          if (Float.compare(0f, (Float) value) == 0)
            json.add(key, 0);
          else
            json.add(key, (Float) value);
        } else if (value instanceof Integer)
          json.add(key, (Integer) value);
        else {
          if (value == null || (value instanceof String && ((String) value).isEmpty()))
            value = "N/A";
          json.add(key, value.toString());
        }
      }
    }

    // return Json.createObjectBuilder().add("spectrum", json.build()).build().toString();
    return json.build().toString();
  }

  /**
   * JSON of data points array
   * 
   * @param dps
   * @return
   */
  private JsonArray genJSONData(DataPoint[] dps) {
    JsonArrayBuilder data = Json.createArrayBuilder();
    JsonArrayBuilder signal = Json.createArrayBuilder();
    for (DataPoint dp : dps) {
      // round to five digits. thats more than enough
      signal.add(((int) (dp.getMZ() * 100000)) / 100000.d);
      signal.add(dp.getIntensity());
      data.add(signal.build());
    }
    return data.build();
  }

  @Override
  public String getTaskDescription() {
    return "Exporting and submitting MS/MS library entries";
  }

}
