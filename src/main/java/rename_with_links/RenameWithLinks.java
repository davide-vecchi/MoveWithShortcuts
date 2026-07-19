/**
 * Created by OpenCode on 2026-05-08 .
 */
package rename_with_links;


import application.AAppContext;
import dfile.file.FileUtilities;
import dfile.shortcut.IShortcutsTargetUpdater;
import dfile.shortcut.IShortcutsTargetUpdater.ShortcutsUpdateOutcome;
import dutil.exception.UserRequestedTermination;
import dutil.exception.exceptions.InvalidExternalValueException;
import dutil.exception.exceptions.MissingExternalValueException;
import dutil.exception.exceptions.NonUniqueExternalValueException;
import dutil.system.OSUtilities;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

import static application.AAppContext.MAX_VERBOSITY;
import static dfile.file.FileUtilities.assertExistingPath;
import static dfile.file.FileUtilities.getCanonicalPath;
import static dfile.file.FileUtilities.getCurrentFolder;
import static dfile.file.FileUtilities.hasPath;
import static dfile.file.FileUtilities.newValidatedFile;
import static dfile.file.FileUtilities.newValidatedFileOrFolder;
import static dfile.file.FileUtilities.newValidatedFolder;
import static duser_input_output.AUserInputOutput.calcCancelCharsPrompt;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.jar.JARUtilities.getClasspathMsg;
import static dutil.list.ListUtilities.assertNoneNull;
import static dutil.list.number.NumberListUtilities.assertNoneNegative;
import static dutil.list.text.TextListUtilities.assertNoneBlankNorTrimmable;
import static dutil.number.NumberUtilities.I;
import static dutil.number.NumberUtilities.MINUS1_i;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.number.NumberUtilities.assertNonNegative;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.string.TextUtilities.FMT0DG;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.NLT;
import static dutil.string.TextUtilities.S;
import static dutil.string.TextUtilities.assertNonBlankNorTrimmable;
import static dutil.string.TextUtilities.assertNonBlankUnlessNull;
import static dutil.string.TextUtilities.dq;
import static dutil.system.OSUtilities.WIN_SHORTCUT_EXTENSION;
import static dutil.system.OSUtilities.assertWindowsOS;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.io.FilenameUtils.EXTENSION_SEPARATOR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.removeStart;


// @formatter:off

/**
 * Renames and / or moves files and folders and updates all the shortcuts that were pointing to them, so that the
 * shortcuts point to the moved location instead of becoming broken.
 */
@Getter
@ToString
public class RenameWithLinks {
  
  
  /**
   * List of chars that the user can enter when answering a question in order to signify "Abort".
   */
  static final String CANCEL_CHARS = "Cc/";
  
  /**
   * Whether after retrieving the shortcuts to possibly update, a confirmation must be requested to the user before
   * starting to process and possibly update the shortcuts. Default {@code true}. Typically set to {@code false} by
   * tests.
   */
  boolean askBeforeProcessingShortcuts = true;
  
  /**
   * The total number of shortcuts that will be checked to see if they need to have their target updated.
   */
  int numTotalShortcuts = MINUS1_i;
  
  /**
   * The total number of shortcuts that have been checked to see if they need to have their target updated.
   */
  int numProcessedShortcuts = MINUS1_i;
  
  /**
   * The total number of shortcuts whose target was updated.
   */
  int numUpdatedShortcuts = MINUS1_i;
  
  /**
   * The total number of shortcuts that were skipped because they could not be processed.
   */
  int numSkippedShortcuts = MINUS1_i;
  
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private final @NotNull AAppContext appContext;
  
  
  /**
   * Private constructor.
   *
   * @param appContext {@link #appContext}.
   */
  private RenameWithLinks(@NotNull AAppContext appContext) {
  
    this.appContext = assertNonNull(appContext);
  }
  
  
  /**
   * Factory method.
   *
   * @param appContext {@link #appContext}.
   */
  public static RenameWithLinks newInstance(@NotNull AAppContext appContext) {
  
    return new RenameWithLinks(appContext);
  }
  
