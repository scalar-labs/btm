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
import bitronix.tm.resource.ResourceLoader;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.Iterator;

/**
 *
 * @author lorban
 */
public class ResourcesTreeModel implements TreeModel {

    private static final String ROOT = "Resource loader";
    private ResourceLoader resourceLoader;

    public ResourcesTreeModel() {
        resourceLoader = TransactionManagerServices.getResourceLoader();
    }

    public Object getRoot() {
        return ROOT;
    }

    public int getChildCount(Object parent) {
        if (parent.equals(ROOT))
            return resourceLoader.getResources().size();
        return 0;
    }

    public boolean isLeaf(Object node) {
        if (node.equals(ROOT))
            return false;
        return true;
    }

    public void addTreeModelListener(TreeModelListener l) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeTreeModelListener(TreeModelListener l) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getChild(Object parent, int index) {
        if (index < 0)
            return ROOT;

        Iterator it = resourceLoader.getResources().entrySet().iterator();
        Object result = null;
        for(int i= -1; i<index ;i++) {
            result = it.next();
        }
        return result;
    }

    public int getIndexOfChild(Object parent, Object child) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
