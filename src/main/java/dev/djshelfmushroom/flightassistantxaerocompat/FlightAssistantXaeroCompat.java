package dev.djshelfmushroom.flightassistantxaerocompat;

import dev.djshelfmushroom.flightassistantxaerocompat.compat.XaeroCompat;
import dev.djshelfmushroom.flightassistantxaerocompat.events.FlightPlanNavigationTickHandler;
import dev.djshelfmushroom.flightassistantxaerocompat.events.WaypointContextMenuHandler;
import dev.djshelfmushroom.flightassistantxaerocompat.render.InWorldWaypointRenderer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main mod class for FlightAssistantXaeroCompat.
 *
 * <p>This mod integrates Xaero's World Map with FlightAssistant. It is a purely
 * client-side mod and will not affect server behaviour in any way.</p>
 *
 * <p>Both Xaero's World Map and FlightAssistant are required runtime dependencies.
 * If either is absent the mod will log a warning and disable the affected
 * features rather than crashing.</p>
 */
@Mod(FlightAssistantXaeroCompat.MOD_ID)
public class FlightAssistantXaeroCompat {

    public static final String MOD_ID = "flightassistant_xaero_compat";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    /** Whether Xaero's World Map is present in the current mod-set. */
    public static boolean xaeroPresent = false;
    /** Whether FlightAssistant is present in the current mod-set. */
    public static boolean flightAssistantPresent = false;

    public FlightAssistantXaeroCompat() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::clientSetup);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        xaeroPresent = ModList.get().isLoaded("xaeroworldmap");
        flightAssistantPresent = ModList.get().isLoaded("flightassistant");

        if (!xaeroPresent) {
            LOGGER.warn("[{}] Xaero's World Map is not loaded — map features will be disabled.", MOD_ID);
        }
        if (!flightAssistantPresent) {
            LOGGER.warn("[{}] FlightAssistant is not loaded — autopilot features will be disabled.", MOD_ID);
        }

        if (xaeroPresent) {
            XaeroCompat.init();
        }

        if (xaeroPresent && flightAssistantPresent) {
            MinecraftForge.EVENT_BUS.register(new WaypointContextMenuHandler());
            LOGGER.info("[{}] Waypoint context menu handler registered.", MOD_ID);
        }

        if (flightAssistantPresent) {
            MinecraftForge.EVENT_BUS.register(new FlightPlanNavigationTickHandler());
            LOGGER.info("[{}] Flight-plan navigation tick handler registered.", MOD_ID);
        }

        if (flightAssistantPresent || xaeroPresent) {
            MinecraftForge.EVENT_BUS.register(new InWorldWaypointRenderer());
            LOGGER.info("[{}] In-world waypoint renderer registered.", MOD_ID);
        }

        LOGGER.info("[{}] Client setup complete. Xaero={}, FA={}",
                MOD_ID, xaeroPresent, flightAssistantPresent);
    }
}
