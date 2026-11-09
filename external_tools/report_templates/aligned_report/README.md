# Aligned feature report templates

These are general native report fixes, also exercised by MCP-triggered exports. Previously, a
summary subreport and its table could consume the same data-source cursor, skipping the first
feature. Unconditional section/detail breaks could also produce pages with only headers and
footers. The fixes preserve every requested feature and create breaks only between actual sections
or records; they are independent of the transport used to request the report.

The `.jasper` files are the templates loaded by mzmine at runtime. The matching `.jrxml` files
are editable JasperReports 7 sources for the cover, feature summary and feature detail reports.
Keep source and compiled files together when changing these templates. Compile with the version
of JasperReports declared in `gradle/libs.versions.toml` in the community repository (currently
7.0.3), for example using `JasperCompileManager.compileReportToFile(sourcePath, outputPath)`
with mzmine's runtime classpath, or the matching Jaspersoft Studio compiler.

The summary table owns its feature data source. Its containing subreport must run exactly once
using `JREmptyDataSource`; sharing the table's cursor with that containing subreport skips the
first feature. Section breaks belong before requested sections in the cover. Within feature
details, insert a break only when another feature follows. Do not add an unconditional final
break or an empty trailing summary band.

The company repository's `AlignedReportTemplateTest` checks the shipped binaries for missing or
duplicate rows, correct feature headings, and pages containing only headers/footers. It covers
one, three and thirty features, summary-only and evidence-only reports, and optional processing
history. Set `MZMINE_REPORT_TEMPLATES` to an absolute template directory to check another copy.
Both the company and community distributions ship a copy of these templates; update both.
The recorded-data `LocalPostprocessingEndToEndTest` also exercises native chart generation and
PDF/HTML export through the MCP transport.
