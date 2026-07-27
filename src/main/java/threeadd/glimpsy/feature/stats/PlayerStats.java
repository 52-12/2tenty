package threeadd.glimpsy.feature.stats;

import java.util.UUID;

public class PlayerStats {

    private final UUID playerId;
    private int fishCaught;
    private int kills;
    private int deaths;
    private long playtimeMillis;

    public PlayerStats(UUID playerId) {
        this(playerId, 0, 0, 0, 0);
    }

    public PlayerStats(UUID playerId, int fishCaught, int kills, int deaths, long playtimeMillis) {
        this.playerId = playerId;
        this.fishCaught = fishCaught;
        this.kills = kills;
        this.deaths = deaths;
        this.playtimeMillis = playtimeMillis;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getFishCaught() {
        return fishCaught;
    }

    public void incrementFishCaught() {
        fishCaught++;
    }

    public int getKills() {
        return kills;
    }

    public void incrementKills() {
        kills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void incrementDeaths() {
        deaths++;
    }

    public long getPlaytimeMillis() {
        return playtimeMillis;
    }

    public void addPlaytimeMillis(long millis) {
        playtimeMillis += millis;
    }
}
