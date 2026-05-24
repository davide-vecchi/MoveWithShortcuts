/**
 * Created by OpenCode on 2026-05-08 .
 */
package rename_with_links;


import dfile.shortcut.IShortcutTargetUpdater;
import dutil.exception.UserRequestedTermination;
import dutil.exception.exceptions.InvalidExternalValueException;
import dutil.exception.exceptions.MissingExternalValueException;
import dutil.exception.exceptions.NonUniqueExternalValueException;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.StandardCopyOption;
import java.util.Collection;

import static dfile.file.FileUtilities.assertExistingFile;
import static dfile.file.FileUtilities.assertValidPath;
import static dfile.file.FileUtilities.getCanonicalPath;
import static dfile.file.FileUtilities.getCanonicalPathAsDescr;
import static dfile.file.FileUtilities.hasPath;
import static duser_input_output.AUserInputOutput.calcCancelCharsPrompt;
import static dutil.list.text.TextListUtilities.assertNoneBlankNorTrimmable;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NLT;
import static dutil.string.TextUtilities.dq;
import static dutil.system.OSUtilities.WIN_SHORTCUT_EXTENSION;
import static dutil.system.OSUtilities.assertWindowsOS;
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
  
  /**
   * @param shortcutTargetUpdater The {@link IShortcutTargetUpdater object} to {@link IShortcutTargetUpdater#updateTargetIfMatch
   *                              update} the target of a shortcut.
   *
   * @throws UserRequestedTermination
   * @throws IOException
   */
  public void run(@NotNull IShortcutTargetUpdater shortcutTargetUpdater) throws UserRequestedTermination, IOException {
    
    assertWindowsOS();

    final File originalFileOrFolder = askOriginalPath();

    final File destinationFileOrFolder = askDestinationFileOrFolder(originalFileOrFolder);

    final File searchDir = askSearchDirectory();
    
    renameFileOrFolder(originalFileOrFolder, destinationFileOrFolder);
    
    updateShortcuts(shortcutTargetUpdater, originalFileOrFolder, destinationFileOrFolder, searchDir);
  }

  /**
   * Runs the rename-and-update operation using the specified paths instead of prompting the user.
   *
   * @param shortcutTargetUpdater The {@link IShortcutTargetUpdater object} to {@link IShortcutTargetUpdater#updateTargetIfMatch
   *                              update} the target of a shortcut.<br>
   *
   * @param originalPath          The path of the file or folder to rename / move.<br>
   *
   * @param destinationPath       The new name/path for the file or folder (may include a path → move).<br>
   *
   * @param searchPath            The path to scan for {@code .lnk} shortcuts to update.
   *
   * @throws IOException                     If the rename/move operation fails.
   * @throws MissingExternalValueException   If the specified {@code originalPath} does not exist.
   * @throws InvalidExternalValueException   If {@code searchPath} does not exist or is not a directory.
   * @throws NonUniqueExternalValueException If the specified destination is the same as the original.
   */
  public void run(@NotNull IShortcutTargetUpdater shortcutTargetUpdater, @NotNull String originalPath
                , @NotNull String                 destinationPath,       @NotNull String searchPath) throws IOException {
    
    assertWindowsOS();
    
    assertNonNull(shortcutTargetUpdater, shortcutTargetUpdater.getClass().getSimpleName() + " updater");
    
    assertNoneBlankNorTrimmable(originalPath, destinationPath, searchPath);
    
    final File originalFileOrFolder = resolveOriginalPath(originalPath);

    final File destinationFileOrFolder = resolveDestinationFileOrFolder(destinationPath, originalFileOrFolder);

    final File searchDir = resolveSearchDirectory(searchPath);

    renameFileOrFolder(originalFileOrFolder, destinationFileOrFolder);

    updateShortcuts(shortcutTargetUpdater, originalFileOrFolder, destinationFileOrFolder, searchDir);
  }
  
  /**
   * TODO @@@@@@ COMMENT
   * @param path
   * @return
   */
  private static File resolveOriginalPath(final String path) {

    final File file = new File(path);

    if (! file.exists()) {

      throw new MissingExternalValueException(dq(path) + " does not exist.");
    }
    return file;
  }
  
  /**
   * TODO @@@@@@ COMMENT
   * @param destinationPath
   * @param original
   * @return
   */
  private static File resolveDestinationFileOrFolder(final String destinationPath, final File original) {

    final String effectiveNewName;

    if (hasPath(destinationPath)) {

      effectiveNewName = getCanonicalPath(destinationPath);
    }
    else {

      final File parent = original.getAbsoluteFile().getParentFile();

      if (parent == null) {

        throw new InvalidExternalValueException("Cannot determine parent folder of " + dq(getCanonicalPath(original)) + ".");
      }
      effectiveNewName = getCanonicalPath(new File(parent, destinationPath).getPath());
    }
    final File destination =  new File(effectiveNewName);

    if (destination.equals(original)) {

      throw new NonUniqueExternalValueException("The specified destination is the same as the original : " + dq(getCanonicalPath(original)) + ".");
    }
    return destination;
  }
  
  /**
   * TODO @@@@@@ COMMENT
   *
   * @param searchPath
   * @return
   */
  private static File resolveSearchDirectory(final String searchPath) {

    final File searchDir = assertExistingFile(new File(assertValidPath(searchPath, true)), true);

    assertExistingFile(searchDir, true);

    return searchDir;
  }

  /**
   * Asks the user for the path of the file or folder to rename / move.
   *
   * @throws UserRequestedTermination If the user responds to the question with one of the {@#link #CANCEL_CHARS}.
   *
   * @throws MissingExternalValueException If the path entered by the user does not exist.
   */
  private File askOriginalPath() throws UserRequestedTermination {

    final String existingPath = this.appContext.userIO.in(
                                                  "Enter the path of the file or folder to rename / move, or type "
                                                             + calcCancelCharsPrompt(CANCEL_CHARS)
                                            , EMPTY, CANCEL_CHARS);

    if (existingPath == null) {

      throw new UserRequestedTermination();
    }
    final File originalFileOrFolder = new File(existingPath);

    if (! originalFileOrFolder.exists()) {

      final String msg = dq(existingPath) + " does not exist.";

      throw new MissingExternalValueException(msg);
    }
    return originalFileOrFolder;
  }

  /**
   * Asks the user for the destination (file or folder) to which the given {@code original} is requested to be renamed /
   * moved. Must be of the same type (that is, file or folder) as the given {@code original}.<br>May include a different
   * path, which means the original is requested to be moved there.
   *
   * @param original The file or folder that was previously specified as to be renamed / moved.
   *
   * @return A {@link File} representing the destination (file or folder) to which the given {@code original} is
   *         requested to be renamed / moved.
   *
   * @throws UserRequestedTermination If the user responds to the question with one of the {@#link #CANCEL_CHARS}.
   *
   * @throws NonUniqueExternalValueException If the specified destination is the same as the given {@code original}.
   *
   * @throws InvalidPathException If the specified destination is a folder but the given {@code original} is a file, or
   *                              viceversa.
   */
  private File askDestinationFileOrFolder(File original) throws UserRequestedTermination {

    final String newName = this.appContext.userIO.in(
                                      "Enter the new name for the file or folder (may include a path), or type "
                                                + calcCancelCharsPrompt(CANCEL_CHARS)
                                , EMPTY, CANCEL_CHARS);

    if (newName == null) {
      
      throw new UserRequestedTermination();
    }
    final String effectiveNewName;
    
    if (hasPath(newName)) {
      
      // : The specified destination represents a file or folder with path info.
      
      effectiveNewName = getCanonicalPath(newName);
    }
    else {
      
      // : The specified destination represents a file or folder without path info.
      
      final File parent = original.getAbsoluteFile().getParentFile();
      
      if (parent == null) {
        
        throw new InvalidExternalValueException("Cannot determine parent folder of " + dq(getCanonicalPath(original)) + ".");
      }
      effectiveNewName = getCanonicalPath(new File(parent, newName).getPath());
    }
    final File destination = new File(effectiveNewName);
    
    if (destination.equals(original)) {
      
      throw new NonUniqueExternalValueException("The specified destination is the same as the original : " + dq(getCanonicalPath(original)) + ".");
    }
    return new File(assertValidPath(effectiveNewName, original.isDirectory()));
  }

  /**
   * Asks the user for the path to scan for {@code .lnk} shortcuts to update.
   */
  private File askSearchDirectory() throws UserRequestedTermination {

    final String searchPath = this.appContext.userIO.in("Enter the path to scan for " + WIN_SHORTCUT_EXTENSION
                                                                + " shortcuts to update, or type "
                                                                + calcCancelCharsPrompt(CANCEL_CHARS)
                                                 , EMPTY, CANCEL_CHARS);

    if (searchPath == null) {

      throw new UserRequestedTermination();
    }
    final File searchDir = new File(assertValidPath(searchPath, true));
    
    assertExistingFile(searchDir, true);
    
    return searchDir;
  }

  /**
   * Renames / moves the file or folder from the existing path to the new path.
   */
  private void renameFileOrFolder(File originalFileOrFolder, File destinationFileOrFolder) throws IOException {

    this.appContext.outUser(NL + "Renaming / moving " + dq(getCanonicalPath(originalFileOrFolder)) + " to " + dq(getCanonicalPath(destinationFileOrFolder)) + "...");

    if (! destinationFileOrFolder.getParentFile().exists()) {

      FileUtils.forceMkdir(destinationFileOrFolder.getParentFile());
    }
    Files.move(originalFileOrFolder.toPath(), destinationFileOrFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
    
    this.appContext.outUser("Done.");
  }

  /**
   * Recursively scans the search directory for {@code .lnk} files and, for each shortcut whose target matches the
   * original path, updates it to the new path.
   */
  private void updateShortcuts(@NotNull IShortcutTargetUpdater shortcutTargetUpdater,   File originalFileOrFolder
                             , @NotNull File                   destinationFileOrFolder, File searchFolder) {

    final Collection<File> shortcuts = FileUtils.listFiles(searchFolder, new String[] { "lnk" }
                                                        , true);
    this.appContext.outUser();
    
    this.appContext.outUser("Found " + shortcuts.size() + " shortcut file(s) in " + dq(getCanonicalPath(searchFolder)) + ". Checking their targets..." + NL);
    
    int numUpdated = ZERO_i;

    for (final File shortcut : shortcuts) {
      
      this.appContext.outUser_Chars("Processing " + getCanonicalPathAsDescr(shortcut) + " ..." + NLT);
      
      shortcutTargetUpdater.updateTargetIfMatch(shortcut
                                   , originalFileOrFolder,     destinationFileOrFolder
                                     , this.appContext::outUser,this.appContext::warnUser
                                     ,this.appContext::errUser);
    }
    this.appContext.outUser(NL + "Finished updating " + numUpdated + " shortcuts out of " + shortcuts.size() + " .");
  }

}
