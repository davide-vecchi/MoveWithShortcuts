/**
 * Created by Davide on 2026-10-23 .
 */
package move_with_shortcuts;


import application.AAppContext;
import dfile.shortcut.IShortcutsUpdater;
import dfile.shortcut.ShortcutFileNamePolicy;
import dutil.exception.UserRequestedTermination;
import dutil.value_holder.ObjectAndDescr;
import jakarta.validation.constraints.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.concurrent.ExecutionException;

import static dfile.file.FileUtilities.getCanonicalPath;
import static dfile.file.FileUtilities.getCurrentFolder;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.dq;
import static org.apache.commons.lang3.StringUtils.isNotBlank;


// @formatter:off


/**
 * The Swing dialog of MWS, shown when the program is started from the Windows Explorer context menu or standalone.
 */
final class MoveWithShortcutsGui {


  /**
   * The application context to use for logging.
   */
  private final AAppContext appContext;
  
  /**
   * The shortcuts updater implementation to use.
   */
  private final IShortcutsUpdater updater;
  
  /**
   * The MWS instance that performs the operation.
   */
  private final MoveWithShortcuts app;
  
  /**
   * The dialog's frame.
   */
  private final JFrame frame;
  
  /**
   * The field with the path of the file or folder to rename / move.
   */
  private final PathTextField originalField;
  
  /**
   * The field with the destination path.
   */
  private final PathTextField destinationField;
  
  /**
   * The field with the folder to scan for shortcuts.
   */
  private final PathTextField searchField;
  
  /**
   * The button that starts the operation.
   */
  private final JButton okButton;
  
  /**
   * The button that closes the dialog.
   */
  private final JButton cancelButton;
  
  /**
   * The progress bar shown while the operation runs.
   */
  private final JProgressBar progressBar;
  
  
  /**
   * Shows the dialog.
   *
   * @param appContext The application context to use for logging.<br>
   *
   * @param updater The shortcuts updater implementation to use.<br>
   *
   * @param originalPath The original path received from Explorer, or {@code null} when started standalone.
   */
  static void show(@NotNull AAppContext appContext, @NotNull IShortcutsUpdater updater, String originalPath) {
  
    new MoveWithShortcutsGui(appContext, updater, originalPath);
  }
  
  /**
   * Private constructor. Builds and shows the dialog.
   *
   * @param appContext {@link #appContext}.<br>
   *
   * @param updater {@link #updater}.<br>
   *
   * @param originalPath The original path received from Explorer, or {@code null} when started standalone.
   */
  private MoveWithShortcutsGui(@NotNull AAppContext appContext, @NotNull IShortcutsUpdater updater
                             , String originalPath) {
                             
    this.appContext = assertNonNull(appContext);
    
    this.updater =    assertNonNull(updater);
    
    this.app = MoveWithShortcuts.newInstance(appContext);
    
    this.frame = new JFrame(MoveWithShortcutsMain.APP_NAME);
    
    this.originalField = PathTextField.newInstance();
    
    this.destinationField = PathTextField.newInstance();
    
    this.searchField = PathTextField.newInstance();
    
    this.okButton = new JButton("Move / rename");
    
    this.cancelButton = new JButton("Cancel");
    
    this.progressBar = new JProgressBar();
    
    initFields(originalPath);
    
    initFrame();
  }
  
  
  /**
   * Fills the fields with their initial values.
   *
   * @param originalPath The original path received from Explorer, or {@code null} when started standalone.
   */
  private void initFields(String originalPath) {
  
    if (isNotBlank(originalPath)) {
    
      this.originalField.setText(   originalPath);
      
      this.destinationField.setText(originalPath);
    }
    this.searchField.setText(getCurrentFolder());
  }
  
  /**
   * Builds the frame, its content and its listeners, and shows it.
   */
  private void initFrame() {
  
    this.okButton.addActionListener(    e -> onOk());
    
    this.cancelButton.addActionListener(e -> cancel());
    
    this.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    
    this.frame.addWindowListener(new WindowAdapter() {
    
      /**
       * Closes the application when the user closes the window.
       *
       * @param e The window event.
       */
      @Override
      public void windowClosing(WindowEvent e) {
      
        cancel();
      }
    });
    this.frame.setContentPane(createContentPanel());
    
    this.frame.pack();
    
    this.frame.setMinimumSize(this.frame.getPreferredSize());
    
    this.frame.setLocationRelativeTo(null);
    
    this.frame.setVisible(true);
  }
  
  /**
   * Creates the dialog's content panel.
   *
   * @return The panel.
   */
  private JPanel createContentPanel() {
  
    final JPanel result = new JPanel(new BorderLayout(8, 8));
    
    result.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    
    result.add(createPathPanel(),    BorderLayout.CENTER);
    
    result.add(createButtonsPanel(), BorderLayout.SOUTH);
    
    return result;
  }
  
