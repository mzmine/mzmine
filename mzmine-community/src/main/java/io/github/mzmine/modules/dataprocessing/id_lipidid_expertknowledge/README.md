# Lipid Validation module
## Overview of .drl Rule Files
`.drl` files are **Drools Rule Language**, which is part of a Business Rule Management System (BRMS) and rules engine for Java. 
They define **rules, queries, functions, and declarations** used by the Lipid Validation module to validate lipid annotations.

### File naming
`.drl` files in this module follow the pattern 
```
<Lipid abbreviation>_<polarity>.drl
``` 
- (e.g., `CA_Positive.drl`). 
- The module searches for the **lipid abbreviation (uppercase)** and the **polarity** (*Positive* or *Negative*) in the file name.

## Structure of a `.drl` file
### 1. Package declaration
Defines the namespace of the rules, similar to Java packages. Helps organize rules and avoid naming conflicts.

- File location: The program will automatically store the files in `src/main/resources/rules_id_lipid_expert_knowledge`. Once the files are uploaded one time, there is no need to upload them everytime the module is used. If the file needs changing and is uploaded again, the previous file with that name will be overwritten.
- Subfolders: `positive/userFiles` and `negative/userFiles` (based on sample polarity).
- Example: 
```
//Positive polarity
package rules_id_lipid_expert_knowledge.positive.userFiles;

//Negative polarity
package rules_id_lipid_expert_knowledge.negative.userFiles;
```

### 2. Imports
Allow the use of external Java classes inside rules.

- Common imports for all rules:
```
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids.FoundLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.FoundAdduct;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.MobilePhases;
import java.util.List;
```
- Additional imports based on polarity:
```
//Positive polarity
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.CommonAdductPositive;

//Negative polarity
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.CommonAdductNegative;
```

### 3. Globals
Define global variables accessible and modifiable by all rules:
```
global FoundLipid lipid;
global List mobilePhases;
```

### 4. Rules
Each `.drl` file contains rules with the following elements:

#### `rule` section
The name section should have the structure:
```
rule "<name>"
```

**Examples by Rule Type**

**1. Positive presence:** 
Check that specific adducts **appear**.
  - Rule name: `Presence <adduct>`

**2. Negative presence (absence):**
Check that the most intense adduct **does not appear**.
  - Rule name: `No presence <adduct>`

**3. Correct intensity order:**
Check if intensity of one adduct is **greater** than another, which is *expected*. It should have this symbol **>**.
  - Rule name: `Intensity <adduct1> > <adduct2>`

**4. Incorrect intensity order:**
Check if intensity of one adduct is **lower** than another, which is *not expected*. It should have this symbol **<**.
  - Rule name: `Intensity <adduct1> < <adduct2>`

#### `when` section
Define conditions (pattern matching facts) for firing the rule.

We search for the specific adduct name, and we have to take into account that some adducts may only appear when specific mobile phases are used in the samples.

- `$adduct`: checks presence of the adduct.
- `$phaseN`: ensures required mobile phases are present.

**Examples by Rule Type**

**1. Positive presence:**
```
  rule "Presence [M+C2H7N2]+"
  when
    $adduct: FoundAdduct(adductName == "[M+C2H7N2]+")
    $phase1 : MobilePhases() from mobilePhases
    eval($phase1 == MobilePhases.NH4)
    $phase2 : MobilePhases() from mobilePhases
    eval($phase2 == MobilePhases.CH3CN)
    $phase3 : MobilePhases() from mobilePhases
    eval($phase3 == MobilePhases.CH3OH)
```

**2. Negative presence (absence):**
```
  rule "No presence [M+NH4]+"
  when
    not FoundAdduct(adductName == "[M+NH4]+")
    $phase1 : MobilePhases() from mobilePhases
    eval($phase1 == MobilePhases.NH4)
```

**3. Correct intensity order:**
```
  rule "Intensity [M+C2H7N2]+ > [M+Na]+"
  when
    $adduct1: FoundAdduct(adductName == "[M+C2H7N2]+")
    $adduct2: FoundAdduct(adductName == "[M+Na]+")
    $phase1 : MobilePhases() from mobilePhases
    eval($phase1 == MobilePhases.NH4)
    $phase2 : MobilePhases() from mobilePhases
    eval($phase2 == MobilePhases.CH3CN)
    $phase3 : MobilePhases() from mobilePhases
    eval($phase3 == MobilePhases.CH3OH)
    eval($adduct1.getIntensity() > $adduct2.getIntensity())
```

**4. Incorrect intensity order:**
```
  rule "Intensity [M+C2H7N2]+ < [M+Na]+"
  when
    $adduct1: FoundAdduct(adductName == "[M+C2H7N2]+")
    $adduct2: FoundAdduct(adductName == "[M+Na]+")
    $phase1 : MobilePhases() from mobilePhases
    eval($phase1 == MobilePhases.NH4)
    $phase2 : MobilePhases() from mobilePhases
    eval($phase2 == MobilePhases.CH3CN)
    $phase3 : MobilePhases() from mobilePhases
    eval($phase3 == MobilePhases.CH3OH)
    eval($adduct1.getIntensity() < $adduct2.getIntensity())
```

#### `then` section
Define actions if conditions are met:
- Set score: adds/subtracts value to lipid score.
- Set description: `.setDescrCorrect(<message>)` or `.setDescrIncorrect(<message>)`.

**Examples by Rule Type**

**1. Positive presence:**
```
  then
    lipid.setScore(1);
    lipid.setDescrCorrect("Contains [M+C2H7N2]+, ");
```

**2. Negative presence (absence):**
```
  then
    lipid.setScore(-1);
    lipid.setDescrIncorrect("Missing [M+NH4]+, ");
```

**3. Correct intensity order:**
```
  then
    lipid.setScore(2);
    lipid.setDescrCorrect("Intensity OK [M+C2H7N2]+ > [M+Na]+, ");
```

**4. Incorrect intensity order:**
```
  then
    lipid.setScore(-2);
    lipid.setDescrIncorrect("Intensity NOT OK [M+C2H7N2]+ < [M+Na]+, ");
```

#### `end` section
Closes rule.

## Sample `.drl` files
There is dummy sample file for each polarity to use as reference when building your own rule files. 
- **Positive polarity**: 
  - Name: `Dummy_Positive.drl` 
  - Location: `src/main/resources/rules_id_lipid_expert_knowledge/positive/userFiles`
- **Negative polarity**:
  - Name: `Dummy_Negative.drl`
  - Location: `src/main/resources/rules_id_lipid_expert_knowledge/negative/userFiles`

## Drools documentation
For more information on Drools syntax check [Drools Documentation](https://docs.drools.org/8.32.0.Final/drools-docs/docs-website/drools/getting-started/index.html)