  /**
   * Invoked if the program has been started without args.<br>
   * Runs the rename-and-update operation first asking to the user the values corresponding to the program args.
   *
   * @param shortcutsTargetUpdater The {@link IShortcutsTargetUpdater object} to {@link IShortcutsTargetUpdater#updateShortcuts(List, File, File)
   *                               update} the target of shortcuts.
   *
   * @throws IOException                     If the rename / move operation fails.<br>
   *
   * @throws MissingExternalValueException   If the specified {@link #askOriginalPath original path} does not exist.<br>
   *
   * @throws InvalidExternalValueException   If the specified {@link #askSearchDirectory()} search path} does not exist
   *                                         or is not a folder.<br>
   *
   * @throws NonUniqueExternalValueException If the specified {@link #askDestinationFileOrFolder destination file or
   *                                         folder} is the same as the specified {@link #askOriginalPath original path}.
   */
  public void run(@NotNull IShortcutsTargetUpdater shortcutsTargetUpdater) throws UserRequestedTermination, IOException {
    
    assertWindowsOS();
    
    assertNonNull(shortcutsTargetUpdater, shortcutsTargetUpdater.getClass().getSimpleName() + " updater");
    
    final File originalFileOrFolder =    askOriginalPath();
    
    final File destinationFileOrFolder = askDestinationFileOrFolder(originalFileOrFolder);
    
    final File searchFolder =            askSearchDirectory();
    
    this.appContext.currentVerbosity =   askVerbosity();
    
    if (askStartConfirmation(originalFileOrFolder, destinationFileOrFolder, searchFolder)) {
      
      // Perform the renaming / moving and the corresponding shortcuts updating :
      
      execute(originalFileOrFolder, destinationFileOrFolder, searchFolder, shortcutsTargetUpdater);
    }
  }

  /**
   * Runs the rename-and-update operation using the specified paths instead of prompting the user.
   *
   * @param shortcutsTargetUpdater The {@link IShortcutsTargetUpdater object} to {@link IShortcutsTargetUpdater#updateShortcuts(List, File, File)}
   *                               update} the target of shortcuts.
   *
   * @param argOriginalPath       The argument given for the path of the file or folder to rename / move.<br>
   *
   * @param argDestinationPath    The argument given for the new name/path for the file or folder (may include a path
   *                              → move).<br>
   *
   * @param argSearchPath         The argument given for the path to scan for {@code .lnk} shortcuts to update.
   *
   * @param argVerbosity          The argument given for the verbosity level. May be {@code null}, defaults to 0 .
   *
   * @throws IOException                     If the rename / move operation fails.<br>
   *
   * @throws MissingExternalValueException   If the specified {@code originalPath} does not exist.<br>
   *
   * @throws InvalidExternalValueException   If {@code searchPath} does not exist or is not a directory.<br>
   *
   * @throws NonUniqueExternalValueException If the specified {@code argDestinationPath} is the same as {@code
   *                                         originalPath}.
   */
  public void run(@NotNull IShortcutsTargetUpdater shortcutsTargetUpdater, @NotNull String argOriginalPath
                , @NotNull String                  argDestinationPath,     @NotNull String argSearchPath
                ,          String                  argVerbosity) throws IOException, UserRequestedTermination {
    
    assertWindowsOS();
    
    assertNonNull(shortcutsTargetUpdater, shortcutsTargetUpdater.getClass().getSimpleName() + " updater");
    
    assertNoneBlankNorTrimmable(argOriginalPath, argDestinationPath, argSearchPath);
    
    assertNonBlankUnlessNull(argVerbosity);
    
    final File originalFileOrFolder =    resolveOriginalPath(  argOriginalPath);

    final File destinationFileOrFolder = resolveDestinationFileOrFolder(argDestinationPath, originalFileOrFolder);

    final File searchFolder =            resolveSearchDirectory(        argSearchPath);
    
    if (askStartConfirmation(originalFileOrFolder, destinationFileOrFolder, searchFolder)) {
      
      // Perform the renaming / moving and the corresponding shortcuts updating :
      
      execute(originalFileOrFolder, destinationFileOrFolder, searchFolder, shortcutsTargetUpdater);
    }
  }
  
