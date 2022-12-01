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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk;

import io.github.msdk.MSDKException;
import io.github.msdk.MSDKMethod;
import io.github.msdk.datamodel.Chromatogram;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.RawDataFile;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLParser;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLRawDataFile;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util.FileMemoryMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.logging.Logger;
import javolution.text.CharArray;
import javolution.xml.internal.stream.XMLStreamReaderImpl;
import javolution.xml.stream.XMLStreamConstants;
import javolution.xml.stream.XMLStreamException;

/**
 * <p>
 * This class contains methods which parse data in MzML format from {@link File File}, {@link Path
 * Path} or {@link InputStream InputStream} <br> {@link MsScan Scan}s and {@link Chromatogram
 * Chromatogram}s will be parsed, and the values pre-loaded when the {@link Predicate Predicate} is
 * passed. Other {@link MsScan Scan}s and {@link Chromatogram Chromatogram}s can be loaded on demand
 * if the source is a {@link File File}, whereas, they will be dropped if the source is an {@link
 * InputStream InputStream}
 * </p>
 */
public class MzMLFileImportMethod implements MSDKMethod<RawDataFile> {

  private final File mzMLFile;

  private final InputStream inputStream;
  private MzMLRawDataFile newRawFile;
  private MzMLParser parser;
  private volatile boolean canceled;
  private int lastLoggedProgress;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private Predicate<MsScan> msScanPredicate = s -> true;
  private Predicate<Chromatogram> chromatogramPredicate = c -> true;

  /**
   * <p>
   * Constructor for MzMLFileImportMethod.
   * </p>
   *
   * @param mzMLFilePath a {@link String String} which contains the absolute path to the MzML File.
   */
  public MzMLFileImportMethod(String mzMLFilePath) {
    this(new File(mzMLFilePath), s -> true, c -> true);
  }

  /**
   * <p>
   * Constructor for MzMLFileImportMethod.
   * </p>
   *
   * @param mzMLFilePath          a {@link String String} which contains the absolute path to the
   *                              MzML File.
   * @param msScanPredicate       Only {@link MsScan MsScan}s which pass this predicate will be
   *                              parsed by the parser and added to the {@link MzMLRawDataFile
   *                              RawDataFile} returned by the {@link #getResult() getResult()}
   *                              method.
   * @param chromatogramPredicate Only {@link Chromatogram Chromatogram}s which pass this predicate
   *                              will be parsed by the parser and added to the {@link
   *                              MzMLRawDataFile RawDataFile} returned by the {@link #getResult()
   *                              getResult()} method.
   */
  public MzMLFileImportMethod(String mzMLFilePath, Predicate<MsScan> msScanPredicate,
      Predicate<Chromatogram> chromatogramPredicate) {
    this(new File(mzMLFilePath), msScanPredicate, chromatogramPredicate);
  }

  /**
   * <p>
   * Constructor for MzMLFileImportMethod.
   * </p>
   *
   * @param mzMLFilePath a {@link Path Path} object which contains the path to the MzML File.
   */
  public MzMLFileImportMethod(Path mzMLFilePath) {
    this(mzMLFilePath.toFile(), s -> false, c -> false);
  }

  /**
   * <p>
   * Constructor for MzMLFileImportMethod.
   * </p>
   *
   * @param mzMLFilePath          a {@link Path Path} object which contains the path to the MzML
   *                              File.
   * @param msScanPredicate       Only {@link MsScan MsScan}s which pass this predicate will be
   *                              parsed by the parser and added to the {@link MzMLRawDataFile
   *                              RawDataFile} returned by the {@link #getResult() getResult()}
   *                              method.
   * @param chromatogramPredicate Only {@link Chromatogram Chromatogram}s which pass this predicate
   *                              will be parsed by the parser and added to the {@link
   *                              MzMLRawDataFile RawDataFile} returned by the {@link #getResult()
   *                              getResult()} method.
   */
  public MzMLFileImportMethod(Path mzMLFilePath, Predicate<MsScan> msScanPredicate,
      Predicate<Chromatogram> chromatogramPredicate) {
    this(mzMLFilePath.toFile(), msScanPredicate, chromatogramPredicate);
  }

  /**
   * <p>
   * Constructor for MzMLFileImportMethod.
   * </p>
   *
   * @param mzMLFile a {@link File File} object instance of the MzML File.
   */
  public MzMLFileImportMethod(File mzMLFile) {
    this(mzMLFile, null, s -> false, c -> false);
  }

  /**
   * <p>
   * Constructor for MzMLFileImportMethod.
   * </p>
   *
   * @param mzMLFile              a {@link File File} object instance of the MzML File.
   * @param msScanPredicate       Only {@link MsScan MsScan}s which pass this predicate will be
   *                              parsed by the parser and added to the {@link MzMLRawDataFile
   *                              RawDataFile} returned by the {@link #getResult() getResult()}
   *                              method.
   * @param chromatogramPredicate Only {@link Chromatogram Chromatogram}s which pass this predicate
   *                              will be parsed by the parser and added to the {@link
   *                              MzMLRawDataFile RawDataFile} returned by the {@link #getResult()
   *                              getResult()} method.
   */
  public MzMLFileImportMethod(File mzMLFile, Predicate<MsScan> msScanPredicate,
      Predicate<Chromatogram> chromatogramPredicate) {
    this(mzMLFile, null, msScanPredicate, chromatogramPredicate);
  }

