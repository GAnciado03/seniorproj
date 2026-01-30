package videoapp.ui;

import javax.swing.*;
import java.io.File;

/**
 * Defines how CSV chooser dialogs are configured and persisted.
 *
 * @author Glenn Anciado
 * @version 1.0
 */
public interface CsvChooserDelegate {
    void configure(JFileChooser chooser);
    File initialDirectory();
    void rememberSelection(File file);
}
