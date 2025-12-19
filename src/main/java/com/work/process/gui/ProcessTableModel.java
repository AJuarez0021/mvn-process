package com.work.process.gui;

import java.util.List;
import javax.swing.table.AbstractTableModel;
import oshi.software.os.OSProcess;

/**
 *
 * @author ajuar
 */
public class ProcessTableModel extends AbstractTableModel {

    private static final String[] COLS = {
        "Process", "PID", "Memory", "Cpu", "User", "Base Priority", "Number of Threads", "Path"
    };

    private transient List<OSProcess> data;

    public ProcessTableModel(List<OSProcess> data) {
        this.data = data;
    }

    public void setData(List<OSProcess> data) {
        this.data = data;
        fireTableDataChanged();
    }

    public String getColumName(int index) {
        return COLS[index];
    }

    @Override
    public int getRowCount() {
        return data == null ? 0 : data.size();
    }

    @Override
    public int getColumnCount() {
        return COLS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLS[column];
    }

    @Override
    public Object getValueAt(int row, int col) {
        OSProcess p = data.get(row);

        return switch (col) {
            case 0 ->
                p.getName();
            case 1 ->
                Integer.toHexString(p.getProcessID()).toUpperCase();
            case 2 ->
                String.format("%.2f MB", p.getResidentSetSize() / (1024.0 * 1024.0));
            case 3 ->
                String.format("%.2f %%", p.getProcessCpuLoadCumulative() * 100);
            case 4 ->
                p.getUser();
            case 5 ->
                p.getPriority();
            case 6 ->
                p.getThreadCount();
            case 7 ->
                p.getPath();
            default ->
                "";
        };
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return switch (col) {
            case 1, 2, 3 ->
                Integer.class;
            default ->
                String.class;
        };
    }
}
