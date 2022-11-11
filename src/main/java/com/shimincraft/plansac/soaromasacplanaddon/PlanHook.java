package com.shimincraft.plansac.soaromasacplanaddon;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.ExtensionService;
import me.korbsti.soaromaac.api.SoaromaAPI;

import static org.bukkit.Bukkit.getLogger;

public class PlanHook {
    private final SoaromaAPI soaromaAPI;
    private final SoaromaSACStorage storage;

    public PlanHook(SoaromaAPI soaromaAPI, SoaromaSACStorage storage) {
        this.soaromaAPI = soaromaAPI;
        this.storage = storage;
    }

    public void hookIntoPlan() {
        if (!areAllCapabilitiesAvailable()) {
            getLogger().warning("PlanVulcan may not be compatible with your Plan version.");
        }
        registerDataExtension();
    }

    private boolean areAllCapabilitiesAvailable() {
        CapabilityService capabilities = CapabilityService.getInstance();
        return capabilities.hasCapability("DATA_EXTENSION_VALUES") && capabilities.hasCapability("DATA_EXTENSION_TABLE");
    }

    private void registerDataExtension() {
        try {
            ExtensionService.getInstance().register(new SoaromaData(storage));
        } catch (IllegalStateException planIsNotEnabled) {
            // Plan is not enabled, handle exception
        }
    }
}
