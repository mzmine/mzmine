/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util.web;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Simple HTTP requests
 */
public class HttpUtils {

  private static final Logger logger = Logger.getLogger(HttpUtils.class.getName());


  /**
   * Simple GET request without any parameters
   *
   * @return the response body
   * @throws HttpResponseException for any response that is not okay (200-299)
   */
  @NotNull
  public static String get(@NotNull final String url)
      throws IOException, InterruptedException, HttpResponseException {
    return get(url, 3L);
  }

  /**
   * Simple GET request without any parameters
   *
   * @return the response body
   * @throws HttpResponseException for any response that is not okay (200-299)
   */
  @NotNull
  public static String get(@NotNull final String url, long timeout)
      throws IOException, InterruptedException, HttpResponseException {
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET()
        .timeout(Duration.ofSeconds(timeout)).build();
    return retryWithDifferentProxies(request);
  }

  public static boolean isOk(int responseCode) {
    return responseCode / 200 == 1;
  }

  /**
   * @param useDefaultProxy with default proxy or without proxy
   * @return an http client
   */
  public static HttpClient createHttpClient(boolean useDefaultProxy) {
    return HttpClient.newBuilder()
        // redirects are required for zenodo for example: master record ID points to latest
        .followRedirects(Redirect.NORMAL)
        .proxy(useDefaultProxy ? ProxySelector.getDefault() : Builder.NO_PROXY).build();
  }

  /**
   * Try first with default proxy, then without
   *
   * @param request the http request to send
   * @return the body string
   * @throws IOException
   * @throws InterruptedException
   * @throws HttpResponseException for any response that is not okay (200-299)
   */
  @NotNull
  public static String retryWithDifferentProxies(HttpRequest request)
      throws IOException, InterruptedException, HttpResponseException {
    // try with default proxy
    try (var proxyClient = createHttpClient(true)) {
      final HttpResponse<String> response = proxyClient.send(request, BodyHandlers.ofString());
      if (isOk(response.statusCode())) {
        return response.body();
      } else {
        throw new HttpResponseException(new RequestResponse(response));
      }
    } catch (Exception e) {
      // retry without proxy
      try (var proxyClient = createHttpClient(false)) {
        final HttpResponse<String> response = proxyClient.send(request, BodyHandlers.ofString());
        if (isOk(response.statusCode())) {
          logger.warning(
              "Default proxy may be misconfigured. Open mzmine preferences and change proxy settings. Using NO proxy worked for this request");
          return response.body();
        } else {
          throw e;
        }
      } catch (Exception _) {
        logger.fine(
            "Failed HTTP request. Either the web host is unavailable or both the configured proxy and NO proxy failed.");
        throw e; // rethrow first exception
      }
    }
  }


}
