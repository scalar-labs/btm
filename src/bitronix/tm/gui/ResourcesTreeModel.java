package bitronix.tm.gui;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.ResourceLoader;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeModelListener;

/**
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
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
        return resourceLoader.getResourcesUniqueNames().get(index);
    }

    public int getIndexOfChild(Object parent, Object child) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
