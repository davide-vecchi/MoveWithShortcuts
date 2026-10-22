/**
 * Created by Davide on 2026-05-08 .
 */
package move_with_shortcuts;


import application.AAppContext;
import dcommandlinearg.CommandLineArgException;
import dcommandlinearg.DCommandLineSpec;
import dcommandlinearg.OptionSpec;
import dcommandlinearg.ParsedArgs;
import dfile.shortcut.IShortcutsUpdater;
import dfile.shortcut.ShortcutFileNamePolicy;
import dfile.shortcut.WinShortcutsUpdater_PSScriptsMulti;
import dlog.log.Log;
import duser_input_output.impl.consoleUserIO.ColorConsoleUserIO;
import dutil.exception.UserRequestedTermination;
import dutil.io.ConditionallyCloseablePrintStream;
import dutil.system.OSUtilities;
import jakarta.validation.constraints.NotNull;

import java.util.Date;

import static application.AAppContext.MAX_VERBOSITY;
import static dfile.file.FileUtilities.getCurrentFolder;
import static dlog.log.Log.writeLogsHeaders;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.list.text.TextListUtilities.listToString;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.string.TextUtilities.CHARSET_UTF_8;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.S;
import static dutil.string.TextUtilities.dq;
import static dutil.system.OSUtilities.WIN_SHORTCUT_EXTENSION;
import static dutil.system.OSUtilities.setSystemEncodingUTF8;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;


// @formatter:off


/**
 * The startup class of the {@code MoveWithShortcuts} application.
 */
public final class MoveWithShortcutsMain {


  /**
   * Private constructor to prevent instantiation.
   */
  private MoveWithShortcutsMain() {}
  
  /**
   * The name of this program. Short name, no description (see #APP_DESCR).
   */
  public static final String APP_NAME = "MoveWithShortcuts";
  
  /**
   * The description of this program. Description, not a Short name (see #APP_NAME).
   */
  public static final String APP_DESCR = APP_NAME + " - Renames / moves a file or folder and updates all " + WIN_SHORTCUT_EXTENSION + " shortcuts accordingly.";
  
  /**
   * The DEFAULT value for the {@link AAppContext#currentVerbosity verbosity} level if not specified.
   */
  public static final int DEFAULT_VERBOSITY = ONE_i;
  
