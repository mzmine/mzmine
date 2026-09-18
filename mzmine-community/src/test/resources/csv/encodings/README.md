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
