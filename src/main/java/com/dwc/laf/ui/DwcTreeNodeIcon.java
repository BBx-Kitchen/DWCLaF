package com.dwc.laf.ui;

import com.dwc.laf.painting.PaintUtils;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * A custom tree node {@link Icon} that renders folder and file icons
 * using stroked outlines, consistent with the {@link DwcTreeExpandIcon}
 * chevron style.
 *
 * <p>Three icon types are supported:
 * <ul>
 *   <li>{@link Type#FOLDER_OPEN} - Open folder with angled top flap</li>
 *   <li>{@link Type#FOLDER_CLOSED} - Closed folder with tab on top</li>
 *   <li>{@link Type#FILE} - Document with folded corner</li>
 * </ul>
 *
 * <p>The icon color is read from {@code UIManager.getColor("Tree.expandedIcon.color")}
 * with a fallback to {@link Color#DARK_GRAY}, matching the expand/collapse chevron
 * icon for visual consistency.</p>
 *
 * <p>Icon size is 16x16 pixels (standard Swing tree node icon size).</p>
 */
public class DwcTreeNodeIcon implements Icon {

    /**
     * The type of tree node icon to render.
     */
    public enum Type {
        /** Open folder icon with angled top flap. */
        FOLDER_OPEN,
        /** Closed folder icon with tab on top. */
        FOLDER_CLOSED,
        /** File/document icon with folded corner. */
        FILE
    }

    private static final int SIZE = 16;
    private static final BasicStroke STROKE =
            new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private final Type type;

    /**
     * Creates a new tree node icon of the specified type.
     *
     * @param type the icon type (folder open, folder closed, or file)
     */
    public DwcTreeNodeIcon(Type type) {
        this.type = type;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Object[] saved = PaintUtils.setupPaintingHints(g2);

            Color color = UIManager.getColor("Tree.expandedIcon.color");
            if (color == null) {
                color = Color.DARK_GRAY;
            }
            g2.setColor(color);
            g2.setStroke(STROKE);

            switch (type) {
                case FOLDER_CLOSED -> paintFolderClosed(g2, x, y);
                case FOLDER_OPEN -> paintFolderOpen(g2, x, y);
                case FILE -> paintFile(g2, x, y);
            }

            PaintUtils.restorePaintingHints(g2, saved);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Paints a closed folder icon: rectangle body with a small tab flap on
     * the top-left corner.
     */
    private void paintFolderClosed(Graphics2D g2, int x, int y) {
        float bx = x + 2f;
        float by = y + 5f;
        float bw = 12f;
        float bh = 8f;

        // Folder body
        Path2D.Float body = new Path2D.Float();
        body.moveTo(bx, by);
        body.lineTo(bx + bw, by);
        body.lineTo(bx + bw, by + bh);
        body.lineTo(bx, by + bh);
        body.closePath();
        g2.draw(body);

        // Tab flap on top-left
        Path2D.Float tab = new Path2D.Float();
        tab.moveTo(bx, by);
        tab.lineTo(bx, by - 2f);
        tab.lineTo(bx + 5f, by - 2f);
        tab.lineTo(bx + 5f, by);
        g2.draw(tab);
    }

    /**
     * Paints an open folder icon: same as closed but with an angled line
     * suggesting the folder lid is open.
     */
    private void paintFolderOpen(Graphics2D g2, int x, int y) {
        float bx = x + 2f;
        float by = y + 5f;
        float bw = 12f;
        float bh = 8f;

        // Folder body
        Path2D.Float body = new Path2D.Float();
        body.moveTo(bx, by);
        body.lineTo(bx + bw, by);
        body.lineTo(bx + bw, by + bh);
        body.lineTo(bx, by + bh);
        body.closePath();
        g2.draw(body);

        // Tab flap on top-left
        Path2D.Float tab = new Path2D.Float();
        tab.moveTo(bx, by);
        tab.lineTo(bx, by - 2f);
        tab.lineTo(bx + 5f, by - 2f);
        tab.lineTo(bx + 5f, by);
        g2.draw(tab);

        // Open flap: angled line from body top to suggest open lid
        Path2D.Float flap = new Path2D.Float();
        flap.moveTo(bx + 5f, by);
        flap.lineTo(bx + bw + 1f, by);
        flap.lineTo(bx + bw - 1f, by + bh);
        g2.draw(flap);
    }

    /**
     * Paints a file/document icon: rectangle with a folded corner at
     * the top-right.
     */
    private void paintFile(Graphics2D g2, int x, int y) {
        float fx = x + 3f;
        float fy = y + 2f;
        float fw = 10f;
        float fh = 12f;
        float fold = 3f;

        // Page outline with folded corner cut
        Path2D.Float page = new Path2D.Float();
        page.moveTo(fx, fy);
        page.lineTo(fx + fw - fold, fy);
        page.lineTo(fx + fw, fy + fold);
        page.lineTo(fx + fw, fy + fh);
        page.lineTo(fx, fy + fh);
        page.closePath();
        g2.draw(page);

        // Fold line (diagonal from corner)
        Path2D.Float foldLine = new Path2D.Float();
        foldLine.moveTo(fx + fw - fold, fy);
        foldLine.lineTo(fx + fw - fold, fy + fold);
        foldLine.lineTo(fx + fw, fy + fold);
        g2.draw(foldLine);
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
