package com.dwc.laf.css;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class CssValueTest {

    @Test
    void colorValueHoldsColor() {
        Color red = new Color(255, 0, 0);
        CssValue.ColorValue cv = new CssValue.ColorValue(red);
        assertEquals(red, cv.color());
    }

    @Test
    void colorValueWithAlpha() {
        Color semiTransparent = new Color(0, 128, 255, 128);
        CssValue.ColorValue cv = new CssValue.ColorValue(semiTransparent);
        assertEquals(128, cv.color().getAlpha());
        assertEquals(0, cv.color().getRed());
        assertEquals(128, cv.color().getGreen());
        assertEquals(255, cv.color().getBlue());
    }

    @Test
    void dimensionValueHoldsValueAndUnit() {
        CssValue.DimensionValue dv = new CssValue.DimensionValue(0.875f, "rem");
        assertEquals(0.875f, dv.value(), 0.001f);
        assertEquals("rem", dv.unit());
    }

    @Test
    void dimensionValueIntValueRoundsCorrectly() {
        // 0.875 rounds to 1
        assertEquals(1, new CssValue.DimensionValue(0.875f, "rem").intValue());
        // 16.0 rounds to 16
        assertEquals(16, new CssValue.DimensionValue(16.0f, "px").intValue());
        // 2.4 rounds to 2
        assertEquals(2, new CssValue.DimensionValue(2.4f, "px").intValue());
        // 2.5 rounds to 3 (Math.round rounds .5 up)
        assertEquals(3, new CssValue.DimensionValue(2.5f, "px").intValue());
        // 0.49 rounds to 0
        assertEquals(0, new CssValue.DimensionValue(0.49f, "em").intValue());
    }

    @Test
    void integerValueHoldsInt() {
        CssValue.IntegerValue iv = new CssValue.IntegerValue(400);
        assertEquals(400, iv.value());
    }

    @Test
    void floatValueHoldsFloat() {
        CssValue.FloatValue fv = new CssValue.FloatValue(1.25f);
        assertEquals(1.25f, fv.value(), 0.001f);
    }

    @Test
    void stringValueHoldsString() {
        String fontStack = "-apple-system, BlinkMacSystemFont, sans-serif";
        CssValue.StringValue sv = new CssValue.StringValue(fontStack);
        assertEquals(fontStack, sv.value());
    }

    @Test
    void rawValueHoldsRawString() {
        String calc = "calc(var(--size-m) / 2)";
        CssValue.RawValue rv = new CssValue.RawValue(calc);
        assertEquals(calc, rv.raw());
    }

    @Test
    void patternMatchingWithSwitch() {
        CssValue[] values = {
                new CssValue.ColorValue(Color.RED),
                new CssValue.DimensionValue(16f, "px"),
                new CssValue.IntegerValue(700),
                new CssValue.FloatValue(1.5f),
                new CssValue.StringValue("solid"),
                new CssValue.RawValue("calc(1 + 2)")
        };

        for (CssValue v : values) {
            String result = switch (v) {
                case CssValue.ColorValue cv -> "color:" + cv.color();
                case CssValue.DimensionValue dv -> "dim:" + dv.value() + dv.unit();
                case CssValue.IntegerValue iv -> "int:" + iv.value();
                case CssValue.FloatValue fv -> "float:" + fv.value();
                case CssValue.StringValue sv -> "str:" + sv.value();
                case CssValue.RawValue rv -> "raw:" + rv.raw();
            };
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }
    }

    @Test
    void sealedInterfaceHasSixPermittedSubclasses() {
        Class<?>[] permitted = CssValue.class.getPermittedSubclasses();
        assertNotNull(permitted);
        assertEquals(6, permitted.length);
    }

    @Test
    void recordEquality() {
        assertEquals(
                new CssValue.IntegerValue(400),
                new CssValue.IntegerValue(400)
        );
        assertNotEquals(
                new CssValue.IntegerValue(400),
                new CssValue.IntegerValue(700)
        );
        assertEquals(
                new CssValue.ColorValue(Color.BLUE),
                new CssValue.ColorValue(new Color(0, 0, 255))
        );
    }
}
