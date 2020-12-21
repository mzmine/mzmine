/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.test;

import java.io.File;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class TestMultipartSendGNPS {

  public static void main(String[] args) {
    try {
      CloseableHttpClient httpclient = HttpClients.createDefault();
      try {
        System.out.println();
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("featuretool", new StringBody("MZMINE2"));
        entity.addPart("email", new StringBody("robinschmid@wwu.de"));
        entity.addPart("featurequantification",
            new FileBody(new File("D:\\Daten\\UCSD\\V12\\wine_quant.csv")));
        entity.addPart("featurems2", new FileBody(new File("D:\\Daten\\UCSD\\V12\\wine.mgf")));
        entity.addPart("samplemetadata",
            new FileBody(new File("D:\\Daten\\UCSD\\isa_gnps_meta_tab.txt")));
        entity.addPart("additionalpairs",
            new FileBody(new File("D:\\Daten\\UCSD\\V12\\wine_edges_msannotation.csv")));

        HttpPost httppost =
            new HttpPost("http://mingwangbeta.ucsd.edu:5050/uploadanalyzefeaturenetworking");
        httppost.setEntity(entity);

        System.out.println("executing request " + httppost.getRequestLine());
        CloseableHttpResponse response = httpclient.execute(httppost);
        try {
          System.out.println("----------------------------------------");
          System.out.println(response.getStatusLine());
          HttpEntity resEntity = response.getEntity();
          if (resEntity != null) {
            System.out.println("Response content length: " + resEntity.getContentLength());
            System.out.println("Response: " + EntityUtils.toString(resEntity));
          }
          EntityUtils.consume(resEntity);
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          response.close();
        }
      } finally {
        httpclient.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
