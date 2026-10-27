# MoveWithShortcuts

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)

Java program, currently for Windows only, that possibly renames and/or moves a file or folder, and updates the Windows
shortcuts (`.lnk` files) so that their target and working folder (the "Start in" field) have their value adjusted
according to the renaming / moving, instead of the shortcut becoming broken.

If you just want to run it without building from source, see the [Quick Start](#quick-start-using-the-distributable-zip).

In detail :

1. Optionally renames and/or moves a file or folder.

   - If the original path exists and the destination path does not, the original will be renamed and/or moved to the
     destination.
     
   - If the original path does not exist and the destination path exists, the moving / renaming from the original to the
     destination will be considered as having already happened outside of the program, and only the corresponding
     shortcut-adjustment operation will take place.
     
   - If both the original path and the destination path exist, or neither one exists, an error is shown and the program
     terminates. It is a prerequisite that one path exists and the other one does not.
     
2. Updates all the shortcuts found under a given search path if their target and/or working folder are pointing to a
   location under the moved element, so that they point to the updated location instead of the shortcut becoming broken.
   
## Usage

MoveWithShortcuts has two launch modes :

- **GUI mode (the default)** : starts the Swing dialog. With no arguments it opens empty; the operation options
  (`-from`, `-to`, `-search`) prefill its fields, and the other operation options are honored too.
- **Console mode** : the traditional console flow. It starts when `-console` is passed or when `Console = true` is set
  in the configuration file. `-gui` overrides `Console = true`.

In a headless environment the program falls back to console mode automatically and prints a message.

The GUI operation-detail area shows the same execution messages as console mode, in execution order (oldest first).
Warnings and errors are prefixed with `WARNING: ` / `ERROR: `, the raw progress lines remain in the progress bar, and the
history keeps the last 1000 lines. GUI mode uses verbosity 1 by default; `-verbosity` overrides it.

The GUI also lets you choose the shortcuts updater implementation and the shortcut-file name policy; the
`-shortcuts-updater` and `-shortcut-file-name` options set their initial values, and each choice shows a short
description.

The console flow can be started with `-console` and no other arguments, in which case it prompts for the needed values.
When started with some named options, only the missing required options (`-from`, `-to`, `-search`) are asked for;
optional options use their defaults.

The command-line format is :

```
MoveWithShortcuts [-console | -gui] [-from <file or folder path>] [-to <new path>] [-search <folder path>] [-verbosity <0-2>] [-shortcut-file-name <never|exact|descendant>] [-shortcuts-updater <id>] [-create-context-menu <true|false>] [-help]
```

In console mode `-from`, `-to` and `-search` are required unless prompted ; in GUI mode they are optional prefills.
If a value cannot be used in GUI mode, the GUI still opens without that prefill and shows the parse error in the
operation-detail area. `-help` prints the console usage text.

1. `-from` : The path of the file or folder to rename / move, or the path it had before if the operation was already
   done elsewhere, in which case no rename / move will be done and only the shortcuts adjustment will be done.
   
2. `-to` : The new name or path for the file or folder, or the path it has now if the operation was already done
   elsewhere, in which case no rename / move will be done and only the shortcuts adjustment will be done. If it does
   not include a parent path, it is assumed to be in the same folder where the original is.
   
   Exactly one of `-from` and `-to` must exist; the other must not.
   
   Must be of the same type (that is, file or folder) as the original.
   
   May include a different path, which means the original is requested to be - or it already has been - moved there
   (and possibly renamed) rather than just renamed.
   
3. `-search` : The path inside which to recursively search for the shortcut files to possibly update. If the renaming
   / moving will be performed by the program (i.e. the original path did exist and the destination did not), and it will
   result in changes to the folder structure (f.ex. because the destination path does not exist so it will be created,
   or because the element to rename is a folder and not a file), then the path specified here must obviously refer to
   the folder structure as it will be after the renaming / moving, not as it is before it.
   
4. `-verbosity` (optional) : Verbosity level, from 0 to 2, default 1. A value of 0 means that only possible error or
   warning messages will be shown.
   
5. `-shortcut-file-name` (optional) : What to do with the shortcut files' own names :

   - `never` : only the target and work folder fields are updated.
   - `exact` (default) : rename a shortcut file whose name (minus `.lnk`) equals the old leaf's full name and whose
     target is exactly the renamed / moved leaf.
   - `descendant` : like `exact`, and also when the shortcut's target is inside the renamed / moved folder.
   
   The Windows default `" - Shortcut"` part (localized) is accepted and preserved when present, and pure moves (same
   leaf name) never rename.
   
6. `-shortcuts-updater` (optional) : The `IShortcutsUpdater` implementation to use. Currently only
   `ps-scripts-multi` is available; it is the default. A configuration file can set it too, and the command line
   overrides the configuration file.
   
7. `-create-context-menu` (optional) : Whether to create or update the Windows Explorer right-click context-menu
   integration at startup, `true` or `false`, default `true`. A configuration file can set it too, and the command line
   overrides the configuration file.
   
8. `-console` (flag) : Starts in console mode instead of the default GUI mode. The flag takes no value.
   
9. `-gui` (flag) : Starts in GUI mode, overriding `Console = true` in the configuration file. The flag takes no value.
   `-console` and `-gui` cannot be used together.
   
10. `-help` : Prints the usage text and exits.

Wrap paths that contain spaces in double quotes. A value that is itself an option name (e.g. a file literally named
`-from`) must be attached to its option with `=`, as in `-to=-from`.

Examples :

```
MoveWithShortcutsGui.BAT -from "C:\Old Name.txt" -to "C:\New Name.txt" -search "C:\Users\Davide"
MoveWithShortcuts.BAT -from "C:\Old Name.txt" -to "C:\New Name.txt" -search "C:\Users\Davide"
java -jar MoveWithShortcuts.jar -from "C:\Old Name.txt" -to "C:\New Name.txt" -search "C:\Users\Davide"
java -jar MoveWithShortcuts.jar -console -from "C:\Old Name.txt" -to "C:\New Name.txt" -search "C:\Users\Davide" -shortcut-file-name exact
```

The console launchers (`MoveWithShortcuts.BAT` / `.ps1`) add `-console` automatically; the GUI launchers
(`MoveWithShortcutsGui.BAT` / `.ps1`) use `javaw` and start the GUI.

### Configuration file

MWS reads an optional `MoveWithShortcuts-Config.TXT` through DParam. The parameters are :

```
ShortcutsUpdater = ps-scripts-multi

CreateContextMenu = true

Console = false
```

When running from the IDE, the file is read from `src/main/resources`. For the deployed JAR, place it next to
`MoveWithShortcuts.jar` (in `C:\MoveWithShortcuts\`). If the file or a parameter is missing, the default is used;
an invalid value is reported and the default is used. The command-line options win over the configuration file.
`Console = true` makes console mode the default for all launches; `-gui` overrides it. If `Console = true`, a
no-argument launch of the JAR starts the console; use the GUI launcher (which passes `-gui`) or `-gui` to start the GUI.

### Windows Explorer integration

MWS installs a per-user Windows Explorer right-click context-menu entry named `Move/rename with MWS`, both for files
and for folders. It requires no administrator rights. Selecting the entry opens a Swing dialog prefilled with the
selected item's path, so the paths can be corrected, browsed or dropped into their fields instead of being typed.
The menu entry uses `MWS.ico`, which must stay next to `MoveWithShortcuts.jar`.

The first time MWS starts, in either mode, it installs (or updates) the Windows Explorer context-menu integration
automatically. If you only want to install the integration without performing a move, start the GUI once and close it,
or start the console with `-console` and cancel at the first interactive question: the registry keys are created at
startup, before any operation. If the new context-menu entry does not appear, refresh or restart Windows Explorer;
restarting MWS is not required.

On Windows 11 the entry appears under **Show more options**.

To open the same dialog without a selected item (for example to install the integration without performing a move),
run :

```
java -cp MoveWithShortcuts.jar move_with_shortcuts.MoveWithShortcutsGuiMain
```

## Quick Start (using the distributable ZIP)

If you just want to use MoveWithShortcuts without building from source:

1. Download the distributable ZIP from the
   [Releases](https://github.com/davide-vecchi/MoveWithShortcuts/releases) page and unzip it into a convenient folder
   (e.g. `C:\MoveWithShortcuts\`).
   
2. Run `MoveWithShortcutsGui.BAT` for the GUI (the default), or `MoveWithShortcuts.BAT` for the console.

   The ZIP contains the executable jar, the console launchers (`MoveWithShortcuts.BAT` / `MoveWithShortcuts.ps1`), the
   GUI launchers (`MoveWithShortcutsGui.BAT` / `MoveWithShortcutsGui.ps1`), the `README.md`, the `LICENSE` and the
   program icon (`MWS.ico`). Java 21 or higher is required.
   
## Prerequisites (for building from source)

- Java 21 or higher installed and configured.
- Maven 3.6 or higher (tested with 3.9).
- Git (to clone the required repositories).
- TestNG is used to run the tests (Maven downloads it automatically).

## Installing the required libraries (DLibs)

*This section is only needed if you are building from source.*

`MoveWithShortcuts` depends on several libraries (`DLibs`) that are distributed as pre-compiled JARs in
the [Libs-JARs](https://github.com/davide-vecchi/Libs-JARs) repository. The steps below explain how to install these
libraries in your local Maven repo.

Alternatively, if you have access to the GitHub Packages repository declared in `pom.xml` (and to the repositories
that publish these libraries), you can configure your credentials for it and let `mvn clean install` resolve the DLibs
directly, skipping the steps below.

**Step 1: Clone the `Libs-JARs` repository**

```bash
git clone https://github.com/davide-vecchi/Libs-JARs.git ../Libs-JARs
```

**Step 2: Install the DLibs libraries**

From the `Libs-JARs` folder, run the installation script:

```bash
cd ../Libs-JARs
```

```bash
./install-all-dlibs.sh        # Linux, macOS, or Git Bash on Windows
```

or

```bash
install-all-dlibs.bat         # Windows Command Prompt or PowerShell
```

Alternatively, you can go inside the `MoveWithShortcuts` folder and run the provided `install-deps.sh` or
`install-deps.bat` script to install only the libraries needed by this project.

Either way, the script will install the JARs into your local Maven repository (`~/.m2/repository`).

When you build the project, e.g. using

```bash
mvn clean install
```

, Maven will resolve these dependencies from your local repository.

## Build and deployment

- Building with Maven (e.g. `mvn clean install`) produces an Uber JAR which is executable and contains all the needed
  dependencies. No other files are needed to run the program.
  
  This JAR is created in the project's root folder and is called `MoveWithShortcuts-<VERSION>-jar-with-dependencies.jar`
  (e.g. `MoveWithShortcuts-2.1.0-jar-with-dependencies.jar`). It's recommended to rename it to `MoveWithShortcuts.jar`
  and to move it to a `C:\MoveWithShortcuts\` folder (but it's not required).
  
  The JAR can be executed normally with e.g. `java -jar MoveWithShortcuts.jar`, but the recommended way is to use the
  script `MoveWithShortcuts.BAT` which in turn calls the PowerShell script `MoveWithShortcuts.ps1` which starts the
  program in Windows Terminal, which is the recommended environment because it provides the best Unicode support.
  Running the program in PowerShell or Command Prompt outside of Windows Terminal may result in poor handling of certain
  Unicode characters.
  
  The content of those 2 `MoveWithShortcuts.*` scripts must be adjusted if the JAR has not been renamed to
  `MoveWithShortcuts.jar` or has not been deployed to a `C:\MoveWithShortcuts\` folder.
  
- To build a distributable ZIP (the same layout as the one on the Releases page), place the executable JAR (renamed to
  `MoveWithShortcuts.jar`) in the `Packaging/` folder and run `Packaging/Package_MoveWithShortcuts.BAT` (Windows) or
  `Packaging/Package_MoveWithShortcuts.sh` (Linux/macOS). The script bundles the JAR, the launcher scripts, the
  `README.md` and the `LICENSE` into `MoveWithShortcuts.zip` (or `MoveWithShortcuts-<version>.zip` when a version is
  passed as the second argument).
  
## Indentation of Java code

The indentation of the Java code is tuned to IntelliJ and it works if the inlay hints are shown, with the setting "Use
editor font for inlay hints" true (Settings / Editor / General / Appearance) and the editor font "JetBrains Mono" size
13.0 (Settings / Editor / Font).

## Project Status

- Active and in use.

## Changelog

See the [CHANGELOG](CHANGELOG.md) for the list of changes.

## Contributing

See the [CONTRIBUTING](CONTRIBUTING.md) page.

## Contacts

You can reach me in two ways:

- **Open an issue:** For bug reports, feature requests, or general questions,
  please [open a new issue](https://github.com/davide-vecchi/MoveWithShortcuts/issues/new) on GitHub.
- **Mention me:** If you need to communicate with me, mention my username (`@davide-vecchi`) in a comment.
  I will be notified.
  
## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
