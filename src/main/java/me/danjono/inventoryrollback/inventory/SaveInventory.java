package me.danjono.inventoryrollback.inventory;

import me.danjono.inventoryrollback.InventoryRollback;
import me.danjono.inventoryrollback.InventoryRollback.VersionName;
import me.danjono.inventoryrollback.data.LogType;
import me.danjono.inventoryrollback.data.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SaveInventory {

    private final Player player;
    private final LogType logType;
    private final DamageCause deathCause;

    private final PlayerInventory mainInventory;
    private final Inventory enderChestInventory;

    public SaveInventory(Player player, LogType logType, DamageCause deathCause, PlayerInventory mainInventory, Inventory enderChestInventory) {
        this.player = player;
        this.logType = logType;
        this.deathCause = deathCause;

        this.mainInventory = mainInventory;
        this.enderChestInventory = enderChestInventory;
    }

    public void createSave() {
        PlayerData data = new PlayerData(player, logType);
        FileConfiguration inventoryData = data.getData();

        ItemStack[] armour = null;
        if (InventoryRollback.getVersion().equals(VersionName.v1_8))
            armour = mainInventory.getArmorContents();

        int maxSaves = data.getMaxSaves();

        //float xp = getTotalExperience(player);
        float xp = player.getExp();
        int level = player.getLevel();
        long time = System.currentTimeMillis();
        int saves = inventoryData.getInt("saves");

        if (data.getFile().exists() && maxSaves > 0) {
            if (saves >= maxSaves) {
                List<Double> timeSaved = new ArrayList<>();

                for (String times : inventoryData.getConfigurationSection("data").getKeys(false)) {
                    timeSaved.add(Double.parseDouble(times));
                }

                int deleteAmount = saves - maxSaves + 1;

                for (int i = 0; i < deleteAmount; i++) {
                    Double deleteData = Collections.min(timeSaved);
                    DecimalFormat df = new DecimalFormat("#.##############");

                    inventoryData.set("data." + df.format(deleteData), null);
                    timeSaved.remove(deleteData);
                    saves--;
                }
            }
        }

        inventoryData.set("data." + time + ".inventory", toBase64(mainInventory));

        if (InventoryRollback.getVersion().equals(VersionName.v1_8) && armour != null)
            inventoryData.set("data." + time + ".armour", toBase64(armour));

        inventoryData.set("data." + time + ".enderchest", toBase64(enderChestInventory));
        inventoryData.set("data." + time + ".level", level);
        inventoryData.set("data." + time + ".xp", xp);
        inventoryData.set("data." + time + ".health", player.getHealth());
        inventoryData.set("data." + time + ".hunger", player.getFoodLevel());
        inventoryData.set("data." + time + ".saturation", player.getSaturation());
        inventoryData.set("data." + time + ".location.world", player.getWorld().getName());
        inventoryData.set("data." + time + ".location.x", Math.floor(player.getLocation().getX()) + 0.5);
        inventoryData.set("data." + time + ".location.y", Math.floor(player.getLocation().getY()));
        inventoryData.set("data." + time + ".location.z", Math.floor(player.getLocation().getZ()) + 0.5);
        inventoryData.set("data." + time + ".logType", logType.name());
        inventoryData.set("data." + time + ".version", InventoryRollback.getPackageVersion());

        if (deathCause != null)
            inventoryData.set("data." + time + ".deathReason", deathCause.name());

        inventoryData.set("saves", saves + 1);

        data.saveData();
    }

    //Conversion to Base64 code courtesy of github.com/JustRayz
    private String toBase64(Inventory inventory) {
        return toBase64(inventory.getContents());
    }

    private String toBase64(ItemStack[] contents) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(contents.length);

            for (ItemStack stack : contents) {
                dataOutput.writeObject(stack);
            }
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

}