  /**
   * Asks for confirmation to the user to start the execution, and if granted executes the program logic: {@link #renameFileOrFolder
   * renames / moves} the given {@code originalFileOrFolder} to the given {@code destinationFileOrFolder} and {@link #updateShortcuts
   * updates} accordingly all the given {@code shortcuts}, using the given {@code shortcutsTargetUpdater}.
   *
   * @param originalFileOrFolder    The file or folder to rename / move.<br>
   *
   * @param destinationFileOrFolder The new file path / name to which to rename / move the {@code originalFileOrFolder}.<br>
   *
   * @param searchFolder            The folder under which, <b>after</b> {@link #renameFileOrFolder performing} the
   *                                rename / move, the existing shortcuts must be possibly have their targets updated.
   *
   * @param shortcutsProcessor      The {@link IShortcutsTargetUpdater shortcuts processor} to use to perform the updates of
   *                                the shortcuts' targets that need it.
   */
  void execute(@NotNull File                    originalFileOrFolder
             , @NotNull File                    destinationFileOrFolder
             , @NotNull File                    searchFolder
             , @NotNull IShortcutsTargetUpdater shortcutsTargetUpdater) throws IOException, UserRequestedTermination {
    
    assertNoneNull(destinationFileOrFolder, shortcutsTargetUpdater);
    
    assertExistingPath(originalFileOrFolder, null);
    
    assertExistingPath(searchFolder, TRUE);
    
    this.numProcessedShortcuts = ZERO_i;
    
    this.numUpdatedShortcuts =   ZERO_i;
    
    this.numSkippedShortcuts =   ZERO_i;
    
    try {
      
      // Do the requested renaming / moving :
      
      renameFileOrFolder(originalFileOrFolder, destinationFileOrFolder);
      
      // Retrieve all the shortcuts that need to be checked and possibly updated :
      
      final List<File> shortcuts = retrieveShortcutFiles(searchFolder);
      
      this.numTotalShortcuts = shortcuts.size();
      
      final boolean userAborted =
            this.askBeforeProcessingShortcuts
         && this.appContext.userIO.in("Press Enter "
                                                + shortcutsTargetUpdater.getPromptTextStartProcessingShortcuts(
                                                                                 I(shortcuts.size()))
                                                + " or type " + calcCancelCharsPrompt(CANCEL_CHARS)
                               , EMPTY, CANCEL_CHARS) == null;
      
      if (! userAborted) {
        
        this.appContext.outUser(NL + "The processing of the " + FMT0DG.format(shortcuts.size()) +
                                             " shortcut file(s) has started, at " + new Date() + " ." + NL);
        
        // Update all the shortcuts that point to the location as it was before the renaming / moving :
        
        final ShortcutsUpdateOutcome outcome = shortcutsTargetUpdater.updateShortcuts(shortcuts
                                                                    , originalFileOrFolder
                                                                    , destinationFileOrFolder);
        if (outcome.userInterrupted()) {
          
          throw new UserRequestedTermination("Program terminated upon user's request during shortcuts targets update.");
        }
        this.numProcessedShortcuts = outcome.numProcessedShortcuts();
        
        this.numUpdatedShortcuts =   outcome.numUpdatedShortcuts();
        
        this.numSkippedShortcuts =   outcome.numSkippedShortcuts();
      }
    }
    catch (Exception e) {
    
      this.appContext.outUserLog(NL2 + e.getLocalizedMessage());
      
      this.appContext.outUserLog(NL2 + getFullDescriptionWithRootCause(e));
      
      this.appContext.outDevLog( NL2 + getClasspathMsg());
      
      throw e;
    }
  }
  
