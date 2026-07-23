# RenameWithLinks
Java program for Windows only, that renames and / or moves files and folders and updates all the shortcuts (`.lnk
` files) that were pointing to the moved elements, so that the shortcuts' target and working directory point to
the new location instead of becoming broken.

## Prerequisites

To build this Maven project, you need several custom dependencies (`DJavaLibraries`), which will be downloaded during the build
if you have a GitHub Personal Access Token (PAT) with `read:packages` scope configured in your `~/.m2/settings.xml`;
if you don't already have that, you can do that following these 2 steps :

1. Create a token at: https://github.com/settings/tokens
   <br><br>
    - Select `read:packages` scope.<br><br>
    - Copy the token value.


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
  
  This jar is created in the project's root folder and is called `RenameWithLinks-<VERSION>-jar-with-dependencies.jar`
  (e.g. `RenameWithLinks-2.1.0-jar-with-dependencies.jar`). It's recommended to rename it to `RenameWithLinks.jar` and
  to move it to a `C:\RenameWithLinks\` folder (but it's not required).<br><br>
  
  The jar can be executed normally with e.g. `java -jar RenameWithLinks.jar`, but the recommended way is to use the
  script `RenameWithLinks.BAT` which in turn calls the PowerShell script `RenameWithLinks.ps1` which starts the program
  in Windows Terminal, which is the recommended environment because it provides the best Unicode support.
  Running the program in PowerShell or Command Prompt outside of Windows Terminal may result in poor handling of certain
  Unicode characters.<br><br>
  
  The content of those 2 scripts must be adjusted if the jar has not been renamed to `RenameWithLinks.jar` or has not
  been deployed to a `C:\RenameWithLinks\` folder.<br><br>
  
- The files `Build List RenameWithLinks *.TXT` found in the root folder are _Build List_ files used by another program,
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
