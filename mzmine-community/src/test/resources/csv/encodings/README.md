# Encoded csv/tsv test files

The same little table written the way the tools out there write it. Used by
`util.CsvEncodingsTest` to check `CharsetUtils` (encoding detection) and
`CSVParsingUtils.autoDetermineSeparator` (separator detection).

| file                        | encoding     | separator | line ends | written like this by       |
|-----------------------------|--------------|-----------|-----------|----------------------------|
| `utf8.csv`                  | UTF-8        | `,`       | LF        | most tools, R, python       |
| `utf8.tsv`                  | UTF-8        | tab       | LF        | most tools                  |
| `utf8-bom.csv`              | UTF-8 + BOM  | `,`       | CRLF      | excel "CSV UTF-8"           |
| `windows1252.csv`           | windows-1252 | `,`       | CRLF      | excel "CSV (comma delimited)" on windows |
| `windows1252-semicolon.csv` | windows-1252 | `;`       | CRLF      | excel in a european locale  |
| `excel-sep-directive.csv`   | windows-1252 | `;`       | CRLF      | excel, separator declared in a leading `sep=;` line |
| `utf16le-bom.tsv`           | UTF-16LE + BOM | tab     | CRLF      | excel "Unicode Text (*.txt)" |
| `utf16be-bom.csv`           | UTF-16BE + BOM | `,`     | LF        | java `Charset UTF-16` writers |
| `utf16le-nobom.tsv`         | UTF-16LE     | tab       | LF        | tools that forget the byte order mark |
| `utf8-decimal-comma.csv`    | UTF-8        | `;`       | CRLF      | european locales, the values contain commas |

The values contain `Ö`, `°` and `µ`, which are encoded differently in UTF-8 and windows-1252,
so a file that is read with the wrong encoding fails the test instead of passing silently.

The files are marked as binary in `/.gitattributes`. Without that, git would normalize their line
endings and destroy the UTF-16 files. Do not open and save them with an editor that re-encodes.

To regenerate them (powershell, from the repository root):

```powershell
$dir = "mzmine-community\src\test\resources\csv\encodings"
$full = (Resolve-Path $dir).Path
$oe = [string][char]0x00D6; $deg = [string][char]0x00B0; $mu = [string][char]0x00B5
function Table([string]$sep, [string]$nl, [bool]$decimalComma) {
  if ($decimalComma) { $mz1 = "195,0877"; $mz2 = "180,0634" } else { $mz1 = "195.0877"; $mz2 = "180.0634" }
  $lines = @(("name" + $sep + "mz" + $sep + "note"),
             ($oe + "l" + $sep + $mz1 + $sep + "25 " + $deg + "C"),
             ("Glucose" + $sep + $mz2 + $sep + "30 " + $mu + "m"))
  return ($lines -join $nl) + $nl
}
function WriteFile([string]$name, [string]$text, $enc) {
  [System.IO.File]::WriteAllText((Join-Path $full $name), $text, $enc)
}
$lf = "`n"; $crlf = "`r`n"
WriteFile "utf8.csv" (Table "," $lf $false) (New-Object System.Text.UTF8Encoding($false))
WriteFile "utf8.tsv" (Table "`t" $lf $false) (New-Object System.Text.UTF8Encoding($false))
WriteFile "utf8-bom.csv" (Table "," $crlf $false) (New-Object System.Text.UTF8Encoding($true))
WriteFile "windows1252.csv" (Table "," $crlf $false) ([System.Text.Encoding]::GetEncoding(1252))
WriteFile "windows1252-semicolon.csv" (Table ";" $crlf $false) ([System.Text.Encoding]::GetEncoding(1252))
WriteFile "excel-sep-directive.csv" ("sep=;" + $crlf + (Table ";" $crlf $false)) ([System.Text.Encoding]::GetEncoding(1252))
WriteFile "utf16le-bom.tsv" (Table "`t" $crlf $false) (New-Object System.Text.UnicodeEncoding($false, $true))
WriteFile "utf16be-bom.csv" (Table "," $lf $false) (New-Object System.Text.UnicodeEncoding($true, $true))
WriteFile "utf16le-nobom.tsv" (Table "`t" $lf $false) (New-Object System.Text.UnicodeEncoding($false, $false))
WriteFile "utf8-decimal-comma.csv" (Table ";" $crlf $true) (New-Object System.Text.UTF8Encoding($false))
```

## The excel exports

The `micometa_` files are one metadata sheet, saved once with every format that the excel save as
dialog offers. They were exported by hand, not by the script above, so do not re-save them. All of
them contain the same table, which is plain ascii, so what is tested here is the separator, the
encoding and the line ending.

| file                     | encoding       | separator | line ends | saved as                                   |
|--------------------------|----------------|-----------|-----------|--------------------------------------------|
| `micometa_csv.csv`       | ascii          | `,`       | CRLF      | CSV (comma delimited)                      |
| `micometa_doscsv.csv`    | ascii          | `,`       | CRLF      | CSV (MS-DOS)                               |
| `micometa_maccsv.csv`    | ascii          | `,`       | CR        | CSV (Macintosh)                            |
| `micometa_utf8.csv`      | UTF-8 + BOM    | `,`       | CRLF      | CSV UTF-8                                  |
| `micometa_tab.txt`       | ascii          | tab       | CRLF      | Text (tab delimited)                       |
| `micometa_dos.txt`       | ascii          | tab       | CRLF      | Text (MS-DOS)                              |
| `micometa_mac.txt`       | ascii          | tab       | CR        | Text (Macintosh)                           |
| `micometa_unicode.txt`   | UTF-16LE + BOM | tab       | CRLF      | Unicode Text                               |
| `micometa_semicolon.csv` | ascii          | `;`       | CRLF      | CSV (comma delimited) of a european excel  |
| `micometa_faketab.csv`   | ascii          | tab       | CRLF      | Text (tab delimited), then renamed to .csv |

The macintosh formats separate their rows with a lone carriage return, `micometa_maccsv.csv` then
ends the file with CRLF anyway. Ascii is valid UTF-8, so the detection reports UTF-8 for the files
that excel wrote as windows-1252.

The last two are named `.csv` but are not comma separated, which is what a european excel and a
renamed tab export look like. They are there to keep the detection from just trusting the file
extension.
