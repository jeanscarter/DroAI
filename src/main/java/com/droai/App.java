package com.droai;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import java.awt.*;

public class App {
    public static void main(String[] args) {
        FlatDarkLaf.setup();

        // ----- Rounded corners & smooth scrolling -----
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 14);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollPane.smoothScrolling", true);

        // ----- TabbedPane -----
        UIManager.put("TabbedPane.selectedBackground", new Color(40, 44, 52));
        UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
        UIManager.put("TabbedPane.focusColor", new Color(100, 160, 255));
        UIManager.put("TabbedPane.hoverColor", new Color(50, 55, 68));

        // ----- Table -----
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.selectionBackground", new Color(55, 90, 150));
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.gridColor", new Color(50, 55, 65));
        UIManager.put("TableHeader.separatorColor", new Color(60, 65, 78));
        UIManager.put("TableHeader.background", new Color(35, 38, 48));
        UIManager.put("TableHeader.foreground", new Color(180, 190, 210));

        // ----- ToolTip -----
        UIManager.put("ToolTip.background", new Color(45, 48, 58));
        UIManager.put("ToolTip.foreground", Color.WHITE);

        SwingUtilities.invokeLater(() -> {
            com.droai.ui.MainFrame frame = new com.droai.ui.MainFrame();
            frame.setVisible(true);
        });
    }
}
