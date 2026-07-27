package threeadd.glimpsy.feature.teams;

import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.persistence.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamRepository extends Repository<TeamData> {

    private static final TeamRepository instance = new TeamRepository();

    public static TeamRepository getInstance() {
        return instance;
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS teams (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "owner VARCHAR(36) NOT NULL," +
                    "invite_mode VARCHAR(16) NOT NULL DEFAULT 'OWNER_ONLY'," +
                    "alliance_id VARCHAR(36)," +
                    "alliance_offer_to VARCHAR(36)" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS team_members (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "team_id VARCHAR(36) NOT NULL" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS team_trusted (" +
                    "team_id VARCHAR(36) NOT NULL," +
                    "uuid VARCHAR(36) NOT NULL," +
                    "PRIMARY KEY (team_id, uuid)" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS team_shield_skins (" +
                    "team_id VARCHAR(36) PRIMARY KEY," +
                    "patterns TEXT NOT NULL" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS alliances (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "name TEXT" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS team_name_rules (" +
                    "name TEXT PRIMARY KEY," +
                    "rule VARCHAR(16) NOT NULL" +
                    ")");
        }

        // CREATE TABLE IF NOT EXISTS is a no-op on a database from before these columns existed -
        // add them separately, ignoring the failure if already there. (Superseded the old,
        // now-unused allied_team column from before multi-team alliances existed.)
        addColumnIfMissing(conn, "alliance_id");
        addColumnIfMissing(conn, "alliance_offer_to");
    }

    private void addColumnIfMissing(Connection conn, String column) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE teams ADD COLUMN " + column + " VARCHAR(36)");
        } catch (SQLException ignored) {
            // Column already exists.
        }
    }

    private static String serializePatterns(List<Pattern> patterns) {
        Registry<PatternType> registry = ShieldSkinDesignParser.bannerPatternRegistry();

        StringBuilder sb = new StringBuilder();
        for (Pattern pattern : patterns) {
            if (!sb.isEmpty()) sb.append(',');
            NamespacedKey key = registry.getKeyOrThrow(pattern.getPattern());
            sb.append(pattern.getColor().name()).append(':').append(key.getKey());
        }
        return sb.toString();
    }

    private static List<Pattern> deserializePatterns(String raw) {
        List<Pattern> patterns = new ArrayList<>();
        if (raw == null || raw.isBlank()) return patterns;

        Registry<PatternType> registry = ShieldSkinDesignParser.bannerPatternRegistry();
        for (String entry : raw.split(",")) {
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) continue;

            PatternType type = registry.get(NamespacedKey.minecraft(parts[1]));
            if (type == null) continue;

            patterns.add(new Pattern(DyeColor.valueOf(parts[0]), type));
        }
        return patterns;
    }

    public Map<UUID, TeamData> loadAllSync() {
        Map<UUID, TeamData> teams = new HashMap<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM teams")) {
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    TeamData team = new TeamData(id, rs.getString("name"), UUID.fromString(rs.getString("owner")));
                    team.getMembers().clear(); // constructor auto-adds owner - membership rows are authoritative
                    team.setInviteMode(TeamData.InviteMode.valueOf(rs.getString("invite_mode")));

                    String allianceId = rs.getString("alliance_id");
                    if (allianceId != null) team.setAllianceId(UUID.fromString(allianceId));

                    String allianceOfferTo = rs.getString("alliance_offer_to");
                    if (allianceOfferTo != null) team.setAllianceOfferTo(UUID.fromString(allianceOfferTo));

                    teams.put(id, team);
                }
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM team_members")) {
                while (rs.next()) {
                    TeamData team = teams.get(UUID.fromString(rs.getString("team_id")));
                    if (team != null) team.getMembers().add(UUID.fromString(rs.getString("uuid")));
                }
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM team_trusted")) {
                while (rs.next()) {
                    TeamData team = teams.get(UUID.fromString(rs.getString("team_id")));
                    if (team != null) team.getTrustedInviters().add(UUID.fromString(rs.getString("uuid")));
                }
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM team_shield_skins")) {
                while (rs.next()) {
                    TeamData team = teams.get(UUID.fromString(rs.getString("team_id")));
                    if (team != null) team.setShieldPatterns(deserializePatterns(rs.getString("patterns")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load teams", e);
        }

        return teams;
    }

    public void saveTeamSync(TeamData team) {
        executeUpdate("REPLACE INTO teams (id, name, owner, invite_mode, alliance_id, alliance_offer_to) VALUES (?, ?, ?, ?, ?, ?)",
                team.getId().toString(), team.getRawName(), team.getOwner().toString(), team.getInviteMode().name(),
                team.getAllianceId() == null ? null : team.getAllianceId().toString(),
                team.getAllianceOfferTo() == null ? null : team.getAllianceOfferTo().toString());

        executeUpdate("DELETE FROM team_members WHERE team_id = ?", team.getId().toString());
        for (UUID member : team.getMembers())
            executeUpdate("REPLACE INTO team_members (uuid, team_id) VALUES (?, ?)", member.toString(), team.getId().toString());

        executeUpdate("DELETE FROM team_trusted WHERE team_id = ?", team.getId().toString());
        for (UUID trusted : team.getTrustedInviters())
            executeUpdate("REPLACE INTO team_trusted (team_id, uuid) VALUES (?, ?)", team.getId().toString(), trusted.toString());

        executeUpdate("DELETE FROM team_shield_skins WHERE team_id = ?", team.getId().toString());
        if (!team.getShieldPatterns().isEmpty())
            executeUpdate("REPLACE INTO team_shield_skins (team_id, patterns) VALUES (?, ?)",
                    team.getId().toString(), serializePatterns(team.getShieldPatterns()));
    }

    public void deleteTeamSync(UUID teamId) {
        executeUpdate("DELETE FROM teams WHERE id = ?", teamId.toString());
        executeUpdate("DELETE FROM team_members WHERE team_id = ?", teamId.toString());
        executeUpdate("DELETE FROM team_trusted WHERE team_id = ?", teamId.toString());
        executeUpdate("DELETE FROM team_shield_skins WHERE team_id = ?", teamId.toString());
    }

    public void runAsyncSave(TeamData team) {
        runAsync(() -> saveTeamSync(team));
    }

    public void runAsyncDelete(UUID teamId) {
        runAsync(() -> deleteTeamSync(teamId));
    }

    // Alliances - membership itself lives on each TeamData's alliance_id, this table only
    // exists to hold the shared name once one is set.

    public Map<UUID, Alliance> loadAlliancesSync() {
        Map<UUID, Alliance> alliances = new HashMap<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM alliances")) {
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                Alliance alliance = new Alliance(id);
                alliance.setRawName(rs.getString("name"));
                alliances.put(id, alliance);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load alliances", e);
        }

        return alliances;
    }

    public void saveAllianceSync(Alliance alliance) {
        executeUpdate("REPLACE INTO alliances (id, name) VALUES (?, ?)",
                alliance.getId().toString(), alliance.getRawName());
    }

    public void deleteAllianceSync(UUID allianceId) {
        executeUpdate("DELETE FROM alliances WHERE id = ?", allianceId.toString());
    }

    public void runAsyncSaveAlliance(Alliance alliance) {
        runAsync(() -> saveAllianceSync(alliance));
    }

    public void runAsyncDeleteAlliance(UUID allianceId) {
        runAsync(() -> deleteAllianceSync(allianceId));
    }

    // Name rules - admin-set per-name overrides on top of SimpleWordFilter. Names are stored
    // lowercased; rule is "BLACKLIST" or "WHITELIST".

    public Map<String, String> loadNameRulesSync() {
        Map<String, String> rules = new HashMap<>();

        try (Connection conn = Glimpsy.getDataBase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM team_name_rules")) {
            while (rs.next())
                rules.put(rs.getString("name"), rs.getString("rule"));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load team name rules", e);
        }

        return rules;
    }

    public void runAsyncSaveNameRule(String name, String rule) {
        runAsync(() -> executeUpdate("REPLACE INTO team_name_rules (name, rule) VALUES (?, ?)", name, rule));
    }

    public void runAsyncDeleteNameRule(String name) {
        runAsync(() -> executeUpdate("DELETE FROM team_name_rules WHERE name = ?", name));
    }
}