  /**
   * <p>
   * Constructor for MzMLFileImportMethod.
   * </p>
   *
   * @param inputStream an {@link InputStream InputStream} which contains data in MzML format.
   */
  public MzMLFileImportMethod(InputStream inputStream) {
    this(null, inputStream, s -> true, c -> true);
  }

  /**
   * <p>
   * Constructor for MzMLFileImportMethod.
   * </p>
   *
   * @param inputStream           an {@link InputStream InputStream} which contains data in MzML
   *                              format.
   * @param msScanPredicate       Only {@link MsScan MsScan}s which pass this predicate will be
   *                              parsed by the parser and added to the {@link MzMLRawDataFile
   *                              RawDataFile} returned by the {@link #getResult() getResult()}
   *                              method.
   * @param chromatogramPredicate Only {@link Chromatogram Chromatogram}s which pass this predicate
   *                              will be parsed by the parser and added to the {@link
   *                              MzMLRawDataFile RawDataFile} returned by the {@link #getResult()
   *                              getResult()} method.
   */
  public MzMLFileImportMethod(InputStream inputStream, Predicate<MsScan> msScanPredicate,
      Predicate<Chromatogram> chromatogramPredicate) {
    this(null, inputStream, msScanPredicate, chromatogramPredicate);
  }

  /**
   * <p>
   * Internal constructor used to initialize instances of this object using other constructors.
   * </p>
   */
  private MzMLFileImportMethod(File mzMLFile, InputStream inputStream,
      Predicate<MsScan> msScanPredicate, Predicate<Chromatogram> chromatogramPredicate) {
    this.mzMLFile = mzMLFile;
    this.inputStream = inputStream;
    this.canceled = false;
    this.lastLoggedProgress = 0;
    this.msScanPredicate = this.msScanPredicate.and(msScanPredicate);
    this.chromatogramPredicate = this.chromatogramPredicate.and(chromatogramPredicate);
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * Parse the MzML data and return the parsed data
   * </p>
   *
   * @return a {@link MzMLRawDataFile MzMLRawDataFile} object containing the parsed data
   */
  @Override
  public MzMLRawDataFile execute() throws MSDKException {

    try {

      InputStream is = null;

      if (mzMLFile != null) {
        logger.finest("Began parsing file: " + mzMLFile.getAbsolutePath());
        is = FileMemoryMapper.mapToMemory(mzMLFile);
      } else if (inputStream != null) {
        logger.finest("Began parsing file from stream");
        is = inputStream;
      } else {
        throw new MSDKException("Invalid input");
      }
      // It's ok to directly create this particular reader, this class is `public final`
      // and we precisely want that fast UFT-8 reader implementation
      final XMLStreamReaderImpl xmlStreamReader = new XMLStreamReaderImpl();
      xmlStreamReader.setInput(is, "UTF-8");

      this.parser = new MzMLParser(this);
      this.newRawFile = parser.getMzMLRawFile();

      lastLoggedProgress = 0;

      int eventType;
      try {
        do {
          // check if parsing has been cancelled?
          if (canceled) {
            return null;
          }

          eventType = xmlStreamReader.next();

          switch (eventType) {
            case XMLStreamConstants.START_ELEMENT:
              final CharArray openingTagName = xmlStreamReader.getLocalName();
              parser.processOpeningTag(xmlStreamReader, is, openingTagName);
              break;

            case XMLStreamConstants.END_ELEMENT:
              final CharArray closingTagName = xmlStreamReader.getLocalName();
              parser.processClosingTag(xmlStreamReader, closingTagName);
              break;

            case XMLStreamConstants.CHARACTERS:
              parser.processCharacters(xmlStreamReader);
              break;
          }

        } while (eventType != XMLStreamConstants.END_DOCUMENT);

      } finally {
        if (xmlStreamReader != null) {
          xmlStreamReader.close();
        }
      }
      logger.finest("Parsing Complete");
    } catch (IOException | XMLStreamException e) {
      throw (new MSDKException(e));
    }

    return newRawFile;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public Float getFinishedPercentage() {
    if (parser == null) {
      return null;
    } else {
      return parser.getFinishedPercentage();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RawDataFile getResult() {
    return newRawFile;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cancel() {
    this.canceled = true;
  }

  /**
   * <p>
   * Getter for the field <code>msScanPredicate</code>.
   * </p>
   *
   * @return {@link Predicate Predicate} specified for {@link MsScan MsScan}s <br> The {@link
   * Predicate Predicate} evaluates to true always, if it wasn't specified on initialization
   */
  public Predicate<MsScan> getMsScanPredicate() {
    return msScanPredicate;
  }

  /**
   * <p>
   * Getter for the field <code>chromatogramPredicate</code>.
   * </p>
   *
   * @return {@link Predicate Predicate} specified for {@link Chromatogram Chromatogram}s <br> The
   * {@link Predicate Predicate} evaluates to true always, if it wasn't specified on initialization
   */
  public Predicate<Chromatogram> getChromatogramPredicate() {
    return chromatogramPredicate;
  }

  /**
   * <p>
   * Getter for the field <code>mzMLFile</code>.
   * </p>
   *
   * @return a {@link File File} instance of the MzML source if being read from a file <br> null if
   * the MzML source is an {@link InputStream InputStream}
   */
  public File getMzMLFile() {
    return mzMLFile;
  }

}
