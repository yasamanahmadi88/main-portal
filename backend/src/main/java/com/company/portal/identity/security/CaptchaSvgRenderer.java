package com.company.portal.identity.security;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Renders a lightweight SVG CAPTCHA without external image libraries.
 * Noise lines and glyph jitter reduce trivial OCR while remaining accessible
 * enough for human operators (refresh is always available).
 */
final class CaptchaSvgRenderer {

    private CaptchaSvgRenderer() { }

    static String render(String answer) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int width = 180;
        int height = 56;
        StringBuilder sb = new StringBuilder(1024);
        sb.append(String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\" role=\"img\" aria-label=\"CAPTCHA\">",
                width, height, width, height));
        sb.append("<rect width=\"100%\" height=\"100%\" fill=\"#eef2f7\"/>");
        for (int i = 0; i < 6; i++) {
            sb.append(String.format(Locale.ROOT,
                    "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#%06x\" stroke-width=\"1\" opacity=\"0.55\"/>",
                    rnd.nextInt(width), rnd.nextInt(height),
                    rnd.nextInt(width), rnd.nextInt(height),
                    rnd.nextInt(0x666666)));
        }
        int step = width / (answer.length() + 1);
        for (int i = 0; i < answer.length(); i++) {
            int x = step * (i + 1) - 6;
            int y = 34 + rnd.nextInt(10) - 4;
            int rotate = rnd.nextInt(24) - 12;
            String color = String.format(Locale.ROOT, "#%06x", 0x1a2744 + rnd.nextInt(0x202020));
            sb.append(String.format(Locale.ROOT,
                    "<text x=\"%d\" y=\"%d\" fill=\"%s\" font-family=\"ui-monospace, monospace\" font-size=\"26\" font-weight=\"700\" transform=\"rotate(%d %d %d)\">%s</text>",
                    x, y, color, rotate, x, y, escape(answer.charAt(i))));
        }
        sb.append("</svg>");
        return sb.toString();
    }

    private static String escape(char c) {
        return switch (c) {
            case '&' -> "&amp;";
            case '<' -> "&lt;";
            case '>' -> "&gt;";
            case '"' -> "&quot;";
            default -> String.valueOf(c);
        };
    }
}
