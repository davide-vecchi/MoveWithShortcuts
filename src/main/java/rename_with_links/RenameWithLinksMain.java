/**
 * Created by OpenCode on 2026-05-08 .
 *
 * @formatter:off
 */
package rename_with_links;


import dlog.log.Log;
import duser_input_output.impl.consoleUserIO.ColorConsoleUserIO;
import dutil.exception.UserRequestedTermination;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import static dfile.file.FileUtilities.getCanonicalPath;
import static dlog.log.Log.writeLogsHeaders;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.string.TextUtilities.CHARSET_UTF_8;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.dq;
import static dutil.system.OSUtilities.WIN_SHORTCUT_EXTENSION;
import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;


/**
 * The startup class of the {@code RenameWithLinks} application.
 */
@SuppressWarnings("PublicConstructor")
public class RenameWithLinksMain {


  /**
   * The name of this program. Short name, no description (see #APP_DESCR).
   */
  public static final String APP_NAME = "Rename With Links";
  
  /**
   * The description of this program. Description, not a Short name (see #APP_NAME).
   */
  public static final String APP_DESCR = APP_NAME + " - Renames / Updates a file or folder and updates all " + WIN_SHORTCUT_EXTENSION + " shortcuts that point to it.";
  
  
  /**
   * Entry point of the RenameWithLinks program.
   *
   * @param args The command line args (currently unused).
   */
  public static void main(String[] args) throws Exception {
  
    try (
      final Log screenLog = new Log(APP_DESCR + " - screen log",    APP_NAME + "_screen-log.LOG", true);
      final Log userLog =   new Log(APP_DESCR + " - user log",      APP_NAME + "_user-log.LOG",   true);
      final Log devLog =    new Log(APP_DESCR + " - developer log", APP_NAME + "_dev-log.LOG",    true);
      final PrintStream out = new PrintStream(     System.out, true, CHARSET_UTF_8);
      final PrintStream err = new PrintStream(System.err, true, CHARSET_UTF_8);
      final AppContext ac = AppContext.newAppContext(
                         ColorConsoleUserIO.newInstance1(System.in,         out,                 err
                                                             , CYAN,   BLACK,   RED
                                                             , BLACK, YELLOW, BLACK)
                                                                       , screenLog,          userLog,            devLog))
    {
      try {
      
        writeLogsHeaders(ac.screenLog, ac.userLog, ac.devLog, APP_NAME, APP_DESCR);
        
        showStartupMessages(ac);
        
        final RenameWithLinks app = RenameWithLinks.newInstance(ac);
        
        app.run();
      }
      catch (UserRequestedTermination t) {
      
        ac.warnUser(NL + (t.getMessage() != null ? t.getMessage() : "Terminated on user request."));
      }
      catch (IOException e) {
      
        ac.errUser(NL2 + "Terminated due to an error : " + e.getClass().getSimpleName() + " :" + NL2T + e.getLocalizedMessage().trim() + NL2);
        
        ac.outUserLog(getFullDescriptionWithRootCause(e));
      }
      finally {
      
        ac.showLogInfo();
      }
    }
  }
  
  
  /**
   * Shows the startup messages.
   *
   * @param ac The application context.
   */
  private static void showStartupMessages(@NotNull AppContext ac) {
  
    ac.outUser();
    
    ac.outUser("Starting " + dq(APP_DESCR) + " on " + new Date() + NL);
    
    ac.outUser("Screen log: " + getCanonicalPath(ac.screenLog.logFile));
    
    ac.outUser("  User log: " + getCanonicalPath(ac.userLog.logFile));
    
    ac.outUser("   Dev log: " + getCanonicalPath(ac.devLog.logFile));
  }
  
}
