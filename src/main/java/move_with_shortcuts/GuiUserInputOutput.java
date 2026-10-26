/**
 * Created by Davide on 2026-10-24 .
 */
package move_with_shortcuts;


import duser_input_output.impl.NeutralUserInputOutput;
import jakarta.validation.constraints.NotNull;

import static dutil.number.NumberUtilities.MINUS1_i;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.string.TextUtilities.CRChar;
import static dutil.string.TextUtilities.LFChar;


// @formatter:off


/**
 * The silent user I/O used by the GUI : it forwards the progress lines received from the updater to the GUI progress
 * reporter, forwards the execution texts to the GUI operation-detail area, and discards all other output.
 */
final class GuiUserInputOutput extends NeutralUserInputOutput {


  /**
   * The tracker that turns the output lines into progress updates.
   */
  private final ProgressTracker progressTracker;
  
  /**
   * The reporter of the execution texts shown in the GUI operation-detail area.
   */
  private final GuiTextReporter textReporter;
  
  /**
   * The text received without a line break yet, to be forwarded when the line is complete.
   */
  private final StringBuilder pendingText = new StringBuilder();
  
  
  /**
   * Factory method.
   *
   * @param progressReporter The reporter to notify of the progress.<br>
   *
   * @param textReporter The reporter to notify of the execution texts.
   *
   * @return The new user I/O.
   */
  static GuiUserInputOutput newInstance(@NotNull GuiProgressReporter progressReporter
                                      , @NotNull GuiTextReporter     textReporter) {
                                      
    return new GuiUserInputOutput(progressReporter, textReporter);
  }
  
  /**
   * Private constructor.
   *
   * @param progressReporter The reporter to notify of the progress.<br>
   *
   * @param textReporter {@link #textReporter}.
   */
  private GuiUserInputOutput(@NotNull GuiProgressReporter progressReporter
                           , @NotNull GuiTextReporter     textReporter) {
                           
    super();
    
    this.progressTracker = new ProgressTracker(progressReporter::report);
    
    this.textReporter = textReporter;
  }
  
  
  /**
   * Parses the given line for a progress update and forwards it to the operation-detail area when it is not a progress
   * line.
   *
   * @param line The output line.
   *
   * @return {@code null}; the text is forwarded to the GUI instead of being written to the console.
   */
  @Override
  public String outLine(String line) {
  
    this.progressTracker.accept(line);
    
    if (! ProgressTracker.isProgressLine(line)) {
    
      appendLine(line);
    }
    return super.outLine(line);
  }
  
  /**
   * Flushes the text received without a line break and adds the empty line.
   */
  @Override
  public void outLine() {
  
    flushPendingText();
    
    super.outLine();
  }
  
  /**
   * Parses the given text for a phase update and forwards it to the operation-detail area.
   *
   * @param chars The user-output text.
   *
   * @return {@code null}; the text is forwarded to the GUI instead of being written to the console.
   */
  @Override
  public String outChars(String chars) {
  
    this.progressTracker.acceptChars(chars);
    
    appendNormalText(chars);
    
    return super.outChars(chars);
  }
  
  /**
   * Forwards the given text to the operation-detail area as a warning.
   *
   * @param chars The warning text.
   *
   * @return {@code null}; the text is forwarded to the GUI instead of being written to the console.
   */
  @Override
  public String warnChars(String chars) {
  
    appendGuardedText(GuiTextKind.WARNING, chars);
    
    return super.warnChars(chars);
  }
  
  /**
   * Forwards the given text to the operation-detail area as an error.
   *
   * @param chars The error text.
   *
   * @return {@code null}; the text is forwarded to the GUI instead of being written to the console.
   */
  @Override
  public String errChars(String chars) {
  
    appendGuardedText(GuiTextKind.ERROR, chars);
    
    return super.errChars(chars);
  }
  
