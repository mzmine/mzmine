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

package io.github.mzmine.modules.io.download;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ZenodoRecord(OffsetDateTime created, OffsetDateTime modified, OffsetDateTime updated,
                           int revision, @JsonProperty("recid") String recid, String doi,
                           @JsonProperty("conceptrecid") String latestRedirectId, String title,
                           Metadata metadata, List<ZenFile> files, Links links) {

  public String getArchiveZipUrl() {
    return links.archive;
  }

  /**
   * Remove files that do not match name pattern
   *
   * @param nameRegex name pattern
   * @return new record
   */
  public ZenodoRecord applyNameFilter(final String nameRegex) {
    var filteredFiles = files.stream().filter(f -> f.name.matches(nameRegex)).toList();

    return new ZenodoRecord(created, modified, updated, revision, recid, doi, latestRedirectId,
        title, metadata, filteredFiles, links);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ZenFile(String id, @JsonProperty("key") String name, long size, String checksum,
                        // unwrap link from links.self
                        @JsonProperty("links") @JsonDeserialize(using = LinksDeserializer.class) String link) {

  }

  /**
   * Links from this record
   *
   * @param self       json of this record
   * @param selfHtml   html of this record
   * @param parent     previous version
   * @param files      endpoint to get files listed - similar to Files in this record?
   * @param archive    download all files endpoint
   * @param latest     link to latest version
   * @param latestHtml link to latest html version
   * @param versions   all versions
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(SnakeCaseStrategy.class)
  public record Links(String self, String selfHtml,
//                      String doi,
//                       String selfDoi,
//                       String selfDoiHtml,
                      String parent,
//                      String parentHtml, String parentDoi,
//                       String parentDoiHtml,
//                       String selfIiifManifest,
//                       String selfIiifSequence,
                      String files,
//                       String mediaFiles,
                      String archive,
//                       String archiveMedia,
                      String latest, String latestHtml, String versions
//                       String draft,
//                       String reserveDoi,
//                       String accessLinks,
//                       String accessGrants,
//                       String accessUsers,
//                       String accessRequest,
//                       String access,
//                       String communities,
//                       String communitiesSuggestions,
//                      String requests
  ) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Metadata(String title, String doi, String publication_date, String description,
                  String access_right, List<Creator> creators, ResourceType resource_type,
                  License license) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Creator(String name, String affiliation, String orcid) {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResourceType(String title, String type) {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record License(String id) {

    }
  }

  /**
   * unwrap File.links object
   */
  private static class LinksDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      ObjectNode node = p.getCodec().readTree(p);
      return node.get("self").asText();
    }
  }

}
