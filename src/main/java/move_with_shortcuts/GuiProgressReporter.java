/**
 * Created by Davide on 2026-10-24 .
 */
package move_with_shortcuts;


import jakarta.validation.constraints.NotNull;

import java.util.function.Consumer;


// @formatter:off


/**
 * Reports the progress of the current operation to the GUI, when a listener is registered.
 */
final class GuiProgressReporter {


  /**
   * The listener to notify, or {@code null} when the GUI is not ready or has been closed.
   */
  private volatile Consumer<ProgressUpdate> listener;
  
  
  /**
   * Factory method.
   *
   * @return The new reporter.
   */
  static GuiProgressReporter newInstance() {
  
    return new GuiProgressReporter();
  }
  
  /**
   * Private constructor.
   */
  private GuiProgressReporter() {}
  
  
  /**
   * Sets the listener to notify.
   *
   * @param listener The listener, or {@code null} to stop notifying.
   */
  void setListener(Consumer<ProgressUpdate> listener) {
  
    this.listener = listener;
  }
  
  /**
   * Reports a phase change or a progress update.
   *
   * @param update The progress update.
   */
  void report(@NotNull ProgressUpdate update) {
  
    final Consumer<ProgressUpdate> currentListener = this.listener;
    
    if (currentListener != null) {
    
      currentListener.accept(update);
    }
  }
}
