package bitronix.tm.gui;

import bitronix.tm.utils.Decoder;
import bitronix.tm.journal.TransactionLogRecord;

import javax.swing.*;
import java.awt.*;
import java.util.Date;
import java.util.Set;
import java.util.Iterator;

/**
 * <p></p>
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class TransactionLogDialog extends JDialog {

    private JPanel labelPanel = new JPanel();
    private JLabel statusLabel = new JLabel("Status");
    private JLabel recordLengthLabel = new JLabel("Record length");
    private JLabel headerLengthLabel = new JLabel("Header length");
    private JLabel timeLabel = new JLabel("Time");
    private JLabel sequenceNumberLabel = new JLabel("Sequence number");
    private JLabel crc32Label = new JLabel("CRC");
    private JLabel gtridLabel = new JLabel("GTRID");
    private JLabel uniqueNamesLabel = new JLabel("Resources");

    private JPanel fieldPanel = new JPanel();
    private JTextField statusField = new JTextField();
    private JTextField recordLengthField = new JTextField();
    private JTextField headerLengthField = new JTextField();
    private JTextField timeField = new JTextField();
    private JTextField sequenceNumberField = new JTextField();
    private JTextField crc32Field = new JTextField();
    private JTextField gtridField = new JTextField();
    private JTextField uniqueNamesField = new JTextField();


    public TransactionLogDialog(JFrame frame, TransactionLogRecord tlog) {
        super(frame, "Transaction log details", true);


        statusField.setText(Decoder.decodeStatus(tlog.getStatus()));
        recordLengthField.setText(""+tlog.getRecordLength());
        headerLengthField.setText(""+tlog.getHeaderLength());
        timeField.setText(Console.dateFormatter.format(new Date(tlog.getTime())));
        sequenceNumberField.setText(""+tlog.getSequenceNumber());
        if (tlog.isCrc32Correct()) {
            crc32Field.setText(""+tlog.getCrc32());
        }
        else {
            crc32Field.setText(tlog.getCrc32() + " (should be: " + tlog.calculateCrc32() + ")");
            crc32Field.setBackground(Color.RED);
        }
        gtridField.setText(tlog.getGtrid().toString());
        uniqueNamesField.setText(buildString(tlog.getUniqueNames()));

        statusField.setEditable(false);
        recordLengthField.setEditable(false);
        headerLengthField.setEditable(false);
        timeField.setEditable(false);
        sequenceNumberField.setEditable(false);
        crc32Field.setEditable(false);
        gtridField.setEditable(false);
        uniqueNamesField.setEditable(false);


        labelPanel.add(statusLabel); fieldPanel.add(statusField);
        labelPanel.add(recordLengthLabel); fieldPanel.add(recordLengthField);
        labelPanel.add(headerLengthLabel); fieldPanel.add(headerLengthField);
        labelPanel.add(timeLabel); fieldPanel.add(timeField);
        labelPanel.add(sequenceNumberLabel); fieldPanel.add(sequenceNumberField);
        labelPanel.add(crc32Label); fieldPanel.add(crc32Field);
        labelPanel.add(gtridLabel); fieldPanel.add(gtridField);
        labelPanel.add(uniqueNamesLabel); fieldPanel.add(uniqueNamesField);

        labelPanel.setLayout(new GridLayout(8, 1));
        fieldPanel.setLayout(new GridLayout(8, 1));
        getContentPane().add(labelPanel);
        getContentPane().add(fieldPanel);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));

        pack();
        int xPos = (frame.getBounds().width - 600) / 2;
        int yPos = (frame.getBounds().height - getSize().height) / 2;
        setBounds(xPos, yPos, 600, getSize().height);
    }

    private String buildString(Set uniqueNames) {
        StringBuffer sb = new StringBuffer();

        Iterator it = uniqueNames.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            sb.append(o);

            if (it.hasNext())
                sb.append(", ");
        }

        return sb.toString();
    }

}
