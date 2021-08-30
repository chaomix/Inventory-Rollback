package me.danjono.inventoryrollback.inventory;

import me.danjono.inventoryrollback.InventoryRollback;
import me.danjono.inventoryrollback.config.MessageData;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    //Credits to Dev_Richard (https://www.spigotmc.org/members/dev_richard.38792/)
    //https://gist.github.com/RichardB122/8958201b54d90afbc6f0
    public static void setTotalExperience(Player player, float xpFloat) {
        int xp = (int) xpFloat;
        int remainder;
        int experienceNeeded;
        int level;
        //Levels 0 through 15
        if (xp >= 0 && xp < 351) {
            int a = 1;
            int b = 6;
            int c = -xp;
            level = (int) (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
            int xpForLevel = (int) (Math.pow(level, 2) + (6 * level));
            remainder = xp - xpForLevel;
            experienceNeeded = (2 * level) + 7;
            
            //Levels 16 through 30
        } else if (xp >= 352 && xp < 1507) {
            double a = 2.5;
            double b = -40.5;
            int c = -xp + 360;
            double dLevel = (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
            level = (int) Math.floor(dLevel);
            int xpForLevel = (int) (2.5 * Math.pow(level, 2) - (40.5 * level) + 360);
            remainder = xp - xpForLevel;
            experienceNeeded = (5 * level) - 38;
            
            //Level 31 and greater
        } else {
            double a = 4.5;
            double b = -162.5;
            int c = -xp + 2220;
            double dLevel = (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
            level = (int) Math.floor(dLevel);
            int xpForLevel = (int) (4.5 * Math.pow(level, 2) - (162.5 * level) + 2220);
            remainder = xp - xpForLevel;
            experienceNeeded = (9 * level) - 158;

        }

        float experience = (float) remainder / (float) experienceNeeded;
        experience = round(experience, 2);

        player.setLevel(level);
        player.setExp(experience);
    }

    public static float getLevel(float floatXP) {
        int xp = (int) floatXP;
        int level;
        //Levels 0 through 15
        if (xp >= 0 && xp < 351) {
            int a = 1;
            int b = 6;
            int c = -xp;
            level = (int) (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);

            //Levels 16 through 30
        } else if (xp >= 352 && xp < 1507) {
            double a = 2.5;
            double b = -40.5;
            int c = -xp + 360;
            double dLevel = (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
            level = (int) Math.floor(dLevel);
            
            //Level 31 and greater
        } else {
            double a = 4.5;
            double b = -162.5;
            int c = -xp + 2220;
            double dLevel = (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
            level = (int) Math.floor(dLevel);
        }
        return level;
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = BigDecimal.valueOf(d);
        bd = bd.setScale(decimalPlace, RoundingMode.HALF_DOWN);
        return bd.floatValue();
    }

}
