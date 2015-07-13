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

import bitronix.tm.journal.TransactionLogHeader;
import bitronix.tm.journal.InterruptibleLockedRandomAccessFile;
import bitronix.tm.utils.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

/**
 * @author Ludovic Orban
 */
public class TransactionLogHeaderPanel extends JPanel {

    private final static Logger log = LoggerFactory.getLogger(TransactionLogHeaderPanel.class);

    private final JTextField logFileField = new JTextField();
    private final JTextField timestampField = new JTextField();
    private final JTextField stateField = new JTextField();
    private final JTextField positionField = new JTextField();

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
        InterruptibleLockedRandomAccessFile raf = new InterruptibleLockedRandomAccessFile(logFile, "r");
        TransactionLogHeader header = new TransactionLogHeader(raf, 0L);
        raf.close();
        if (log.isDebugEnabled()) { log.debug("read header: " + header); }
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
