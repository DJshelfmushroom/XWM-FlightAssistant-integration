package dev.djshelfmushroom.flightassistantxaerocompat.map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.djshelfmushroom.flightassistantxaerocompat.compat.FlightAssistantCompat;
import dev.djshelfmushroom.flightassistantxaerocompat.compat.XaeroCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the active FlightAssistant flight plan as an overlay on Xaero's
 * World Map screen.
 *
 * <p>The overlay is drawn during {@code ScreenEvent.Render.Post} (see
 * {@link dev.djshelfmushroom.flightassistantxaerocompat.events.WaypointContextMenuHandler})
 * when the current screen is {@code xaero.map.gui.GuiMap}.  No fake entries are
 * injected into Xaero's waypoint list.</p>
 *
 * <h3>Visual elements</h3>
 * <ul>
 *   <li><b>Departure</b> — green filled square at departure X/Z</li>
 *   <li><b>Enroute waypoints</b> — cyan filled squares labelled with index and altitude</li>
 *   <li><b>Active waypoint</b> — bright yellow filled square (the waypoint FA is
 *       currently navigating toward)</li>
 *   <li><b>Arrival</b> — red filled square at arrival X/Z</li>
 *   <li><b>Route line</b> — cyan line connecting Departure → Enroute WPs → Arrival</li>
 * </ul>
 *
 * <p>All coordinates are polled from FlightAssistant's state every render tick.</p>
 */
public class FlightPlanMapOverlay {

    // Marker half-size in screen pixels
    private static final int MARKER_HALF = 4;

    // Argb colours for each element
    private static final int COLOR_DEPARTURE = 0xFF00BB00;  // green
    private static final int COLOR_ENROUTE   = 0xFF00BBBB;  // cyan
    private static final int COLOR_ACTIVE    = 0xFFFFFF00;  // bright yellow
    private static final int COLOR_ARRIVAL   = 0xFFBB0000;  // red
    private static final int COLOR_ROUTE     = 0x8800BBBB;  // semi-transparent cyan

    // Label text colour (white)
    private static final int TEXT_COLOR = 0xFFFFFF;

    /**
     * Entry point called every render frame when GuiMap is open.
     *
     * @param graphics  the current frame's {@link GuiGraphics}
     * @param guiMap    the Xaero {@code GuiMap} screen instance (passed as Object to
     *                  avoid a hard compile-time dependency)
     */
    public static void render(GuiGraphics graphics, Screen guiMap) {
        if (guiMap == null) return;

        // --- Gather FA flight-plan data ---
        Object departure = FlightAssistantCompat.getDepartureData();
        Object arrival   = FlightAssistantCompat.getArrivalData();
        List<Object> enroute = FlightAssistantCompat.getEnrouteWaypoints();
        int activeIdx = FlightAssistantCompat.getActiveWaypointIndex();

        boolean hasDeparture = departure != null && !FlightAssistantCompat.isPlanDataDefault(departure);
        boolean hasArrival   = arrival   != null && !FlightAssistantCompat.isPlanDataDefault(arrival);
        boolean hasEnroute   = enroute != null && !enroute.isEmpty();

        // Nothing to draw
        if (!hasDeparture && !hasArrival && !hasEnroute) return;

        // --- Get map camera / scale via reflection on GuiMap ---
        double cameraX = XaeroCompat.getGuiMapCameraX(guiMap);
        double cameraZ = XaeroCompat.getGuiMapCameraZ(guiMap);
        double scale   = XaeroCompat.getGuiMapScale(guiMap);
        int screenW    = guiMap.width;
        int screenH    = guiMap.height;

        // --- Build ordered list of route points for line drawing ---
        // Format: [x, z] pairs in world coordinates
        List<double[]> routePoints = new ArrayList<>();

        if (hasDeparture) {
            Integer dx = FlightAssistantCompat.getPlanCoordinatesX(departure);
            Integer dz = FlightAssistantCompat.getPlanCoordinatesZ(departure);
            if (dx != null && dz != null) routePoints.add(new double[]{dx, dz});
        }

        if (hasEnroute) {
            for (Object wp : enroute) {
                Integer wx = FlightAssistantCompat.getPlanCoordinatesX(wp);
                Integer wz = FlightAssistantCompat.getPlanCoordinatesZ(wp);
                if (wx != null && wz != null) routePoints.add(new double[]{wx, wz});
            }
        }

        if (hasArrival) {
            Integer ax = FlightAssistantCompat.getPlanCoordinatesX(arrival);
            Integer az = FlightAssistantCompat.getPlanCoordinatesZ(arrival);
            if (ax != null && az != null) routePoints.add(new double[]{ax, az});
        }

        // --- Draw route line ---
        if (routePoints.size() >= 2) {
            drawRouteLine(routePoints, cameraX, cameraZ, scale, screenW, screenH);
        }

        // --- Draw departure marker ---
        if (hasDeparture) {
            Integer dx = FlightAssistantCompat.getPlanCoordinatesX(departure);
            Integer dz = FlightAssistantCompat.getPlanCoordinatesZ(departure);
            if (dx != null && dz != null) {
                float sx = (float)(screenW / 2.0 + (dx - cameraX) * scale);
                float sy = (float)(screenH / 2.0 + (dz - cameraZ) * scale);
                drawMarker(graphics, sx, sy, COLOR_DEPARTURE);
                graphics.drawString(
                        net.minecraft.client.Minecraft.getInstance().font,
                        "DEP", (int) sx + MARKER_HALF + 2, (int) sy - 4,
                        TEXT_COLOR, true);
            }
        }

        // --- Draw enroute waypoints ---
        if (hasEnroute) {
            for (int i = 0; i < enroute.size(); i++) {
                Object wp = enroute.get(i);
                Integer wx  = FlightAssistantCompat.getPlanCoordinatesX(wp);
                Integer wz  = FlightAssistantCompat.getPlanCoordinatesZ(wp);
                Integer alt = FlightAssistantCompat.getEnrouteAltitude(wp);
                if (wx == null || wz == null) continue;

                float sx = (float)(screenW / 2.0 + (wx - cameraX) * scale);
                float sy = (float)(screenH / 2.0 + (wz - cameraZ) * scale);
                int color = (i == activeIdx) ? COLOR_ACTIVE : COLOR_ENROUTE;
                drawMarker(graphics, sx, sy, color);

                String label = (i + 1) + (alt != null ? "/" + alt : "");
                graphics.drawString(
                        net.minecraft.client.Minecraft.getInstance().font,
                        label, (int) sx + MARKER_HALF + 2, (int) sy - 4,
                        TEXT_COLOR, true);
            }
        }

        // --- Draw arrival marker ---
        if (hasArrival) {
            Integer ax = FlightAssistantCompat.getPlanCoordinatesX(arrival);
            Integer az = FlightAssistantCompat.getPlanCoordinatesZ(arrival);
            if (ax != null && az != null) {
                float sx = (float)(screenW / 2.0 + (ax - cameraX) * scale);
                float sy = (float)(screenH / 2.0 + (az - cameraZ) * scale);
                drawMarker(graphics, sx, sy, COLOR_ARRIVAL);
                graphics.drawString(
                        net.minecraft.client.Minecraft.getInstance().font,
                        "ARR", (int) sx + MARKER_HALF + 2, (int) sy - 4,
                        TEXT_COLOR, true);
            }
        }
    }

