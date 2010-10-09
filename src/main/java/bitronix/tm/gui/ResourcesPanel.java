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

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.PropertyUtils;
import bitronix.tm.resource.ResourceLoader;
import bitronix.tm.resource.common.ResourceBean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author lorban
 */
public class ResourcesPanel extends JPanel {

    private JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    private JTree resourcesTree = new JTree();
    private JScrollPane resourcesTreeScrollpane = new JScrollPane(resourcesTree);

    private JTextArea activeResource = new JTextArea();

    public ResourcesPanel() {
        setLayout(new GridLayout(1, 1));
        resourcesTree.setModel(new ResourcesTreeModel());

        resourcesTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JTree tree = (JTree) evt.getSource();
                int selectedRow = tree.getSelectionRows()[0] -1;

                ResourceLoader resourceLoader = TransactionManagerServices.getResourceLoader();
                Iterator it = resourceLoader.getResources().entrySet().iterator();
                ResourceBean resource = null;
                for (int i=0; i<selectedRow+1 ;i++) {
                    Map.Entry entry = (Map.Entry) it.next();
                    resource = (ResourceBean) entry.getValue();
                }


                if (resource != null) {
                    try {
                        Map properties = PropertyUtils.getProperties(resource);
                        StringBuffer sb = new StringBuffer();
                        Iterator it2 = properties.entrySet().iterator();
                        while (it2.hasNext()) {
                            Map.Entry entry = (Map.Entry) it2.next();
                            String name = (String) entry.getKey();
                            Object valueObject = entry.getValue();
                            String value = valueObject == null ? null : valueObject.toString();

                            sb.append(name);
                            sb.append('=');
                            sb.append(value);
                            if (it2.hasNext())
                                sb.append('\n');
                        }

                        activeResource.setText(sb.toString());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "error querying resource loader", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                else
                    activeResource.setText("");
            }
        });

        splitPane.add(resourcesTreeScrollpane);
        splitPane.add(activeResource);
        add(splitPane);
    }

}
