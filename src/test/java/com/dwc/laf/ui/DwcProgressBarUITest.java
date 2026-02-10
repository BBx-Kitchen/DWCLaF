package com.dwc.laf.ui;

import com.dwc.laf.DwcLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JProgressBar;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcProgressBarUI} verifying UI installation, per-component instances,
 * determinate and indeterminate painting, color variant support, and variant color
 * differentiation.
 */
class DwcProgressBarUITest {

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
        JProgressBar pb = new JProgressBar();
        assertInstanceOf(DwcProgressBarUI.class, pb.getUI(),
                "JProgressBar should use DwcProgressBarUI when DwcLookAndFeel is active");
    }

    @Test
    void testPerComponentInstance() {
        JProgressBar pb1 = new JProgressBar();
        JProgressBar pb2 = new JProgressBar();
        assertNotSame(pb1.getUI(), pb2.getUI(),
                "Each JProgressBar should have its own DwcProgressBarUI instance");
    }

    @Test
    void testPaintDeterminateNoError() {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(50);
        pb.setSize(200, 20);

        BufferedImage img = new BufferedImage(200, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> pb.paint(g2),
                    "Painting determinate progress bar should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testPaintIndeterminateNoError() {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setIndeterminate(true);
        pb.setSize(200, 20);

        BufferedImage img = new BufferedImage(200, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> pb.paint(g2),
                    "Painting indeterminate progress bar should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testSuccessVariantPaintNoError() {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(75);
        pb.putClientProperty("dwc.progressType", "success");
        pb.setSize(200, 20);

        BufferedImage img = new BufferedImage(200, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> pb.paint(g2),
                    "Painting success variant progress bar should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testDangerVariantPaintNoError() {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(75);
        pb.putClientProperty("dwc.progressType", "danger");
        pb.setSize(200, 20);

        BufferedImage img = new BufferedImage(200, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> pb.paint(g2),
                    "Painting danger variant progress bar should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testWarningVariantPaintNoError() {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(75);
        pb.putClientProperty("dwc.progressType", "warning");
        pb.setSize(200, 20);

        BufferedImage img = new BufferedImage(200, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> pb.paint(g2),
                    "Painting warning variant progress bar should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testInfoVariantPaintNoError() {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(75);
        pb.putClientProperty("dwc.progressType", "info");
        pb.setSize(200, 20);

        BufferedImage img = new BufferedImage(200, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> pb.paint(g2),
                    "Painting info variant progress bar should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testVariantColorsDifferFromDefault() {
        JProgressBar defaultPb = new JProgressBar(0, 100);
        defaultPb.setValue(75);
        defaultPb.setSize(200, 20);

        JProgressBar successPb = new JProgressBar(0, 100);
        successPb.setValue(75);
        successPb.putClientProperty("dwc.progressType", "success");
        successPb.setSize(200, 20);

        BufferedImage defaultImg = new BufferedImage(200, 20, BufferedImage.TYPE_INT_ARGB);
        BufferedImage successImg = new BufferedImage(200, 20, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g1 = defaultImg.createGraphics();
        try {
            defaultPb.paint(g1);
        } finally {
            g1.dispose();
        }

        Graphics2D g2 = successImg.createGraphics();
        try {
            successPb.paint(g2);
        } finally {
            g2.dispose();
        }

        // Sample center pixel -- variant colors should differ
        int defaultPixel = defaultImg.getRGB(50, 10);
        int successPixel = successImg.getRGB(50, 10);
        assertNotEquals(defaultPixel, successPixel,
                "Success variant progress bar should render with different colors than default");
    }
}
