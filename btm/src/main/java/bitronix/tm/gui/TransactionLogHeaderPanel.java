/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm.gui;

import bitronix.tm.utils.Decoder;
import bitronix.tm.journal.TransactionLogHeader;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * <p></p>
 *
 * @author lorban
 */
public class TransactionLogHeaderPanel extends JPanel {

    private final static Logger log = LoggerFactory.getLogger(TransactionLogHeaderPanel.class);

    private JTextField logFileField = new JTextField();
    private JTextField timestampField = new JTextField();
    private JTextField stateField = new JTextField();
    private JTextField positionField = new JTextField();

    public TransactionLogHeaderPanel() {
        logFileField.setEditable(false);
        timestampField.setEditable(false);
        stateField.setEditable(false);
        positionField.setEditable(false);

        logFileField.setBorder(null);
        timestampField.setBorder(null);
        stateField.setBorder(null);
        positionField.setBorder(null);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(logFileField);
        add(timestampField);
        add(stateField);
        add(positionField);
    }

    public void setLogFile(File logFile) {
        logFileField.setText(logFile.getName());
    }

    public void setTimestamp(long timestamp) {
        timestampField.setText(Console.dateFormatter.format(new Date(timestamp)));
    }

    public void setState(byte state) {
        stateField.setText(Decoder.decodeHeaderState(state));
    }

    public void setPosition(long position) {
        positionField.setText("" + position);
    }

    public void read(File logFile, boolean active) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(logFile, "r");
        TransactionLogHeader header = new TransactionLogHeader(raf.getChannel(), 0L);
        raf.close();
        if (log.isDebugEnabled()) log.debug("read header: " + header);
        setLogFile(logFile);
        setTimestamp(header.getTimestamp());
        setState(header.getState());
        setPosition(header.getPosition());

        Font font;
        if (active) {
            font = logFileField.getFont().deriveFont(Font.BOLD);
        }
        else {
            font = logFileField.getFont().deriveFont(Font.PLAIN);
        }
        logFileField.setFont(font);
        timestampField.setFont(font);
        stateField.setFont(font);
        positionField.setFont(font);
    }
}
