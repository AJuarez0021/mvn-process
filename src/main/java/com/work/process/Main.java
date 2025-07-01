package com.work.process;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.SwingUtilities;

/**
 *
 * @author ajuar
 */
public class Main {

    
    
    public static void main(String[] args) {
        // Tema oscuro
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(Process::new);
    }
}
