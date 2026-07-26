# MoveWithShortcuts
Java program, currently for Windows only, that possibly renames and/or moves a file or folder, and updates the Windows
shortcuts (`.lnk` files) so that their target and working directory (the "Start in" field) point to the updated location
and the shortcuts still works, instead of becoming broken.

In detail :

1. Optionally renames and/or moves a file or folder.<br><br>

   - If the given original path exists and the given destination path does not, the original will be moved to the
     destination.<br><br>
     
   - If the given original path does not exist and the given destination path exists, the moving / renaming from the
     original to the destination will be considered as having already happened outside of the program.<br><br>

   - If both the given original path and the given destination path exist, or neither one exists, an error is shown and
     the program terminates. It is a prerequisite that one exists and the other one does not.<br><br>

2. Updates all the shortcuts found under a given search path if their target and/or working directory are pointing to
   the moved element, so that they point to the updated location instead of becoming broken.


## Usage

The program can be started without arguments, in which case it will prompt on the console for the 4 needed values.<br>

In alternative, it can be started with 3 or 4 arguments :<br>

1. Original : The path to the file or folder to rename / move or that has already been renamed / moved.<br><br>

2. Destination : The file or folder to which the given original is requested to be renamed / moved or
   already was. If it does not include a parent path, it is assumed to be in the same folder of the given original.<br><br>
   
   Must be of the same type (that is, file or folder) as the given original.<br><br>
   
   May include a different path, which means the original is requested to be - or it already has been - moved there
   rather than just renamed.<br><br>
   
3. Search path : The path inside which to search for the shortcut files to possibly update. If the renaming / moving
   resulted in changes to the filesystem structure (f.ex. because the destination path did not exist so it has been
   created), this value obviously refers to the filesystem as it is after the renaming / moving, not before it.<br><br>

4. (Optional) Verbosity level, from 0 to 3. A value of 0 means that only possible error or warning messages will be
   shown.

## Prerequisites

To build this Maven project, you need several custom dependencies (`DJavaLibraries`), which will be downloaded during
the build if you have a GitHub Personal Access Token (PAT) with at least `read:packages` scope configured in your
`~/.m2/settings.xml`; if you don't already have that, you can follow these 2 steps :

1. Create a token at: https://github.com/settings/tokens
   <br><br>
    - Select `read:packages` scope.<br><br>
    - Copy the token value.<br><br>

2. Add the token to your Maven `settings.xml` file (located at `~/.m2/settings.xml`) :

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_TOKEN</password>
        </server>
    </servers>
</settings>
```
Of course if the `<servers>` section already exists in your `settings.xml`, just add the `<server>` section inside it. 

When you build the project, e.g. using

```bash
mvn clean install
```

, Maven will download the required `DJavaLibraries` dependencies from GitHub Packages using your token.

## Build and deployment

- Building with Maven (e.g. `mvn clean install`) produces an Uber jar which is executable and contains all the needed
  dependencies. No other files are needed to run the program.<br><br>
  
  This jar is created in the project's root folder and is called `MoveWithShortcuts-<VERSION>-jar-with-dependencies.jar`
  (e.g. `MoveWithShortcuts-2.1.0-jar-with-dependencies.jar`). It's recommended to rename it to `MoveWithShortcuts.jar` and
  to move it to a `C:\MoveWithShortcuts\` folder (but it's not required).<br><br>
  
  The jar can be executed normally with e.g. `java -jar MoveWithShortcuts.jar`, but the recommended way is to use the
  script `MoveWithShortcuts.BAT` which in turn calls the PowerShell script `MoveWithShortcuts.ps1` which starts the program
  in Windows Terminal, which is the recommended environment because it provides the best Unicode support.
  Running the program in PowerShell or Command Prompt outside of Windows Terminal may result in poor handling of certain
  Unicode characters.<br><br>
  
  The content of those 2 scripts must be adjusted if the jar has not been renamed to `MoveWithShortcuts.jar` or has not
  been deployed to a `C:\MoveWithShortcuts\` folder.<br><br>
  
- The files `Build List MoveWithShortcuts *.TXT` found in the root folder are _Build List_ files used by another program,
  _BuildOrchestrator_, which is currently not published. So these files can currently be ignored.
  
## Indentation of Java code

The indentation of the Java code is tuned to IntelliJ and it works if the inlay hints are shown, with the setting "Use
editor font for inlay hints" true (Settings / Editor / General / Appearance) and the editor font "JetBrains Mono" size
13.0 (Settings / Editor / Font).

## Requires

- At least Java 21.
- TestNG to run the tests.
- See dependencies in pom.xml .

## Project Status

- Active and in use.

## Contacts

- ciustea@lorettastan.eu
