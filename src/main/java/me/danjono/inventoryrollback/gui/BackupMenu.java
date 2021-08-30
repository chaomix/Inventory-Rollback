package me.danjono.inventoryrollback.gui;

import me.danjono.inventoryrollback.config.MessageData;
import me.danjono.inventoryrollback.data.LogType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BackupMenu {

    private final Player staff;
    private final UUID playerUUID;
    private final LogType logType;
    private final Long timestamp;
    private final ItemStack[] mainInventory;
    private final ItemStack[] armour;
    private final String location;
    private final boolean enderChestAvailable;
    private final double health;
    private final int hunger;
    private final float saturation;
    private final float xp;

    public BackupMenu(Player staff, UUID playerUUID, LogType logType, Long timestamp,
                        ItemStack[] main, ItemStack[] armour, String location, boolean enderchest,
                        Double health, int hunger, float saturation, float xp) {
        this.staff = staff;
        this.playerUUID = playerUUID;
        this.logType = logType;
        this.timestamp = timestamp;
        this.mainInventory = main;
        this.armour = armour;
        this.location = location;
        this.enderChestAvailable = enderchest;
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.xp = xp;
    }

    public Inventory showItems() {
        //Size 54 gives 6 rows of 9 columns.
        //Fifth row for amour, sixth row for buttons
        Inventory inv = Bukkit.createInventory(staff, 54, InventoryName.BACKUP.getName());
        Buttons buttons = new Buttons();

        int item = 0;
        int position = 0;

        //If the backup file is invalid it will return null, we want to catch it here
        try {
            //Add items -- Not sure why the length has 5 subtracted?
            for (; item < mainInventory.length - 5; item++) {
                if (mainInventory[item] != null) {
                    inv.setItem(position, mainInventory[item]);
                    position++;
                }          
            }
        } catch (NullPointerException e) {
            staff.sendMessage(MessageData.pluginName + MessageData.errorInventory);
            return null;
        }

        item = 36; //Last inventory slot+1, anything above it, should be impossible?
        position = 44; //End of 2nd to last row

        //Add armour
        if (armour != null) {
            for (ItemStack itemStack : armour) {
                inv.setItem(position, itemStack);
                position--;
            }
        } else {
            //Not sure if this loop will go through as max inventory is 36 (this is going from 37+)
            for (;item < mainInventory.length; item++) {
                if (mainInventory[item] != null) {
                    inv.setItem(position, mainInventory[item]);
                    position--;
                }
            }
        }

        //Add back button
        inv.setItem(46, buttons.inventoryMenuBackButton(MessageData.backButton, playerUUID, logType));

        //Add teleport back button
        if (location != null)
            inv.setItem(48, buttons.enderPearlButton(playerUUID, logType, timestamp, location));

        //Add Enderchest icon
        if (enderChestAvailable)
            inv.setItem(50, buttons.enderChestButton(playerUUID, logType, timestamp));

        //Add health icon
        inv.setItem(51, buttons.healthButton(playerUUID, logType, health));

        //Add hunger icon
        inv.setItem(52, buttons.hungerButton(playerUUID, logType, hunger, saturation));

        //Add Experience Bottle
        inv.setItem(53, buttons.experiencePotion(playerUUID, logType, xp));

        return inv;
    }

}
