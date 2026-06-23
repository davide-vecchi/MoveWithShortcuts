/**
 * Created by OpenCode on 2026-05-08 .
 */
package rename_with_links;

import dlog.log.Log;
import duser_input_output.AUserInputOutput;
import dutil.application.AAppContext;
import jakarta.validation.constraints.NotNull;

import static dfile.file.FileUtilities.getCanonicalPathAsDescr;
import static dutil.string.TextUtilities.NL;


// @formatter:off


/**
 * Implementation of {@link AAppContext} for {@link RenameWithLinks}.
 */
public class AppContext extends AAppContext {
  
  
  /**
   * Private constructor. Sets {@link #currentVerbosity} to {@link AAppContext#MAX_VERBOSITY max}.
   *
   * @param userIO    {@link AAppContext#userIO userIO}.
   * @param screenLog {@link AAppContext#userIO screenLog}.
   * @param userLog   {@link AAppContext#userIO userLog}.
   * @param devLog    {@link AAppContext#userIO devLog}.
   */
  private AppContext(@NotNull AUserInputOutput userIO, @NotNull Log screenLog
                                                     , @NotNull Log userLog, @NotNull Log devLog) {
    super(userIO, screenLog, userLog, devLog);
    
    this.currentVerbosity = MAX_VERBOSITY;
  }
  
  /**
   * Factory method. Sets {@link #currentVerbosity} to {@link AAppContext#MAX_VERBOSITY max}.
   *
   * @param userIO    {@link AAppContext#userIO userIO}.
   * @param screenLog {@link AAppContext#userIO screenLog}.
   * @param userLog   {@link AAppContext#userIO userLog}.
   * @param devLog    {@link AAppContext#userIO devLog}.
   */
  public static AppContext newAppContext(@NotNull AUserInputOutput userIO, @NotNull Log screenLog
                                                                         , @NotNull Log userLog, @NotNull Log devLog) {
    return new AppContext(userIO, screenLog, userLog, devLog);
  }


  @Override
  public void showLogInfo(int verbosity) {

    outUser(verbosity, NL + "Screen log: " + getCanonicalPathAsDescr(this.screenLog.logFile) + ".");

    outUser(verbosity,      "  User log: " + getCanonicalPathAsDescr(this.userLog  .logFile) + ".");

    outUser(verbosity,      "   Dev log: " + getCanonicalPathAsDescr(this.devLog   .logFile) + ".");
  }

}
