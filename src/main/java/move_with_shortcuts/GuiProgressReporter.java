/**
 * Created by Davide on 2026-10-24 .
 */
package move_with_shortcuts;


import jakarta.validation.constraints.NotBlank;

import java.util.function.BiConsumer;


// @formatter:off


/**
 * Reports the progress of the current operation to the GUI, when a listener is registered.
 */
final class GuiProgressReporter {


  /**
   * The listener to notify, or {@code null} when the GUI is not ready or has been closed.
   */
  private volatile BiConsumer<String, Integer> listener;
  
  
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
  void setListener(BiConsumer<String, Integer> listener) {
  
    this.listener = listener;
  }
  
  /**
   * Reports a phase change or a progress update.
   *
   * @param phase The phase label.<br>
   *
   * @param percentage The percentage, or a negative value when the exact percentage is not known yet.
   */
  void report(@NotBlank String phase, int percentage) {
  
    final BiConsumer<String, Integer> currentListener = this.listener;
    
    if (currentListener != null) {
    
      currentListener.accept(phase, percentage);
    }
  }
}
