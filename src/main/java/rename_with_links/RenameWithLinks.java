/**
 * Created by OpenCode on 2026-05-08 .
 */
package rename_with_links;


import dfile.file.FileUtilities;
import dfile.shortcut.IShortcutTargetUpdater;
import dutil.exception.UserRequestedTermination;
import dutil.exception.exceptions.InvalidExternalValueException;
import dutil.exception.exceptions.MissingExternalValueException;
import dutil.exception.exceptions.NonUniqueExternalValueException;
import dutil.io.IOUtilities;
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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static dfile.file.FileUtilities.assertExistingFile;
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
import static dutil.string.TextUtilities.NLT;
import static dutil.string.TextUtilities.NLT2;
import static dutil.string.TextUtilities.TAB2;
import static dutil.string.TextUtilities.assertNonBlankNorTrimmable;
import static dutil.string.TextUtilities.assertNonBlankUnlessNull;
import static dutil.string.TextUtilities.dq;
import static dutil.string.TextUtilities.dqStr;
import static dutil.system.OSUtilities.WIN_SHORTCUT_EXTENSION;
import static dutil.system.OSUtilities.assertWindowsOS;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.io.FilenameUtils.EXTENSION_SEPARATOR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static rename_with_links.RenameWithLinksMain.MAX_VERBOSITY;


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
    
    final File searchDir = askSearchDirectory();
    
    this.appContext.currentVerbosity = askVerbosity();
    
    execute(originalFileOrFolder, destinationFileOrFolder, searchDir, shortcutTargetUpdater);
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

    final File searchDir = resolveSearchDirectory(argSearchPath);
    
    execute(originalFileOrFolder, destinationFileOrFolder, searchDir, shortcutTargetUpdater);
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
   * @param shortcutTargetUpdater   The updater to use to perform the update of the shortcuts' target that need it.
   */
  private void execute(@NotNull File                   originalFileOrFolder
                     , @NotNull File                   destinationFileOrFolder
                     , @NotNull File                   searchFolder
                     , @NotNull IShortcutTargetUpdater shortcutTargetUpdater) throws IOException {
    
    if (askStartConfirmation(originalFileOrFolder, destinationFileOrFolder, searchFolder)) {
      
      // Do the requested renaming / moving :
      
      renameFileOrFolder(originalFileOrFolder, destinationFileOrFolder);
      
      // Update all the shortcuts that point to the location as it was before the renaming / moving :
      
      updateShortcuts(shortcutTargetUpdater, originalFileOrFolder, destinationFileOrFolder
                    , searchFolder);
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
                                     , @NotNull File searchDir) {
    
    assertNoneNull(originalFileOrFolder, destinationFileOrFolder, searchDir);
    
    boolean confirmed = this.appContext.userIO.in("Press Enter to confirm renaming / moving "
                                                          + (originalFileOrFolder.isFile() ? "file" : "folder")
                                                          + dq(getCanonicalPath(originalFileOrFolder))    + " to "
                                                          + dq(getCanonicalPath(destinationFileOrFolder)) + ","
                                                          + " or type " + calcCancelCharsPrompt(CANCEL_CHARS)
                                           , EMPTY, CANCEL_CHARS) != null;
    return confirmed;
    
  }
  
  /**
   * TODO @@@@@@ COMMENT
   * @param argPath
   * @return
   */
  private static @NotNull File resolveOriginalPath(final String argPath) {

    final File file = new File(argPath);

    if (! file.exists()) {

      throw new MissingExternalValueException(dq(argPath) + " does not exist.");
    }
    return file;
  }
  
  /**
   * TODO @@@@@@ COMMENT
   * @param argDestinationPath
   * @param original
   * @return
   */
  private static @NotNull File resolveDestinationFileOrFolder(final String argDestinationPath, final File original) {

    final String effectiveNewName;

    if (hasPath(argDestinationPath)) {

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
    return destination;
  }
  
  /**
   * TODO @@@@@@ COMMENT
   *
   * @param argSearchPath
   * @return
   */
  private static @NotNull File resolveSearchDirectory(final String argSearchPath) {

    final File searchDir = assertExistingFile(new File(assertValidPath(argSearchPath
                                                                                  , TRUE))
                                               , TRUE);

    assertExistingFile(searchDir, TRUE);
    
    return searchDir;
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
                                                        + " shortcuts to update, or press Enter for the default,"
                                                        + " or type " + calcCancelCharsPrompt(CANCEL_CHARS)
                                         , getCurrentFolder(), CANCEL_CHARS);
    if (searchPath == null) {

      throw new UserRequestedTermination();
    }
    final File searchDir = new File(assertValidPath(searchPath, TRUE));
    
    assertExistingFile(searchDir, TRUE);
    
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
    
    this.appContext.outUser(ONE_i, NL + "Renaming / moving" + NLT + dq(getCanonicalPath(originalFileOrFolder)) + NL + "to" + NLT + dq(getCanonicalPath(destinationFileOrFolder)) + NL + "...");

    if (! destinationFileOrFolder.getParentFile().exists()) {

      FileUtils.forceMkdir(destinationFileOrFolder.getParentFile());
    }
    Files.move(originalFileOrFolder.toPath(), destinationFileOrFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
    
    this.appContext.outUser(ONE_i, "Done.");
  }

  /**
   * Recursively scans the search directory for {@code .lnk} files and, for each shortcut whose target matches the
   * original path, updates it to the new path.
   *
   * @param shortcutTargetUpdater The object to use to {@link IShortcutTargetUpdater#updateTargetIfMatch update} the
   *                              target of the shortcuts found under {@code searchFolder} tree that have it equal to {@code
   *                              oldTarget}.<br>
   *
   * @param oldTarget             The target that - if present in a shortcut - must be updated to {@code newTarget}.<br>
   *
   * @param newTarget             The target to set into the shortcuts that have it equal to {@code oldTarget}.<br>
   *
   * @param searchFolder          The folder under which to recursively search for shortcuts to update. If {@code null},
   *                              the {@link FileUtilities#getCurrentFolder() current folder} is used.
   *
   * @return Whether the user has interrupted the process.
   */
  private boolean updateShortcuts(@NotNull IShortcutTargetUpdater shortcutTargetUpdater, @NotNull File oldTarget
                                , @NotNull File                   newTarget,                      File searchFolder) {
    
    assertNoneNull(shortcutTargetUpdater, oldTarget, newTarget);
    
    final Path effectiveSearchFolder = searchFolder != null ? searchFolder.toPath() : Path.of(getCurrentFolder());
    
    this.appContext.outUser(ONE_i, NL + "Retrieving shortcuts to check for needed target update, under folder "
                                                        + dq(getCanonicalPath(effectiveSearchFolder.toFile())) + " ...");
    
    final char abortFromPause = CANCEL_CHARS.charAt(CANCEL_CHARS.length() - ONE_i);
    
    final List<File> shortcuts = FileUtilities.listFiles(
                                             effectiveSearchFolder
                                            , new String[] { removeStart(WIN_SHORTCUT_EXTENSION
                                                                                , EXTENSION_SEPARATOR) }
                                      , true);
    
    final int numShortcuts = shortcuts.size();
    
    this.appContext.outUser(ONE_i, NL + "Found " + FMT0DG.format(numShortcuts) + " shortcut file(s) under " + dq(getCanonicalPath(effectiveSearchFolder.toFile())) + ".");
    
    boolean userAborted = this.appContext.userIO.in("Press Enter to start processing the " + FMT0DG.format(numShortcuts)
                                                            + " shortcut file(s) under " + dq(getCanonicalPath(
                                                                               effectiveSearchFolder.toFile()))
                                                            + " (the processing can be paused with the Enter key)"
                                                            + " or type " + calcCancelCharsPrompt(CANCEL_CHARS)
                                             , EMPTY, CANCEL_CHARS) == null;
    if (! userAborted) {
      
      int numUpdated = ZERO_i;
      
      String previousPercent = null;
      
      for (int iShortcut = ZERO_i; iShortcut < numShortcuts && ! userAborted; iShortcut++) {
        
        final File shortcut = shortcuts.get(iShortcut);
        
        try {
          
          final String notUpdated = shortcutTargetUpdater.updateTargetIfMatch(shortcut, oldTarget, newTarget
                                                                   , null
                                                         , s -> this.appContext.warnUser(
                                                                                            ZERO_i, s)
                                                         ,   s -> this.appContext.errUser(
                                                                                            ZERO_i, s));
          if (notUpdated == null) {
          
            ++numUpdated;
            
            this.appContext.outUser(ZERO_i
                                     , NL2 + "Shortcut " + getCanonicalPathAsDescr(shortcut)
                                             + NLT + " : target updated from" + NLT2 + dqStr(oldTarget)
                                             + NLT + "to"                     + NLT2 + dqStr(newTarget) + ".");
          }
          else {
          
            this.appContext.outUser(TWO_i, notUpdated);
          }
          userAborted = IOUtilities.handleUserInput( abortFromPause);
          
          if (userAborted) {
            
            this.appContext.warnUser(ZERO_i, NL2 + "Interruption requested by the user after "
                                                                   + FMT0DG.format((iShortcut + ONE_i)) + " shortcuts were processed and "
                                                                   + FMT0DG.format((numUpdated))        + " of them were updated.");
          }
        }
        catch (IOException | InvalidExternalValueException | UncheckedIOException e) {
          
          this.appContext.outUserLog(getFullDescriptionWithRootCause(e));
          
          this.appContext.errUser(ZERO_i, NL2 + "Skipping " + dq(getCanonicalPath(shortcut))
                                                                + ". Reason : " + e.getLocalizedMessage() + NL);
        }
        // Update progress display :
        
        previousPercent = showProgress(
                        iShortcut, shortcut, previousPercent, numShortcuts);
      }
      this.appContext.outUser(ONE_i, NL2 + "Finished updating " + FMT0DG.format(numUpdated) + " shortcuts out of " + FMT0DG.format(numShortcuts) + " .");
    }
    return userAborted;
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
   * @return The percentage this method just displayed, calculated based on {@code iLastProcessed}, or the last
   *         displayed one if it's the same.
   */
  private String showProgress(int iLastProcessed, File lastProcessed, @NotBlank String previousPercent, int totToProcess) {
    
    String result = previousPercent != null ? assertNonBlankNorTrimmable(previousPercent) : null;
    
    final String currentPercent = FMT0D.format(Math.floor(percent(iLastProcessed + ONE_d, totToProcess)));
    
    if (this.appContext.currentVerbosity >= TWO_i) {
      
      this.appContext.outUser_Chars(TWO_i, NL2 + "Processing #" + FMT0DG.format(iLastProcessed + ONE_l)
                                                                 + " of "         + FMT0DG.format(totToProcess)
                                                                 + " ("           + leftPad(currentPercent, 3)
                                                                 + "%) : "        + getCanonicalPathAsDescr(lastProcessed)
                                                                 + " ..."         + NLT);
    }
    else if (this.appContext.currentVerbosity >= ONE_i && ! currentPercent.equals(result)) {
      
      result = currentPercent;
      
      this.appContext.outUser_Chars(ONE_i, leftPad(result, 3)
                                                              + "% (" + FMT0DG.format(iLastProcessed + ONE_l)
                                                              + " / " + FMT0DG.format(totToProcess)
                                                              + ")"   + TAB2);
    }
    return result;
  }

}
