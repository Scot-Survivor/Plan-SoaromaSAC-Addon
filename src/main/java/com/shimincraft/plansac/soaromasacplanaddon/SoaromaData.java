package com.shimincraft.plansac.soaromasacplanaddon;

import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@PluginInfo(
        name = "SoaromaSAC Statistics",
        iconName = "vial",
        iconFamily = Family.SOLID,
        color = Color.RED
)
@TabInfo(
        tab = "SoaromaSAC",
        iconName = "skull-crossbones",
        iconFamily = Family.SOLID,
        elementOrder = {}
)
public class SoaromaData implements DataExtension {
    private final SoaromaSACStorage storage;

    public SoaromaData(SoaromaSACStorage storage) {
        this.storage = storage;
    }

    @NumberProvider(
            text = "Number of Violations",
            description = "Number of violations, a player has",
            iconName = "bookmark",
            iconColor = Color.GREEN,
            priority = 10,
            showInPlayerTable = true
    )
    @Tab("SoaromaSAC")
    public long getNumberOfViolations(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        assert player != null;
        return storage.getTotalViolations(player.getUniqueId());
    }

    @TableProvider(tableColor = Color.RED)
    @Tab("SoaromaSAC")
    public Table topViolationsProvider() {
        Table.Factory table = Table.builder()
                .columnOne("Users", Icon.called("users").build())
                .columnTwo("Violations", Icon.called("signal").build());

        storage.getTotalViolationCounts().forEach(table::addRow);
        return table.build();
    }

    @TableProvider(tableColor = Color.GREY)
    @Tab("SoaromaSAC")
    public Table violationTotalTypesProvider() {
        Table.Factory table = Table.builder()
                .columnOne("Violation Type", Icon.called("users").build())
                .columnTwo("Count", Icon.called("target").build());
        table.addRow("Combat", storage.getSumViolation("soaroma_combat_violations"));
        table.addRow("Movement", storage.getSumViolation("soaroma_movement_violations"));
        table.addRow("World", storage.getSumViolation("soaroma_world_violations"));
        table.addRow("Other", storage.getSumViolation("soaroma_other_violations"));
        return table.build();
    }
}
