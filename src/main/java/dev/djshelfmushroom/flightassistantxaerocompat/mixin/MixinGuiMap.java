package dev.djshelfmushroom.flightassistantxaerocompat.mixin;

import dev.djshelfmushroom.flightassistantxaerocompat.FlightAssistantXaeroCompat;
import dev.djshelfmushroom.flightassistantxaerocompat.compat.FlightAssistantCompat;
import dev.djshelfmushroom.flightassistantxaerocompat.gui.WaypointAltitudeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Mixin on {@code xaero.map.gui.GuiMap} to inject FlightAssistant context-menu
 * entries into Xaero's World Map right-click dropdown.
 *
 * <p>Five entries are added whenever FlightAssistant is loaded:</p>
 * <ol>
 *   <li><b>Fly Here (FlightAssistant)</b> — sets COORDS target to the right-clicked
 *       X/Z position.</li>
 *   <li><b>[FA] Set as COORDS Target</b> — same action in the FA section.</li>
 *   <li><b>[FA] Add to Flight Plan (Enroute)</b> — opens
 *       {@link WaypointAltitudeScreen}.</li>
 *   <li><b>[FA] Set as Departure</b> — sets the flight plan departure point.</li>
 *   <li><b>[FA] Set as Arrival</b> — sets the flight plan arrival point.</li>
 * </ol>
 *
 * <p>All coordinates come from the Xaero-managed {@code rightClickX}/{@code rightClickZ}
 * fields, verified against Xaero's World Map 1.39.2.</p>
 */
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public abstract class MixinGuiMap {

    /**
     * Block X coordinate of the most recent right-click on the map.
     * <p>Xaero 1.39.2: {@code private int rightClickX} on GuiMap.</p>
     */
    @Shadow private int rightClickX;

    /**
     * Block Z coordinate of the most recent right-click on the map.
     * <p>Xaero 1.39.2: {@code private int rightClickZ} on GuiMap.</p>
     */
    @Shadow private int rightClickZ;

    /**
     * Injects at the RETURN of {@code GuiMap.getRightClickOptions()}, appending
     * FlightAssistant actions to the existing option list.
     *
     * <p>This mirrors the pattern used by XaeroPlus 1.20.1 (MixinGuiMap).</p>
     */
    @Inject(
        method = "getRightClickOptions",
        at = @At("RETURN"),
        remap = false
    )
    private void injectFAOptions(CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        if (!FlightAssistantXaeroCompat.xaeroPresent || !FlightAssistantXaeroCompat.flightAssistantPresent) {
            return;
        }

        ArrayList<RightClickOption> options = cir.getReturnValue();
        if (options == null) return;

        // Guard against Xaero's unset-coordinate sentinels
        final int clickX = rightClickX;
        final int clickZ = rightClickZ;
        if (clickX == Integer.MAX_VALUE || clickX == Integer.MIN_VALUE
                || clickZ == Integer.MAX_VALUE || clickZ == Integer.MIN_VALUE) {
            FlightAssistantXaeroCompat.LOGGER.warn(
                    "[FA-Xaero] Right-click coords appear invalid: {}, {}", clickX, clickZ);
            return;
        }

        // ---- "Fly Here (FlightAssistant)" ----
        // Cast: at runtime this IS a GuiMap which implements IRightClickableElement
        @SuppressWarnings("UnnecessaryCast")
        final xaero.map.gui.IRightClickableElement self =
                (xaero.map.gui.IRightClickableElement) (Object) this;

        options.add(new RightClickOption("Fly Here (FlightAssistant)", options.size(), self) {
            @Override
            public void onAction(Screen screen) {
                boolean ok = FlightAssistantCompat.setCoordsTarget(clickX, clickZ);
                if (ok) {
                    FlightAssistantCompat.sendChatMessage(
                            String.format("§fFlying to X: %d, Z: %d", clickX, clickZ));
                } else {
                    FlightAssistantCompat.sendChatMessage(
                            "§cFailed to set COORDS target — check logs.");
                }
            }
        });

        // ---- "[FA] Set as COORDS Target" ----
        options.add(new RightClickOption("[FA] Set as COORDS Target", options.size(), self) {
            @Override
            public void onAction(Screen screen) {
                boolean ok = FlightAssistantCompat.setCoordsTarget(clickX, clickZ);
                if (ok) {
                    FlightAssistantCompat.sendChatMessage(
                            String.format("§fCOORDS set to X: %d, Z: %d", clickX, clickZ));
                } else {
                    FlightAssistantCompat.sendChatMessage(
                            "§cFailed to set COORDS target — check logs.");
                }
            }
        });

        // ---- "[FA] Add to Flight Plan (Enroute)" ----
        options.add(new RightClickOption("[FA] Add to Flight Plan (Enroute)", options.size(), self) {
            @Override
            public void onAction(Screen screen) {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(new WaypointAltitudeScreen(
                        clickX, clickZ, "X:" + clickX + " Z:" + clickZ));
            }
        });

        // ---- "[FA] Set as Departure" ----
        options.add(new RightClickOption("[FA] Set as Departure", options.size(), self) {
            @Override
            public void onAction(Screen screen) {
                boolean ok = FlightAssistantCompat.setDepartureWaypoint(clickX, clickZ);
                if (ok) {
                    FlightAssistantCompat.sendChatMessage(
                            String.format(Locale.ROOT, "§fDeparture set to X: %d, Z: %d", clickX, clickZ));
                } else {
                    FlightAssistantCompat.sendChatMessage(
                            "§cFailed to set departure — check logs.");
                }
            }
        });

        // ---- "[FA] Set as Arrival" ----
        options.add(new RightClickOption("[FA] Set as Arrival", options.size(), self) {
            @Override
            public void onAction(Screen screen) {
                boolean ok = FlightAssistantCompat.setArrivalWaypoint(clickX, clickZ);
                if (ok) {
                    FlightAssistantCompat.sendChatMessage(
                            String.format(Locale.ROOT, "§fArrival set to X: %d, Z: %d", clickX, clickZ));
                } else {
                    FlightAssistantCompat.sendChatMessage(
                            "§cFailed to set arrival — check logs.");
                }
            }
        });
    }
}