  static final String FROM_OPTION_NAME      = "from";
  static final String TO_OPTION_NAME        = "to";
  static final String SEARCH_OPTION_NAME    = "search";
  private static final String VERBOSITY_OPTION_NAME = "verbosity";
  private static final String SHORTCUT_FILE_NAME_OPTION_NAME = "shortcut-file-name";
  
  
  /**
   * Entry point of the MoveWithShortcuts program.
   *
   * @param originalArgs The command line args, as named options :
   *                     {@code -from <file or folder path> -to <value> -search <folder path>}
   *                     {@code [-verbosity <int>] [-shortcut-file-name <never|exact|descendant>] [-help]} ; zero args
   *                     start the interactive mode.
   */
  public static void main(String[] originalArgs) throws Exception {
  
    setSystemEncodingUTF8();
    
    final DCommandLineSpec argsSpec = newArgsSpec();
    final ParsedArgs        args     = originalArgs.length == ZERO_i ? null : parseArgs(argsSpec, originalArgs);
    
    // In the help / bad-args case parseArgs already printed the usage and set the exit code, and never returns :
    
    if (args == null && originalArgs.length > ZERO_i) {
    
      return;
    }
    // Start the execution :
    
    try (
    
      final Log screenLog = new Log(APP_DESCR + " - screen log",    APP_NAME + "_screen-log.LOG", true);
      
      final Log userLog =   new Log(APP_DESCR + " - user log",      APP_NAME + "_user-log.LOG",   true);
      
      final Log devLog =    new Log(APP_DESCR + " - developer log", APP_NAME + "_dev-log.LOG",    true);
      
      final ConditionallyCloseablePrintStream out = new ConditionallyCloseablePrintStream(
                                                                                          System.out,   true
                                                                                        , CHARSET_UTF_8, false);
                                                                                        
      final ConditionallyCloseablePrintStream err = new ConditionallyCloseablePrintStream(
                                                                             System.err,   true
                                                                                        , CHARSET_UTF_8, false);
      final AAppContext ac = AppContext.newAppContext(
                       ColorConsoleUserIO.newInstance1(System.in,         out,                 err
                                                           , CYAN,   BLACK,   RED
                                                           , BLACK, YELLOW, BLACK)
                                                                      , screenLog,          userLog,            devLog))
      {
      
        final MoveWithShortcuts app = MoveWithShortcuts.newInstance(ac);
        
        try {
        
          ac.currentVerbosity = args != null ? args.getInt(VERBOSITY_OPTION_NAME) : DEFAULT_VERBOSITY;
          
          final ShortcutFileNamePolicy shortcutFileNamePolicy = args != null
                                                    ? ShortcutFileNamePolicy.parse(
                                                              args.getString(SHORTCUT_FILE_NAME_OPTION_NAME))
                                                    : ShortcutFileNamePolicy.NEVER;
                                                    
          writeLogsHeaders(ac.screenLog, ac.userLog, ac.devLog, APP_NAME, APP_DESCR);
          
          
          // -------------------------------------------------------
          // Create the desired type of shortcuts updater instance :
          
//          final IShortcutsUpdater shortcutsTargetUpdater =
//                                                            WinShortcutsUpdater_PSScriptsMulti.newInstance(ac);

          final IShortcutsUpdater shortcutsTargetUpdater =
                             WinShortcutsUpdater_PSScriptsMulti.newInstance(null, ac);
                             
//          final IShortcutsUpdater shortcutsTargetUpdater = WinShortcutsUpdater_OneByOne.newInstance(
//                  WinShortcutUpdater_PS_WSH01.newInstance(false, ac.devLog)
//                         , CANCEL_CHARS , ac);

          // -------------------------------------------------------
          
          
          showStartupMessages(ac, shortcutsTargetUpdater, originalArgs, shortcutFileNamePolicy);
          
          // Perform the renaming operation using the chosen updater :
          
          if (args == null) {
          
            app.run(shortcutsTargetUpdater, shortcutFileNamePolicy);
          }
          else {
          
            app.run(shortcutsTargetUpdater, args, shortcutFileNamePolicy);
          }
        }
        catch (UserRequestedTermination t) {
        
          ac.warnUser(ZERO_i, NL + (t.getMessage() != null ? t.getMessage() : "Terminated on user request."));
        }
        catch (Exception e) {
        
          ac.errUser(ZERO_i, NL2 + "Terminated due to an error : " + e.getClass().getSimpleName() + " :" + NL2T + e.getLocalizedMessage() + NL2);
          
          ac.outUserLog(getFullDescriptionWithRootCause(e));
        }
        finally {
        
          ac.outUser(NL + "Total shortcuts to process :     " + app.numTotalShortcuts     + " .");
          
          ac.outUser(NL + "Total shortcuts needing update : " + app.numProcessedShortcuts + " .");
          
          ac.outUser(NL + "Total shortcuts updated :        " + app.numUpdatedShortcuts   + " .");
          
          ac.outUser(NL + "Total shortcut files renamed :   " + app.numRenamedShortcuts + " .");
          
          ac.outUser(NL + "Total shortcut-file rename collisions : " + app.numRenameCollisions + " .");
          
          ac.outUser(NL + "Total shortcut-file renames skipped : " + app.numRenameSkippedShortcuts + " .");
          
          ac.outUser(NL + "Total shortcuts skipped :        " + app.numSkippedShortcuts   + " .");
          
          ac.showLogInfo(ONE_i);
        }
      }
  }
  
