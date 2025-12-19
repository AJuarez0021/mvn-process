package com.work.process;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.work.process.gui.ProcessFrame;

import javax.swing.SwingUtilities;

/**
 *
 * @author ajuar
 */
public class Main {

    
    
    public static void main(String[] args) {
        FlatMacDarkLaf.setup();
        SwingUtilities.invokeLater(ProcessFrame::new);
    }
}
