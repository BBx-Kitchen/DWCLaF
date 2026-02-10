package com.dwc.laf.ui;

import com.dwc.laf.DwcLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcTableUI} verifying UI installation, per-component instances,
 * grid disabled, intercell spacing, row height, fill viewport, custom renderers,
 * alternate row color, and paint pipeline smoke tests.
 */
class DwcTableUITest {

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
        JTable table = createSampleTable();
        assertInstanceOf(DwcTableUI.class, table.getUI(),
                "JTable should use DwcTableUI when DwcLookAndFeel is active");
    }

    @Test
    void testPerComponentInstance() {
        JTable table1 = createSampleTable();
        JTable table2 = createSampleTable();
        assertNotSame(table1.getUI(), table2.getUI(),
                "Each JTable should have its own DwcTableUI instance");
    }

    @Test
    void testGridDisabled() {
        JTable table = createSampleTable();
        assertFalse(table.getShowHorizontalLines(),
                "Horizontal grid lines should be disabled");
        assertFalse(table.getShowVerticalLines(),
                "Vertical grid lines should be disabled");
    }

    @Test
    void testIntercellSpacingZero() {
        JTable table = createSampleTable();
        assertEquals(new Dimension(0, 0), table.getIntercellSpacing(),
                "Intercell spacing should be zero for modern clean look");
    }

    @Test
    void testRowHeightMinimum() {
        JTable table = createSampleTable();
        assertTrue(table.getRowHeight() >= 24,
                "Row height should be at least 24px, but was " + table.getRowHeight());
    }

    @Test
    void testFillsViewportHeight() {
        JTable table = createSampleTable();
        assertTrue(table.getFillsViewportHeight(),
                "Table should fill viewport height even with few rows");
    }

    @Test
    void testDefaultRendererInstalled() {
        JTable table = createSampleTable();
        assertInstanceOf(DwcTableCellRenderer.class, table.getDefaultRenderer(Object.class),
                "Default cell renderer should be DwcTableCellRenderer");
    }

    @Test
    void testHeaderRendererInstalled() {
        JTable table = createSampleTable();
        assertInstanceOf(DwcTableHeaderRenderer.class, table.getTableHeader().getDefaultRenderer(),
                "Header renderer should be DwcTableHeaderRenderer");
    }

    @Test
    void testAlternateRowColorExists() {
        assertNotNull(UIManager.getColor("Table.alternateRowColor"),
                "Table.alternateRowColor should be set in UIDefaults");
    }

    @Test
    void testPaintNoError() {
        JTable table = createSampleTable();
        table.setSize(400, 200);

        BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> table.paint(g2),
                    "Painting table should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testPaintWithSelectionNoError() {
        JTable table = createSampleTable();
        table.setSize(400, 200);
        table.setRowSelectionInterval(0, 0);

        BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> table.paint(g2),
                    "Painting table with selected row should not throw");
        } finally {
            g2.dispose();
        }
    }

    /**
     * Creates a sample JTable with 5 rows and 3 columns for testing.
     */
    private JTable createSampleTable() {
        Object[][] data = {
            {"A1", "B1", "C1"},
            {"A2", "B2", "C2"},
            {"A3", "B3", "C3"},
            {"A4", "B4", "C4"},
            {"A5", "B5", "C5"}
        };
        String[] columns = {"Col 1", "Col 2", "Col 3"};
        return new JTable(data, columns);
    }
}