  /**
   * Creates the panel with the three path rows.
   *
   * @return The panel.
   */
  private JPanel createPathPanel() {
  
    final JPanel result = new JPanel(new GridBagLayout());
    
    final GridBagConstraints gbc = new GridBagConstraints();
    
    gbc.insets = new Insets(4, 4, 4, 4);
    
    gbc.anchor = GridBagConstraints.WEST;
    
    gbc.fill = GridBagConstraints.HORIZONTAL;
    
    gbc.gridy = ZERO_i;
    
    addPathRow(result, gbc, "Original",      this.originalField,    this::browseOriginal);
    
    addPathRow(result, gbc, "Destination",   this.destinationField, this::browseDestination);
    
    addPathRow(result, gbc, "Search folder", this.searchField,      this::browseSearch);
    
    return result;
  }
  
  /**
   * Creates the panel with the buttons and the progress bar.
   *
   * @return The panel.
   */
  private JPanel createButtonsPanel() {
  
    final JPanel result = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    this.progressBar.setIndeterminate(false);
    
    this.progressBar.setVisible(false);
    
    this.progressBar.setPreferredSize(new Dimension(120, 16));
    
    result.add(this.progressBar);
    
    result.add(this.okButton);
    
    result.add(this.cancelButton);
    
    return result;
  }
  
  /**
   * Adds one labelled path field with its browse button to the given panel.
   *
   * @param panel The panel to add to.<br>
   *
   * @param gbc The constraints, updated in place.<br>
   *
   * @param label The row's label.<br>
   *
   * @param field The field.<br>
   *
   * @param browseAction The action to run when the browse button is pressed.
   */
  private static void addPathRow(@NotNull JPanel        panel, @NotNull GridBagConstraints gbc, @NotNull String label
                               , @NotNull PathTextField field, @NotNull Runnable           browseAction) {
    gbc.gridx = ZERO_i;
    
    gbc.weightx = 0.0;
    
    panel.add(new JLabel(label + ":"), gbc);
    
    gbc.gridx = 1;
    
    gbc.weightx = 1.0;
    
    panel.add(field, gbc);
    
    gbc.gridx = 2;
    
    gbc.weightx = 0.0;
    
    final JButton browseButton = new JButton("...");
    
    browseButton.addActionListener(e -> browseAction.run());
    
    panel.add(browseButton, gbc);
    
    gbc.gridy++;
  }
  
  /**
   * Lets the user choose the original file or folder.
   */
  private void browseOriginal() {
  
    browse(this.originalField, JFileChooser.FILES_AND_DIRECTORIES, "Select the file or folder to move / rename");
  }
  
  /**
   * Lets the user choose the destination path.
   */
  private void browseDestination() {
  
    browse(this.destinationField, JFileChooser.FILES_AND_DIRECTORIES, "Select the new path");
  }
  
  /**
   * Lets the user choose the folder to scan for shortcuts.
   */
  private void browseSearch() {
  
    browse(this.searchField, JFileChooser.DIRECTORIES_ONLY, "Select the folder to scan for shortcuts");
  }
  
  /**
   * Shows a file chooser and sets the selected path into the given field.
   *
   * @param field The field to update.<br>
   *
   * @param selectionMode The {@link JFileChooser} selection mode.<br>
   *
   * @param title The chooser's title.
   */
  private void browse(@NotNull PathTextField field, int selectionMode, @NotNull String title) {
  
    final JFileChooser chooser = new JFileChooser();
    
    chooser.setDialogTitle(title);
    
    chooser.setFileSelectionMode(selectionMode);
    
    final String currentText = field.getText().trim();
    
    if (! currentText.isEmpty()) {
    
      final File current = new File(currentText);
      
      if (current.exists()) {
      
        chooser.setSelectedFile(current);
      }
      else if (current.getParentFile() != null && current.getParentFile().isDirectory()) {
      
        chooser.setCurrentDirectory(current.getParentFile());
        
        chooser.setSelectedFile(current);
      }
    }
    if (chooser.showOpenDialog(this.frame) == JFileChooser.APPROVE_OPTION) {
    
      field.setText(getCanonicalPath(chooser.getSelectedFile()));
    }
  }
  
  /**
   * Validates the entered paths, asks for confirmation and starts the operation.
   */
  private void onOk() {
  
    final ObjectAndDescr<GuiMoveRequest> built = MoveWithShortcutsGuiController.buildRequest(
                                                                             this.originalField.getText()
                                                                        , this.destinationField.getText()
                                                                             , this.searchField.getText());
    if (built.description != null) {
    
      JOptionPane.showMessageDialog(this.frame, built.description, "Invalid paths"
                                     , JOptionPane.ERROR_MESSAGE);
    }
    else {
    
      final GuiMoveRequest request = built.object;
      
      final int answer = JOptionPane.showConfirmDialog(this.frame, buildConfirmationMessage(request)
                                                              , "Confirm"
                                                          , JOptionPane.YES_NO_OPTION
                                                         , JOptionPane.WARNING_MESSAGE);
      if (answer == JOptionPane.YES_OPTION) {
      
        execute(request);
      }
    }
  }
  
