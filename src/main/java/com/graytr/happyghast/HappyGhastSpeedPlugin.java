package com.graytr.happyghast;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.UUID;

public final class HappyGhastSpeedPlugin extends JavaPlugin implements Listener {

    private static final UUID SPEED_MOD_UUID = UUID.fromString("a3c5f7a1-98b5-4278-9f0a-3a5f9c8e6d1a");

    // Version-tolerant caches
    private Attribute ATTR_FLYING_SPEED;
    private Attribute ATTR_MOVEMENT_SPEED;
    private PotionEffectType TYPE_SLOW; // SLOW or SLOWNESS depending on API

    @Override
    public void onEnable() {
        // Resolve attributes (GENERIC_* vs new names)
        ATTR_FLYING_SPEED = resolveAttribute("GENERIC_FLYING_SPEED", "FLYING_SPEED");
        ATTR_MOVEMENT_SPEED = resolveAttribute("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED");

        // Resolve slowness effect type (SLOW or SLOWNESS)
        TYPE_SLOW = resolveEffectType("SLOW", "SLOWNESS");

        if (ATTR_MOVEMENT_SPEED == null) {
            getLogger().warning("Could not resolve MOVEMENT_SPEED attribute; the plugin will have limited/no effect.");
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("HappyGhastSpeed enabled.");

        // Refresh currently-loaded entities
        Bukkit.getWorlds().forEach(w -> w.getEntities().forEach(e -> {
            if (e instanceof LivingEntity le && isTargetHappyGhast(le)) {
                PotionEffect effS = le.getPotionEffect(PotionEffectType.SPEED);
                if (effS != null) applySpeedChange(le, effS.getAmplifier(), true);
                if (TYPE_SLOW != null) {
                    PotionEffect effL = le.getPotionEffect(TYPE_SLOW);
                    if (effL != null) applySpeedChange(le, effL.getAmplifier(), false);
                }
            }
        }));
    }

    @Override
    public void onDisable() {
        // Remove our speed modifier cleanly.
        Bukkit.getWorlds().forEach(w -> w.getEntities().forEach(e -> {
            if (e instanceof LivingEntity le) clearModifier(le);
        }));
        getLogger().info("HappyGhastSpeed disabled.");
    }

    @EventHandler
    public void onPotionEffectChange(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof LivingEntity le)) return;
        if (!isTargetHappyGhast(le)) return;

        PotionEffectType changed = event.getModifiedType();
        boolean isSpeed = (changed != null && changed.equals(PotionEffectType.SPEED));
        boolean isSlow  = (changed != null && TYPE_SLOW != null && changed.equals(TYPE_SLOW));

        if (!isSpeed && !isSlow) return;

        PotionEffect newEff = event.getNewEffect();
        if (newEff == null) {
            // effect removed
            clearModifier(le);
            return;
        }
        applySpeedChange(le, newEff.getAmplifier(), isSpeed);
    }

    private boolean isTargetHappyGhast(LivingEntity e) {
        // Official entity class/type (modern)
        try {
            if (e.getType() == EntityType.HAPPY_GHAST || e instanceof HappyGhast) return true;
        } catch (NoClassDefFoundError ignored) {
            // Older API without HappyGhast class; fall through to ghast+tags.
        }
        // Fallback for datapacks/backports: normal ghast with a tag/name marker.
        if (e instanceof Ghast) {
            if (e.getScoreboardTags().contains("happy_ghast")) return true;
            String name = e.getCustomName();
            if (name != null && name.toLowerCase(Locale.ROOT).contains("happy ghast")) return true;
        }
        return false;
    }

    private Attribute resolveAttribute(String... candidateNames) {
        for (String name : candidateNames) {
            try {
                Field f = Attribute.class.getField(name);
                Object v = f.get(null);
                if (v instanceof Attribute attr) {
                    getLogger().info("Resolved Attribute: " + name);
                    return attr;
                }
            } catch (NoSuchFieldException ignored) {
            } catch (IllegalAccessException e) {
                getLogger().warning("Failed to access Attribute." + name + ": " + e.getMessage());
            }
        }
        getLogger().warning("Could not resolve Attribute from candidates: " + String.join(", ", candidateNames));
        return null;
    }

    private PotionEffectType resolveEffectType(String... candidateNames) {
        for (String name : candidateNames) {
            try {
                Field f = PotionEffectType.class.getField(name);
                Object v = f.get(null);
                if (v instanceof PotionEffectType type) {
                    getLogger().info("Resolved PotionEffectType: " + name);
                    return type;
                }
            } catch (NoSuchFieldException ignored) {
            } catch (IllegalAccessException e) {
                getLogger().warning("Failed to access PotionEffectType." + name + ": " + e.getMessage());
            }
        }
        getLogger().warning("Could not resolve PotionEffectType from candidates: " + String.join(", ", candidateNames));
        return null;
    }

    private void applySpeedChange(LivingEntity entity, int amplifier, boolean isSpeed) {
        AttributeInstance inst = null;
        if (ATTR_FLYING_SPEED != null) inst = entity.getAttribute(ATTR_FLYING_SPEED);
        if (inst == null && ATTR_MOVEMENT_SPEED != null) inst = entity.getAttribute(ATTR_MOVEMENT_SPEED);
        if (inst == null) return;

        clearModifier(entity);

        double magnitude;
        if (amplifier <= 0) {
            magnitude = 0.40; // I
        } else if (amplifier == 1) {
            magnitude = 0.80; // II
        } else {
            magnitude = 0.40 * (amplifier + 1);
        }
        double multiplier = isSpeed ? magnitude : -magnitude;

        AttributeModifier mod = new AttributeModifier(
                SPEED_MOD_UUID,
                "HappyGhastSpeedBoost",
                multiplier,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
        );
        inst.addModifier(mod);
    }

    private void clearModifier(LivingEntity entity) {
        AttributeInstance inst = null;
        if (ATTR_FLYING_SPEED != null) inst = entity.getAttribute(ATTR_FLYING_SPEED);
        if (inst == null && ATTR_MOVEMENT_SPEED != null) inst = entity.getAttribute(ATTR_MOVEMENT_SPEED);
        if (inst == null) return;

        for (AttributeModifier mod : inst.getModifiers()) {
            if (mod.getUniqueId().equals(SPEED_MOD_UUID) || "HappyGhastSpeedBoost".equals(mod.getName())) {
                inst.removeModifier(mod);
            }
        }
    }
}
