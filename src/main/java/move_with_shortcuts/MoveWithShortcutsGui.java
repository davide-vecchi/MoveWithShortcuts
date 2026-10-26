/**
 * Created by Davide on 2026-10-23 .
 */
package move_with_shortcuts;


import application.AAppContext;
import dfile.shortcut.ShortcutFileNamePolicy;
import dutil.exception.UserRequestedTermination;
import dutil.system.OSUtilities;
import dutil.value_holder.ObjectAndDescr;
import jakarta.validation.constraints.NotNull;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static dfile.file.FileUtilities.getCanonicalPath;
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
   * The factor by which the dialog's preferred width is enlarged.
   */
  private static final double WIDER_FRAME_FACTOR = 1.3;
  
  /**
   * The color of the progress bar and status text after a successful operation.
   */
  private static final Color SUCCESS_COLOR = new Color(0, 128, 0);
  
  /**
   * The color of the progress bar and status text after a failed operation.
   */
  private static final Color FAILURE_COLOR = new Color(200, 0, 0);
  
  /**
   * The maximum number of lines kept in the operation-detail area.
   */
  private static final int MAX_STATUS_LINES = 1000;
  
  
  /**
   * The application context to use for logging.
   */
  private final AAppContext appContext;
  
  /**
   * The shortcuts updater implementation to use initially.
   */
  private final ShortcutsUpdaterKind updaterKind;
  
  /**
   * The MWS instance that performs the operation.
   */
  private final MoveWithShortcuts app;
  
  /**
   * The shortcut-file name policy to use initially.
   */
  private final ShortcutFileNamePolicy shortcutFileNamePolicy;
  
  /**
   * The dialog's frame.
   */
  private final JFrame frame;
  
  /**
   * The application icon, or {@code null} when it cannot be loaded.
   */
  private final ImageIcon appIcon;
  
  /**
   * The scaled icon for the About dialog, or {@code null}.
   */
  private final ImageIcon aboutIcon;
  
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
   * The combo that selects the shortcuts updater implementation.
   */
  private final JComboBox<ShortcutsUpdaterKind> updaterKindCombo;
  
  /**
   * The combo that selects the shortcut-file name policy.
   */
  private final JComboBox<ShortcutFileNamePolicy> shortcutFileNamePolicyCombo;
  
  /**
   * The button that starts the operation.
   */
  private final JButton okButton;
  
  /**
   * The button that closes the dialog.
   */
  private final JButton cancelButton;
  
  /**
   * The button that shows the program information.
   */
  private final JButton aboutButton;
  
  /**
   * The button that shows the command-line help.
   */
  private final JButton helpButton;
  
  /**
   * The buttons to disable while an operation is running.
   */
  private final List<JButton> processingDisabledButtons = new ArrayList<>();
  
  /**
   * The progress bar shown while the operation runs.
   */
  private final JProgressBar progressBar;
  
  /**
   * The default foreground color of the progress bar.
   */
  private final Color defaultProgressBarForeground;
  
  /**
   * The text area that describes the current operation.
   */
  private final JTextArea statusArea;
  
  /**
   * The default foreground color of the status area.
   */
  private final Color defaultStatusForeground;
  
  /**
   * The reporter of the progress of the current operation.
   */
  private final GuiProgressReporter progressReporter;
  
  /**
   * The reporter of the execution texts of the current operation.
   */
  private final GuiTextReporter textReporter;
  
  /**
   * The label of the last reported phase.
   */
  private String lastPhase = ProgressTracker.PHASE_WORKING;
  
  /**
   * The total of the last reported phase, or {@code 0} when it is not known.
   */
  private int lastTotal;
  
  
  /**
   * Shows the dialog.
   *
   * @param appContext The application context to use for logging.<br>
   *
   * @param updaterKind The shortcuts updater implementation to use initially.<br>
   *
   * @param originalPath The original path to prefill, or {@code null}.<br>
   *
   * @param destinationPath The destination path to prefill, or {@code null}.<br>
   *
   * @param searchPath The search folder to prefill, or {@code null}.<br>
   *
   * @param shortcutFileNamePolicy The shortcut-file name policy to use initially.<br>
   *
   * @param progressReporter The reporter of the progress of the operation.<br>
   *
   * @param textReporter The reporter of the execution texts of the operation.<br>
   *
   * @param warningMessage A message to show in the operation-detail area, or {@code null}.
   */
  static void show(@NotNull AAppContext appContext, @NotNull ShortcutsUpdaterKind updaterKind, String originalPath
                 , String destinationPath, String searchPath
                 , @NotNull ShortcutFileNamePolicy shortcutFileNamePolicy
                 , @NotNull GuiProgressReporter progressReporter, @NotNull GuiTextReporter textReporter
                 , String warningMessage) {
                  
    new MoveWithShortcutsGui(appContext, updaterKind, originalPath, destinationPath, searchPath
                           , shortcutFileNamePolicy, progressReporter, textReporter, warningMessage);
  }
  
  /**
   * Private constructor. Builds and shows the dialog.
   *
   * @param appContext {@link #appContext}.<br>
   *
   * @param updaterKind {@link #updaterKind}.<br>
   *
   * @param originalPath The original path to prefill, or {@code null}.<br>
   *
   * @param destinationPath The destination path to prefill, or {@code null}.<br>
   *
   * @param searchPath The search folder to prefill, or {@code null}.<br>
   *
   * @param shortcutFileNamePolicy {@link #shortcutFileNamePolicy}.<br>
   *
   * @param progressReporter {@link #progressReporter}.<br>
   *
   * @param textReporter {@link #textReporter}.<br>
   *
   * @param warningMessage A message to show in the operation-detail area, or {@code null}.
   */
  private MoveWithShortcutsGui(@NotNull AAppContext            appContext, @NotNull ShortcutsUpdaterKind  updaterKind
                                      , String                 originalPath
                                      , String                 destinationPath,     String                searchPath
                             , @NotNull ShortcutFileNamePolicy shortcutFileNamePolicy
                             , @NotNull GuiProgressReporter    progressReporter
                             , @NotNull GuiTextReporter        textReporter,        String                warningMessage) {
                             
    this.appContext = assertNonNull(appContext);
    
    this.updaterKind = assertNonNull(updaterKind);
    
    this.app = MoveWithShortcuts.newInstance(appContext);
    
    this.shortcutFileNamePolicy = assertNonNull(shortcutFileNamePolicy);
    
    this.frame = new JFrame(MoveWithShortcutsMain.APP_NAME);
    
    this.appIcon = loadAppIcon();
    
    this.aboutIcon = scaleIcon(this.appIcon, 64);
    
    if (this.appIcon != null) {
    
      this.frame.setIconImage(this.appIcon.getImage());
    }
    this.originalField = PathTextField.newInstance();
    
    this.destinationField = PathTextField.newInstance();
    
    this.searchField = PathTextField.newInstance();
    
    this.updaterKindCombo = new JComboBox<>(ShortcutsUpdaterKind.values());
    
    this.updaterKindCombo.setSelectedItem(this.updaterKind);
    
    this.updaterKindCombo.setToolTipText("The implementation used to update the shortcuts.");
    
    final DefaultListCellRenderer updaterKindRenderer = new DefaultListCellRenderer();
    
    this.updaterKindCombo.setRenderer((
  list, value, index, isSelected
                            , cellHasFocus) -> {
    
      final JLabel label = (JLabel) updaterKindRenderer.getListCellRendererComponent(list, value, index, isSelected
                                                                                                       , cellHasFocus);
      if (value instanceof ShortcutsUpdaterKind kind) {
      
        label.setText(kind.getId());
      }
      return label;
    });
    
    this.shortcutFileNamePolicyCombo = new JComboBox<>(ShortcutFileNamePolicy.values());
    
    this.shortcutFileNamePolicyCombo.setSelectedItem(this.shortcutFileNamePolicy);
    
    this.shortcutFileNamePolicyCombo.setToolTipText(
        "Whether and when the shortcut files themselves are renamed: never, exact or descendant.");
        
    final DefaultListCellRenderer shortcutFileNamePolicyRenderer = new DefaultListCellRenderer();
    
    this.shortcutFileNamePolicyCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
    
      final JLabel label = (JLabel) shortcutFileNamePolicyRenderer.getListCellRendererComponent(
                                                                              list, value, index, isSelected
                                                                               , cellHasFocus);
                                                                               
      if (value instanceof ShortcutFileNamePolicy policy) {
      
        label.setText(policy.name().toLowerCase(Locale.ROOT));
      }
      return label;
    });
    
    this.okButton = new JButton("Move / rename");
    
    this.cancelButton = new JButton("Cancel");
    
    this.aboutButton = new JButton("About");
    
    final Icon questionIcon = UIManager.getIcon("OptionPane.questionIcon");
    
    this.helpButton = questionIcon != null ? new JButton(scaleIcon(questionIcon, 16)) : new JButton("?");
    
    this.helpButton.getAccessibleContext().setAccessibleName("Help");
    
    this.processingDisabledButtons.add(this.aboutButton);
    
    this.processingDisabledButtons.add(this.helpButton);
    
    this.progressBar = new JProgressBar();
    
    this.defaultProgressBarForeground = this.progressBar.getForeground();
    
    this.statusArea = StatusTextArea.newInstance();
    
    this.statusArea.setEditable(false);
    
    this.statusArea.setLineWrap(true);
    
    this.statusArea.setWrapStyleWord(true);
    
    this.statusArea.setFocusable(false);
    
    this.statusArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    
    final Font statusFont = this.statusArea.getFont();
    
    this.statusArea.setFont(new Font("Tahoma", Font.PLAIN, statusFont != null ? statusFont.getSize() : 12));
    
    this.defaultStatusForeground = this.statusArea.getForeground();
    
    this.originalField.setToolTipText("The file or folder to move / rename, or the path it had before.");
    
    this.destinationField.setToolTipText("The new path, or the path it has now if the move was already done.");
    
    this.searchField.setToolTipText("The folder under which to search for shortcuts to update.");
    
    this.okButton.setToolTipText("Rename / move the file or folder and update the shortcuts (Enter).");
    
    this.cancelButton.setToolTipText("Close without performing the operation (Esc).");
    
    this.aboutButton.setToolTipText("Show program information.");
    
    this.helpButton.setToolTipText("Show help.");
    
    this.progressBar.setToolTipText("Progress of the current operation.");
    
    this.progressReporter = assertNonNull(progressReporter);
    
    this.progressReporter.setListener(this::onProgress);
    
    this.textReporter = assertNonNull(textReporter);
    
    this.textReporter.setListener(this::onTextUpdate);
    
    initFields(originalPath, destinationPath, searchPath, warningMessage);
    
    initFrame();
  }
  
  
  /**
   * Loads the application icon from the classpath.
   *
   * @return The icon, or {@code null} when it cannot be loaded.
   */
  private static ImageIcon loadAppIcon() {
  
    ImageIcon result = null;
    
    try (InputStream input = MoveWithShortcutsGui.class.getResourceAsStream("/MWS.png")) {
    
      if (input != null) {
      
        final BufferedImage image = ImageIO.read(input);
        
        if (image != null) {
        
          result = new ImageIcon(image);
        }
      }
    }
    catch (IOException e) {
    
      result = null;
    }
    return result;
  }
  
  /**
   * Scales an icon to a square of the given size.
   *
   * @param icon The icon to scale, or {@code null}.<br>
   *
   * @param size The target side length.
   *
   * @return The scaled icon, or {@code null} when the given icon is {@code null} or empty.
   */
  private static ImageIcon scaleIcon(ImageIcon icon, int size) {
  
    ImageIcon result = null;
    
    if (icon != null && icon.getIconWidth() > ZERO_i && icon.getIconHeight() > ZERO_i) {
    
      final Image scaled = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
      
      result = new ImageIcon(scaled);
    }
    return result;
  }
  
  /**
   * Scales an icon to a square of the given size.
   *
   * @param icon The icon to scale, or {@code null}.<br>
   *
   * @param size The target side length.
   *
   * @return The scaled icon, or the original one when it cannot be scaled.
   */
  private static Icon scaleIcon(Icon icon, int size) {
  
    Icon result = icon;
    
    if (icon instanceof ImageIcon imageIcon) {
    
      final Image scaled = imageIcon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
      
      result = new ImageIcon(scaled);
    }
    return result;
  }
  
  /**
   * Fills the fields with their initial values.
   *
   * @param originalPath The original path to prefill, or {@code null}.<br>
   *
   * @param destinationPath The destination path to prefill, or {@code null}.<br>
   *
   * @param searchPath The search folder to prefill, or {@code null}.<br>
   *
   * @param warningMessage A message to show in the operation-detail area, or {@code null}.
   */
  private void initFields(String originalPath, String destinationPath, String searchPath
                        , String warningMessage) {
                        
    if (isNotBlank(originalPath)) {
    
      this.originalField.setText(originalPath);
    }
    if (isNotBlank(destinationPath)) {
    
      this.destinationField.setText(destinationPath);
    }
    else if (isNotBlank(originalPath)) {
    
      this.destinationField.setText(originalPath);
    }
    if (isNotBlank(searchPath)) {
    
      this.searchField.setText(searchPath);
    }
    else {
    
      this.searchField.setText(MoveWithShortcutsGuiController.defaultSearchPath(originalPath));
    }
    if (isNotBlank(warningMessage)) {
    
      appendConsoleText(GuiTextKind.WARNING, warningMessage);
    }
  }
  
  /**
   * Builds the frame, its content and its listeners, and shows it.
   */
  private void initFrame() {
  
    this.okButton.addActionListener(    e -> onOk());
    
    this.cancelButton.addActionListener(e -> cancel());
    
    this.aboutButton.addActionListener(e -> showAbout());
    
    this.helpButton.addActionListener(e -> showHelp());
    
    this.frame.getRootPane().setDefaultButton(this.okButton);
    
    this.frame.getRootPane().registerKeyboardAction(e -> cancel()
                                                 , KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
                                                 , JComponent.WHEN_IN_FOCUSED_WINDOW);
                                                 
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
    
    final Dimension preferredSize = this.frame.getSize();
    
    this.frame.setMinimumSize(preferredSize);
    
    this.frame.setSize((int) Math.round(preferredSize.width * WIDER_FRAME_FACTOR), preferredSize.height);
    
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
    
    final JPanel topPanel = new JPanel(new BorderLayout(0, 4));
    
    topPanel.add(createPathPanel(), BorderLayout.CENTER);
    
    topPanel.add(createButtonsPanel(), BorderLayout.SOUTH);
    
    result.add(topPanel, BorderLayout.NORTH);
    
    result.add(createStatusScrollPane(), BorderLayout.CENTER);
    
    return result;
  }
  
  /**
   * Creates the panel with the path and option rows.
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
    
    this.processingDisabledButtons.add(
        addPathRow(result, gbc, "Original", this.originalField, this::browseOriginal
                 , "Browse for the original file or folder."));
                 
    this.processingDisabledButtons.add(
        addPathRow(result, gbc, "Destination", this.destinationField, this::browseDestination
                 , "Browse for the destination path."));
                 
    this.processingDisabledButtons.add(
        addPathRow(result, gbc, "Search folder", this.searchField, this::browseSearch
                 , "Browse for the search folder."));
                 
    addComboRow(result, gbc, "Updater", this.updaterKindCombo);
    
    addComboRow(result, gbc, "Shortcut file policy", this.shortcutFileNamePolicyCombo);
    
    return result;
  }
  
  /**
   * Creates the panel with the buttons and the progress bar.
   *
   * @return The panel.
   */
  private JPanel createButtonsPanel() {
  
    final JPanel result = new JPanel(new GridBagLayout());
    
    final GridBagConstraints gbc = new GridBagConstraints();
    
    gbc.gridy = ZERO_i;
    
    gbc.insets = new Insets(0, 4, 0, 4);
    
    final JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    
    leftPanel.add(this.aboutButton);
    
    leftPanel.add(this.helpButton);
    
    gbc.gridx = ZERO_i;
    
    gbc.weightx = 0.0;
    
    gbc.fill = GridBagConstraints.NONE;
    
    gbc.anchor = GridBagConstraints.WEST;
    
    result.add(leftPanel, gbc);
    
    this.progressBar.setIndeterminate(false);
    
    this.progressBar.setVisible(false);
    
    this.progressBar.setStringPainted(true);
    
    this.progressBar.setPreferredSize(new Dimension(340, 16));
    
    final JPanel progressPanel = new JPanel(new BorderLayout());
    
    progressPanel.setOpaque(false);
    
    progressPanel.add(this.progressBar, BorderLayout.CENTER);
    
    gbc.gridx = 1;
    
    gbc.weightx = 1.0;
    
    gbc.fill = GridBagConstraints.HORIZONTAL;
    
    gbc.anchor = GridBagConstraints.CENTER;
    
    result.add(progressPanel, gbc);
    
    final JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    
    rightPanel.add(this.okButton);
    
    rightPanel.add(this.cancelButton);
    
    gbc.gridx = 2;
    
    gbc.weightx = 0.0;
    
    gbc.fill = GridBagConstraints.NONE;
    
    gbc.anchor = GridBagConstraints.EAST;
    
    result.add(rightPanel, gbc);
    
    return result;
  }
  
  /**
   * Creates the scrollable panel that shows the operation details.
   *
   * @return The scroll pane.
   */
  private JScrollPane createStatusScrollPane() {
  
    final JScrollPane result = new JScrollPane(this.statusArea);
    
    result.setPreferredSize(new Dimension(600, 90));
    
    result.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    
    result.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    
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
   * @param browseAction The action to run when the browse button is pressed.<br>
   *
   * @param browseTooltip The tooltip of the browse button.
   *
   * @return The browse button.
   */
  private static JButton addPathRow(@NotNull JPanel        panel, @NotNull GridBagConstraints gbc, @NotNull String label
                                  , @NotNull PathTextField field, @NotNull Runnable           browseAction
                                  , @NotNull String        browseTooltip) {
                                  
    gbc.gridx = ZERO_i;
    
    gbc.weightx = 0.0;
    
    final JLabel labelComponent = new JLabel(label + ":");
    
    labelComponent.setToolTipText(field.getToolTipText());
    
    panel.add(labelComponent, gbc);
    
    gbc.gridx = 1;
    
    gbc.weightx = 1.0;
    
    panel.add(field, gbc);
    
    gbc.gridx = 2;
    
    gbc.weightx = 0.0;
    
    final JButton browseButton = new JButton("...");
    
    browseButton.setToolTipText(browseTooltip);
    
    browseButton.addActionListener(e -> browseAction.run());
    
    panel.add(browseButton, gbc);
    
    gbc.gridy++;
    
    return browseButton;
  }
  
  /**
   * Adds one row with a label and a combo box to the given panel.
   *
   * @param panel The panel to add the row to.<br>
   *
   * @param gbc The constraints to use and update.<br>
   *
   * @param label The row label.<br>
   *
   * @param combo The combo box.
   */
  private static void addComboRow(@NotNull JPanel panel, @NotNull GridBagConstraints gbc, @NotNull String label
                                , @NotNull JComboBox<?> combo) {
                                
    gbc.gridx = ZERO_i;
    
    gbc.weightx = 0.0;
    
    final JLabel labelComponent = new JLabel(label + ":");
    
    labelComponent.setToolTipText(combo.getToolTipText());
    
    panel.add(labelComponent, gbc);
    
    gbc.gridx = 1;
    
    gbc.weightx = 1.0;
    
    gbc.gridwidth = 2;
    
    panel.add(combo, gbc);
    
    gbc.gridwidth = 1;
    
    gbc.gridy++;
  }
  
  /**
   * Lets the user choose the original file or folder.
   */
  private void browseOriginal() {
  
    browse(this.originalField, JFileChooser.FILES_AND_DIRECTORIES
         , "Select the file or folder to move / rename", true);
  }
  
  /**
   * Lets the user choose the destination path.
   */
  private void browseDestination() {
  
    browse(this.destinationField, JFileChooser.FILES_AND_DIRECTORIES, "Select the new path", true);
  }
  
  /**
   * Lets the user choose the folder to scan for shortcuts.
   */
  private void browseSearch() {
  
    browse(this.searchField, JFileChooser.DIRECTORIES_ONLY
         , "Select the folder to scan for shortcuts", false);
  }
  
  /**
   * Shows a file chooser and sets the selected path into the given field.
   *
   * @param field The field to update.<br>
   *
   * @param selectionMode The {@link JFileChooser} selection mode.<br>
   *
   * @param title The chooser's title.<br>
   *
   * @param showFileTypeFilter Whether to show the "Files of type" filter list.
   */
  private void browse(@NotNull PathTextField field, int selectionMode, @NotNull String title
                    , boolean showFileTypeFilter) {
                    
    final JFileChooser chooser = showFileTypeFilter ? new JFileChooser() : new FolderFileChooser();
    
    chooser.setDialogTitle(title);
    
    chooser.setFileSelectionMode(selectionMode);
    
    chooser.setApproveButtonText("Select");
    
    chooser.setApproveButtonToolTipText("Select");
    
    if (! showFileTypeFilter) {
    
      chooser.setAcceptAllFileFilterUsed(false);
    }
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
      
      final ShortcutsUpdaterKind selectedUpdaterKind =
                    assertNonNull((ShortcutsUpdaterKind) this.updaterKindCombo.getSelectedItem());
                    
      final ShortcutFileNamePolicy selectedShortcutFileNamePolicy =
                    assertNonNull((ShortcutFileNamePolicy) this.shortcutFileNamePolicyCombo.getSelectedItem());
                    
      final int answer = JOptionPane.showConfirmDialog(this.frame
                                                              , buildConfirmationMessage(request
                                                                                       , selectedUpdaterKind
                                                                                , selectedShortcutFileNamePolicy)
                                                              , "Confirm"
                                                          , JOptionPane.YES_NO_OPTION
                                                         , JOptionPane.QUESTION_MESSAGE);
      if (answer == JOptionPane.YES_OPTION) {
      
        execute(request, selectedUpdaterKind, selectedShortcutFileNamePolicy);
      }
    }
  }
  
  /**
   * Builds the confirmation message for the given request and selections.
   *
   * @param request The request.<br>
   *
   * @param updaterKind The selected shortcuts updater implementation.<br>
   *
   * @param shortcutFileNamePolicy The selected shortcut-file name policy.
   *
   * @return The message.
   */
  private static String buildConfirmationMessage(@NotNull GuiMoveRequest request
                                               , @NotNull ShortcutsUpdaterKind updaterKind
                                               , @NotNull ShortcutFileNamePolicy shortcutFileNamePolicy) {
                                               
    final String operation = request.mustMove()
                             ? "The file or folder will be renamed / moved, and the shortcuts will be updated."
                             : "The rename / move is assumed to have already been done, so only the shortcuts will"
                             + " be updated.";
                             
    return operation + NL2
                 + "Original: "      + dq(getCanonicalPath(             request.originalFileOrFolder()))    + NL2
                 + "Destination: "   + dq(getCanonicalPath(             request.destinationFileOrFolder())) + NL2
                 + "Search folder: " + dq(getCanonicalPath(request.searchFolder()))            + NL2
                 + "Updater: "       + updaterKind.getId() + NL2
                 + "Shortcut file policy: " + shortcutFileNamePolicy.name().toLowerCase(Locale.ROOT) + NL2
                 + "Continue?";
  }
  
  /**
   * Runs the operation in the background, showing the progress bar, then shows the summary or the error.
   *
   * @param request The validated request.<br>
   *
   * @param updaterKind The selected shortcuts updater implementation.<br>
   *
   * @param shortcutFileNamePolicy The selected shortcut-file name policy.
   */
  private void execute(@NotNull GuiMoveRequest request, @NotNull ShortcutsUpdaterKind updaterKind
                     , @NotNull ShortcutFileNamePolicy shortcutFileNamePolicy) {
                     
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
        
          MoveWithShortcutsGui.this.app.runGui(updaterKind.newUpdater(MoveWithShortcutsGui.this.appContext)
                                             , request, shortcutFileNamePolicy);
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
        
        boolean success = false;
        
        try {
        
          get();
          
          success = true;
        }
        catch (InterruptedException e) {
        
          Thread.currentThread().interrupt();
          
          showError(e);
        }
        catch (ExecutionException e) {
        
          showError(e.getCause() != null ? e.getCause() : e);
        }
        finally {
        
          logFinalSummary();
        }
        if (success) {
        
          if (this.userInterrupted) {
          
            MoveWithShortcutsGui.this.progressBar.setIndeterminate(false);
            
            MoveWithShortcutsGui.this.progressBar.setString(MoveWithShortcutsGui.this.lastPhase + ": interrupted");
            
            JOptionPane.showMessageDialog(MoveWithShortcutsGui.this.frame
                                               , "The operation was interrupted on user request."
                                                 , MoveWithShortcutsMain.APP_NAME
                                            , JOptionPane.WARNING_MESSAGE);
          }
          else {
          
            setProgressTo100();
            
            JOptionPane.showMessageDialog(MoveWithShortcutsGui.this.frame, buildSummaryMessage()
                                                 , MoveWithShortcutsMain.APP_NAME
                                            , JOptionPane.INFORMATION_MESSAGE);
                                            
            markProgressCompleted();
          }
        }
        else {
        
          markProgressFailed();
        }
      }
    }.execute();
  }
  
  /**
   * Enables or disables the dialog's controls and starts or stops the progress display.
   *
   * @param busy Whether the operation is running.
   */
  private void setBusy(boolean busy) {
  
    this.originalField.setEnabled(    ! busy);
    
    this.destinationField.setEnabled( ! busy);
    
    this.searchField.setEnabled(      ! busy);
    
    this.updaterKindCombo.setEnabled(             ! busy);
    
    this.shortcutFileNamePolicyCombo.setEnabled(  ! busy);
    
    this.okButton.setEnabled(         ! busy);
    
    this.cancelButton.setEnabled(     ! busy);
    
    for (final JButton button : this.processingDisabledButtons) {
    
      button.setEnabled(! busy);
    }
    if (busy) {
    
      this.lastPhase = ProgressTracker.PHASE_WORKING;
      
      this.lastTotal = ZERO_i;
      
      this.statusArea.setForeground(this.defaultStatusForeground);
      
      this.progressBar.setForeground(this.defaultProgressBarForeground);
      
      this.progressBar.setIndeterminate(true);
      
      this.progressBar.setValue(ZERO_i);
      
      this.progressBar.setString(ProgressTracker.PHASE_WORKING + "...");
    }
    this.progressBar.setVisible(true);
  }
  
  /**
   * Receives a progress update from the operation thread and forwards it to the event dispatch thread.
   *
   * @param update The progress update.
   */
  private void onProgress(@NotNull ProgressUpdate update) {
  
    SwingUtilities.invokeLater(() -> updateProgressBar(update));
  }
  
  /**
   * Updates the progress bar and the operation-detail text. Must be called on the event dispatch thread.
   *
   * @param update The progress update.
   */
  private void updateProgressBar(@NotNull ProgressUpdate update) {
  
    this.lastPhase = update.phase();
    
    this.lastTotal = update.total();
    
    final String label = buildPhaseLabel(update.phase(), update.total());
    
    if (update.percentage() < ZERO_i) {
    
      this.progressBar.setIndeterminate(true);
      
      this.progressBar.setString(label + "...");
    }
    else {
    
      this.progressBar.setIndeterminate(false);
      
      this.progressBar.setValue(update.percentage());
      
      this.progressBar.setString(label + ": " + update.percentage() + "%");
    }
  }
  
  /**
   * Builds the phase label, including the total when it is known.
   *
   * @param phase The phase label.<br>
   *
   * @param total The total, or {@code 0} when it is not known.
   *
   * @return The label.
   */
  private static String buildPhaseLabel(@NotNull String phase, int total) {
  
    String result = phase;
    
    if (total > ZERO_i) {
    
      final String suffix = " shortcuts";
      
      if (phase.endsWith(suffix)) {
      
        result = phase.substring(0, phase.length() - suffix.length()) + " " + total + suffix;
      }
      else {
      
        result = phase + " " + total + suffix;
      }
    }
    return result;
  }
  
  /**
   * Sets the progress bar to 100% before the summary dialog is shown.
   */
  private void setProgressTo100() {
  
    this.progressBar.setIndeterminate(false);
    
    this.progressBar.setValue(100);
    
    this.progressBar.setString(buildPhaseLabel(this.lastPhase, this.lastTotal) + ": 100%");
  }
  
  /**
   * Receives an execution text from the operation thread and forwards it to the event dispatch thread.
   *
   * @param update The text update.
   */
  private void onTextUpdate(@NotNull GuiTextUpdate update) {
  
    SwingUtilities.invokeLater(() -> appendConsoleText(update.kind(), update.text()));
  }
  
  /**
   * Appends one execution text to the operation-detail area in execution order, and trims the history.
   *
   * @param kind The kind of the text.<br>
   *
   * @param text The text to append.
   */
  private void appendConsoleText(@NotNull GuiTextKind kind, @NotNull String text) {
  
    final String trimmed = text.strip();
    
    if (! trimmed.isEmpty()) {
    
      final String prefix = kind == GuiTextKind.WARNING ? "WARNING: "
                          : kind == GuiTextKind.ERROR   ? "ERROR: "
                          :                               "";
                          
      this.statusArea.append(prefix + trimmed + NL);
      
      trimStatusArea();
      
      this.statusArea.setCaretPosition(this.statusArea.getDocument().getLength());
      
      this.statusArea.setToolTipText(prefix + trimmed);
    }
  }
  
  /**
   * Removes the oldest lines from the operation-detail area when the history exceeds {@link #MAX_STATUS_LINES}.
   */
  private void trimStatusArea() {
  
    final int lineCount = this.statusArea.getLineCount();
    
    if (lineCount > MAX_STATUS_LINES) {
    
      try {
      
        final int removeTo = this.statusArea.getLineStartOffset(lineCount - MAX_STATUS_LINES);
        
        this.statusArea.getDocument().remove(ZERO_i, removeTo);
      }
      catch (BadLocationException e) {
      
        // A concurrent update changed the document; the next append trims it again.
      }
    }
  }
  
  /**
   * Returns the canonical path of a folder, followed by the platform name separator.
   *
   * @param folder The folder.
   *
   * @return The path with a trailing separator.
   */
  private static String folderWithSeparator(@NotNull File folder) {
  
    final String path = getCanonicalPath(folder);
    
    return path.endsWith(File.separator) ? path : path + File.separator;
  }
  
  /**
   * Marks the operation as completed, with a green progress bar.
   */
  private void markProgressCompleted() {
  
    this.progressBar.setIndeterminate(false);
    
    this.progressBar.setValue(100);
    
    this.progressBar.setString("Completed");
    
    this.progressBar.setForeground(SUCCESS_COLOR);
    
    this.statusArea.setForeground(SUCCESS_COLOR);
  }
  
  /**
   * Marks the operation as failed, with a red progress bar.
   */
  private void markProgressFailed() {
  
    this.progressBar.setIndeterminate(false);
    
    this.progressBar.setString("Failed");
    
    this.progressBar.setForeground(FAILURE_COLOR);
    
    this.statusArea.setForeground(FAILURE_COLOR);
  }
  
  /**
   * Builds the summary message shown at the end of a successful operation.
   *
   * @return The message.
   */
  private String buildSummaryMessage() {
  
    return "The operation completed."                                               + NL2
         + "Shortcuts found: "                 + this.app.numTotalShortcuts         + NL
         + "Shortcuts needing update: "        + this.app.numProcessedShortcuts     + NL
         + "Shortcuts updated: "               + this.app.numUpdatedShortcuts       + NL
         + "Shortcut files renamed: "          + this.app.numRenamedShortcuts       + NL
         + "Shortcut-file rename collisions: " + this.app.numRenameCollisions       + NL
         + "Shortcut-file renames skipped: "   + this.app.numRenameSkippedShortcuts + NL
         + "Shortcuts skipped: "               + this.app.numSkippedShortcuts       + NL2
         + buildLogsLocationMessage();
  }
  
  /**
   * Logs the final shortcut totals, so that the logs end with the same summary shown by the CLI.
   */
  private void logFinalSummary() {
  
    this.appContext.outUser(NL + "Total shortcuts to process :     "        + this.app.numTotalShortcuts        + " .");
    
    this.appContext.outUser(NL + "Total shortcuts needing update : "        + this.app.numProcessedShortcuts    + " .");
    
    this.appContext.outUser(NL + "Total shortcuts updated :        "        + this.app.numUpdatedShortcuts      + " .");
    
    this.appContext.outUser(NL + "Total shortcut files renamed :   "        + this.app.numRenamedShortcuts      + " .");
    
    this.appContext.outUser(NL + "Total shortcut-file rename collisions : " + this.app.numRenameCollisions      + " .");
    
    this.appContext.outUser(NL + "Total shortcut-file renames skipped : "   + this.app.numRenameSkippedShortcuts + " .");
    
    this.appContext.outUser(NL + "Total shortcuts skipped :        "        + this.app.numSkippedShortcuts       + " .");
  }
  
  /**
   * Builds the message that tells where the logs were written.
   *
   * @return The message.
   */
  @SuppressWarnings("ConstantValue")
  private String buildLogsLocationMessage() {
  
    String result = "Logs: not written to files.";
    
    if (this.appContext.userLog != null && this.appContext.userLog.logFile != null) {
    
      final File logFile = this.appContext.userLog.logFile;
      
      final File logFolder = logFile.getParentFile() == null ? logFile : logFile.getParentFile();
      
      result = "Logs written to folder: " + dq(folderWithSeparator(logFolder));
    }
    return result;
  }
  
  /**
   * Shows the program information.
   */
  private void showAbout() {
  
    showTextDialog("About " + MoveWithShortcutsMain.APP_NAME, buildAboutMessage(), this.aboutIcon);
  }
  
  /**
   * Shows the command-line usage text.
   */
  private void showHelp() {
  
    showTextDialog("Help", MoveWithShortcutsMain.getUsageText(), this.aboutIcon);
  }
  
  /**
   * Builds the program information message.
   *
   * @return The message.
   */
  private static String buildAboutMessage() {
  
    final ObjectAndDescr<String> version = AAppContext.getVersion(MoveWithShortcutsGuiMain.class);
    
    final String versionText = version.object != null ? version.object
                                                      : version.description != null ? version.description : "unknown";
                                                      
    final Package appPackage = MoveWithShortcutsGuiMain.class.getPackage();
    
    final String vendor = appPackage != null ? appPackage.getImplementationVendor() : null;
    
    final StringBuilder result = new StringBuilder();
    
    result.append(MoveWithShortcutsMain.APP_NAME).append(NL2);
    
    result.append(MoveWithShortcutsMain.APP_DESCR).append(NL2);
    
    result.append("Version: ").append(versionText).append(NL);
    
    if (isNotBlank(vendor)) {
    
      result.append("Vendor: ").append(vendor).append(NL);
    }
    result.append(NL);
    
    result.append(OSUtilities.getDescription()).append(NL2);
    
    result.append("Author: Davide Vecchi").append(NL);
    
    result.append("Copyright © 2026 Davide Vecchi").append(NL);
    
    result.append("GitHub: https://github.com/davide-vecchi/MoveWithShortcuts");
    
    return result.toString();
  }
  
  /**
   * Shows a resizable, scrollable, read-only text dialog.
   *
   * @param title The dialog title.<br>
   *
   * @param text The text to show.<br>
   *
   * @param icon The icon to show next to the text, or {@code null}.
   */
  private void showTextDialog(@NotNull String title, @NotNull String text, Icon icon) {
  
    final StatusTextArea textArea = StatusTextArea.newInstance();
    
    textArea.setText(text);
    
    textArea.setEditable(false);
    
    textArea.setLineWrap(true);
    
    textArea.setWrapStyleWord(true);
    
    textArea.setCaretPosition(ZERO_i);
    
    textArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    
    final JScrollPane scrollPane = new JScrollPane(textArea);
    
    scrollPane.setPreferredSize(new Dimension(600, 300));
    
    final JButton closeButton = new JButton("Close");
    
    final JDialog dialog = new JDialog(this.frame, title, true);
    
    closeButton.addActionListener(e -> dialog.dispose());
    
    final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    buttonPanel.add(closeButton);
    
    final JPanel contentPanel = new JPanel(new BorderLayout(8, 8));
    
    contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    
    if (icon != null) {
    
      contentPanel.add(new JLabel(icon), BorderLayout.WEST);
    }
    contentPanel.add(scrollPane, BorderLayout.CENTER);
    
    contentPanel.add(buttonPanel, BorderLayout.SOUTH);
    
    dialog.setContentPane(contentPanel);
    
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    
    if (this.appIcon != null) {
    
      dialog.setIconImage(this.appIcon.getImage());
    }
    dialog.getRootPane().setDefaultButton(closeButton);
    
    dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose()
                                                 , KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
                                                 , JComponent.WHEN_IN_FOCUSED_WINDOW);
                                                 
    dialog.pack();
    
    dialog.setMinimumSize(new Dimension(400, 250));
    
    dialog.setResizable(true);
    
    dialog.setLocationRelativeTo(this.frame);
    
    dialog.setVisible(true);
  }
  
  /**
   * Shows an error dialog.
   *
   * @param e The error.
   */
  private void showError(@NotNull Throwable e) {
  
    final String description = getFullDescriptionWithRootCause(e);
    
    appendConsoleText(GuiTextKind.ERROR, description);
    
    JOptionPane.showMessageDialog(this.frame, description + NL2 + buildLogsLocationMessage()
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
  
    this.textReporter.setListener(null);
    
    this.progressReporter.setListener(null);
    
    try {
    
      this.appContext.close();
    }
    catch (Exception e) {
    
      // The application is exiting, so there is nowhere to report this.
    }
  }
  
  
  /**
   * A read-only text area that fills the available height when its content is shorter than the viewport, and scrolls
   * when the content is longer.
   */
  private static final class StatusTextArea extends JTextArea {
  
  
    @Serial
    private static final long serialVersionUID = 1L;
    
    
    /**
     * Factory method.
     *
     * @return The new text area.
     */
    static StatusTextArea newInstance() {
    
      return new StatusTextArea();
    }
    
    /**
     * Private constructor.
     */
    private StatusTextArea() {
    
      super(0, 40);
    }
    
    
    /**
     * Tracks the viewport height when the content is shorter than the viewport.
     *
     * @return Whether to track the viewport height.
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
    
      return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
    }
  }
  
  
  /**
   * A file chooser that hides the "Files of type" filter controls, for folder-only selections.
   */
  private static final class FolderFileChooser extends JFileChooser {
  
  
    @Serial
    private static final long serialVersionUID = 1L;
    
    
    /**
     * Creates the dialog and hides the "Files of type" filter controls.
     *
     * @param parent The parent component, or {@code null}.
     *
     * @return The dialog.
     *
     * @throws HeadlessException If the dialog cannot be created in a headless environment.
     */
    @Override
    protected JDialog createDialog(Component parent) {
    
      final JDialog result = super.createDialog(parent);
      
      hideFileTypeFilter(result.getContentPane());
      
      return result;
    }
    
    /**
     * Hides the "Files of type" label and its combo box, if present.
     *
     * @param component The component to inspect.
     */
    private static void hideFileTypeFilter(@NotNull Component component) {
    
      if (component instanceof JLabel label && isFilesOfTypeLabel(label)) {
      
        label.setVisible(false);
        
        final Component target = label.getLabelFor();
        
        if (target != null) {
        
          target.setVisible(false);
        }
      }
      else if (component instanceof Container container) {
      
        for (final Component child : container.getComponents()) {
        
          hideFileTypeFilter(child);
        }
      }
    }
    
    /**
     * Tells whether the given label is the "Files of type" label.
     *
     * @param label The label to inspect.
     *
     * @return Whether it is the "Files of type" label.
     */
    private static boolean isFilesOfTypeLabel(@NotNull JLabel label) {
    
      final String text = UIManager.getString("FileChooser.filesOfTypeLabelText");
      
      return text != null && text.equals(label.getText());
    }
  }
  
}
