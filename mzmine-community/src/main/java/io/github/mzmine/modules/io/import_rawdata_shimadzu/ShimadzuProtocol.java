/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.import_rawdata_shimadzu;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mzmine.util.io.JsonUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;

/**
 * Wire-protocol I/O for the ShimadzuBridge child process. The bridge speaks
 * newline-delimited UTF-8 JSON request lines on stdin and JSON header line +
 * optional little-endian {@code float64} binary blobs on stdout.
 */
final class ShimadzuProtocol {

  private final InputStream in;
  private final OutputStream out;

  ShimadzuProtocol(@NotNull InputStream in, @NotNull OutputStream out) {
    this.in = in;
    this.out = out;
  }

  /**
   * Send a JSON request line. The trailing {@code \n} is appended here so call
   * sites don't have to worry about it.
   */
  void send(@NotNull String json) throws IOException {
    out.write(json.getBytes(StandardCharsets.UTF_8));
    out.write('\n');
    out.flush();
  }

  /**
   * Read one newline-terminated JSON response header. Returns the parsed node.
   * Throws on EOF or malformed JSON.
   */
  @NotNull JsonNode readHeader() throws IOException {
    final ByteArrayOutputStream buf = new ByteArrayOutputStream(256);
    int b;
    while ((b = in.read()) != -1) {
      if (b == '\n') break;
      if (b == '\r') continue;
      buf.write(b);
    }
    if (buf.size() == 0 && b == -1) {
      throw new IOException("ShimadzuBridge closed stdout without responding");
    }
    final String line = buf.toString(StandardCharsets.UTF_8);
    return JsonUtils.MAPPER.readTree(line);
  }

  /**
   * Read exactly {@code count} little-endian float64 values from the bridge's
   * stdout into a fresh {@code double[]}.
   */
  double[] readDoubles(int count) throws IOException {
    if (count == 0) return new double[0];
    final byte[] bytes = readFully(count * 8);
    final double[] out = new double[count];
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer().get(out);
    return out;
  }

  private byte[] readFully(int n) throws IOException {
    final byte[] buf = new byte[n];
    int off = 0;
    while (off < n) {
      int r = in.read(buf, off, n - off);
      if (r < 0) throw new IOException("EOF while reading " + n + " bytes from ShimadzuBridge");
      off += r;
    }
    return buf;
  }

  // -------------------------------------------------------------------------
  // Request builders
  // -------------------------------------------------------------------------

  static String openRequest(String absolutePath) {
    return "{\"op\":\"open\",\"path\":" + jsonString(absolutePath) + "}";
  }

  static String scanRangeRequest(int from, int to, boolean profile) {
    return "{\"op\":\"scanRange\",\"from\":" + from + ",\"to\":" + to + ",\"profile\":" + profile
        + "}";
  }

  static String metadataRequest() {
    return "{\"op\":\"metadata\"}";
  }

  static String closeRequest() {
    return "{\"op\":\"close\"}";
  }

  static String shutdownRequest() {
    return "{\"op\":\"shutdown\"}";
  }

  private static String jsonString(String s) {
    // Jackson is the right tool, but for a single string this hand-written
    // path avoids constructing a whole ObjectNode per request.
    final StringBuilder sb = new StringBuilder(s.length() + 4);
    sb.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\', '"' -> {
          sb.append('\\');
          sb.append(c);
        }
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
          else sb.append(c);
        }
      }
    }
    sb.append('"');
    return sb.toString();
  }
}
