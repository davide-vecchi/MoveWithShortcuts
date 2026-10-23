/**
 * Created by Davide on 2026-10-24 .
 */
package move_with_shortcuts;


import duser_input_output.impl.NeutralUserInputOutput;
import jakarta.validation.constraints.NotNull;


// @formatter:off


/**
 * The silent user I/O used by the GUI : it forwards the progress lines received from the updater to the GUI progress
 * reporter, and discards all other output.
 */
final class GuiUserInputOutput extends NeutralUserInputOutput {


  /**
   * The tracker that turns the output lines into progress updates.
   */
  private final ProgressTracker progressTracker;
  
  
  /**
   * Factory method.
   *
   * @param progressReporter The reporter to notify of the progress.
   *
   * @return The new user I/O.
   */
  static GuiUserInputOutput newInstance(@NotNull GuiProgressReporter progressReporter) {
  
    return new GuiUserInputOutput(progressReporter);
  }
  
  /**
   * Private constructor.
   *
   * @param progressReporter The reporter to notify of the progress.
   */
  private GuiUserInputOutput(@NotNull GuiProgressReporter progressReporter) {
  
    super();
    
    this.progressTracker = new ProgressTracker(progressReporter::report);
  }
  
  
  /**
   * Parses the given line for a progress update and discards it from the user output.
   *
   * @param line The output line.
   *
   * @return Always {@code null}.
   */
  @Override
  public String outLine(String line) {
  
    this.progressTracker.accept(line);
    
    return super.outLine(line);
  }
  
  /**
   * Parses the given text for a phase update and discards it from the user output.
   *
   * @param chars The user-output text.
   *
   * @return Always {@code null}.
   */
  @Override
  public String outChars(String chars) {
  
    this.progressTracker.acceptChars(chars);
    
    return super.outChars(chars);
  }
}
