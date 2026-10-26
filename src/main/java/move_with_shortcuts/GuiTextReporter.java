/**
 * Created by Davide on 2026-10-26 .
 */
package move_with_shortcuts;


import jakarta.validation.constraints.NotNull;

import java.util.function.Consumer;


// @formatter:off


/**
 * Reports the execution texts of the current operation to the GUI, when a listener is registered.
 */
final class GuiTextReporter {


  /**
   * The listener to notify, or {@code null} when the GUI is not ready or has been closed.
   */
  private volatile Consumer<GuiTextUpdate> listener;
  
  
  /**
   * Factory method.
   *
   * @return The new reporter.
   */
  static GuiTextReporter newInstance() {
  
    return new GuiTextReporter();
  }
  
  /**
   * Private constructor.
   */
  private GuiTextReporter() {}
  
  
  /**
   * Sets the listener to notify.
   *
   * @param listener The listener, or {@code null} to stop notifying.
   */
  void setListener(Consumer<GuiTextUpdate> listener) {
  
    this.listener = listener;
  }
  
  /**
   * Reports one execution text update.
   *
   * @param update The text update.
   */
  void report(@NotNull GuiTextUpdate update) {
  
    final Consumer<GuiTextUpdate> currentListener = this.listener;
    
    if (currentListener != null) {
    
      currentListener.accept(update);
    }
  }
}
