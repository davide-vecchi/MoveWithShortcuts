# RenameWithLinks
Java program for Windows only, that renames and / or moves files and folders and updates all the shortcuts (<code>.lnk
</code> files) that were pointing to the moved elements, so that the shortcuts' target and working directory point to
the new location instead of becoming broken.

## Prerequisites

To build this project, you need a GitHub Personal Access Token (PAT) with `read:packages` scope in your `~/.m2/settings.xml`;
if you don't already have one, you can create it following these 2 steps :

1. Create a token at: https://github.com/settings/tokens
    - Select `read:packages` scope
    - Copy the token value


2. Add the token to your Maven `settings.xml` file (located at `~/.m2/settings.xml`):

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

When you build the project, e.g. using

```bash
mvn clean install
```

, Maven will download the required `DLibs` dependencies from GitHub Packages using your token.


## Requires
- At least Java 21.
- TestNG to run the tests.
- See dependencies in pom.xml .

### INDENTATION

The indentation of the Java code is tuned to IntelliJ and it works if the inlay hints are shown, with the setting "Use
editor font for inlay hints" true (Settings / Editor / General / Appearance) and the editor font "JetBrains Mono" size
13.0 (Settings / Editor / Font).

## Project Status
- Active and in use.

## Contacts
- ciustea@lorettastan.eu
