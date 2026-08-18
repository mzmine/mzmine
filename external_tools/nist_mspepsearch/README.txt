NIST MSPepSearch runtime (not included)
=======================================

mzmine uses the NIST MSPepSearch command line tool for headless EI library searches, including
batch processing. The program is NOT distributed with mzmine and is not part of a standard NIST
MS Search installation. You must download it yourself:

    https://chemdata.nist.gov/dokuwiki/doku.php?id=peptidew:mspepsearch

Illustrated setup instructions, including where the module lives in the menus, are in
docs/nist-mspepsearch/README.md.

Setup
-----
1. Download the MSPepSearch archive for your platform (64-bit on modern Windows).
2. Extract it anywhere. Keep MSPepSearch64.exe together with the DLLs shipped beside it
   (ctnt66a64.dll, dForm64.dll, nistdl64a.dll, zlib1_x64.dll) - the program will not start
   without them.
3. In mzmine, open the NIST MSPepSearch module and set "MSPepSearch directory" to the folder
   you extracted.

Extracting into this directory also works: mzmine falls back to
external_tools/nist_mspepsearch/MSPepSearch64.exe when the parameter is left empty, which is how
packaged builds can ship the runtime. The binaries themselves are intentionally excluded from
version control (see .gitignore).

Spectral libraries
------------------
No NIST spectral library is included or downloadable here. Users must supply their own licensed
NIST mainlib and/or replib installation. The default location is C:\NIST23\MSSEARCH and can be
changed in the module. Library data never leaves the workstation.

MSPepSearch itself may be redistributed without restriction according to NIST, but mzmine does not
redistribute it, to keep vendor binaries out of the repository.
