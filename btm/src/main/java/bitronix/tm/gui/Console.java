/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.gui;

import bitronix.tm.BitronixXid;
import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.journal.JournalRecord;
import bitronix.tm.utils.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.transaction.Status;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p></p>
 *
 * @author Ludovic Orban
 */
public class Console extends JFrame {

    private final static Logger log = LoggerFactory.getLogger(Console.class);

    protected static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JTable rawViewTransactionsTable = new JTable();
    private final JTable pendingViewTransactionsTable = new JTable();
    private final JScrollPane rawTransactionsTableScrollpane = new JScrollPane(rawViewTransactionsTable);
    private final JScrollPane pendingTransactionsTableScrollpane = new JScrollPane(pendingViewTransactionsTable);
    private final ResourcesPanel resourcesPanel = new ResourcesPanel();
    private final JPanel statusBarPanel = new JPanel();
    private final JLabel statusLabel = new JLabel();
    private final TransactionLogHeaderPanel transactionLogHeaderPanel1 = new TransactionLogHeaderPanel();
    private final TransactionLogHeaderPanel transactionLogHeaderPanel2 = new TransactionLogHeaderPanel();
    private final JMenuBar menuBar = new JMenuBar();


    public Console() throws IOException {
        final Configuration configuration = TransactionManagerServices.getConfiguration();

        JMenu findMenu = new JMenu("Find");
        menuBar.add(findMenu);
        JMenuItem bySequenceItem = new JMenuItem("First by sequence");
        JMenuItem byGtridItem = new JMenuItem("First by GTRID");
        findMenu.add(bySequenceItem);
        findMenu.add(byGtridItem);

        JMenu analysisMenu = new JMenu("Analysis");
        menuBar.add(analysisMenu);
        JMenuItem switchLogFilesItem = new JMenuItem("Switch log files");
        analysisMenu.add(switchLogFilesItem);
        JMenuItem countDuplicatedGtridsItem = new JMenuItem("Count duplicated GTRID");
        analysisMenu.add(countDuplicatedGtridsItem);
        JMenuItem countByStatus = new JMenuItem("Count by status");
        analysisMenu.add(countByStatus);

        transactionLogHeaderPanel1.read(getActiveLogFile(configuration), true);
        transactionLogHeaderPanel2.read(getPassiveLogFile(configuration), false);

        pendingViewTransactionsTable.setModel(new PendingTransactionTableModel(getActiveLogFile(configuration)));
        pendingViewTransactionsTable.addMouseListener(new TransactionTableMouseListener(this, pendingViewTransactionsTable));

        rawViewTransactionsTable.setDefaultRenderer(String.class, new TransactionTableCellRenderer());
        rawViewTransactionsTable.setModel(new RawTransactionTableModel(getActiveLogFile(configuration)));
        rawViewTransactionsTable.addMouseListener(new TransactionTableMouseListener(this, rawViewTransactionsTable));

        final JPopupMenu rawViewTransactionsTablePopupMenu = new JPopupMenu();
        final JCheckBoxMenuItem filterByGtridItem = new JCheckBoxMenuItem("Filter by GTRID");
        filterByGtridItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterByGtrid(filterByGtridItem.isSelected());
            }
        });
        rawViewTransactionsTablePopupMenu.add(filterByGtridItem);
        rawViewTransactionsTable.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }
            @Override
            public void mouseEntered(MouseEvent e) {
            }
            @Override
            public void mouseExited(MouseEvent e) {
            }
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    rawViewTransactionsTablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    int row = rawViewTransactionsTable.rowAtPoint(new Point(e.getX(), e.getY()));
                    selectTableRow(rawViewTransactionsTable, row);
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                mousePressed(e);
            }
        });

        tabbedPane.add("Pending logs", pendingTransactionsTableScrollpane);
        tabbedPane.add("Raw logs", rawTransactionsTableScrollpane);
        tabbedPane.add("Resources", resourcesPanel);

        refreshStatus();

        statusBarPanel.setLayout(new GridLayout(3, 1, 1, 1));
        statusBarPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        statusBarPanel.add(transactionLogHeaderPanel1);
        statusBarPanel.add(transactionLogHeaderPanel2);
        statusBarPanel.add(statusLabel);

        switchLogFilesItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                switchLogFiles(configuration);
            }
        });

        countDuplicatedGtridsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                countDuplicatedGtrids();
            }
        });

        countByStatus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                countByStatus();
            }
        });

        bySequenceItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                findBySequence();
            }
        });

        byGtridItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                findByGtrid();
            }
        });

        setTitle("Bitronix Transaction Manager Console");
        setJMenuBar(menuBar);
        getContentPane().setLayout(new BorderLayout(0, 2));
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(statusBarPanel, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setVisible(true);
    }


    private File activeLogFile;
    private File passiveLogFile;
    private File realActiveLogFile;

    private File getActiveLogFile(Configuration configuration) throws IOException {
        if (activeLogFile == null) {
            activeLogFile = pickCurrentLogFile(new File(configuration.getLogPart1Filename()), new File(configuration.getLogPart2Filename()));
            realActiveLogFile = activeLogFile;
            if (log.isDebugEnabled()) { log.debug("active file is " + activeLogFile.getName()); }
        }
        return activeLogFile;
    }

    public static File pickCurrentLogFile(File file1, File file2) throws IOException {
        RandomAccessFile activeRandomAccessFile = null;
        long timestamp1;
        try {
	        activeRandomAccessFile = new RandomAccessFile(file1, "r");
	        int formatId1 = activeRandomAccessFile.readInt();
	        if (formatId1 != BitronixXid.FORMAT_ID)
	            throw new IOException("log file 1 " + file1.getName() + " is not a Bitronix Log file (incorrect header)");
	        timestamp1 = activeRandomAccessFile.readLong();
        }
        finally {
        	activeRandomAccessFile.close();
        }

        activeRandomAccessFile = new RandomAccessFile(file2, "r");
        try {
	        int formatId2 = activeRandomAccessFile.readInt();
	        if (formatId2 != BitronixXid.FORMAT_ID)
	            throw new IOException("log file 2 " + file2.getName() + " is not a Bitronix Log file (incorrect header)");
	        long timestamp2 = activeRandomAccessFile.readLong();

	        if (timestamp1 > timestamp2) {
	            return file1;
	        } else {
	            return file2;
	        }
        }
        finally {
        	activeRandomAccessFile.close();
        }
    }

    private File getPassiveLogFile(Configuration configuration) throws IOException {
        if (passiveLogFile == null) {
            if (getActiveLogFile(configuration).getName().equals(configuration.getLogPart1Filename()))
                passiveLogFile = new File(configuration.getLogPart2Filename());
            else
                passiveLogFile = new File(configuration.getLogPart1Filename());
        }
        return passiveLogFile;
    }

    private void refreshStatus() {
        statusLabel.setText("active log file is " + realActiveLogFile.getName() + " - displayed log file contains " + pendingViewTransactionsTable.getModel().getRowCount() + " dangling transaction log(s) over " + rawViewTransactionsTable.getModel().getRowCount() + " total transaction log(s)");
    }

    private void switchLogFiles(Configuration configuration) {
        File temp = activeLogFile;
        activeLogFile = passiveLogFile;
        passiveLogFile = temp;
        File realPassive = activeLogFile == realActiveLogFile ? passiveLogFile : activeLogFile;


        try {
            transactionLogHeaderPanel1.read(realActiveLogFile, configuration.getLogPart1Filename().equals(activeLogFile.getName()));
            transactionLogHeaderPanel2.read(realPassive, configuration.getLogPart2Filename().equals(activeLogFile.getName()));

            pendingViewTransactionsTable.setModel(new PendingTransactionTableModel(getActiveLogFile(configuration)));
            rawViewTransactionsTable.setModel(new RawTransactionTableModel(getActiveLogFile(configuration)));

            refreshStatus();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Reloading model of switched logs failed. Try again.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void countDuplicatedGtrids() {
        TransactionTableModel transactionTableModel = (TransactionTableModel) rawViewTransactionsTable.getModel();

        HashMap gtrids = new HashMap();
        HashMap redundantGtrids = new HashMap();

        for (int i = 0; i < transactionTableModel.getRowCount(); i++) {
            JournalRecord tlog = transactionTableModel.getRow(i);
            if (tlog.getStatus() == Status.STATUS_COMMITTING) {
                Uid gtrid = tlog.getGtrid();
                if (gtrids.containsKey(gtrid)) {
                    java.util.List tlogs = (java.util.List) gtrids.get(gtrid);
                    tlogs.add(tlog);
                    redundantGtrids.put(gtrid, tlogs);
                }
                else {
                    java.util.List tlogs = new ArrayList();
                    tlogs.add(tlog);
                    gtrids.put(gtrid, tlogs);
                }
            }
        }

        JTable table = new JTable(new DuplicatedGtridTableModel(redundantGtrids));
        JScrollPane scrollPane = new JScrollPane(table);
        JDialog dialog = new JDialog(this, redundantGtrids.size() + " duplicated GTRIDs found");
        dialog.getContentPane().add(scrollPane);
        dialog.pack();
        dialog.setModal(false);
        dialog.setVisible(true);

//        JOptionPane.showMessageDialog(this, redundantGtrids.size() + " duplicated GTRID", "Duplicated GTRID count", JOptionPane.INFORMATION_MESSAGE);
    }

    private void countByStatus() {
        TransactionTableModel transactionTableModel = (TransactionTableModel) rawViewTransactionsTable.getModel();

        int preparing = 0;
        int prepared = 0;
        int rollingback = 0;
        int rolledback = 0;
        int committing = 0;
        int committed = 0;
        int active = 0;
        int unknown = 0;

        for (int i = 0; i < transactionTableModel.getRowCount(); i++) {
            JournalRecord tlog = transactionTableModel.getRow(i);
            switch (tlog.getStatus()) {
                case Status.STATUS_ACTIVE:
                    active++;
                    break;
                case Status.STATUS_PREPARING:
                    preparing++;
                    break;
                case Status.STATUS_PREPARED:
                    prepared++;
                    break;
                case Status.STATUS_COMMITTING:
                    committing++;
                    break;
                case Status.STATUS_COMMITTED:
                    committed++;
                    break;
                case Status.STATUS_ROLLING_BACK:
                    rollingback++;
                    break;
                case Status.STATUS_ROLLEDBACK:
                    rolledback++;
                    break;
                default:
                    unknown++;
            }
        }

        String message =  "Active: " + active + "\n"
                + "Preparing: " + preparing + "\n"
                + "Prepared: " + prepared + "\n"
                + "Committing: " + committing + "\n"
                + "Committed: " + committed + "\n"
                + "Rolling back: " + rollingback + "\n"
                + "Rolled back: " + rolledback;
        if (unknown > 0)
            message += "\nUnknown: " + unknown;

        JOptionPane.showMessageDialog(this, message, "Count by status", JOptionPane.INFORMATION_MESSAGE);
    }

    private void findBySequence() {
        String sequence = JOptionPane.showInputDialog(this, "Enter sequence to search for");
        int searchedSequence;
        try {
            searchedSequence = new Integer(sequence).intValue();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please input a number", "Find by sequence", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (tabbedPane.getSelectedComponent() == pendingTransactionsTableScrollpane) {
            TransactionTableModel transactionTableModel = (TransactionTableModel) pendingViewTransactionsTable.getModel();
            selectTLogMatchingSequence(transactionTableModel, searchedSequence, pendingViewTransactionsTable);
        } else {
            TransactionTableModel transactionTableModel = (TransactionTableModel) rawViewTransactionsTable.getModel();
            selectTLogMatchingSequence(transactionTableModel, searchedSequence, rawViewTransactionsTable);
        }
    }

    private void findByGtrid() {
        String gtrid = JOptionPane.showInputDialog(this, "Enter GTRID to search for");

        if (tabbedPane.getSelectedComponent() == pendingTransactionsTableScrollpane) {
            TransactionTableModel transactionTableModel = (TransactionTableModel) pendingViewTransactionsTable.getModel();
            selectTLogMatchingGtrid(transactionTableModel, gtrid, pendingViewTransactionsTable);
        } else {
            TransactionTableModel transactionTableModel = (TransactionTableModel) rawViewTransactionsTable.getModel();
            selectTLogMatchingGtrid(transactionTableModel, gtrid, rawViewTransactionsTable);
        }
    }

    private void filterByGtrid(boolean filter) {
        RawTransactionTableModel model = (RawTransactionTableModel) rawViewTransactionsTable.getModel();
        if (filter) {
            int selectedRow = rawViewTransactionsTable.getSelectedRow();
            String gtrid = (String) model.getValueAt(selectedRow, RawTransactionTableModel.GTRID_COL);
            model.filterByGtrid(gtrid);
        }
        else {
            model.filterByGtrid(null);
        }
        rawViewTransactionsTable.repaint();
    }

    private void selectTLogMatchingSequence(TransactionTableModel transactionTableModel, int sequenceNumber, JTable table) {
        int startIndex = table.getSelectedRow() + 1;
        Number sequence = sequenceNumber;

        for (int i = startIndex; i < transactionTableModel.getRowCount(); i++) {
            JournalRecord tlog = transactionTableModel.getRow(i);
            if (sequence.equals(tlog.getRecordProperties().get("sequenceNumber"))) {
                selectTableRow(table, i);
                return;
            }
        }

        // if it is not found, search starting back at the beginning of the list up to where we previously started
        if (startIndex > 0) {
            for (int i = 0; i < startIndex; i++) {
                JournalRecord tlog = transactionTableModel.getRow(i);
                if (sequence.equals(tlog.getRecordProperties().get("sequenceNumber"))) {
                    selectTableRow(table, i);
                    return;
                }
            }
        }

        JOptionPane.showMessageDialog(this, "Not found", "Find by sequence", JOptionPane.INFORMATION_MESSAGE);
    }

    private void selectTLogMatchingGtrid(TransactionTableModel transactionTableModel, String gtrid, JTable table) {
        int startIndex = table.getSelectedRow() + 1;

        for (int i = startIndex; i < transactionTableModel.getRowCount(); i++) {
            JournalRecord tlog = transactionTableModel.getRow(i);
            if (tlog.getGtrid().toString().equals(gtrid)) {
                selectTableRow(table, i);
                return;
            }
        }

        // if it is not found, search starting back at the beginning of the list up to where we previously started
        if (startIndex > 0) {
            for (int i = 0; i < startIndex; i++) {
                JournalRecord tlog = transactionTableModel.getRow(i);
                if (tlog.getGtrid().toString().equals(gtrid)) {
                    selectTableRow(table, i);
                    return;
                }
            }
        }

        JOptionPane.showMessageDialog(this, "Not found", "Find by GTRID", JOptionPane.INFORMATION_MESSAGE);
    }

    private void selectTableRow(JTable table, int rowNum) {
        if (rowNum == -1)
            return;

        // select the row
        table.setRowSelectionInterval(rowNum, rowNum);

        // now scroll to the selected row

        JViewport viewport = (JViewport) table.getParent();
        Rectangle rect = table.getCellRect(rowNum, 0, true);
        // The location of the view relative to the table
        Rectangle viewRect = viewport.getViewRect();

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0).
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);
        viewport.scrollRectToVisible(rect);
    }

    public static void main(String[] args) throws Exception {
        try {
            new Console();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

}
