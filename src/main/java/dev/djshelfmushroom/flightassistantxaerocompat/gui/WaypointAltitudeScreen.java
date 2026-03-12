package dev.djshelfmushroom.flightassistantxaerocompat.gui;

import dev.djshelfmushroom.flightassistantxaerocompat.compat.FlightAssistantCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * A small, vanilla-styled GUI screen that prompts the player to enter a target
 * altitude (required) and an optional target speed before adding an enroute
 * waypoint to FlightAssistant's flight plan.
 *
 * <p>If the player closes the screen or presses Cancel the waypoint is NOT
 * added and no state is changed.</p>
 */
public class WaypointAltitudeScreen extends Screen {

    private static final int PANEL_WIDTH  = 200;
    private static final int PANEL_HEIGHT = 120;

    private final double waypointX;
    private final double waypointZ;
    private final String waypointName;

    /** EditBox for the required altitude field. */
    private EditBox altitudeField;
    /** EditBox for the optional speed field. */
    private EditBox speedField;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new altitude/speed prompt for the given Xaero waypoint.
     *
     * @param waypointX    world X of the waypoint
     * @param waypointZ    world Z of the waypoint
     * @param waypointName display name of the waypoint (used in the title)
     */
    public WaypointAltitudeScreen(double waypointX, double waypointZ, String waypointName) {
        super(Component.literal("Add Enroute Waypoint"));
        this.waypointX    = waypointX;
        this.waypointZ    = waypointZ;
        this.waypointName = waypointName != null ? waypointName : "Waypoint";
    }

    // -------------------------------------------------------------------------
    // Screen lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void init() {
        int cx = width  / 2;
        int cy = height / 2;

        int panelLeft = cx - PANEL_WIDTH  / 2;
        int panelTop  = cy - PANEL_HEIGHT / 2;

        // ---- Altitude field (required) ----
        int altY = panelTop + 28;
        altitudeField = new EditBox(
                font,
                panelLeft + 60, altY,
                PANEL_WIDTH - 70, 20,
                Component.literal("Altitude"));
        altitudeField.setMaxLength(8);
        altitudeField.setHint(Component.literal("required"));
        altitudeField.setResponder(this::onAltitudeChanged);
        addRenderableWidget(altitudeField);

        // ---- Speed field (optional) ----
        int speedY = altY + 28;
        speedField = new EditBox(
                font,
                panelLeft + 60, speedY,
                PANEL_WIDTH - 70, 20,
                Component.literal("Speed"));
        speedField.setMaxLength(8);
        speedField.setHint(Component.literal("optional"));
        addRenderableWidget(speedField);

        // ---- Confirm button ----
        int btnY = panelTop + PANEL_HEIGHT - 24;
        addRenderableWidget(Button.builder(
                Component.literal("Confirm"),
                btn -> onConfirm())
                .pos(panelLeft, btnY)
                .size(PANEL_WIDTH / 2 - 2, 20)
                .build());

        // ---- Cancel button ----
        addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> onCancel())
                .pos(panelLeft + PANEL_WIDTH / 2 + 2, btnY)
                .size(PANEL_WIDTH / 2 - 2, 20)
                .build());

        setInitialFocus(altitudeField);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int cx = width  / 2;
        int cy = height / 2;
        int panelLeft = cx - PANEL_WIDTH  / 2;
        int panelTop  = cy - PANEL_HEIGHT / 2;

        // Panel background
        graphics.fill(panelLeft - 4, panelTop - 4,
                panelLeft + PANEL_WIDTH + 4, panelTop + PANEL_HEIGHT + 4,
                0xC0101010);

        // Title
        graphics.drawCenteredString(font,
                "Add Enroute WP: " + waypointName,
                cx, panelTop + 6,
                0xFFFFFF);

        // Field labels
        graphics.drawString(font, "Alt (Y):",
                panelLeft, altitudeField.getY() + 5, 0xCCCCCC);
        graphics.drawString(font, "Speed:",
                panelLeft, speedField.getY() + 5, 0xCCCCCC);

        // Coordinates subtitle
        graphics.drawCenteredString(font,
                String.format("§7X: %.0f  Z: %.0f", waypointX, waypointZ),
                cx, altitudeField.getY() - 11,
                0xAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // -------------------------------------------------------------------------
    // Input handling
    // -------------------------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter confirms if altitude is filled
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (isAltitudeValid()) {
                onConfirm();
                return true;
            }
        }
        // Escape cancels
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onCancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void onAltitudeChanged(String text) {
        // Force the confirm button to re-evaluate on every keystroke
        // (handled implicitly — button is always rendered, validation on click)
    }

    private boolean isAltitudeValid() {
        try {
            Integer.parseInt(altitudeField.getValue().trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void onConfirm() {
        String altText = altitudeField.getValue().trim();
        if (altText.isEmpty()) {
            // Flash the field to indicate it is required
            altitudeField.setFocused(true);
            return;
        }

        int altitude;
        try {
            altitude = Integer.parseInt(altText);
        } catch (NumberFormatException e) {
            altitudeField.setFocused(true);
            return;
        }

        Integer speed = null;
        String speedText = speedField.getValue().trim();
        if (!speedText.isEmpty()) {
            try {
                speed = Integer.parseInt(speedText);
            } catch (NumberFormatException ignored) {
                // Invalid speed — treat as unset rather than aborting
            }
        }

        boolean added = FlightAssistantCompat.addEnrouteWaypoint(
                waypointX, waypointZ, altitude, speed != null ? (double) speed : null);

        if (added) {
            FlightAssistantCompat.sendChatMessage(
                    String.format("§fWaypoint added: X: %.0f, Z: %.0f, ALT: %d",
                            waypointX, waypointZ, altitude));
        } else {
            FlightAssistantCompat.sendChatMessage(
                    "§cFailed to add waypoint — check logs.");
        }

        onClose();
    }

    private void onCancel() {
        // No state change — just close
        onClose();
    }
}