  /**
   * Builds the confirmation message for the given request.
   *
   * @param request The request.
   *
   * @return The message.
   */
  private static String buildConfirmationMessage(@NotNull GuiMoveRequest request) {
  
    final String operation = request.mustMove()
                             ? "The file or folder will be renamed / moved, and the shortcuts will be updated."
                             : "The rename / move is assumed to have already been done, so only the shortcuts will"
                             + " be updated.";
                             
    return operation + NL2
                 + "Original: "      + dq(getCanonicalPath(             request.originalFileOrFolder()))    + NL
                 + "Destination: "   + dq(getCanonicalPath(             request.destinationFileOrFolder())) + NL
                 + "Search folder: " + dq(getCanonicalPath(request.searchFolder()))            + NL2
                 + "Continue?";
  }
  
  /**
   * Runs the operation in the background, showing the progress bar, then shows the summary or the error.
   *
   * @param request The validated request.
   */
  private void execute(@NotNull GuiMoveRequest request) {
  
    setBusy(true);
    
    new SwingWorker<Void, Void>() {
    
      /**
       * Whether the shortcuts update was interrupted on user request.
       */
      private boolean userInterrupted;
      
      /**
       * Runs the MWS operation.
       *
       * @return Always {@code null}.
       *
       * @throws Exception If the operation fails.
       */
      @Override
      protected Void doInBackground() throws Exception {
      
        try {
        
          MoveWithShortcutsGui.this.app.runGui(MoveWithShortcutsGui.this.updater, request
                       , ShortcutFileNamePolicy.NEVER);
        }
        catch (UserRequestedTermination t) {
        
          this.userInterrupted = true;
        }
        return null;
      }
      
      /**
       * Shows the summary, the interruption message or the error.
       */
      @Override
      protected void done() {
      
        setBusy(false);
        
        try {
        
          get();
          
          if (this.userInterrupted) {
          
            JOptionPane.showMessageDialog(MoveWithShortcutsGui.this.frame
                                               , "The operation was interrupted on user request."
                                                 , MoveWithShortcutsMain.APP_NAME
                                            , JOptionPane.WARNING_MESSAGE);
          }
          else {
          
            JOptionPane.showMessageDialog(MoveWithShortcutsGui.this.frame, buildSummaryMessage()
                                                 , MoveWithShortcutsMain.APP_NAME
                                            , JOptionPane.INFORMATION_MESSAGE);
          }
        }
        catch (InterruptedException e) {
        
          Thread.currentThread().interrupt();
          
          showError(e);
        }
        catch (ExecutionException e) {
        
          showError(e.getCause() != null ? e.getCause() : e);
        }
      }
    }.execute();
  }
  
  /**
   * Enables or disables the dialog's controls and shows or hides the progress bar.
   *
   * @param busy Whether the operation is running.
   */
  private void setBusy(boolean busy) {
  
    this.originalField.setEnabled(    ! busy);
    
    this.destinationField.setEnabled( ! busy);
    
    this.searchField.setEnabled(      ! busy);
    
    this.okButton.setEnabled(         ! busy);
    
    this.cancelButton.setEnabled(     ! busy);
    
    this.progressBar.setIndeterminate(busy);
    
    this.progressBar.setVisible(      busy);
  }
  
  /**
   * Builds the summary message shown at the end of a successful operation.
   *
   * @return The message.
   */
  private String buildSummaryMessage() {
  
    return "The operation completed." + NL2
         + "Shortcuts found: " + this.app.numTotalShortcuts + NL
         + "Shortcuts needing update: " + this.app.numProcessedShortcuts + NL
         + "Shortcuts updated: " + this.app.numUpdatedShortcuts + NL
         + "Shortcut files renamed: " + this.app.numRenamedShortcuts + NL
         + "Shortcut-file rename collisions: " + this.app.numRenameCollisions + NL
         + "Shortcut-file renames skipped: " + this.app.numRenameSkippedShortcuts + NL
         + "Shortcuts skipped: " + this.app.numSkippedShortcuts;
  }
  
  /**
   * Shows an error dialog.
   *
   * @param e The error.
   */
  private void showError(@NotNull Throwable e) {
  
    JOptionPane.showMessageDialog(this.frame,  getFullDescriptionWithRootCause(e)
                                         , "Error", JOptionPane.ERROR_MESSAGE);
  }
  
  /**
   * Closes the application.
   */
  private void cancel() {
  
    this.frame.dispose();
    
    closeAppContext();
    
    System.exit(ZERO_i);
  }
  
  /**
   * Closes the application context, ignoring any failure because the application is exiting.
   */
  private void closeAppContext() {
  
    try {
    
      this.appContext.close();
    }
    catch (Exception e) {
    
      // The application is exiting, so there is nowhere to report this.
    }
  }
  
}