  /**
   * Asks for confirmation to {@link #renameFileOrFolder rename} the given {@code originalFileOrFolder} to the given {@code
   * destinationFileOrFolder} and to {@link #updateShortcuts update} accordingly all the shortcuts found under the given {@code
   * searchDir}.
   *
   * @return Whether the user confirmed that the processing can start.
   */
  private boolean askStartConfirmation(@NotNull File originalFileOrFolder, @NotNull File destinationFileOrFolder
                                     , @NotNull File searchFolder) {
    
    assertNoneNull(originalFileOrFolder, destinationFileOrFolder, searchFolder);
    
    boolean confirmed = this.appContext.userIO.in("Press Enter to confirm renaming / moving "
                                                          + (originalFileOrFolder.isFile() ? "file" : "folder")    + NL2T
                                                          + dq(getCanonicalPath(originalFileOrFolder))      + NL2T + "to" + NL2T
                                                          + dq(getCanonicalPath(destinationFileOrFolder))   + NL2T
                                                          + "and updating the targets and/or the working directories of the shortcuts pointing to it that are found under" + NL2T
                                                          + dq(getCanonicalPath(searchFolder)) + NL2T
                                                          + ", or type " + calcCancelCharsPrompt(CANCEL_CHARS)
                                           , EMPTY, CANCEL_CHARS) != null;
    return confirmed;
  }
  
  /**
   * TODO @@@@@@ COMMENT
   * @param argPath
   * @return
   */
  private static @NotNull File resolveOriginalPath(@NotBlank String argPath) {

  return newValidatedFileOrFolder(argPath, true);
  }
  
  /**
   * TODO @@@@@@ COMMENT
   * @param argDestinationPath
   * @param original
   * @return
   */
  private static @NotNull File resolveDestinationFileOrFolder(@NotBlank String argDestinationPath
                                                            , @NotNull  File   original) {
    final String effectiveNewName;

    if (hasPath(assertNonBlankNorTrimmable(argDestinationPath))) {

      effectiveNewName = argDestinationPath;
    }
    else {

      final File parent = original.getAbsoluteFile().getParentFile();

      if (parent == null) {

        throw new InvalidExternalValueException("Cannot determine parent folder of " + dq(getCanonicalPath(original)) + ".");
      }
      effectiveNewName = new File(parent, argDestinationPath).getPath();
    }
    final File destination = original.isFile() ?
                             newValidatedFile(    effectiveNewName, false, MINUS1_i)
                             :
                             newValidatedFolder(effectiveNewName, false, null);
    
    if (destination.equals(original)) {
      
      throw new NonUniqueExternalValueException("The specified destination is the same as the original : " + dq(destination.getPath()) + ".");
    }
    return destination;
  }
  
  /**
   * TODO @@@@@@ COMMENT
   *
   * @param argSearchPath
   * @return
   */
  private static @NotNull File resolveSearchDirectory(@NotBlank String argSearchPath) {

    return newValidatedFolder(argSearchPath, true, FALSE);// new File(assertExistingPath(argSearchPath, TRUE)).getCanonicalFile();
  }
  
  /**
   * TODO @@@@@@ COMMENT
   *
   * @param verbosity
   * @param maxVerbosity
   * @param defaultVerbosity
   * @return
   */
  static int resolveVerbosity(final String argVerbosity, int maxVerbosity, int defaultVerbosity) {
    
    assertNoneNegative(maxVerbosity, defaultVerbosity, maxVerbosity - defaultVerbosity);
    
    final int verbosity;
    
    if (argVerbosity != null) {
      
      verbosity = I(assertNonBlankNorTrimmable(argVerbosity)).intValue();
      
      if (verbosity > maxVerbosity) {
        
        throw new InvalidExternalValueException("Invalid verbosity value " + verbosity + ", max is " + maxVerbosity + " .");
      }
    }
    else {
      
      verbosity = defaultVerbosity;
    }
    return assertNonNegative(verbosity);
  }

