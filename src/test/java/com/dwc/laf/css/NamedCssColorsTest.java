package com.dwc.laf.css;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NamedCssColorsTest {

    @Test
    void resolveRed() {
        Optional<Color> result = NamedCssColors.resolve("red");
        assertTrue(result.isPresent());
        assertEquals(new Color(255, 0, 0), result.get());
    }

    @Test
    void resolveRedCaseInsensitive() {
        Optional<Color> upper = NamedCssColors.resolve("RED");
        Optional<Color> lower = NamedCssColors.resolve("red");
        Optional<Color> mixed = NamedCssColors.resolve("Red");
        assertTrue(upper.isPresent());
        assertTrue(lower.isPresent());
        assertTrue(mixed.isPresent());
        assertEquals(lower.get(), upper.get());
        assertEquals(lower.get(), mixed.get());
    }

    @Test
    void resolveAliceBlue() {
        Optional<Color> result = NamedCssColors.resolve("aliceblue");
        assertTrue(result.isPresent());
        Color c = result.get();
        assertEquals(240, c.getRed());
        assertEquals(248, c.getGreen());
        assertEquals(255, c.getBlue());
    }

    @Test
    void resolveTransparent() {
        Optional<Color> result = NamedCssColors.resolve("transparent");
        assertTrue(result.isPresent());
        Color c = result.get();
        assertEquals(0, c.getRed());
        assertEquals(0, c.getGreen());
        assertEquals(0, c.getBlue());
        assertEquals(0, c.getAlpha());
    }

    @Test
    void resolveUnknownReturnsEmpty() {
        Optional<Color> result = NamedCssColors.resolve("notacolor");
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveNullReturnsEmpty() {
        Optional<Color> result = NamedCssColors.resolve(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveEmptyStringReturnsEmpty() {
        Optional<Color> result = NamedCssColors.resolve("");
        assertTrue(result.isEmpty());
    }

    // Spot-check colors across the spectrum
    @Test
    void resolveDodgerBlue() {
        Optional<Color> result = NamedCssColors.resolve("dodgerblue");
        assertTrue(result.isPresent());
        assertEquals(new Color(30, 144, 255), result.get());
    }

    @Test
    void resolveForestGreen() {
        Optional<Color> result = NamedCssColors.resolve("forestgreen");
        assertTrue(result.isPresent());
        assertEquals(new Color(34, 139, 34), result.get());
    }

    @Test
    void resolveCrimson() {
        Optional<Color> result = NamedCssColors.resolve("crimson");
        assertTrue(result.isPresent());
        assertEquals(new Color(220, 20, 60), result.get());
    }

    @Test
    void resolveGold() {
        Optional<Color> result = NamedCssColors.resolve("gold");
        assertTrue(result.isPresent());
        assertEquals(new Color(255, 215, 0), result.get());
    }

    @Test
    void resolveRebeccaPurple() {
        Optional<Color> result = NamedCssColors.resolve("rebeccapurple");
        assertTrue(result.isPresent());
        assertEquals(new Color(102, 51, 153), result.get());
    }

    @Test
    void resolveBlack() {
        Optional<Color> result = NamedCssColors.resolve("black");
        assertTrue(result.isPresent());
        assertEquals(new Color(0, 0, 0), result.get());
    }

    @Test
    void resolveWhite() {
        Optional<Color> result = NamedCssColors.resolve("white");
        assertTrue(result.isPresent());
        assertEquals(new Color(255, 255, 255), result.get());
    }

    @Test
    void sizeReturns149() {
        // 148 named colors + transparent
        assertEquals(149, NamedCssColors.size());
    }

    @Test
    void greyAndGrayBothExist() {
        // CSS has both spellings for several colors
        assertTrue(NamedCssColors.resolve("gray").isPresent());
        assertTrue(NamedCssColors.resolve("grey").isPresent());
        assertEquals(NamedCssColors.resolve("gray").get(), NamedCssColors.resolve("grey").get());

        assertTrue(NamedCssColors.resolve("darkgray").isPresent());
        assertTrue(NamedCssColors.resolve("darkgrey").isPresent());
        assertEquals(NamedCssColors.resolve("darkgray").get(), NamedCssColors.resolve("darkgrey").get());
    }
}
