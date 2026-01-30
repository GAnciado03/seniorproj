package videoapp.ui;

import videoapp.util.ChooserUtils;

import javax.swing.*;
import java.io.File;

/**
 * Default implementation that applies CSV filters and remembers folders.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public final class DefaultCsvChooserDelegate implements CsvChooserDelegate {
    @Override
    public void configure(JFileChooser chooser) {
        ChooserUtils.applyCsvFilter(chooser);
    }

    @Override
    public File initialDirectory() {
        return ChooserUtils.initialCsvDir();
    }

    @Override
    public void rememberSelection(File file) {
        ChooserUtils.rememberCsvSelection(file);
    }
}
