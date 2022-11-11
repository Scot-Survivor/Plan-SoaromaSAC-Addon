package com.shimincraft.plansac.soaromasacplanaddon;

import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.query.QueryService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bukkit.Bukkit.getLogger;

public class SoaromaSACStorage {
    private final QueryService queryService;

    private final HashMap<ViolationType, String> violationTypeHashMap = new HashMap<>();

    public SoaromaSACStorage() {
        this.queryService = QueryService.getInstance();
        createTable();
        queryService.subscribeDataClearEvent(this::recreateTable);
        queryService.subscribeToPlayerRemoveEvent(this::removePlayer);
        violationTypeHashMap.put(ViolationType.COMBAT_VIOLATION, "soaroma_combat_violations");
        violationTypeHashMap.put(ViolationType.MOVEMENT_VIOLATION, "soaroma_movement_violations");
        violationTypeHashMap.put(ViolationType.WORLD_VIOLATION, "soaroma_world_violations");
        violationTypeHashMap.put(ViolationType.OTHER_VIOLATION, "soaroma_other_violations");
    }

    private void createTable() {
        String dbtype = queryService.getDBType();
        boolean sqlite = dbtype.equalsIgnoreCase("SQLITE");

        String sql = "CREATE TABLE IF NOT EXISTS plan_soaroma (" +
                "id int " + (sqlite ? "PRIMARY KEY" : "NOT NULL AUTO_INCREMENT") + ',' +
                "uuid varchar(36) NOT NULL UNIQUE," +
                "soaroma_violations int NOT NULL DEFAULT 0," +
                "soaroma_combat_violations int NOT NULL DEFAULT 0," +
                "soaroma_movement_violations int NOT NULL DEFAULT 0," +
                "soaroma_world_violations int NOT NULL DEFAULT 0," +
                "soaroma_other_violations int NOT NULL DEFAULT 0" +
                (sqlite ? "" : ",PRIMARY KEY (id)") +
                ")";
        try {
            queryService.execute(sql, PreparedStatement::execute).get();
        } catch (InterruptedException | ExecutionException e) {
            getLogger().severe("Failed to create table: " + e.getMessage());
        }
    }

    private void dropTable() {
        queryService.execute("DROP TABLE IF EXISTS plan_soaroma", PreparedStatement::execute);
    }

    private void recreateTable() {
        dropTable();
        createTable();
    }

    private void removePlayer(UUID playerUUID) {
        queryService.execute(
                "DELETE FROM plan_soaroma WHERE uuid=?",
                statement -> {
                    statement.setString(1, playerUUID.toString());
                    statement.execute();
                }
        );
    }

    public int getTypeViolations(UUID uuid, String violation_string) {
        String sql = "SELECT " + violation_string + " FROM plan_soaroma WHERE uuid=?";
        return queryService.query(sql, statement -> {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(violation_string) : 0;
            }
        });
    }

    public int getSumViolation(String violation_string) {
        String sql = "SELECT SUM(" + violation_string + ") FROM plan_soaroma";
        try {
            return queryService.query(sql, statement -> {
                ResultSet resultSet = statement.executeQuery();
                return resultSet.next() ? resultSet.getInt(1) : 0;
            });
        } catch (Exception e) {
            getLogger().severe("Failed to get sum of violations: " + e.getMessage());
            return 0;
        }
    }

    public int getTotalViolations(UUID uuid) {
        String sql = "SELECT soaroma_violations FROM plan_soaroma WHERE uuid=?";
        return queryService.query(sql, statement -> {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("soaroma_violations") : 0;
            }
        });
    }

    public void storeViolation(UUID uuid, ViolationType violation_type, int violations) throws SQLException, ExecutionException, InterruptedException {
        String violation_string = "";
        try {
            violation_string = violationTypeHashMap.get(violation_type);
            if (violation_string == null || violation_string.isEmpty()) {
                getLogger().warning("Violation type not found: " + violation_type);
                return;
            }
        } catch (NullPointerException e) {
            getLogger().warning("Violation type not found: " + violation_type);
            return;
        }
        String update = "UPDATE plan_soaroma SET " + violation_string + "=?, soaroma_violations=? WHERE uuid=?";
        String insert = "INSERT INTO plan_soaroma (uuid, " + violation_string + ", soaroma_violations) VALUES (?, ?, ?)";

        int previousTypeViolations = getTypeViolations(uuid, violation_string);
        int previousTotalViolations = getTotalViolations(uuid);
        AtomicBoolean updateSuccess = new AtomicBoolean(false);
        violations = violations != 0 ? 1 : 0;  // Violations are increasing as it goes, always only add 1, instead of constantly adding violations.
        int finalViolations = violations;
        int finalTotalViolations = violations + previousTotalViolations;
        queryService.execute(update, statement -> {
            getLogger().info("Updating violation: " + uuid.toString() + " " + violation_type.toString() + " " + finalViolations);
            statement.setInt(1, previousTypeViolations + finalViolations);
            statement.setInt(2, finalTotalViolations);
            statement.setString(3, uuid.toString());
            updateSuccess.set(statement.executeUpdate() == 1);
        }).get();
        if (!updateSuccess.get()) {
            queryService.execute(insert, statement -> {
                getLogger().info("Inserting violation: " + uuid.toString() + " " + violation_type.toString() + " " + finalViolations);
                statement.setString(1, uuid.toString());
                statement.setInt(2, finalViolations + previousTotalViolations);
                statement.setInt(3, finalTotalViolations);
                statement.execute();
            });
        }
    }

    public Map<String, Integer> getTotalViolationCounts() throws NotReadyException {
        UUID serverUUID = queryService.getServerUUID().orElseThrow(NotReadyException::new);
        final String sql = "SELECT plan_soaroma.uuid, plan_soaroma.soaroma_violations FROM plan_soaroma " +
                "INNER JOIN plan_users ON plan_soaroma.uuid = plan_users.uuid " +
                "INNER JOIN plan_user_info ON plan_user_info.user_id = plan_users.id " +
                "WHERE plan_user_info.server_id = (SELECT id FROM plan_servers WHERE uuid = ?) " +
                "GROUP BY soaroma_violations";

        return queryService.query(sql, statement -> {
            statement.setString(1, serverUUID.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<String, Integer> violations = new HashMap<>();
                while (resultSet.next()) {
                    UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                    Player player = Bukkit.getPlayer(uuid);
                    String name = player != null ? player.getName() : "Unknown";
                    int violationsCount = resultSet.getInt("soaroma_violations");
                    violations.put(name, violationsCount);
                }
                return violations;
            }
        });
    }
}