    // =========================================================================
    // Rendering primitives
    // =========================================================================

    /**
     * Draws a small filled square centred at ({@code sx}, {@code sy}).
     */
    private static void drawMarker(GuiGraphics graphics, float sx, float sy, int argbColor) {
        int x1 = (int) sx - MARKER_HALF;
        int y1 = (int) sy - MARKER_HALF;
        int x2 = (int) sx + MARKER_HALF;
        int y2 = (int) sy + MARKER_HALF;
        graphics.fill(x1, y1, x2, y2, argbColor);
        // White outline
        graphics.fill(x1 - 1, y1 - 1, x2 + 1, y1,     0xFFFFFFFF);
        graphics.fill(x1 - 1, y2,     x2 + 1, y2 + 1,  0xFFFFFFFF);
        graphics.fill(x1 - 1, y1,     x1,     y2,      0xFFFFFFFF);
        graphics.fill(x2,     y1,     x2 + 1, y2,      0xFFFFFFFF);
    }

    /**
     * Draws a poly-line connecting the given world-coordinate waypoints.
     * The line is rendered using the Tesselator in {@code LINES} mode.
     *
     * <p>Screen coordinates are computed inline (no per-call allocation) using:
     * {@code screenX = screenW/2 + (worldX - cameraX) * scale}</p>
     */
    private static void drawRouteLine(List<double[]> points,
                                      double cameraX, double cameraZ,
                                      double scale,
                                      int screenW, int screenH) {
        if (points.size() < 2) return;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.0f);

        float r = ((COLOR_ROUTE >> 16) & 0xFF) / 255f;
        float g = ((COLOR_ROUTE >>  8) & 0xFF) / 255f;
        float b = ( COLOR_ROUTE        & 0xFF) / 255f;
        float a = ((COLOR_ROUTE >> 24) & 0xFF) / 255f;

        Tesselator tes = Tesselator.getInstance();
        BufferBuilder buf = tes.getBuilder();
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < points.size() - 1; i++) {
            float fromX = (float)(screenW / 2.0 + (points.get(i)[0]   - cameraX) * scale);
            float fromY = (float)(screenH / 2.0 + (points.get(i)[1]   - cameraZ) * scale);
            float toX   = (float)(screenW / 2.0 + (points.get(i+1)[0] - cameraX) * scale);
            float toY   = (float)(screenH / 2.0 + (points.get(i+1)[1] - cameraZ) * scale);
            buf.vertex(fromX, fromY, 0f).color(r, g, b, a).endVertex();
            buf.vertex(  toX,   toY, 0f).color(r, g, b, a).endVertex();
        }

        tes.end();
        RenderSystem.disableBlend();
    }
}
