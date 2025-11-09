package com.devst.verservidores;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class GameParser {
    private static final Gson gson = new Gson();

    public static List<GameServer> parseGames(String json) {
        List<GameServer> list = new ArrayList<>();
        if (json == null) return list;
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        if (!obj.has("games")) return list;
        JsonObject games = obj.getAsJsonObject("games");

        for (String key : games.keySet()) {
            JsonObject g = games.getAsJsonObject(key);
            boolean online = g.has("online") && g.get("online").getAsBoolean();
            int players = g.has("players_online") ? g.get("players_online").getAsInt() : 0;
            String status = g.has("status") ? g.get("status").getAsString() : "unknown";
            list.add(new GameServer(key.toUpperCase(), online, players, status));
        }
        return list;
    }
}