  /**
   * Forwards the given text to the operation-detail area as normal text.
   *
   * @param chars The as-is text.
   *
   * @return {@code null}; the text is forwarded to the GUI instead of being written to the console.
   */
  @Override
  public String asIsChars(String chars) {
  
    appendNormalText(chars);
    
    return super.asIsChars(chars);
  }
  
  /**
   * Forwards any text still waiting for a line break, then closes the output.
   */
  @Override
  public void closeOut() {
  
    flushPendingText();
    
    super.closeOut();
  }
  
  
  /**
   * Appends the given line to the operation-detail area, completing any text that was waiting for a line break.
   *
   * @param line The line, or {@code null}.
   */
  private void appendLine(String line) {
  
    if (line == null) {
    
      flushPendingText();
    }
    else if (this.pendingText.length() > ZERO_i) {
    
      this.pendingText.append(line);
      
      final String text = this.pendingText.toString();
      
      this.pendingText.setLength(ZERO_i);
      
      reportText(GuiTextKind.NORMAL, text);
    }
    else {
    
      reportText(GuiTextKind.NORMAL, line);
    }
  }
  
  /**
   * Appends the given normal text, forwarding the lines it completes and keeping the incomplete tail.
   *
   * @param text The text, or {@code null}.
   */
  private void appendNormalText(String text) {
  
    if (text != null && ! text.isEmpty()) {
    
      this.pendingText.append(text);
      
      emitCompleteLines();
    }
  }
  
  /**
   * Forwards the pending text, then the given warning or error text.
   *
   * @param kind The kind of the text, either {@link GuiTextKind#WARNING} or {@link GuiTextKind#ERROR}.<br>
   *
   * @param text The text, or {@code null}.
   */
  private void appendGuardedText(@NotNull GuiTextKind kind, String text) {
  
    flushPendingText();
    
    reportText(kind, text);
  }
  
  /**
   * Forwards every complete line currently held in the pending text, keeping the last incomplete line.
   */
  private void emitCompleteLines() {
  
    int lineBreakIndex = findLineBreak(this.pendingText);
    
    while (lineBreakIndex >= ZERO_i) {
    
      final String line = this.pendingText.substring(ZERO_i, lineBreakIndex);
      
      final int lineBreakLength = this.pendingText.charAt(lineBreakIndex) == CRChar
                               && lineBreakIndex + ONE_i < this.pendingText.length()
                               && this.pendingText.charAt(lineBreakIndex + ONE_i) == LFChar ? 2 : 1;
                               
      this.pendingText.delete(ZERO_i, lineBreakIndex + lineBreakLength);
      
      reportText(GuiTextKind.NORMAL, line);
      
      lineBreakIndex = findLineBreak(this.pendingText);
    }
  }
  
  /**
   * Returns the index of the first complete line break in the given text.
   *
   * @param text The text to search.
   *
   * @return The index of the line break, or {@code -1} when the text holds no complete line break.
   */
  private static int findLineBreak(@NotNull StringBuilder text) {
  
    int result = MINUS1_i;
    
    for (int i = ZERO_i; i < text.length(); i++) {
    
      final char currentChar = text.charAt(i);
      
      if (currentChar == LFChar || currentChar == CRChar && i < text.length() - ONE_i) {
      
        result = i;
        
        i = text.length();
      }
    }
    return result;
  }
  
  /**
   * Forwards the text received without a line break, if any.
   */
  private void flushPendingText() {
  
    if (this.pendingText.length() > ZERO_i) {
    
      final String text = this.pendingText.toString();
      
      this.pendingText.setLength(ZERO_i);
      
      reportText(GuiTextKind.NORMAL, text);
    }
  }
  
  /**
   * Reports one non-blank execution text.
   *
   * @param kind The kind of the text.<br>
   *
   * @param text The text, or {@code null}.
   */
  private void reportText(@NotNull GuiTextKind kind, String text) {
  
    if (text != null && ! text.isBlank()) {
    
      this.textReporter.report(new GuiTextUpdate(kind, text));
    }
  }
}
