package dev.djshelfmushroom.flightassistantxaerocompat.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.djshelfmushroom.flightassistantxaerocompat.FlightAssistantXaeroCompat;
import dev.djshelfmushroom.flightassistantxaerocompat.compat.FlightAssistantCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Renders FlightAssistant flight-plan waypoints as 3D in-world markers while
 * the player is flying, giving real-time spatial awareness of the active route.
 *
 * <p>Waypoints are rendered during
 * {@link RenderLevelStageEvent.Stage#AFTER_TRANSLUCENT_BLOCKS} so they appear
 * on top of most world geometry but beneath the HUD. Markers are rendered
 * without depth-testing so they remain visible through terrain.</p>
 *
 * <h3>Visual elements (per waypoint)</h3>
 * <ul>
 *   <li>A wire-frame box outline at the waypoint's target altitude (or camera
 *       altitude for departure/arrival which have no explicit altitude).</li>
 *   <li>A billboard text label floating above the box showing the waypoint
 *       identifier, target altitude (enroute only), and the horizontal
 *       distance from the player.</li>
 * </ul>
 *
 * <h3>Colour coding</h3>
 * <ul>
 *   <li><b>Departure</b> — green</li>
 *   <li><b>Enroute</b> — cyan</li>
 *   <li><b>Active enroute waypoint</b> — bright yellow</li>
 *   <li><b>Arrival</b> — red</li>
 * </ul>
 *
 * <p>Waypoints further than 4096 blocks (horizontal) are culled.</p>
 */
public class InWorldWaypointRenderer {

    /** Maximum horizontal range (blocks) at which in-world markers are rendered. */
    private static final double MAX_DIST = 4096.0;

    /** Half-size of the wire-frame box, in blocks. */
    private static final float BOX_HALF = 0.4f;

    // Colours (0xAARRGGBB)
    private static final int COLOR_DEPARTURE = 0xFF00BB00;
    private static final int COLOR_ENROUTE   = 0xFF00BBBB;
    private static final int COLOR_ACTIVE    = 0xFFFFFF00;
    private static final int COLOR_ARRIVAL   = 0xFFBB0000;

    // =========================================================================
    // Event handler
    // =========================================================================

