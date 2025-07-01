package com.work.process;

import java.awt.BorderLayout;
import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 *
 * @author ajuar
 */
public class Process extends JFrame {

    private final OperatingSystem os;
    private final ProcessTableModel model;

    private final JTable table;

    private Timer updateTimer;

    public Process() {

        os = new SystemInfo().getOperatingSystem();
        model = new ProcessTableModel(fetchProcesses());

        JToolBar toolBar = new JToolBar();
        JButton refreshButton = new JButton("Refrescar");
        JButton killButton = new JButton("Terminar Proceso");
        JButton exportButton = new JButton("Exportar");

        toolBar.add(refreshButton);
        toolBar.add(killButton);
        toolBar.add(exportButton);

        // Acciones
        refreshButton.addActionListener(e -> loadProcesses());
        killButton.addActionListener(e -> killSelectedProcess());
        exportButton.addActionListener(e -> exportProcesses());

        table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        startAutoUpdate();

        setTitle("Process");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        chooser.setDialogTitle("Guardar como");

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            try (PrintWriter writer = new PrintWriter(file)) {
                for (int i = 0; i < model.getRowCount(); i++) {

                    for (int j = 0; j < ProcessTableModel.COLS.length; j++) {
                        writer.printf("%s: %s%n",
                                ProcessTableModel.COLS[j],
                                model.getValueAt(i, j));
                    }

                    writer.println();
                }
                showMessage("Exportación completada.");
            } catch (Exception ex) {
                showError("Error al exportar: " + ex.getMessage());
            }
        }
    }

    private void loadProcesses() {
        model.setData(fetchProcesses());
    }

    private void killSelectedProcess() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            showError("Selecciona un proceso para terminar.");
            return;
        }
        int pid = Integer.parseInt(model.getValueAt(selectedRow, 1).toString(), 16);
        OSProcess process = os.getProcess(pid);

        if (process == null) {
            showError("No se encontro el proceso.");
            return;
        }

        boolean result = killProcess(process);
        if (result) {
            showMessage("Proceso " + process.getName() + " terminado.");
            loadProcesses();
        } else {
            showError("No se pudo terminar el proceso.");
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
            System.out.println("Intentando terminar proceso: " + process.getName());

            if (ph.supportsNormalTermination()) {
                result = ph.destroy(); // Terminación amigable
            } else {
                result = ph.destroyForcibly(); // Fuerza terminación
            }

            ph.onExit().thenRun(()
                    -> System.out.println("Proceso " + process.getName() + " terminado")
            );
        }

        return result;
    }

    private void startAutoUpdate() {
        if (updateTimer != null && updateTimer.isRunning()) {
            return;
        }

        updateTimer = new Timer(5000, e -> updateCpuAndMemory()); // cada 5 segundos
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
                table.setRowSelectionInterval(selectedRow, selectedRow); // Restaurar selección
                table.scrollRectToVisible(table.getCellRect(selectedRow, 0, true)); // Asegurar visibilidad
            }
        });
    }
}
