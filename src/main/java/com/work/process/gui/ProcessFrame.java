package com.work.process.gui;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ajuar
 */
public class ProcessFrame extends JFrame {
    private static final Logger log
            = Logger.getLogger(ProcessFrame.class.getName());

    private final transient OperatingSystem os;
    private final ProcessTableModel model;

    private final JTable table;

    private Timer updateTimer;

    public ProcessFrame() {

        os = new SystemInfo().getOperatingSystem();
        model = new ProcessTableModel(fetchProcesses());

        JToolBar toolBar = new JToolBar();
        JButton refreshButton = new JButton("Refresh");
        JButton killButton = new JButton("End Process");
        JButton exportButton = new JButton("Export");

        toolBar.add(refreshButton);
        toolBar.add(killButton);
        toolBar.add(exportButton);

        refreshButton.addActionListener(e -> loadProcesses());
        killButton.addActionListener(e -> killSelectedProcess());
        exportButton.addActionListener(e -> exportProcesses());

        table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        startAutoUpdate();

        setTitle("Process");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        setSize(800, 600);

        setLocationRelativeTo(null);
        setVisible(true);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopAutoUpdate();
            }
        });

    }

    private void exportProcesses() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save as");

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            try (PrintWriter writer = new PrintWriter(file)) {
                for (int i = 0; i < model.getRowCount(); i++) {

                    for (int j = 0; j < model.getColumnCount(); j++) {
                        writer.printf("%s: %s%n",
                                model.getColumName(j),
                                model.getValueAt(i, j));
                    }

                    writer.println();
                }
                showMessage("Export completed.");
            } catch (Exception ex) {
                showError("Export error: " + ex.getMessage());
            }
        }
    }

    private void loadProcesses() {
        model.setData(fetchProcesses());
    }

    private void killSelectedProcess() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            showError("Select a process to finish.");
            return;
        }
        int pid = Integer.parseInt(model.getValueAt(selectedRow, 1).toString(), 16);
        OSProcess process = os.getProcess(pid);

        if (process == null) {
            showError("The process was not found.");
            return;
        }

        boolean result = killProcess(process);
        if (result) {
            showMessage("Process " + process.getName() + " completed.");
            loadProcesses();
        } else {
            showError("The process could not be completed.");
        }
    }

    private void showMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private List<OSProcess> fetchProcesses() {
        return os.getProcesses(OperatingSystem.ProcessFiltering.VALID_PROCESS,
                OperatingSystem.ProcessSorting.NAME_ASC, 0);
    }

    private boolean killProcess(OSProcess process) {
        boolean result = false;
        Optional<ProcessHandle> optProcess = ProcessHandle.of(process.getProcessID());
        if (optProcess.isPresent()) {
            ProcessHandle ph = optProcess.get();
            log.log(Level.INFO, "Trying to end process: {0}", process.getName());

            if (ph.supportsNormalTermination()) {
                result = ph.destroy();
            } else {
                result = ph.destroyForcibly();
            }

            ph.onExit().thenRun(()
                    -> log.log(Level.INFO, "Proceso {0} terminado", process.getName())
            );
        }

        return result;
    }

    private void startAutoUpdate() {
        if (updateTimer != null && updateTimer.isRunning()) {
            return;
        }

        updateTimer = new Timer(5000, e -> updateCpuAndMemory());
        updateTimer.start();
    }

    private void stopAutoUpdate() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
    }

    private void updateCpuAndMemory() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalScrollBar = ((JScrollPane) table.getParent().getParent()).getVerticalScrollBar();
            int scrollPosition = verticalScrollBar.getValue();
            int selectedRow = table.getSelectedRow();
            model.setData(fetchProcesses());
            verticalScrollBar.setValue(scrollPosition);
            if (selectedRow >= 0 && selectedRow < table.getRowCount()) {
                table.setRowSelectionInterval(selectedRow, selectedRow);
                table.scrollRectToVisible(table.getCellRect(selectedRow, 0, true));
            }
        });
    }
}
