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
