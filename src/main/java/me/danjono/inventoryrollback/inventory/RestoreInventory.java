package me.danjono.inventoryrollback.inventory;

import me.danjono.inventoryrollback.InventoryRollback;
import me.danjono.inventoryrollback.config.MessageData;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;

public class RestoreInventory {

    private final FileConfiguration playerData;
    private final Long timestamp;

    public RestoreInventory(FileConfiguration playerData, Long timestamp) {
        this.playerData = playerData;
        this.timestamp = timestamp;
    }

    public ItemStack[] retrieveMainInventory() {
        ItemStack[] inv = null;

        try {
            inv = stacksFromBase64(playerData.getString("data." + timestamp + ".inventory"));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        return inv;
    }

    public ItemStack[] retrieveArmour() {
        ItemStack[] inv = null;

        try {
            inv = stacksFromBase64(playerData.getString("data." + timestamp + ".armour"));

            if (inv.length == 0)
                inv = null;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        return inv;
    }

    public ItemStack[] retrieveEnderChestInventory() {
        ItemStack[] inv = null;

        try {
            inv = stacksFromBase64(playerData.getString("data." + timestamp + ".enderchest"));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        return inv;
    }

    private ItemStack[] stacksFromBase64(String data) {
        if (data == null || Base64Coder.decodeLines(data).equals(null))
            return new ItemStack[]{};

        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
        BukkitObjectInputStream dataInput = null;
        ItemStack[] stacks = null;

        try {
            dataInput = new BukkitObjectInputStream(inputStream);
            stacks = new ItemStack[dataInput.readInt()];
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        for (int i = 0; i < stacks.length; i++) {
            try {
                stacks[i] = (ItemStack) dataInput.readObject();
            } catch (IOException | ClassNotFoundException e) {
                String packageVersion = playerData.getString("data." + timestamp + ".version");

                //Backup generated before InventoryRollback 1.3
                if (packageVersion == null) {
                    InventoryRollback.logger.log(Level.SEVERE, ChatColor.stripColor(MessageData.pluginName) + "There was an error deserializing the material data. This is likely caused by a now incompatible material ID if the backup was originally generated on a different Minecraft server version.");
                }
                //Backup was not generated on the same server version
                else if (!packageVersion.equalsIgnoreCase(InventoryRollback.getPackageVersion())) {
                    InventoryRollback.logger.log(Level.SEVERE, ChatColor.stripColor(MessageData.pluginName) + "There was an error deserializing the material data. The backup was generated on a " + packageVersion + " version server whereas you are now running a " + InventoryRollback.getPackageVersion() + " version server. It is likely a material ID inside the backup is no longer valid on this Minecraft server version and cannot be convereted.");
                }
                //Unknown error
                else if (packageVersion.equalsIgnoreCase(InventoryRollback.getPackageVersion())) {
                    InventoryRollback.logger.log(Level.SEVERE, ChatColor.stripColor(MessageData.pluginName) + "There was an error deserializing the material data. Please upload the affected players backup file to Pastebin and send a link to it in the discussion page on Spigot for InventoryRollback detailing the problem as accurately as you can.");
                }

                try {
                    dataInput.close();
                } catch (IOException ignored) {
                }
                return null;
            }
        }

        try {
            dataInput.close();
        } catch (IOException ignored) {
        }

        return stacks;
    }

    public Double getHealth() {
        return playerData.getDouble("data." + timestamp + ".health");
    }

    public int getHunger() {
        return playerData.getInt("data." + timestamp + ".hunger");
    }

    public float getSaturation() {
        return (float) playerData.getDouble("data." + timestamp + ".saturation");
    }

    public float getXP() {
        return (float) playerData.getDouble("data." + timestamp + ".xp");
    }

    public int getLevel() {
        return playerData.getInt("data." + timestamp + ".level");
    }

}
