/**
 * Created by OpenCode on 2026-05-08 .
 */
package rename_with_links;


import dutil.exception.UserRequestedTermination;
import dutil.exception.exceptions.InvalidExternalValueException;
import dutil.exception.exceptions.MissingExternalValueException;
import dutil.system.OSUtilities;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import mslinks.ShellLink;
import mslinks.ShellLinkException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;

import static dfile.file.FileUtilities.assertValidPath;
import static dfile.file.FileUtilities.getCanonicalPath;
import static duser_input_output.AUserInputOutput.calcCancelCharsPrompt;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.object.ObjectUtilities.assertTrue;
import static dutil.string.TextUtilities.COLON;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.SLASH;
import static dutil.string.TextUtilities.TAB;
import static dutil.string.TextUtilities.dq;
import static dutil.system.OSUtilities.WIN_SHORTCUT_EXTENSION;
import static org.apache.commons.lang3.StringUtils.EMPTY;


// @formatter:off


@Getter
@ToString
public final class RenameWithLinks {
  

  static final String CANCEL_CHARS = "Cc/";
  
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private final @NotNull AppContext appContext;
  
  
  private RenameWithLinks(@NotNull AppContext appContext) {
  
    this.appContext = assertNonNull(appContext);
  }
  
  
  public static RenameWithLinks newInstance(@NotNull AppContext appContext) {
  
    return new RenameWithLinks(appContext);
  }
  
  
  public void run() throws UserRequestedTermination, IOException {

    assertWindowsOS();

    final File existingFileOrFolder = readExistingPath();
    final File newFile = readNewFile(existingFileOrFolder);
    final File searchDir = readSearchDirectory();

    renameFileOrFolder(existingFileOrFolder, newFile);
    updateShortcuts(existingFileOrFolder, newFile, searchDir);
  }

  private static void assertWindowsOS() {

    assertTrue(SystemUtils.IS_OS_WINDOWS, "The OS is not Windows. Instead it is :" + NL2T + OSUtilities.getDescription());
  }

  private File readExistingPath() throws UserRequestedTermination {

    final String existingPath = this.appContext.userIO.in(
                                                  "Enter the path of the file or folder to rename / move, or type "
                                                             + calcCancelCharsPrompt(CANCEL_CHARS)
                                            , EMPTY, CANCEL_CHARS);
    if (existingPath == null) {

      this.appContext.warnUser(NL + "Terminating as requested by the user.");

      throw new UserRequestedTermination();
    }
    final File existingFileOrFolder = new File(existingPath);

    if (! existingFileOrFolder.exists()) {

      final String msg = dq(existingPath) + " does not exist.";

      throw new MissingExternalValueException(msg);
    }
    return existingFileOrFolder;
  }

  private File readNewFile(final File existingFileOrFolder) throws UserRequestedTermination {

    final String newName = this.appContext.userIO.in(
                                      "Enter the new name for the file or folder (may include a path), or type "
                                                + calcCancelCharsPrompt(CANCEL_CHARS)
                                , EMPTY, CANCEL_CHARS);
    if (newName == null) {

      this.appContext.warnUser(NL + "Terminating as requested by the user.");

      throw new UserRequestedTermination();
    }
    final String effectiveNewName;

    if (newName.contains(File.separator) || newName.contains(SLASH) || newName.contains(COLON)) {

      effectiveNewName = newName;
    }
    else {

      final File parent = existingFileOrFolder.getAbsoluteFile().getParentFile();

      if (parent == null) {

        throw new InvalidExternalValueException("Cannot determine parent directory of " + dq(getCanonicalPath(existingFileOrFolder)) + ".");
      }
      if (newName.equalsIgnoreCase(existingFileOrFolder.getName())) {

        throw new InvalidExternalValueException(dq(newName) + ": new name is identical to current name.");
      }
      effectiveNewName = new File(parent, newName).getPath();
    }
    return new File(assertValidPath(effectiveNewName, existingFileOrFolder.isDirectory()));
  }

  private File readSearchDirectory() throws UserRequestedTermination {

    final String searchPath = this.appContext.userIO.in("Enter the path to scan for " + WIN_SHORTCUT_EXTENSION
                                                                + " shortcuts to update, or type "
                                                                + calcCancelCharsPrompt(CANCEL_CHARS)
                                                 , EMPTY, CANCEL_CHARS);
    if (searchPath == null) {

      this.appContext.warnUser(NL + "Terminating as requested by the user.");

      throw new UserRequestedTermination();
    }
    final File searchDir = new File(assertValidPath(searchPath, true));

    if (! searchDir.exists() || ! searchDir.isDirectory()) {

      throw new InvalidExternalValueException(dq(searchPath) + " does not exist or is not a directory.");
    }
    return searchDir;
  }

  private void renameFileOrFolder(final File existingFileOrFolder, final File newFile) throws IOException {

    this.appContext.outUser(NL + "Renaming / moving " + dq(getCanonicalPath(existingFileOrFolder)) + " to " + dq(getCanonicalPath(newFile)) + "...");

    if (! newFile.getParentFile().exists()) {

      FileUtils.forceMkdir(newFile.getParentFile());
    }
    Files.move(existingFileOrFolder.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
  }

  private void updateShortcuts(final File existingFileOrFolder, final File newFile, final File searchDir) {

    final Collection<File> lnkFiles = FileUtils.listFiles(searchDir, new String[] { "lnk" }, true);

    this.appContext.outUser("Found " + lnkFiles.size() + " shortcut file(s) in " + dq(getCanonicalPath(searchDir)) + ". Checking their targets...");

    int numUpdated = ZERO_i;
    ShellLink sl = null;

    for (final File lnk : lnkFiles) {

      try {

        sl = new ShellLink(lnk);

        if (getCanonicalPath(existingFileOrFolder).equalsIgnoreCase(sl.resolveTarget())) {

          this.appContext.outUser_Chars("Updating target of " + dq(getCanonicalPath(lnk)) + " from " + dq(getCanonicalPath(existingFileOrFolder)) + " to " + dq(getCanonicalPath(newFile)) + "...");

          OSUtilities.updateTargetPath(lnk, getCanonicalPath(newFile));

          this.appContext.outUser(" done.");

          ++numUpdated;
        }
      }
      catch (final IOException | ShellLinkException e) {

        this.appContext.warnUser(NL + "Warning: could not read shortcut " + dq(getCanonicalPath(lnk)) + ": " + e.getMessage());

        this.appContext.warnUser(TAB + "Shortcut representation : " + sl);

        this.appContext.outUserLog(getFullDescriptionWithRootCause(e));
      }
    }
    this.appContext.outUser(NL2 + "Finished updating " + numUpdated + " shortcuts out of " + lnkFiles.size() + " .");
  }

}
