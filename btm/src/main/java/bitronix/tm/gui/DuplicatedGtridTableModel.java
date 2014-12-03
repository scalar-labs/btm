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

import bitronix.tm.journal.JournalRecord;

import javax.swing.table.DefaultTableModel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class DuplicatedGtridTableModel extends DefaultTableModel {

    private final Map duplicatedGtrids;

    public DuplicatedGtridTableModel(Map map) {
        super(map.size(), 2);
        this.duplicatedGtrids = map;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0: return "Sequence number";
            case 1: return "GTRID";
            default: return "?";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        Iterator it = duplicatedGtrids.entrySet().iterator();
        List tlogs = null;
        int i=0;
        while (i<=row && it.hasNext()) {
            i++;
            Map.Entry entry = (Map.Entry) it.next();
            tlogs = (List) entry.getValue();
        }

        if (tlogs == null)
            return null;

        switch (column) {
            case 0: return buildTlogsSequenceNumber(tlogs);
            case 1: return buildTlogsGtrid(tlogs);
            default: return "?";
        }
    }

    private String buildTlogsSequenceNumber(List tlogs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tlogs.size(); i++) {
            JournalRecord tlog = (JournalRecord) tlogs.get(i);
            sb.append(tlog.getRecordProperties().get("sequenceNumber"));
            if (i < tlogs.size() -1)
                sb.append(", ");
        }
        return sb.toString();
    }

    private String buildTlogsGtrid(List tlogs) {
        JournalRecord tlog = (JournalRecord) tlogs.get(0);
        return tlog.getGtrid().toString();
    }

}