  /**
   * Builds the specification of this program's named command-line arguments.
   *
   * @return the command-line specification
   */
  private static DCommandLineSpec newArgsSpec() {
  
    return DCommandLineSpec.newSpec(APP_NAME, APP_DESCR)
                           .addOption(OptionSpec.option(FROM_OPTION_NAME
                                                       , "The path of the file or folder to rename / move, or the path it"
                                                       + " had before if the operation was already done elsewhere, in"
                                                       + " which case no rename / move will be done and only the shortcuts"
                                                       + " adjustment will be done.")
                                               .required().existingFileOrFolder())
                           .addOption(OptionSpec.option(TO_OPTION_NAME
                                                       , "The new name or path for the file or folder, or the path it has"
                                                       + " now if the operation was already done elsewhere, in which case no"
                                                       + " rename / move will be done and only the shortcuts adjustment will"
                                                       + " be done. Exactly one of -from and -to must exist, the other must"
                                                       + " not.")
                                               .required())
                           .addOption(OptionSpec.option(SEARCH_OPTION_NAME
                                                       , "The folder to scan for " + WIN_SHORTCUT_EXTENSION + " shortcuts to update.")
                                               .required().existingFolder())
                           .addOption(OptionSpec.option(SHORTCUT_FILE_NAME_OPTION_NAME
                                                       , "The shortcut file name policy : never, exact or descendant"
                                                       + " (default never). exact / descendant rename a shortcut file"
                                                       + " named after the old leaf, preserving the Windows default"
                                                       + " \" - Shortcut\" part.")
                                               .allowedValues("never", "exact", "descendant")
                                               .defaultValue("never"))
                           .addOption(OptionSpec.option(VERBOSITY_OPTION_NAME
                                                       , "The verbosity level (0-" + MAX_VERBOSITY + ")")
                                               .intRange(ZERO_i, MAX_VERBOSITY).defaultValue(S(DEFAULT_VERBOSITY)));
                                               
  }
  
  /**
   * Parses the given (non-empty) command-line arguments against this program's specification ; on a help request or
   * an argument error, prints the usage text, sets the exit code (0 for help, 1 for an error) and returns {@code
   * null} — after {@code System.exit}, so nothing follows in practice.
   *
   * @param argsSpec      The command-line specification, see {@link #newArgsSpec()}.<br>
   *
   * @param originalArgs  The command line args, not empty.
   *
   * @return The parsed args, or {@code null} after printing the usage text.
   */
  private static ParsedArgs parseArgs(@NotNull DCommandLineSpec argsSpec, @NotNull String[] originalArgs) {
  
    ParsedArgs args;
    
    try {
    
      args = argsSpec.parsePartial(originalArgs);
      
      if (args.isHelpRequested()) {
      
        System.out.println(argsSpec.getUsageText());
        System.exit(ZERO_i);
        args = null;
      }
    }
    catch (CommandLineArgException e) {
    
      System.out.println(e.getMessage());
      System.out.println(argsSpec.getUsageText());
      System.exit(ONE_i);
      args = null;
    }
    return args;
    
  }
  
  /**
   * Shows the startup messages.
   *
   * @param ac                     The application context.<br>
   *
   * @param shortcutsTargetUpdater The {@link IShortcutsUpdater} instance that this execution will use.<br>
   *
   * @param args                   The command line arguments with which this execution has been started.<br>
   *
   * @param shortcutFileNamePolicy The {@link ShortcutFileNamePolicy} selected for this execution.
   */
  private static void showStartupMessages(@NotNull AAppContext ac,  @NotNull IShortcutsUpdater shortcutsTargetUpdater
                                        , @NotNull String[]    args
                                        , @NotNull ShortcutFileNamePolicy shortcutFileNamePolicy) {
                                        
    ac.outUser(ONE_i, NL +"Starting " + dq(APP_DESCR) + " on " + new Date());
    
    if (args.length > ZERO_i) {
    
      ac.outUser(ONE_i, NL + listToString(asList(args), "Program arguments", EMPTY, EMPTY, NL2));
    }
    ac.outUser(ONE_i, NL + "Current folder : " + dq(getCurrentFolder()) + ".");
    
    ac.showLogInfo(ONE_i);
    
    ac.outUser(ONE_i, NL + OSUtilities.getDescription());
    
    ac.outUser(ONE_i, NL + "Using "    + IShortcutsUpdater.class.getSimpleName() + " implementation "
                                           + dq(shortcutsTargetUpdater.getClass().getSimpleName()) + ".");
                                           
    ac.outUser(ONE_i, NL + "Current verbosity (0-" + MAX_VERBOSITY + ") : " + ac.currentVerbosity + " .");
    
    ac.outUser(ONE_i, NL + "Shortcut file name policy : " + shortcutFileNamePolicy + " .");
    
    ac.outUser(ONE_i, NL);
  }
  
}
