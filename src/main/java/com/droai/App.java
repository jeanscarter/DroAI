package com.droai;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import java.awt.*;

public class App {
    public static void main(String[] args) {
        FlatDarkLaf.setup();

        // ----- Rounded corners & smooth scrolling (structural, theme-agnostic) -----
        UIManager.put("Button.arc", 14);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollPane.smoothScrolling", true);

        // ----- Table structural settings -----
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("Table.rowHeight", 30);

        SwingUtilities.invokeLater(() -> {
            com.droai.ui.MainFrame frame = new com.droai.ui.MainFrame();
            frame.setVisible(true);
        });
    }
}
