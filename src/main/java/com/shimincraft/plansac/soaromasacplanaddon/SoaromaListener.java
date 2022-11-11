package com.shimincraft.plansac.soaromasacplanaddon;

import me.korbsti.soaromaac.api.SoaromaFlagEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getScheduler;

public class SoaromaListener implements Listener {
    private final SoaromaSACStorage storage;
    private final Plugin plugin;
    private final HashMap<String, ViolationType> violationTypeHashMap = new HashMap<>();

    public SoaromaListener(SoaromaSACStorage storage, Plugin plugin) {
        this.storage = storage;
        this.plugin = plugin;
        violationTypeHashMap.put("Flight", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("BunnyHop", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("Glide", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("FastClimb", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("FluidWalk", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("NoFall", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("Speed", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("NoSlowDown", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("Spider", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("Step", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("ElytraFlight", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("IrregularStartup", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("MedianSpeed", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("BadPackets", ViolationType.MOVEMENT_VIOLATION);

        violationTypeHashMap.put("Reach", ViolationType.COMBAT_VIOLATION);
        violationTypeHashMap.put("Criticals", ViolationType.COMBAT_VIOLATION);

        violationTypeHashMap.put("Irregular Placement", ViolationType.WORLD_VIOLATION);
        violationTypeHashMap.put("Fast placement", ViolationType.WORLD_VIOLATION);
        violationTypeHashMap.put("ReachBreak", ViolationType.WORLD_VIOLATION);
        violationTypeHashMap.put("ReachPlace", ViolationType.WORLD_VIOLATION);
        violationTypeHashMap.put("Nuker", ViolationType.WORLD_VIOLATION);

        violationTypeHashMap.put("IrregularEventCounter", ViolationType.OTHER_VIOLATION);
        violationTypeHashMap.put("Regen", ViolationType.OTHER_VIOLATION);
        violationTypeHashMap.put("AutoClicker", ViolationType.OTHER_VIOLATION);
        violationTypeHashMap.put("IrregularMovement", ViolationType.OTHER_VIOLATION);
        violationTypeHashMap.put("Baritone", ViolationType.OTHER_VIOLATION);
        violationTypeHashMap.put("GhostHand", ViolationType.OTHER_VIOLATION);
        violationTypeHashMap.put("SemiPrediction", ViolationType.OTHER_VIOLATION);
    }

    @EventHandler
    public void onFlag(SoaromaFlagEvent event) throws SQLException, ExecutionException, InterruptedException {
        Player flagged_player = event.getFlaggedPlayer();
        if (flagged_player == null) {
            getLogger().warning("Player is null on flag. Likely due to have sac.bypass permission.");
            return;
        }
        ViolationType violationType = violationTypeHashMap.get(event.getCheckFlagged());
        if (violationType == null) {
            getLogger().info("Unknown violation type: " + event.getCheckFlagged());
            return;
        }
        getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                storage.storeViolation(flagged_player.getUniqueId(), violationType, event.getTotalViolationAmount(flagged_player));
            } catch (SQLException e) {
                getLogger().info("Failed to add violation to database");
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