    /**
     * Main entry point — called every rendered frame during level rendering.
     *
     * @param event the render-level stage event
     */
    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!FlightAssistantXaeroCompat.flightAssistantPresent) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Object departure  = FlightAssistantCompat.getDepartureData();
        Object arrival    = FlightAssistantCompat.getArrivalData();
        List<Object> enroute = FlightAssistantCompat.getEnrouteWaypoints();
        int activeIdx     = FlightAssistantCompat.getActiveWaypointIndex();

        boolean hasDep = departure != null && !FlightAssistantCompat.isPlanDataDefault(departure);
        boolean hasArr = arrival   != null && !FlightAssistantCompat.isPlanDataDefault(arrival);
        boolean hasEnr = enroute   != null && !enroute.isEmpty();

        if (!hasDep && !hasArr && !hasEnr) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera       = event.getCamera();
        Vec3 camPos         = camera.getPosition();

        // Shared BufferSource for all label renders this frame — avoids
        // allocating a new BufferBuilder per waypoint.
        MultiBufferSource.BufferSource labelBuf =
                MultiBufferSource.immediate(new BufferBuilder(256));

        try {
            if (hasDep) {
                Integer wx = FlightAssistantCompat.getPlanCoordinatesX(departure);
                Integer wz = FlightAssistantCompat.getPlanCoordinatesZ(departure);
                // Departure may have an altitude; fall back to camera Y if not
                Integer alt = FlightAssistantCompat.getEnrouteAltitude(departure);
                if (wx != null && wz != null) {
                    double wy = alt != null ? alt : camPos.y;
                    renderWaypoint(poseStack, camera, camPos, wx, wy, wz,
                            "DEP", COLOR_DEPARTURE, labelBuf);
                }
            }

            if (hasEnr) {
                for (int i = 0; i < enroute.size(); i++) {
                    Object wp  = enroute.get(i);
                    Integer wx  = FlightAssistantCompat.getPlanCoordinatesX(wp);
                    Integer wz  = FlightAssistantCompat.getPlanCoordinatesZ(wp);
                    Integer alt = FlightAssistantCompat.getEnrouteAltitude(wp);
                    if (wx == null || wz == null) continue;
                    double wy = alt != null ? alt : camPos.y;
                    int color = (i == activeIdx) ? COLOR_ACTIVE : COLOR_ENROUTE;
                    String label = "WP" + (i + 1) + (alt != null ? "/" + alt : "");
                    renderWaypoint(poseStack, camera, camPos, wx, wy, wz, label, color, labelBuf);
                }
            }

            if (hasArr) {
                Integer wx = FlightAssistantCompat.getPlanCoordinatesX(arrival);
                Integer wz = FlightAssistantCompat.getPlanCoordinatesZ(arrival);
                Integer alt = FlightAssistantCompat.getEnrouteAltitude(arrival);
                if (wx != null && wz != null) {
                    double wy = alt != null ? alt : camPos.y;
                    renderWaypoint(poseStack, camera, camPos, wx, wy, wz,
                            "ARR", COLOR_ARRIVAL, labelBuf);
                }
            }

            labelBuf.endBatch();
        } catch (Exception e) {
            FlightAssistantXaeroCompat.LOGGER.warn(
                    "[FA-Xaero] InWorldWaypointRenderer failed", e);
        }
    }

    // =========================================================================
    // Per-waypoint rendering
    // =========================================================================

    /**
     * Renders the box and label for a single waypoint.
     *
     * @param poseStack current level render pose stack
     * @param camera    active camera (for billboard orientation)
     * @param camPos    camera world position
     * @param wx        waypoint world X
     * @param wy        waypoint world Y (target altitude)
     * @param wz        waypoint world Z
     * @param label     text to display (e.g. "WP1/250")
     * @param argbColor 0xAARRGGBB colour for this waypoint type
     * @param labelBuf  shared buffer source for text rendering
     */
    private static void renderWaypoint(PoseStack poseStack, Camera camera, Vec3 camPos,
                                        int wx, double wy, int wz,
                                        String label, int argbColor,
                                        MultiBufferSource.BufferSource labelBuf) {
        double relX = wx - camPos.x;
        double relY = wy - camPos.y;
        double relZ = wz - camPos.z;

        // Cull by horizontal distance
        double horizDistSq = relX * relX + relZ * relZ;
        if (horizDistSq > MAX_DIST * MAX_DIST) return;

        float r = ((argbColor >> 16) & 0xFF) / 255f;
        float g = ((argbColor >>  8) & 0xFF) / 255f;
        float b = ( argbColor        & 0xFF) / 255f;

        // Draw wire-frame box at the waypoint position
        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);
        drawBox(poseStack, BOX_HALF, r, g, b);
        poseStack.popPose();

        // Draw billboard label above the box
        double horizDist = Math.sqrt(horizDistSq);
        String fullLabel = label + "  " + formatDist(horizDist);
        drawLabel(poseStack, camera, relX, relY + BOX_HALF + 0.6, relZ, fullLabel, argbColor, labelBuf);
    }

    // =========================================================================
    // Rendering primitives
    // =========================================================================

    /**
     * Draws a wire-frame axis-aligned box centred at the current pose origin.
     * Depth testing is disabled so the outline is visible through terrain.
     *
     * @param poseStack pose stack (translated to the box centre)
     * @param half      half-size of the box (all axes)
     * @param r         red component [0,1]
     * @param g         green component [0,1]
     * @param b         blue component [0,1]
     */
    private static void drawBox(PoseStack poseStack, float half,
                                 float r, float g, float b) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.0f);

        Matrix4f mat = poseStack.last().pose();
        Tesselator tes = Tesselator.getInstance();
        BufferBuilder buf = tes.getBuilder();
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float a = 1.0f;
        // Bottom face
        line(buf, mat, -half, -half, -half,  half, -half, -half, r, g, b, a);
        line(buf, mat,  half, -half, -half,  half, -half,  half, r, g, b, a);
        line(buf, mat,  half, -half,  half, -half, -half,  half, r, g, b, a);
        line(buf, mat, -half, -half,  half, -half, -half, -half, r, g, b, a);
        // Top face
        line(buf, mat, -half,  half, -half,  half,  half, -half, r, g, b, a);
        line(buf, mat,  half,  half, -half,  half,  half,  half, r, g, b, a);
        line(buf, mat,  half,  half,  half, -half,  half,  half, r, g, b, a);
        line(buf, mat, -half,  half,  half, -half,  half, -half, r, g, b, a);
        // Vertical edges
        line(buf, mat, -half, -half, -half, -half,  half, -half, r, g, b, a);
        line(buf, mat,  half, -half, -half,  half,  half, -half, r, g, b, a);
        line(buf, mat,  half, -half,  half,  half,  half,  half, r, g, b, a);
        line(buf, mat, -half, -half,  half, -half,  half,  half, r, g, b, a);

        tes.end();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Draws a camera-facing (billboard) text label at the given camera-relative
     * position.
     *
     * @param poseStack pose stack (at world-relative-to-camera origin)
     * @param camera    active camera (used for billboard rotation)
     * @param relX      camera-relative X of the label anchor
     * @param relY      camera-relative Y of the label anchor
     * @param relZ      camera-relative Z of the label anchor
     * @param text      text to render
     * @param argbColor ARGB colour for the text
     * @param bufSource shared buffer source — caller is responsible for {@code endBatch()}
     */
    private static void drawLabel(PoseStack poseStack, Camera camera,
                                   double relX, double relY, double relZ,
                                   String text, int argbColor,
                                   MultiBufferSource.BufferSource bufSource) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);

        // Rotate so the label faces the camera (billboard)
        poseStack.mulPose(camera.rotation());

        // Scale to a readable world-space size
        // Minecraft GUI font is ~8px; 0.025f makes text ~0.2 blocks tall
        float scale = 0.025f;
        poseStack.scale(-scale, -scale, scale);

        float textX = -font.width(text) / 2.0f;
        int textColor = argbColor | 0xFF000000;

        font.drawInBatch(
                text,
                textX, 0f,
                textColor,
                false,
                poseStack.last().pose(),
                bufSource,
                Font.DisplayMode.SEE_THROUGH,
                0x55000000, // semi-transparent dark background
                0xF000F0    // full-bright packed light
        );

        poseStack.popPose();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static void line(BufferBuilder buf, Matrix4f mat,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float r, float g, float b, float a) {
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a).endVertex();
    }

    /** Formats a horizontal block distance as a compact string (e.g. "512m" or "1.5km"). */
    private static String formatDist(double dist) {
        if (dist >= 1000.0) {
            return String.format("%.1fkm", dist / 1000.0);
        }
        return String.format("%.0fm", dist);
    }
}
