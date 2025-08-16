package com.gtrxac.discord;

import cc.nnproject.json.*;
import javax.microedition.rms.*;
import java.util.*;

public class FavoriteGuilds {
    private static JSONArray guilds;  // array of favorite guild IDs
    private static boolean hasChanged;  // list of fav guilds has changed (items added/removed)?

//#ifdef OVER_100KB
    private static JSONArray muted;  // array of muted guild/channel IDs, where the user wont receive notifications
//#endif

    static {
        RecordStore rms = null;
        try {
            rms = RecordStore.openRecordStore("favguild", false);
        }
        catch (Exception e) {}
        
        try {
            guilds = JSON.getArray(new String(rms.getRecord(1)));
        }
        catch (Exception e) {
            guilds = new JSONArray();
        }
//#ifdef OVER_100KB
        try {
            muted = JSON.getArray(new String(rms.getRecord(2)));
        }
        catch (Exception e) {
            muted = new JSONArray();
        }
//#endif

        Util.closeRecordStore(rms);
    }

    private static void save() {
        RecordStore rms = null;
        try {
            rms = RecordStore.openRecordStore("favguild", true);
            byte[] bytes = guilds.build().getBytes();
            Util.setOrAddRecord(rms, 1, bytes);
//#ifdef OVER_100KB
            bytes = muted.build().getBytes();
            Util.setOrAddRecord(rms, 2, bytes);
//#endif
        }
        catch (Exception e) {
            App.error(e);
        }
        Util.closeRecordStore(rms);
    }

    public static void add(Guild g) {
        if (has(g)) return;
        guilds.add(g.id);
        hasChanged = true;
        save();
    }

    public static void remove(int index) {
        guilds.remove(index);
        hasChanged = true;
        save();
    }

    public static boolean empty() {
        return guilds.size() == 0;
    }

    public static boolean has(Guild g) {
        for (int i = 0; i < guilds.size(); i++) {
            if (guilds.getString(i).equals(g.id)) return true;
        }
        return false;
    }

//#ifdef OVER_100KB
    public static void toggleMute(String id) {
        if (isMuted(id)) {
            muted.remove(id);
        } else {
            muted.add(id);
        }
        save();
    }

    public static boolean isMuted(String id) {
        return muted.indexOf(id) != -1;
    }
//#endif

    public static void openSelector(boolean refresh, boolean forceRefresh) {
        if (Settings.highRamMode) refresh = false;
        
        // If guilds not loaded, load them. This method is called again by the thread when it's done.
        if (App.guilds == null || refresh || forceRefresh) {
            HTTPThread h = new HTTPThread(HTTPThread.FETCH_GUILDS);
            h.showFavGuilds = true;
            h.start();
            return;
        }
        if (App.guildSelector == null || !App.guildSelector.isFavGuilds || hasChanged) {
            Vector guildsVec = new Vector();

            for (int i = 0; i < guilds.size(); i++) {
                Guild g = Guild.getById(guilds.getString(i));
                if (g != null) guildsVec.addElement(g);
            }

            try {
                App.guildSelector = new GuildSelector(guildsVec, true);
                App.guildSelector.isFavGuilds = true;
            }
            catch (Exception e) {
                App.error(e);
                return;
            }
        }
        App.disp.setCurrent(App.guildSelector);

        hasChanged = false;
    }
}