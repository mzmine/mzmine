/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Detects the character encoding of text files (csv, tsv, txt, ...) and opens readers that never
 * fail on unexpected bytes. Users export tabular files from all kinds of tools and locales: Excel
 * writes windows-1252 for "CSV (comma delimited)", UTF-8 with a byte order mark for "CSV UTF-8" and
 * UTF-16LE for "Unicode Text", while most other tools write plain UTF-8. Reading all of those with
 * {@link Files#newBufferedReader(Path)} (strict UTF-8) throws
 * {@link java.nio.charset.MalformedInputException} for anything that is not UTF-8.
 * <p>
 * Detection order:
 * <ol>
 *   <li>byte order mark (UTF-8, UTF-16, UTF-32) - the mark is stripped from the reader</li>
 *   <li>UTF-16 without a byte order mark, recognised by the NUL bytes of ASCII content</li>
 *   <li>UTF-8, if a sample of the file decodes without error (this also covers pure ASCII)</li>
 *   <li>{@link #LEGACY_FALLBACK} (windows-1252) for anything else</li>
 * </ol>
 * Readers created here decode with {@link CodingErrorAction#REPLACE}, so bytes that do not match
 * the detected charset become the replacement character instead of aborting the whole import.
 */
public final class CharsetUtils {

  /**
   * Single byte fallback encoding. windows-1252 is a superset of ISO-8859-1 in the printable range
   * and the default of Excel on windows systems.
   */
  public static final Charset LEGACY_FALLBACK = charsetOrElse("windows-1252",
      StandardCharsets.ISO_8859_1);

  /**
   * Number of bytes read from the start of a file to detect its charset. Bytes after this sample
   * cannot change the detected charset, they are decoded with replacement characters if they do not
   * match.
   */
  public static final int DETECTION_SAMPLE_BYTES = 1 << 18; // 256 kB

  private static final Logger logger = Logger.getLogger(CharsetUtils.class.getName());

  /**
   * Longer marks first: the UTF-32LE mark starts with the complete UTF-16LE mark.
   */
  private static final List<ByteOrderMark> BYTE_ORDER_MARKS = List.of(
      new ByteOrderMark(charsetOrNull("UTF-32LE"), bytes(0xFF, 0xFE, 0x00, 0x00)),
      new ByteOrderMark(charsetOrNull("UTF-32BE"), bytes(0x00, 0x00, 0xFE, 0xFF)),
      new ByteOrderMark(StandardCharsets.UTF_8, bytes(0xEF, 0xBB, 0xBF)),
      new ByteOrderMark(StandardCharsets.UTF_16LE, bytes(0xFF, 0xFE)),
      new ByteOrderMark(StandardCharsets.UTF_16BE, bytes(0xFE, 0xFF)));

  private CharsetUtils() {
  }

  /**
   * Opens a reader for a text file of unknown encoding. A leading byte order mark is skipped and
   * bytes that do not match the detected charset are replaced instead of throwing.
   *
   * @return buffered reader positioned after an optional byte order mark
   */
  public static @NotNull BufferedReader newBufferedReader(@NotNull final File file)
      throws IOException {
    return newBufferedReader(file.toPath());
  }

  /**
   * Opens a reader for a text file of unknown encoding. A leading byte order mark is skipped and
   * bytes that do not match the detected charset are replaced instead of throwing.
   *
   * @return buffered reader positioned after an optional byte order mark
   */
  public static @NotNull BufferedReader newBufferedReader(@NotNull final Path path)
      throws IOException {
    final CharsetDetection detection = detect(path);

    InputStream in = null;
    try {
      in = new BufferedInputStream(Files.newInputStream(path));
      in.skipNBytes(detection.bomLength());
      return new BufferedReader(new InputStreamReader(in, replacingDecoder(detection.charset())));
    } catch (IOException | RuntimeException e) {
      if (in != null) {
        in.close();
      }
      throw e;
    }
  }

  /**
   * Opens a reader for a stream of unknown encoding, e.g., a resource or a network response. The
   * stream is sampled with mark/reset, so only the first {@link #DETECTION_SAMPLE_BYTES} bytes are
   * buffered. Closing the returned reader closes the stream.
   *
   * @return buffered reader positioned after an optional byte order mark
   */
  public static @NotNull BufferedReader newBufferedReader(@NotNull final InputStream stream)
      throws IOException {
    final BufferedInputStream in =
        stream instanceof BufferedInputStream buffered ? buffered : new BufferedInputStream(stream);

    in.mark(DETECTION_SAMPLE_BYTES + 1);
    final byte[] sample = in.readNBytes(DETECTION_SAMPLE_BYTES);
    in.reset();

    final CharsetDetection detection = detect(sample);
    in.skipNBytes(detection.bomLength());
    return new BufferedReader(new InputStreamReader(in, replacingDecoder(detection.charset())));
  }

  /**
   * @return the detected charset of the file, {@link StandardCharsets#UTF_8} if the file cannot be
   * read
   */
  public static @NotNull Charset detectCharset(@NotNull final File file) {
    return detect(file.toPath()).charset();
  }

  /**
   * Detects the charset of a file. Never throws, an unreadable file is reported as plain UTF-8 and
   * the following read of that file will fail with the actual IO error.
   */
  public static @NotNull CharsetDetection detect(@NotNull final Path path) {
    try (InputStream in = Files.newInputStream(path)) {
      return detect(in.readNBytes(DETECTION_SAMPLE_BYTES));
    } catch (IOException e) {
      logger.log(Level.FINE, "Cannot read %s to detect its charset.".formatted(path), e);
      return new CharsetDetection(StandardCharsets.UTF_8, 0, false);
    }
  }

  /**
   * Detects the charset from the first bytes of a text file.
   *
   * @param sample the beginning of the file, may be the whole file
   */
  public static @NotNull CharsetDetection detect(final byte @NotNull [] sample) {
    for (ByteOrderMark bom : BYTE_ORDER_MARKS) {
      if (bom.charset() != null && bom.matches(sample)) {
        return new CharsetDetection(bom.charset(), bom.bytes().length, true);
      }
    }

    final Charset utf16 = detectUtf16WithoutBom(sample);
    if (utf16 != null) {
      return new CharsetDetection(utf16, 0, false);
    }

    // ascii files decode as utf-8 as well, so utf-8 is also the default for empty files
    final Charset charset = isValidUtf8(sample) ? StandardCharsets.UTF_8 : LEGACY_FALLBACK;
    return new CharsetDetection(charset, 0, false);
  }

  /**
   * UTF-16 encoded ASCII content is half NUL bytes, all of them on the same side of a two byte
   * unit. Text in any single byte encoding or in UTF-8 never contains NUL bytes.
   *
   * @return the UTF-16 variant or null if this does not look like UTF-16
   */
  private static @Nullable Charset detectUtf16WithoutBom(final byte @NotNull [] sample) {
    final int pairs = sample.length / 2;
    if (pairs < 8) {
      return null; // too short to tell
    }

    int nulAtEven = 0;
    int nulAtOdd = 0;
    for (int i = 0; i < pairs * 2; i++) {
      if (sample[i] == 0) {
        if ((i & 1) == 0) {
          nulAtEven++;
        } else {
          nulAtOdd++;
        }
      }
    }

    // ascii content in utf-16 has one NUL per character. Non ascii characters and the occasional
    // NUL on the other side are tolerated, but the NUL bytes must clearly favour one side.
    final int required = Math.max(4, pairs / 4);
    if (nulAtOdd >= required && nulAtEven <= pairs / 100) {
      return StandardCharsets.UTF_16LE; // 'a' -> 0x61 0x00
    }
    if (nulAtEven >= required && nulAtOdd <= pairs / 100) {
      return StandardCharsets.UTF_16BE; // 'a' -> 0x00 0x61
    }
    return null;
  }

  /**
   * @param sample the beginning of a file. A multi byte character that is cut off at the end of the
   *               sample is not counted as an error.
   */
  private static boolean isValidUtf8(final byte @NotNull [] sample) {
    final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT);

    final ByteBuffer in = ByteBuffer.wrap(sample);
    final CharBuffer out = CharBuffer.allocate(8192);
    while (in.hasRemaining()) {
      // endOfInput=false: an incomplete character at the end of the sample results in underflow
      final CoderResult result = decoder.decode(in, out, false);
      if (result.isError()) {
        return false;
      }
      if (result.isUnderflow()) {
        break; // all input consumed, or only an incomplete character left
      }
      out.clear(); // overflow, the decoded characters are not needed
    }
    return true;
  }

  private static @NotNull CharsetDecoder replacingDecoder(@NotNull final Charset charset) {
    return charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE);
  }

  private static @Nullable Charset charsetOrNull(@NotNull final String name) {
    return Charset.isSupported(name) ? Charset.forName(name) : null;
  }

  private static @NotNull Charset charsetOrElse(@NotNull final String name,
      @NotNull final Charset fallback) {
    final Charset charset = charsetOrNull(name);
    return charset != null ? charset : fallback;
  }

  private static byte[] bytes(final int... values) {
    final byte[] bytes = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      bytes[i] = (byte) values[i];
    }
    return bytes;
  }

  /**
   * @param charset    the detected charset
   * @param bomLength  number of leading bytes that belong to the byte order mark and need to be
   *                   skipped before decoding
   * @param fromBom   true if the charset was defined by a byte order mark and not guessed
   */
  public record CharsetDetection(@NotNull Charset charset, int bomLength, boolean fromBom) {

  }

  private record ByteOrderMark(@Nullable Charset charset, byte[] bytes) {

    boolean matches(final byte[] sample) {
      if (sample.length < bytes.length) {
        return false;
      }
      for (int i = 0; i < bytes.length; i++) {
        if (sample[i] != bytes[i]) {
          return false;
        }
      }
      return true;
    }
  }
}
