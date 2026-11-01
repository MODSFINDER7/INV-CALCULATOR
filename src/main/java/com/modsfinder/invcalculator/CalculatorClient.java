package com.modsfinder.invcalculator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = InventoryCalculator.MOD_ID, value = Dist.CLIENT)
public final class CalculatorClient {
    private static final int PANEL_W = 150, PANEL_H = 202, BUTTON_SIZE = 18;
    private static boolean open, dragging;
    private static int panelX = -1, panelY = -1, dragOffsetX, dragOffsetY;
    private static String expression = "", result = "0", status = "";
    private static final String[] KEYS = {"7","8","9","÷","4","5","6","×","1","2","3","−","0",".","(",")","AC","⌫","=","+"};
    private CalculatorClient() {}

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int left = (screen.width - 176) / 2, top = (screen.height - 166) / 2;
        int bx = left + 181, by = top + 8;
        g.fill(bx, by, bx + BUTTON_SIZE, by + BUTTON_SIZE, 0xFF303030);
        g.fill(bx + 1, by + 1, bx + BUTTON_SIZE - 1, by + BUTTON_SIZE - 1, 0xFF565656);
        g.centeredText(font, "÷", bx + BUTTON_SIZE / 2, by + 5, 0xFFFFFFFF);
        if (!open) return;
        ensurePosition(screen);
        int x = panelX, y = panelY;
        g.fill(x - 1, y - 1, x + PANEL_W + 1, y + PANEL_H + 1, 0xFF101010);
        g.fill(x, y, x + PANEL_W, y + PANEL_H, 0xFF303030);
        g.fill(x, y, x + PANEL_W, y + 18, 0xFF484848);
        g.text(font, "Calculator", x + 7, y + 5, 0xFFFFFFFF);
        g.fill(x + PANEL_W - 17, y + 2, x + PANEL_W - 3, y + 16, 0xFF8B3333);
        g.centeredText(font, "x", x + PANEL_W - 10, y + 5, 0xFFFFFFFF);
        g.fill(x + 6, y + 24, x + PANEL_W - 6, y + 56, 0xFF171717);
        String shown = expression.isEmpty() ? "0" : expression;
        if (font.width(shown) > PANEL_W - 18) shown = font.plainSubstrByWidth(shown, PANEL_W - 18);
        g.text(font, shown, x + 10, y + 29, 0xFFEAEAEA);
        String output = status.isEmpty() ? result : status;
        if (font.width(output) > PANEL_W - 18) output = font.plainSubstrByWidth(output, PANEL_W - 18);
        g.text(font, output, x + 10, y + 43, status.isEmpty() ? 0xFF75D69A : 0xFFFF7777);
        for (int i = 0; i < KEYS.length; i++) {
            int col = i % 4, row = i / 4, kx = x + 6 + col * 35, ky = y + 63 + row * 23;
            g.fill(kx, ky, kx + 31, ky + 19, KEYS[i].equals("=") ? 0xFF356C43 : 0xFF555555);
            g.centeredText(font, KEYS[i], kx + 15, ky + 5, 0xFFFFFFFF);
        }
        g.text(font, "Copy result", x + 8, y + 188, 0xFFB9D9FF);
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen) || event.getButton() != 0) return;
        int mx = (int) Math.floor(event.getMouseX()), my = (int) Math.floor(event.getMouseY());
        int left = (screen.width - 176) / 2, top = (screen.height - 166) / 2;
        int bx = left + 181, by = top + 8;
        if (inside(mx, my, bx, by, BUTTON_SIZE, BUTTON_SIZE)) {
            open = !open; dragging = false;
            if (open) ensurePosition(screen);
            event.setCanceled(true);
            return;
        }
        if (!open) return;
        ensurePosition(screen);
        int x = panelX, y = panelY;
        if (!inside(mx, my, x, y, PANEL_W, PANEL_H)) return;
        event.setCanceled(true);
        if (inside(mx, my, x + PANEL_W - 18, y, 18, 18)) { open = false; dragging = false; return; }
        if (inside(mx, my, x, y, PANEL_W, 19)) {
            dragging = true; dragOffsetX = mx - panelX; dragOffsetY = my - panelY; return;
        }
        if (inside(mx, my, x + 6, y + 184, 90, 14)) { copyResult(); return; }
        int gx = mx - (x + 6), gy = my - (y + 63);
        if (gx < 0 || gy < 0) return;
        int col = gx / 35, row = gy / 23;
        if (col >= 4 || row >= 5 || gx % 35 >= 31 || gy % 23 >= 19) return;
        pressKey(KEYS[row * 4 + col]);
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen) || !dragging) return;
        ensurePosition(screen);
        panelX = clamp((int) Math.round(event.getMouseX() - dragOffsetX), 0, Math.max(0, screen.width - PANEL_W));
        panelY = clamp((int) Math.round(event.getMouseY() - dragOffsetY), 0, Math.max(0, screen.height - PANEL_H));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getScreen() instanceof InventoryScreen && dragging) { dragging = false; save(); }
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen) || !open) return;
        int key = event.getKeyCode();
        if (key == GLFW.GLFW_KEY_ESCAPE) { open = false; dragging = false; event.setCanceled(true); }
        else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { calculate(); event.setCanceled(true); }
        else if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!expression.isEmpty()) expression = expression.substring(0, expression.length() - 1);
            status = ""; event.setCanceled(true);
        } else if (key == GLFW.GLFW_KEY_DELETE) {
            expression = ""; status = ""; result = "0"; event.setCanceled(true);
        } else if (key == GLFW.GLFW_KEY_C && (event.getModifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            copyResult(); event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCharacterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen) || !open) return;
        int cp = event.getCodePoint();
        if (cp >= 32 && cp <= 126) {
            char c = (char) cp;
            if (Character.isDigit(c) || "+-*/().".indexOf(c) >= 0) { expression += c; status = ""; event.setCanceled(true); }
        }
    }

    private static void pressKey(String key) {
        switch (key) {
            case "AC" -> { expression = ""; result = "0"; status = ""; }
            case "⌫" -> { if (!expression.isEmpty()) expression = expression.substring(0, expression.length() - 1); status = ""; }
            case "=" -> calculate();
            default -> { expression += key; status = ""; }
        }
    }
    private static void calculate() {
        try {
            double value = ExpressionParser.evaluate(expression);
            result = format(value); expression = result; status = ""; save();
        } catch (RuntimeException ex) {
            status = ex.getMessage() == null ? "Invalid expression" : ex.getMessage();
        }
    }
    private static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1.0e15) return Long.toString((long) value);
        return String.format(java.util.Locale.ROOT, "%.10f", value).replaceAll("0+$", "").replaceAll("\\\\.$", "");
    }
    private static void copyResult() { Minecraft.getInstance().keyboardHandler.setClipboard(result); }
    private static void ensurePosition(Screen screen) {
        if (panelX < 0 || panelY < 0) {
            panelX = clamp((screen.width - PANEL_W) / 2 + 92, 0, Math.max(0, screen.width - PANEL_W));
            panelY = clamp((screen.height - PANEL_H) / 2, 0, Math.max(0, screen.height - PANEL_H));
            load();
        }
    }
    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("invcalculator.properties");
    }
    private static void save() {
        try {
            Path path = configPath(); Files.createDirectories(path.getParent());
            Properties p = new Properties();
            p.setProperty("expression", expression); p.setProperty("result", result);
            p.setProperty("x", Integer.toString(panelX)); p.setProperty("y", Integer.toString(panelY));
            try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) { p.store(writer, "Inventory Calculator"); }
        } catch (IOException ignored) {}
    }
    private static void load() {
        Path path = configPath();
        if (!Files.isRegularFile(path)) return;
        Properties p = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            p.load(reader); expression = p.getProperty("expression", expression); result = p.getProperty("result", result);
            panelX = Integer.parseInt(p.getProperty("x", Integer.toString(panelX)));
            panelY = Integer.parseInt(p.getProperty("y", Integer.toString(panelY)));
            panelX = clamp(panelX, 0, Math.max(0, Minecraft.getInstance().getWindow().getGuiScaledWidth() - PANEL_W));
            panelY = clamp(panelY, 0, Math.max(0, Minecraft.getInstance().getWindow().getGuiScaledHeight() - PANEL_H));
        } catch (IOException | NumberFormatException ignored) {}
    }
}
