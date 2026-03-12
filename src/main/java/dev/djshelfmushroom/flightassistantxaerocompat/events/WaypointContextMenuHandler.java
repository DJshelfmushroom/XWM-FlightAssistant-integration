package dev.djshelfmushroom.flightassistantxaerocompat.events;

import dev.djshelfmushroom.flightassistantxaerocompat.FlightAssistantXaeroCompat;
import dev.djshelfmushroom.flightassistantxaerocompat.map.FlightPlanMapOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge event listener that handles rendering the FlightAssistant flight-plan
 * overlay on top of Xaero's World Map screen.
 *
 * <p>This listener is registered to {@code MinecraftForge.EVENT_BUS} from
 * {@link FlightAssistantXaeroCompat#clientSetup}.</p>
 *
 * <p>Right-click context-menu injection is handled via the
 * {@link dev.djshelfmushroom.flightassistantxaerocompat.mixin.MixinGuiMap} mixin
 * rather than here, keeping the two concerns separated.</p>
 */
public class WaypointContextMenuHandler {

    /** Fully-qualified name of Xaero's world-map screen class. */
    private static final String GUI_MAP_CLASS = "xaero.map.gui.GuiMap";

    /**
     * Called after every screen render frame.  When the current screen is
     * Xaero's {@code GuiMap}, delegates to {@link FlightPlanMapOverlay#render}
     * to draw the flight-plan overlay on top.
     *
     * @param event the post-render screen event
     */
    @SubscribeEvent
    public void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!FlightAssistantXaeroCompat.xaeroPresent
                || !FlightAssistantXaeroCompat.flightAssistantPresent) {
            return;
        }

        Screen screen = event.getScreen();
        if (screen == null) return;

        // Check without creating a hard compile-time dependency on Xaero's class
        if (!isGuiMap(screen)) return;

        try {
            FlightPlanMapOverlay.render(event.getGuiGraphics(), screen);
        } catch (Exception e) {
            FlightAssistantXaeroCompat.LOGGER.warn(
                    "[FA-Xaero] FlightPlanMapOverlay.render failed", e);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Returns {@code true} if the given screen is an instance of Xaero's
     * {@code GuiMap} class.  Uses a cached class reference to avoid re-loading
     * the class on every render tick.
     */
    private static Class<?> guiMapClass;
    private static boolean guiMapClassLoadFailed;

    private static boolean isGuiMap(Screen screen) {
        if (guiMapClassLoadFailed) return false;
        if (guiMapClass == null) {
            try {
                guiMapClass = Class.forName(GUI_MAP_CLASS);
            } catch (ClassNotFoundException e) {
                guiMapClassLoadFailed = true;
                return false;
            }
        }
        return guiMapClass.isInstance(screen);
    }
}
