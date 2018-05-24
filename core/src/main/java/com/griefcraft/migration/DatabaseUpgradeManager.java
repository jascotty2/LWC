package com.griefcraft.migration;

import com.griefcraft.bukkit.EntityBlock;
import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import com.griefcraft.sql.PhysDB;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for converting databases from https://github.com/Brokkonaut/LWCEntityLocking into something we can use
 * @author jascotty2
 */
public class DatabaseUpgradeManager {
    public static void run() {
        try {
            LWC lwc = LWC.getInstance();
            PhysDB physicalDatabase = lwc.getPhysicalDatabase();
            
            PreparedStatement stmt = physicalDatabase.prepare("SELECT COUNT(*)  FROM " + physicalDatabase.getPrefix() + "protections"
                    + " WHERE blockid = " + EntityBlock.ENTITY_BLOCK_ID);
            ResultSet res = stmt.executeQuery();
            
            // is a conversion needed?
            if(res.next() && res.getInt(1) != 0) {
                System.out.println("[LWC] Database upgrade in progress");
                
                res.close();
                stmt.close();
                
                int changes = 0;
                
                // positive integers are not affected
                stmt = physicalDatabase.prepare("UPDATE " + physicalDatabase.getPrefix() + "protections"
                    + " SET blockid = " + EntityBlock.UNKNOWN_ENTITY_BLOCK_ID + " WHERE blockid = " + EntityBlock.ENTITY_BLOCK_ID
                    + " AND x >= " + EntityBlock.POSITION_OFFSET);
                changes += stmt.executeUpdate();
                stmt.close();
                
                // negative numbers need to be negative
                stmt = physicalDatabase.prepare("UPDATE " + physicalDatabase.getPrefix() + "protections"
                    + " SET blockid = " + EntityBlock.UNKNOWN_ENTITY_BLOCK_ID
                    + ", x = x - " + (EntityBlock.POSITION_OFFSET * 2)
                    + ", y = y - " + (EntityBlock.POSITION_OFFSET * 2)
                    + ", z = z - " + (EntityBlock.POSITION_OFFSET * 2)
                    + " WHERE blockid = " + EntityBlock.ENTITY_BLOCK_ID
                    + " AND x < " + EntityBlock.POSITION_OFFSET);
                changes += stmt.executeUpdate();
                stmt.close();
                
                System.out.println("[LWC] Database upgrade complete: " + changes + " records updated");
                
            } else {
                res.close();
                stmt.close();
            }
            
            
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseUpgradeManager.class.getName()).log(Level.SEVERE, "Failed to run database upgrade", ex);
        }
    }
}
