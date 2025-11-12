package videoapp.ui;

import java.awt.Color;

/**
 * Encapsulates a light and dark theme. Implemented as an enum so each preset is
 * a singleton and can be reused anywhere without reallocation.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public enum ThemePalette {
    LIGHT(
            new Color(248, 248, 248), // window
            new Color(232, 232, 232), // slider surface
            new Color(232, 232, 232), // video bg
            new Color(248, 248, 248), // panels
            new Color(248, 248, 248), // controls background
            new Color(32, 32, 32),    // control fg
            new Color(210, 210, 210), // borders
            new Color(20, 20, 20),    // text
            new Color(0, 0, 0),       // icons
            new Color(55, 125, 255),  // accent
            false
    ),
    DARK(
            new Color(11, 11, 11),
            new Color(34, 34, 34),
            new Color(24, 24, 24),
            new Color(11, 11, 11),
            new Color(11, 11, 11),
            new Color(235, 235, 235),
            new Color(45, 45, 45),
            new Color(235, 235, 235),
            new Color(235, 235, 235),
            new Color(90, 155, 255),
            true
    );

    private final Color windowBackground;
    private final Color surfaceBackground;
    private final Color videoBackground;
    private final Color panelBackground;
    private final Color controlBackground;
    private final Color controlForeground;
    private final Color borderColor;
    private final Color textColor;
    private final Color iconColor;
    private final Color accentColor;
    private final boolean dark;

    ThemePalette(Color windowBackground, Color surfaceBackground, Color videoBackground,
                 Color panelBackground, Color controlBackground, Color controlForeground,
                 Color borderColor, Color textColor, Color iconColor, Color accentColor,
                 boolean dark) {
        this.windowBackground = windowBackground;
        this.surfaceBackground = surfaceBackground;
        this.videoBackground = videoBackground;
        this.panelBackground = panelBackground;
        this.controlBackground = controlBackground;
        this.controlForeground = controlForeground;
        this.borderColor = borderColor;
        this.textColor = textColor;
        this.iconColor = iconColor;
        this.accentColor = accentColor;
        this.dark = dark;
    }

    public static ThemePalette of(boolean dark) {
        return dark ? DARK : LIGHT;
    }

    public Color windowBackground() { return windowBackground; }
    public Color surfaceBackground() { return surfaceBackground; }
    public Color videoBackground() { return videoBackground; }
    public Color panelBackground() { return panelBackground; }
    public Color controlBackground() { return controlBackground; }
    public Color controlForeground() { return controlForeground; }
    public Color borderColor() { return borderColor; }
    public Color textColor() { return textColor; }
    public Color iconColor() { return iconColor; }
    public Color accentColor() { return accentColor; }
    public boolean isDark() { return dark; }
}
