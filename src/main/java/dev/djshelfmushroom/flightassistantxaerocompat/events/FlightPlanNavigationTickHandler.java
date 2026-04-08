package dev.djshelfmushroom.flightassistantxaerocompat.events;

import dev.djshelfmushroom.flightassistantxaerocompat.compat.FlightAssistantCompat;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge event listener that drives per-tick flight-plan navigation monitoring.
 *
 * <p>On every client tick (start phase only) this handler delegates to
 * {@link FlightAssistantCompat#tickNavigation()}, which detects when
 * FlightAssistant's internal plan has advanced past the last enroute waypoint
 * and ensures any stale lateral-mode COORDS override is cleared so that the
 * autopilot correctly transitions to the arrival/approach phase.</p>
 *
 * <p>This listener is registered to {@code MinecraftForge.EVENT_BUS} from
 * {@link dev.djshelfmushroom.flightassistantxaerocompat.FlightAssistantXaeroCompat#clientSetup}
 * only when FlightAssistant is present.</p>
 */
public class FlightPlanNavigationTickHandler {

    /**
     * Called at the start of every client tick.
     * The {@link TickEvent.Phase#START} guard ensures we execute exactly once
     * per tick (Forge fires both START and END phases).
     *
     * @param event the client tick event
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        FlightAssistantCompat.tickNavigation();
    }
}
