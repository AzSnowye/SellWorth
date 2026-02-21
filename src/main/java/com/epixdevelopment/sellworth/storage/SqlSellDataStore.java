package com.epixdevelopment.sellworth.storage;

import com.epixdevelopment.sellworth.Sell;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SqlSellDataStore implements SellDataStore {
    private final Sell plugin;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public SqlSellDataStore(Sell plugin, String jdbcUrl, String username, String password) {
        this.plugin = plugin;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public void init() throws Exception {
        try (Connection con = openConnection()) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS sellworth_players (uuid VARCHAR(36) PRIMARY KEY, total_sold DOUBLE NOT NULL)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS sellworth_categories (uuid VARCHAR(36) NOT NULL, category VARCHAR(64) NOT NULL, amount DOUBLE NOT NULL, PRIMARY KEY (uuid, category))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS sellworth_items (uuid VARCHAR(36) NOT NULL, item_key VARCHAR(128) NOT NULL, count DOUBLE NOT NULL, revenue DOUBLE NOT NULL, PRIMARY KEY (uuid, item_key))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS sellworth_toggleworth_disabled (uuid VARCHAR(36) PRIMARY KEY)");
            }
        }
    }

    @Override
    public void loadAll(
            Map<UUID, Double> totalSold,
            Map<UUID, Map<String, Double>> soldByCategory,
            Map<UUID, Map<String, Sell.Stats>> itemHistory,
            Set<UUID> toggleWorthDisabled
    ) throws Exception {
        totalSold.clear();
        soldByCategory.clear();
        itemHistory.clear();
        toggleWorthDisabled.clear();

        try (Connection con = openConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT uuid, total_sold FROM sellworth_players")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = safeUuid(rs.getString(1));
                        if (uuid != null) {
                            totalSold.put(uuid, rs.getDouble(2));
                        }
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT uuid, category, amount FROM sellworth_categories")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = safeUuid(rs.getString(1));
                        if (uuid == null) {
                            continue;
                        }
                        String category = rs.getString(2);
                        double amount = rs.getDouble(3);

                        soldByCategory.computeIfAbsent(uuid, k -> new HashMap<>()).put(category, amount);
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT uuid, item_key, count, revenue FROM sellworth_items")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = safeUuid(rs.getString(1));
                        if (uuid == null) {
                            continue;
                        }
                        String itemKey = rs.getString(2);
                        double count = rs.getDouble(3);
                        double revenue = rs.getDouble(4);

                        itemHistory.computeIfAbsent(uuid, k -> new HashMap<>()).put(itemKey, new Sell.Stats(count, revenue));
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT uuid FROM sellworth_toggleworth_disabled")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = safeUuid(rs.getString(1));
                        if (uuid != null) {
                            toggleWorthDisabled.add(uuid);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void saveAll(
            Map<UUID, Double> totalSold,
            Map<UUID, Map<String, Double>> soldByCategory,
            Map<UUID, Map<String, Sell.Stats>> itemHistory,
            Set<UUID> toggleWorthDisabled
    ) throws Exception {
        Set<UUID> allUuids = new HashSet<>();
        allUuids.addAll(totalSold.keySet());
        allUuids.addAll(soldByCategory.keySet());
        allUuids.addAll(itemHistory.keySet());

        try (Connection con = openConnection()) {
            boolean oldAuto = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                try (PreparedStatement delPlayer = con.prepareStatement("DELETE FROM sellworth_players WHERE uuid = ?");
                     PreparedStatement insPlayer = con.prepareStatement("INSERT INTO sellworth_players (uuid, total_sold) VALUES (?, ?)");
                     PreparedStatement delCats = con.prepareStatement("DELETE FROM sellworth_categories WHERE uuid = ?");
                     PreparedStatement insCat = con.prepareStatement("INSERT INTO sellworth_categories (uuid, category, amount) VALUES (?, ?, ?)");
                     PreparedStatement delItems = con.prepareStatement("DELETE FROM sellworth_items WHERE uuid = ?");
                     PreparedStatement insItem = con.prepareStatement("INSERT INTO sellworth_items (uuid, item_key, count, revenue) VALUES (?, ?, ?, ?)");
                     PreparedStatement clearToggle = con.prepareStatement("DELETE FROM sellworth_toggleworth_disabled");
                     PreparedStatement insToggle = con.prepareStatement("INSERT INTO sellworth_toggleworth_disabled (uuid) VALUES (?)")
                ) {
                    for (UUID uuid : allUuids) {
                        String u = uuid.toString();

                        delPlayer.setString(1, u);
                        delPlayer.executeUpdate();

                        insPlayer.setString(1, u);
                        insPlayer.setDouble(2, totalSold.getOrDefault(uuid, 0.0D));
                        insPlayer.executeUpdate();

                        delCats.setString(1, u);
                        delCats.executeUpdate();
                        Map<String, Double> cats = soldByCategory.get(uuid);
                        if (cats != null) {
                            for (Map.Entry<String, Double> e : cats.entrySet()) {
                                insCat.setString(1, u);
                                insCat.setString(2, e.getKey());
                                insCat.setDouble(3, e.getValue());
                                insCat.executeUpdate();
                            }
                        }

                        delItems.setString(1, u);
                        delItems.executeUpdate();
                        Map<String, Sell.Stats> items = itemHistory.get(uuid);
                        if (items != null) {
                            for (Map.Entry<String, Sell.Stats> e : items.entrySet()) {
                                Sell.Stats st = e.getValue();
                                insItem.setString(1, u);
                                insItem.setString(2, e.getKey());
                                insItem.setDouble(3, st.count);
                                insItem.setDouble(4, st.revenue);
                                insItem.executeUpdate();
                            }
                        }
                    }

                    clearToggle.executeUpdate();
                    for (UUID uuid : toggleWorthDisabled) {
                        insToggle.setString(1, uuid.toString());
                        insToggle.executeUpdate();
                    }
                }

                con.commit();
            } catch (SQLException e) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                }
                throw e;
            } finally {
                try {
                    con.setAutoCommit(oldAuto);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public void saveToggleWorth(Set<UUID> toggleWorthDisabled) throws Exception {
        try (Connection con = openConnection()) {
            boolean oldAuto = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                try (PreparedStatement clear = con.prepareStatement("DELETE FROM sellworth_toggleworth_disabled");
                     PreparedStatement ins = con.prepareStatement("INSERT INTO sellworth_toggleworth_disabled (uuid) VALUES (?)")
                ) {
                    clear.executeUpdate();
                    for (UUID uuid : toggleWorthDisabled) {
                        ins.setString(1, uuid.toString());
                        ins.executeUpdate();
                    }
                }
                con.commit();
            } catch (SQLException e) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                }
                throw e;
            } finally {
                try {
                    con.setAutoCommit(oldAuto);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public void resetPlayer(UUID uuid) throws Exception {
        String u = uuid.toString();

        try (Connection con = openConnection()) {
            boolean oldAuto = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                try (PreparedStatement delPlayer = con.prepareStatement("DELETE FROM sellworth_players WHERE uuid = ?");
                     PreparedStatement delCats = con.prepareStatement("DELETE FROM sellworth_categories WHERE uuid = ?");
                     PreparedStatement delItems = con.prepareStatement("DELETE FROM sellworth_items WHERE uuid = ?");
                     PreparedStatement delToggle = con.prepareStatement("DELETE FROM sellworth_toggleworth_disabled WHERE uuid = ?")
                ) {
                    delPlayer.setString(1, u);
                    delPlayer.executeUpdate();

                    delCats.setString(1, u);
                    delCats.executeUpdate();

                    delItems.setString(1, u);
                    delItems.executeUpdate();

                    delToggle.setString(1, u);
                    delToggle.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                }
                throw e;
            } finally {
                try {
                    con.setAutoCommit(oldAuto);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public void close() {
    }

    private Connection openConnection() throws SQLException {
        if (username != null) {
            return DriverManager.getConnection(jdbcUrl, username, password == null ? "" : password);
        }
        return DriverManager.getConnection(jdbcUrl);
    }

    private UUID safeUuid(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid UUID in SQL storage: " + raw);
            return null;
        }
    }
}
