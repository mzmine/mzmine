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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Uses Java 11 HttpRequest and HttpClient to send requests (get or post)
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class WebAPIUtils {

  public static HttpRequest getRequest(URI uri) {
    return HttpRequest.newBuilder()
        .GET()
        .uri(uri)
        .setHeader("User-Agent", "MZmine")
        .build();
  }

  public static HttpRequest getRequest(String url) {
    return getRequest(URI.create(url));
  }

  public static HttpRequest getRequest(String url, String param, String value)
      throws URISyntaxException {
    final URI uri = new URIBuilder(url).addParameter(param, value).build();
    return getRequest(uri);
  }


  /**
   * Status code is 200 - 299
   *
   * @param response a response from a HttpRequest
   * @return true if status code signals success
   */
  public static boolean isSuccessful(@NotNull HttpResponse response) {
    // all codes from 200 - 299 mean success
    return response.statusCode() / 100 == 2;
  }
}
