package com.dwc.laf.ui;

import com.dwc.laf.DwcLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcTreeUI} and {@link DwcTreeExpandIcon} verifying UI installation,
 * per-component instances, row height, custom icons, and paint pipeline smoke tests.
 */
class DwcTreeUITest {

    private LookAndFeel previousLaf;

    @BeforeEach
    void setUp() throws Exception {
        previousLaf = UIManager.getLookAndFeel();
        UIManager.setLookAndFeel(new DwcLookAndFeel());
    }

    @AfterEach
    void tearDown() throws Exception {
        UIManager.setLookAndFeel(previousLaf);
    }

    @Test
    void testUIInstalled() {
        JTree tree = new JTree();
        assertInstanceOf(DwcTreeUI.class, tree.getUI(),
                "JTree should use DwcTreeUI when DwcLookAndFeel is active");
    }

    @Test
    void testPerComponentInstance() {
        JTree tree1 = new JTree();
        JTree tree2 = new JTree();
        assertNotSame(tree1.getUI(), tree2.getUI(),
                "Each JTree should have its own DwcTreeUI instance");
    }

    @Test
    void testRowHeight() {
        JTree tree = new JTree();
        assertEquals(24, tree.getRowHeight(),
                "Tree row height should be 24px");
    }

    @Test
    void testExpandedIconIsCustom() {
        Icon icon = UIManager.getIcon("Tree.expandedIcon");
        assertInstanceOf(DwcTreeExpandIcon.class, icon,
                "Tree.expandedIcon should be DwcTreeExpandIcon");
    }

    @Test
    void testCollapsedIconIsCustom() {
        Icon icon = UIManager.getIcon("Tree.collapsedIcon");
        assertInstanceOf(DwcTreeExpandIcon.class, icon,
                "Tree.collapsedIcon should be DwcTreeExpandIcon");
    }

    @Test
    void testExpandIconDimensions() {
        Icon expandedIcon = UIManager.getIcon("Tree.expandedIcon");
        assertEquals(12, expandedIcon.getIconWidth(),
                "Expand icon width should be 12");
        assertEquals(12, expandedIcon.getIconHeight(),
                "Expand icon height should be 12");
    }

    @Test
    void testPaintNoError() {
        JTree tree = createSampleTree();
        tree.setSize(200, 200);

        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> tree.paint(g2),
                    "Painting tree should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testPaintWithSelectionNoError() {
        JTree tree = createSampleTree();
        tree.setSize(200, 200);
        tree.setSelectionRow(0);

        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> tree.paint(g2),
                    "Painting tree with selection should not throw");
        } finally {
            g2.dispose();
        }
    }

    private JTree createSampleTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("Child 1");
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("Child 2");
        DefaultMutableTreeNode grandchild = new DefaultMutableTreeNode("Grandchild");
        child1.add(grandchild);
        root.add(child1);
        root.add(child2);
        return new JTree(new DefaultTreeModel(root));
    }
}
