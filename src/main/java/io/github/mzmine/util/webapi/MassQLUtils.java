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

package io.github.mzmine.util.webapi;

import io.github.mzmine.modules.tools.massql.MassQLFilter;
import io.github.mzmine.modules.tools.massql.MassQLQuery;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MassQLUtils {

  // use this as an example
  public final static String EXAMPLE_REQUEST = "QUERY scaninfo(MS2DATA) WHERE MS2PROD=660.2:TOLERANCEMZ=0.1";

  private static final Logger logger = Logger.getLogger(MassQLUtils.class.getName());
  // one client, reuse, max 5 seconds wait
  private final static HttpClient httpClient = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMillis(5000)).build();
  private final static String MASSQL_URL = "https://msql.ucsd.edu/parse";
  private final static String QUERY_PARAMETER = "query";

  /**
   * Sends a get request to parse the query by the MassQL web API. Results in a json representation
   * of the query
   *
   * @param query the query to be parsed
   * @return the response by the webserver
   * @throws URISyntaxException
   * @throws IOException
   * @throws InterruptedException
   */
  public static HttpResponse<String> parseQuery(String query)
      throws URISyntaxException, IOException, InterruptedException {
    HttpRequest request = WebAPIUtils.getRequest(MASSQL_URL, QUERY_PARAMETER, query);
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }


  /**
   * Sends a get request to parse the query by the MassQL web API. Results in a json representation
   * of the query
   *
   * @param query the query to be parsed
   * @return the response as json
   * @throws URISyntaxException
   * @throws IOException
   * @throws InterruptedException
   */
  public static JSONObject parseQueryGetJson(String query)
      throws URISyntaxException, IOException, InterruptedException {
    HttpResponse<String> response = parseQuery(query);
    if (WebAPIUtils.isSuccessful(response)) {
      return new JSONObject(response.body());
    }
    return null;
  }


  public static MassQLQuery getFilter(String query) {
    try {
      JSONObject jsonQuery = parseQueryGetJson(query);
      return jsonQuery==null? MassQLQuery.NONE : new MassQLQuery(jsonQuery);
    } catch (URISyntaxException | IOException | InterruptedException e) {
      return MassQLQuery.NONE;
    }
  }
}
