/**
 * Created by Davide on 2026-05-08 .
 */
package move_with_shortcuts;


import application.AAppContext;
import dfile.shortcut.IShortcutsUpdater;
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
import static dutil.number.NumberUtilities.TWO_i;
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
import static move_with_shortcuts.MoveWithShortcuts.resolveVerbosity;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;


// @formatter:off


/**
 * The startup class of the {@code MoveWithShortcuts} application.
 */
@SuppressWarnings("PublicConstructor")
public class MoveWithShortcutsMain {


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
  
  
  /**
   * Entry point of the MoveWithShortcuts program.
   *
   * @param originalArgs The command line args.
   */
  public static void main(String[] originalArgs) throws Exception {
    
    setSystemEncodingUTF8();
    
    // If 3 args were given, the 4th (the verbosity) must be the default, so add it as if it had been given :
    
    String[] args = originalArgs;
    
    if (args.length == 3) {
      
      args = new String[] {args[ZERO_i], args[ONE_i], args[TWO_i], S(DEFAULT_VERBOSITY)};
    }
    // Start the execution :
    
    if (args.length == ZERO_i || args.length == 4) {

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
          
          ac.currentVerbosity = resolveVerbosity(args.length >= 4 ? args[3] : null
                                               , MAX_VERBOSITY, DEFAULT_VERBOSITY);
          
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
          
          
          showStartupMessages(ac, shortcutsTargetUpdater, args);
          
          // Perform the renaming operation using the chosen updater :
          
          if (args.length == ZERO_i) {
            
            app.run(shortcutsTargetUpdater);
          }
          else {
            
            app.run(shortcutsTargetUpdater, args[ZERO_i], args[ONE_i]
                                           , args[TWO_i],       args[3]);
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
          
          ac.outUser(NL + "Total shortcuts skipped :        " + app.numSkippedShortcuts   + " .");
          
          ac.showLogInfo(ONE_i);
        }
      }
    }
    else {
      
      // : Bad launch args, don't run.
      
      showUsage();
    }
  }
  
  /**
   * Shows the startup messages.
   *
   * @param ac                     The application context.<br>
   *
   * @param shortcutsTargetUpdater The {@link IShortcutsUpdater} instance that this execution will use.<br>
   *
   * @param args                   The command line arguments with which this execution has been started.
   */
  private static void showStartupMessages(@NotNull AAppContext ac,  @NotNull IShortcutsUpdater shortcutsTargetUpdater
                                        , @NotNull String[]    args) {
  
    ac.outUser(ONE_i, NL +"Starting " + dq(APP_DESCR) + " on " + new Date());
    
    if (args.length > ZERO_i) {
      
      ac.outUser(ONE_i, NL + listToString(asList(args), "Program arguments", EMPTY, EMPTY, NL));
    }
    ac.outUser(ONE_i, NL + "Current folder : " + dq(getCurrentFolder()) + ".");
    
    ac.showLogInfo(ONE_i);
    
    ac.outUser(ONE_i, NL + OSUtilities.getDescription());
    
    ac.outUser(ONE_i, NL + "Using "    + IShortcutsUpdater.class.getSimpleName() + " implementation "
                                           + dq(shortcutsTargetUpdater.getClass().getSimpleName()) + ".");
    
    ac.outUser(ONE_i, NL + "Current verbosity (0-" + MAX_VERBOSITY + ") : " + ac.currentVerbosity + " .");
    
    ac.outUser(ONE_i, NL);
  }
  
  /**
   * Displays a description of the launch arguments.
   */
  private static void showUsage() {

    System.out.println();
    
    System.out.println(APP_DESCR);
    
    System.out.println();
    
    System.out.println("Usage: MoveWithShortcutsMain [ <originalPath> <destinationPath> <searchPath> [verbosity] ]");
    
    System.out.println();
    
    System.out.println("  When no arguments are provided, the program prompts interactively.");
    
    System.out.println("  When 3 arguments are provided, they are used as the paths and the verbosity level (0-" + MAX_VERBOSITY + ") defaults to " + DEFAULT_VERBOSITY + " (no prompts).");
    
    System.out.println("  When 4 arguments are provided, the first 3 are used as the paths and the last one is the verbosity level (0-" + MAX_VERBOSITY + ") (no prompts).");
    
    System.out.println("  Any other number of arguments prints this message.");
  }
  
}
