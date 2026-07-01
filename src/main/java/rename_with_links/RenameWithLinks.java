/**
 * Created by OpenCode on 2026-05-08 .
 */
package rename_with_links;


import dfile.file.FileUtilities;
import dfile.shortcut.IShortcutTargetUpdater;
import dfile.shortcut.IShortcutTargetUpdater.TargetUpdateOutcome;
import dutil.exception.UserRequestedTermination;
import dutil.exception.exceptions.InvalidExternalValueException;
import dutil.exception.exceptions.MissingExternalValueException;
import dutil.exception.exceptions.NonUniqueExternalValueException;
import dutil.io.IOUtilities;
import dutil.system.OSUtilities;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Function;

import static application.AAppContext.MAX_VERBOSITY;
import static dfile.file.FileUtilities.assertExistingPath;
import static dfile.file.FileUtilities.assertValidPath;
import static dfile.file.FileUtilities.getCanonicalPath;
import static dfile.file.FileUtilities.getCanonicalPathAsDescr;
import static dfile.file.FileUtilities.getCurrentFolder;
import static dfile.file.FileUtilities.hasPath;
import static duser_input_output.AUserInputOutput.calcCancelCharsPrompt;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.list.ListUtilities.assertNoneNull;
import static dutil.list.number.NumberListUtilities.assertNoneNegative;
import static dutil.list.text.TextListUtilities.assertNoneBlankNorTrimmable;
import static dutil.number.NumberUtilities.I;
import static dutil.number.NumberUtilities.MINUS1_i;
import static dutil.number.NumberUtilities.ONE_d;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.ONE_l;
import static dutil.number.NumberUtilities.TWO_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.number.NumberUtilities.assertNonNegative;
import static dutil.number.NumberUtilities.percent;
import static dutil.object.ObjectUtilities.B;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.string.TextUtilities.FMT0D;
import static dutil.string.TextUtilities.FMT0DG;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.NL2T2;
import static dutil.string.TextUtilities.NLT;
import static dutil.string.TextUtilities.NLT2;
import static dutil.string.TextUtilities.assertNonBlankNorTrimmable;
import static dutil.string.TextUtilities.assertNonBlankUnlessNull;
import static dutil.string.TextUtilities.dq;
import static dutil.system.OSUtilities.WIN_SHORTCUT_EXTENSION;
import static dutil.system.OSUtilities.assertWindowsOS;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.io.FilenameUtils.EXTENSION_SEPARATOR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.leftPad;
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
  private final @NotNull AppContext appContext;
  
  
  private RenameWithLinks(@NotNull AppContext appContext) {
  
    this.appContext = assertNonNull(appContext);
  }
  
  
  public static RenameWithLinks newInstance(@NotNull AppContext appContext) {
  
    return new RenameWithLinks(appContext);
  }
  
  /**
   * Invoked if the program has been started without args.<br>
   * Runs the rename-and-update operation first asking to the user the values corresponding to the program args.
   *
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
    
    final File searchFolder = askSearchDirectory();
    
    this.appContext.currentVerbosity = askVerbosity();
    
    if (askStartConfirmation(originalFileOrFolder, destinationFileOrFolder, searchFolder)) {
      
      // Retrieve all the shortcuts that need to be checked and possibly updated :
      
      final List<File> shortcuts = retrieveShortcutFiles(searchFolder);
      
      // Perform the renaming / moving and the corresponding shortcuts updating :
      
      execute(originalFileOrFolder, destinationFileOrFolder, shortcuts, shortcutTargetUpdater);
    }
  }

  /**
   * Runs the rename-and-update operation using the specified paths instead of prompting the user.
   *
   * @param shortcutTargetUpdater The {@link IShortcutTargetUpdater object} to {@link IShortcutTargetUpdater#updateTargetIfMatch
   *                              update} the target of a shortcut.<br>
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
   * @throws IOException                     If the rename/move operation fails.
   * @throws MissingExternalValueException   If the specified {@code originalPath} does not exist.
   * @throws InvalidExternalValueException   If {@code searchPath} does not exist or is not a directory.
   * @throws NonUniqueExternalValueException If the specified destination is the same as the original.
   */
  public void run(@NotNull IShortcutTargetUpdater shortcutTargetUpdater, @NotNull String argOriginalPath
                , @NotNull String                 argDestinationPath,    @NotNull String argSearchPath
                ,          String                 argVerbosity) throws IOException {
    
    assertWindowsOS();
    
    assertNonNull(shortcutTargetUpdater, shortcutTargetUpdater.getClass().getSimpleName() + " updater");
    
    assertNoneBlankNorTrimmable(argOriginalPath, argDestinationPath, argSearchPath);
    
    assertNonBlankUnlessNull(argVerbosity);
    
    final File originalFileOrFolder = resolveOriginalPath(argOriginalPath);

    final File destinationFileOrFolder = resolveDestinationFileOrFolder(argDestinationPath, originalFileOrFolder);

    final File searchFolder = resolveSearchDirectory(argSearchPath);
    
    if (askStartConfirmation(originalFileOrFolder, destinationFileOrFolder, searchFolder)) {
      
      // Retrieve all the shortcuts that need to be checked and possibly updated :
      
      final List<File> shortcuts = retrieveShortcutFiles(searchFolder);
      
      // Perform the renaming / moving and the corresponding shortcuts updating :
      
      execute(originalFileOrFolder, destinationFileOrFolder, shortcuts, shortcutTargetUpdater);
    }
  }
  
  /**
   * {@link #askStartConfirmation Asks for confirmation} to the user to start the execution, and if granted executes the
   * program logic: {@link #renameFileOrFolder renames} the given {@code originalFileOrFolder} to the given {@code
   * destinationFileOrFolder} and {@link #updateShortcuts updates} accordingly all the shortcuts found under the given {@code
   * searchDir}, using the given {@code shortcutTargetUpdater updater}.
   *
   * @param originalFileOrFolder    The file or folder to rename / move.<br>
   *
   * @param destinationFileOrFolder The new file path / name to which to rename / move the {@code originalFileOrFolder}.<br>
   *
   * @param searchFolder            The folder under which to search for shortcut files whose target needs updating.<br>
   *
   * @param shortcutsProcessor      The {@link IShortcutsTargetUpdater shortcuts processor} to use to perform the updates of
   *                                the shortcuts' targets that need it.
   */
  void execute(@NotNull File                originalFileOrFolder
             , @NotNull File                destinationFileOrFolder
             , @NotNull List<File>          shortcuts
             , @NotNull IShortcutsTargetUpdater shortcutTargetUpdater) throws IOException {
    
    // Do the requested renaming / moving :
    
    renameFileOrFolder(originalFileOrFolder, destinationFileOrFolder);
    
    final boolean userAborted =  this.askBeforeProcessingShortcuts
                           && this.appContext.userIO.in("Press Enter to start processing the " + FMT0DG.format(shortcuts.size())
                                                              + " shortcut file(s)" + NL2T
                                                              + "(the processing can be paused with the Enter key)"
                                                              + " or type " + calcCancelCharsPrompt(CANCEL_CHARS)
                                               , EMPTY, CANCEL_CHARS) == null;
    if (! userAborted) {
      
      // Update all the shortcuts that point to the location as it was before the renaming / moving :
      
      / // @@@@ q
      shortcutsProcessor.updateShortcuts(shortcuts, originalFileOrFolder, destinationFileOrFolder);
      
      updateShortcuts(shortcutTargetUpdater, originalFileOrFolder, destinationFileOrFolder
                    , shortcuts);
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
                                                          + "and updating the targets of the shortcuts pointing to it that are found under" + NL2T
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
  private static @NotNull File resolveOriginalPath(@NotBlank String argPath) throws IOException {

    final File file = new File(assertNonBlankNorTrimmable(argPath));

    if (! file.exists()) {

      throw new MissingExternalValueException(dq(argPath) + " does not exist.");
    }
    return file.getCanonicalFile();
  }
  
  /**
   * TODO @@@@@@ COMMENT
   * @param argDestinationPath
   * @param original
   * @return
   */
  private static @NotNull File resolveDestinationFileOrFolder(@NotBlank String argDestinationPath
                                                            , @NotNull  File   original) throws IOException {
    final String effectiveNewName;

    if (hasPath(assertNonBlankNorTrimmable(argDestinationPath))) {

      effectiveNewName = getCanonicalPath(argDestinationPath);
    }
    else {

      final File parent = original.getAbsoluteFile().getParentFile();

      if (parent == null) {

        throw new InvalidExternalValueException("Cannot determine parent folder of " + dq(getCanonicalPath(original)) + ".");
      }
      effectiveNewName = getCanonicalPath(new File(parent, argDestinationPath).getPath());
    }
    final File destination =  new File(effectiveNewName);

    if (destination.equals(original)) {

      throw new NonUniqueExternalValueException("The specified destination is the same as the original : " + dq(getCanonicalPath(original)) + ".");
    }
    return destination.getCanonicalFile();
  }
  
  /**
   * TODO @@@@@@ COMMENT
   *
   * @param argSearchPath
   * @return
   */
  private static @NotNull File resolveSearchDirectory(@NotBlank String argSearchPath) throws IOException {

    final File searchDir = assertExistingPath(new File(assertValidPath(argSearchPath
                                                                                  , TRUE))
                                               , TRUE);

    assertExistingPath(searchDir, TRUE);
    
    return searchDir.getCanonicalFile();
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
  private @NotNull File askOriginalPath() throws UserRequestedTermination {

    final String existingPath = this.appContext.userIO.in(
                                                  "Enter the path of the file or folder to rename / move, either absolute"
                                                          + " or relative to the current folder (" + dq(getCurrentFolder()) + "),"
                                                          + " or type " + calcCancelCharsPrompt(CANCEL_CHARS)
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
  private @NotNull File askDestinationFileOrFolder(@NotNull File original) throws UserRequestedTermination {

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
    return new File(assertValidPath(effectiveNewName, B(original.isDirectory())));
  }

  /**
   * Asks the user for the path to scan for {@code .lnk} shortcuts to update.
   */
  private @NotNull File askSearchDirectory() throws UserRequestedTermination {
    
    final String searchPath = this.appContext.userIO.in(
                                                "Enter the path to scan for " + WIN_SHORTCUT_EXTENSION
                                                        + " shortcuts to update (if the new name entered in the previous question included a path and that caused the folder tree to change, this path to scan must refer to the new folder tree)"
                                                        + " or press Enter for the default,"
                                                        + "Or, type " + calcCancelCharsPrompt(CANCEL_CHARS)
                                         , getCurrentFolder(), CANCEL_CHARS);
    if (searchPath == null) {

      throw new UserRequestedTermination();
    }
    final File searchDir = new File(assertValidPath(searchPath, TRUE));
    
    assertExistingPath(searchDir, TRUE);
    
    return searchDir;
  }
  
  /**
   * Asks the user for the {@link AppContext#currentVerbosity verbosity level}.
   */
  private int askVerbosity() throws UserRequestedTermination {
    
    final String verbosity = this.appContext.userIO.in(
                                               "Enter the verbosity level (0 - " + MAX_VERBOSITY
                                                     + "), or press Enter for the default,"
                                                     + " or type " + calcCancelCharsPrompt(CANCEL_CHARS)
                                        , "1", CANCEL_CHARS);
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
   * For each of the given {@code shortcuts}, it its target matches the original path, updates it to the new path.
   *
   * @param shortcutTargetUpdater The object to use to {@link IShortcutTargetUpdater#updateTargetIfMatch update} the
   *                              target of the shortcuts found under {@code searchFolder} tree that have it {@link FileUtilities#isDescendant
   *                              matching} {@code oldParentTarget}.<br>
   *
   * @param oldParentTarget      See {@code oldParentTarget} param of {@link
   *                             IShortcutTargetUpdater#updateTargetIfMatch(File, File, File, Function, Function, Function)}.<br>
   *
   * @param newParentTarget      See {@code newParentTarget} param of {@link
   *                             IShortcutTargetUpdater#updateTargetIfMatch(File, File, File, Function, Function, Function)}.<br>
   *
   * @param searchFolder          The folder under which to recursively search for shortcuts to update. If {@code null},
   *                              the {@link FileUtilities#getCurrentFolder() current folder} is used.
   *
   * @return Whether the user has interrupted the process.
   */
  private boolean updateShortcuts(@NotNull IShortcutTargetUpdater shortcutTargetUpdater
                                , @NotNull File                   oldParentTarget
                                , @NotNull File                   newParentTarget
                                , @NotNull List<File>             shortcuts) {
    
    assertNoneNull(shortcutTargetUpdater, oldParentTarget, newParentTarget);
    
    this.numTotalShortcuts =   shortcuts.size();
    
    this.numUpdatedShortcuts = MINUS1_i;
    
    this.numSkippedShortcuts = MINUS1_i;
    
    final char abortFromPause = CANCEL_CHARS.charAt(CANCEL_CHARS.length() - ONE_i);
    
    this.numProcessedShortcuts = ZERO_i;
    
    this.numUpdatedShortcuts =   ZERO_i;
    
    this.numSkippedShortcuts = ZERO_i;
    
    String previousPercent = null;
    
    boolean userAborted = false;
    
    for (int iShortcut = ZERO_i; iShortcut < this.numTotalShortcuts && ! userAborted; iShortcut++) {
      
      final File shortcut = shortcuts.get(iShortcut);
      
      try {
        
        final TargetUpdateOutcome updateOutcome = shortcutTargetUpdater.updateTargetIfMatch(
                                                                           shortcut, oldParentTarget, newParentTarget
                                                                 , null
                                                       , s -> this.appContext.warnUser(
                                                                                          ZERO_i, s)
                                                       ,   s -> this.appContext.errUser(
                                                                                          ZERO_i, s));
        ++this.numProcessedShortcuts;
        
        if (updateOutcome.notUpdated() == null) {
          
          // : The shortcut has had its target updated.
          
          ++this.numUpdatedShortcuts;
          
          this.appContext.warnUser(ZERO_i
                                    , NL   + "Shortcut"              + NLT + getCanonicalPathAsDescr(shortcut)
                                            + NL2T + ": target updated from" + NL2T2 + dq(updateOutcome.fromTo().o1)
                                            + NLT  + "to"                    + NLT2 + dq(updateOutcome.fromTo().o2) + ".");
        }
        else {
          
          // : The shortcut has not had its target updated.
        
          this.appContext.outUser(TWO_i, updateOutcome.notUpdated());
        }
        userAborted = IOUtilities.handleUserInput( abortFromPause);
        
        if (userAborted) {
          
          this.appContext.warnUser(ZERO_i, NL2 + "Interruption requested by the user after "
                                                                 + FMT0DG.format(this.numProcessedShortcuts) + " shortcuts of the total "
                                                                 + FMT0DG.format(this.numTotalShortcuts)     + " were processed, "
                                                                 + FMT0DG.format(this.numUpdatedShortcuts)   + " of them were updated and "
                                                                 + FMT0DG.format(this.numSkippedShortcuts)   + " of them were skipped.");
        }
      }
      catch (IOException | InvalidExternalValueException | UncheckedIOException e) {
        
        ++this.numSkippedShortcuts;
        
        this.appContext.outUserLog(getFullDescriptionWithRootCause(e));
        
        this.appContext.errUser(ZERO_i, NL2 + "Skipping" + NL2T + dq(getCanonicalPath(shortcut))
                                                        + NL2T + ". Reason :" + NLT + e.getLocalizedMessage() + NL);
      }
      // Update progress display :
      
      previousPercent = showProgress(
                      iShortcut,      shortcut
                                   , previousPercent, this.numTotalShortcuts
                                                       ,this.numUpdatedShortcuts
                                                       , this.numSkippedShortcuts);
    }
    this.appContext.outUser(ONE_i, NL2 + "Finished updating " + FMT0DG.format(this.numUpdatedShortcuts) + " shortcuts out of " + FMT0DG.format(this.numTotalShortcuts) + " .");
    
    return userAborted;
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
    
    this.appContext.outUser(ONE_i, NL + "Retrieving shortcuts to check for needed target update, under folder"
                                                 + NL2T + dq(searchFolderPath) + " ...");
    
    final List<File> shortcuts = FileUtilities.listFiles(
                        Paths.get(searchFolderPath)
                       , new String[] { removeStart(WIN_SHORTCUT_EXTENSION, EXTENSION_SEPARATOR) }
                 , true);
    
    this.appContext.outUser(ONE_i, NLT + "Found " + FMT0DG.format(this.numTotalShortcuts) + " shortcut file(s) under"
                                                   + NL2T + dq(searchFolderPath) + ".");
    return shortcuts;
  }
  
  /**
   * Calculates the percentage corresponding to {@code iLastProcessed}, and displays it if it's different from the
   * previously displayed one.<br>If the {@link AppContext#currentVerbosity currently set verbosity} allows, also
   * displays the last processed shortcut.
   *
   * @param iLastProcessed Index (so 0-based) of the last shortcut that has been processed.<br>
   *
   * @param lastProcessed  The last processed shortcut. May be {@code null} if the currently set verbosity does not
   *                       require to show it.
   *
   * @param previousPercent The last percentage that has been shown.<br>
   *
   * @param totToProcess The total number of shortcuts to process.
   *
   * @param totUpdated The total number of shortcuts updated so far.
   *
   * @param totSkipped The total number of shortcuts skipped so far because they could not be processed.
   *
   * @return The percentage this method just displayed, calculated based on {@code iLastProcessed}, or the last
   *         displayed one if it's the same.
   */
  private String showProgress(int iLastProcessed, File lastProcessed, @NotBlank String previousPercent, int totToProcess
                            , int totUpdated,     int  totSkipped) {
    
    String result = previousPercent != null ? assertNonBlankNorTrimmable(previousPercent) : null;
    
    final String currentPercent = FMT0D.format(Math.floor(percent(iLastProcessed + ONE_d, totToProcess)));
    
    if (this.appContext.currentVerbosity >= TWO_i) {
      
      this.appContext.outUser_Chars(TWO_i, NL2 + "Processing #" + FMT0DG.format(iLastProcessed + ONE_l)
                                                                 + " of "         + FMT0DG.format(totToProcess)
                                                                 + " ("           + leftPad(currentPercent, 3)
                                                                 + "%) :" + NL2T  + getCanonicalPathAsDescr(lastProcessed)
                                                                 + " (updated : " + FMT0DG.format(totUpdated)
                                                                 + "; skipped : " + FMT0DG.format(totSkipped)
                                                                 + ") ..." + NLT);
    }
    else if (this.appContext.currentVerbosity >= ONE_i && ! currentPercent.equals(result)) {
      
      result = currentPercent;
      
      this.appContext.outUser(ONE_i, leftPad(result, 3)
                                                        + "% (" + FMT0DG.format(iLastProcessed + ONE_l) + " tot"
                                                        + " / " + FMT0DG.format(totToProcess)           + " processed"
                                                        + " / " + FMT0DG.format(totUpdated)             + " updated"
                                                        + " / " + FMT0DG.format(totSkipped)             + " skipped"
                                                        + ")");
    }
    return result;
  }

}