  /**
   * Asks the user for the path of the file or folder to rename / move.
   *
   * @throws UserRequestedTermination If the user responds to the question with one of the {@#link #CANCEL_CHARS}.
   *
   * @throws MissingExternalValueException If the path entered by the user does not exist.
   */
  private @NotNull File askOriginalPath() throws UserRequestedTermination, IOException {

    final String existingPath = this.appContext.userIO.in(
                                                  "Enter the path of the file or folder to rename / move, either absolute"
                                                          + " or relative to the current folder (" + dq(getCurrentFolder()) + "),"
                                                          + " or type " + calcCancelCharsPrompt(CANCEL_CHARS)
                                           , EMPTY, CANCEL_CHARS);
    if (existingPath == null) {
      
      throw new UserRequestedTermination();
    }
    return resolveOriginalPath(existingPath);
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
  private @NotNull File askDestinationFileOrFolder(@NotNull File original) throws UserRequestedTermination, IOException {

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
    return resolveDestinationFileOrFolder(effectiveNewName, original);// new File(assertValidPath(effectiveNewName, B(original.isDirectory())));
  }

  /**
   * Asks the user for the path to scan for {@code .lnk} shortcuts to update.
   */
  private @NotNull File askSearchDirectory() throws UserRequestedTermination {
    
    final String searchPath = this.appContext.userIO.in(
                                                "Enter the path of the folder to scan for " + WIN_SHORTCUT_EXTENSION
                                                        + " shortcuts to update (if the \"new name\" entered in the previous question included a path and that causes the folder tree to change, this path to scan must refer to the new folder tree)"
                                                        + " or press Enter for the default, or type " + calcCancelCharsPrompt(CANCEL_CHARS)
                                         , getCurrentFolder(), CANCEL_CHARS);
    if (searchPath == null) {

      throw new UserRequestedTermination();
    }
    return resolveSearchDirectory(searchPath);
  }
  
  /**
   * Asks the user for the {@link AAppContext#currentVerbosity verbosity level}.
   */
  private int askVerbosity() throws UserRequestedTermination {
    
    final String verbosity = this.appContext.userIO.in(
                                               "Enter the verbosity level (0 - " + MAX_VERBOSITY
                                                     + "), or press Enter for the default,"
                                                     + " or type " + calcCancelCharsPrompt(    CANCEL_CHARS)
                                        , S(this.appContext.currentVerbosity), CANCEL_CHARS);
    if (verbosity == null) {
      
      throw new UserRequestedTermination();
    }
    return this.appContext.assertValidVerbosity(I(verbosity).intValue());
  }

  /**
   * Renames / moves the file or folder from the existing path to the new path.
   */
  private void renameFileOrFolder(File originalFileOrFolder, File destinationFileOrFolder) throws IOException {
    
    this.appContext.outUser(ONE_i, NL + "Renaming / moving" + NL2T + dq(getCanonicalPath(originalFileOrFolder)) + NL2T + "to" + NL2T + dq(getCanonicalPath(destinationFileOrFolder)) + NL2T + "...");

    if (! destinationFileOrFolder.getParentFile().exists()) {

      FileUtils.forceMkdir(destinationFileOrFolder.getParentFile());
    }
    Files.move(originalFileOrFolder.toPath(), destinationFileOrFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
    
    this.appContext.outUser(ONE_i, "Done.");
  }
  
  /**
   * Retrieves all files under the given {@code searchFolder} tree that have the {@link OSUtilities#WIN_SHORTCUT_EXTENSION
   * extension} of Windows shortcut files.
   *
   * @param searchFolder
   * @return
   */
  @NotNull List<File> retrieveShortcutFiles(@NotNull File searchFolder) {
    
    final String searchFolderPath = getCanonicalPath(searchFolder);
    
    this.appContext.outUser(ONE_i, NL   + "Retrieving shortcuts to check for needed target update, under folder"
                                                   + NL2T + dq(searchFolderPath) + " ...");
    
    final List<File> shortcuts = FileUtilities.listFiles(
                        Paths.get(searchFolderPath)
                       , new String[] { removeStart(WIN_SHORTCUT_EXTENSION, EXTENSION_SEPARATOR) }
                 , true);
    
    this.numTotalShortcuts =   shortcuts.size();
    
    this.appContext.outUser(ONE_i, NLT + "Found " + FMT0DG.format(this.numTotalShortcuts) + " shortcut file(s) under"
                                                   + NL2T + dq(searchFolderPath) + ".");
    return shortcuts;
  }

}
