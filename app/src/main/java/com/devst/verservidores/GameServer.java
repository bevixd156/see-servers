package com.devst.verservidores;

public class GameServer {
    public String name, status;
    public boolean online;
    public int players;

    public GameServer(String name, boolean online, int players, String status) {
        this.name = name;
        this.online = online;
        this.players = players;
        this.status = status;
    }
}