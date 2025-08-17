package com.tonnom.maplink;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MapLinkPlugin extends JavaPlugin {

    private String webUrl;
    private File tokenFile;
    private long tokenExpirySeconds;
    private final Map<String, TokenEntry> tokens = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        webUrl = getConfig().getString("web-url", "http://127.0.0.1:8100/");
        if (!webUrl.endsWith("/")) webUrl += "/";

        String tokenPath = getConfig().getString("web-token-file", "plugins/BlueMap/web/maplink_tokens.json");
        tokenFile = new File(tokenPath);
        tokenExpirySeconds = getConfig().getLong("token-expiry-seconds", 300);

        // Charge fichier tokens existant si présent
        loadTokensFromFile();

        getLogger().info("MapLink (token) activé - web-url=" + webUrl + " token-file=" + tokenFile.getPath());
    }

    @Override
    public void onDisable() {
        saveTokensToFile();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("maplink")) return false;

        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("maplink.reload")) {
                sender.sendMessage("Tu n'as pas la permission de recharger la config.");
                return true;
            }
            reloadConfig();
            webUrl = getConfig().getString("web-url", "http://127.0.0.1:8100/");
            if (!webUrl.endsWith("/")) webUrl += "/";
            tokenFile = new File(getConfig().getString("web-token-file", "plugins/BlueMap/web/maplink_tokens.json"));
            tokenExpirySeconds = getConfig().getLong("token-expiry-seconds", 300);
            loadTokensFromFile();
            sender.sendMessage("MapLink : configuration rechargée. web-url=" + webUrl);
            return true;
        }

        // /maplink token -> génère token
        if (args.length == 1 && args[0].equalsIgnoreCase("token")) {
            String token = generateTokenForPlayer(player.getUniqueId().toString());
            String link = webUrl + "?token=" + token;
            player.sendMessage("§aTon lien BlueMap (token) : §r" + link);
            player.sendMessage("§7(Token valable " + tokenExpirySeconds + " secondes)");
            return true;
        }

        // par défaut : lien avec UUID
        String uuid = player.getUniqueId().toString();
        String link = webUrl + "?player=" + uuid;
        player.sendMessage("§aTon lien BlueMap : §r" + link);
        player.sendMessage("§7(Utilise /maplink token pour obtenir un lien privé temporaire)");
        return true;
    }

    private String generateTokenForPlayer(String uuid) {
        // nettoie tokens expirés
        cleanExpiredTokens();

        String token = UUID.randomUUID().toString().replace("-", "");
        long expiresAt = Instant.now().getEpochSecond() + tokenExpirySeconds;
        tokens.put(token, new TokenEntry(uuid, expiresAt));
        saveTokensToFile();
        return token;
    }

    private void cleanExpiredTokens() {
        long now = Instant.now().getEpochSecond();
        Iterator<Map.Entry<String, TokenEntry>> it = tokens.entrySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Map.Entry<String, TokenEntry> e = it.next();
            if (e.getValue().expiresAt < now) {
                it.remove();
                changed = true;
            }
        }
        if (changed) saveTokensToFile();
    }

    private synchronized void saveTokensToFile() {
        try {
            // s'assure que le dossier existe
            File parent = tokenFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, TokenEntry> e : tokens.entrySet()) {
                if (!first) sb.append(",\n");
                first = false;
                sb.append("\"" + """).append(e.getKey()).append("\":{");
                sb.append("\"uuid\":\"").append(e.getValue().uuid).append("\", ");
                sb.append("\"expires\":").append(e.getValue().expiresAt);
                sb.append("}");
            }
            sb.append("}\n");

            try (FileWriter fw = new FileWriter(tokenFile, StandardCharsets.UTF_8)) {
                fw.write(sb.toString());
            }
        } catch (IOException ex) {
            getLogger().severe("Impossible d'ecrire le fichier de tokens: " + ex.getMessage());
        }
    }

    private synchronized void loadTokensFromFile() {
        tokens.clear();
        if (!tokenFile.exists()) return;
        try {
            String s = new String(Files.readAllBytes(tokenFile.toPath()), StandardCharsets.UTF_8).trim();
            if (s.length() == 0) return;
            // parsing très simple (attend format généré par saveTokensToFile)
            // on cherche "token":{"uuid":"...","expires":123456789}
            String working = s;
            for (String entry : working.replaceAll("\n"," ").split("},")) {
                int keyStart = entry.indexOf('"');
                if (keyStart == -1) continue;
                int keyEnd = entry.indexOf('"', keyStart+1);
                if (keyEnd == -1) continue;
                String token = entry.substring(keyStart+1, keyEnd);
                int uuidIdx = entry.indexOf("\"uuid\":\"");
                if (uuidIdx == -1) continue;
                int uuidStart = uuidIdx + "\"uuid\":\"".length();
                int uuidEnd = entry.indexOf('"', uuidStart);
                if (uuidEnd == -1) continue;
                String uuid = entry.substring(uuidStart, uuidEnd);
                int expIdx = entry.indexOf("\"expires\":");
                if (expIdx == -1) continue;
                int expStart = expIdx + "\"expires\":".length();
                int expEnd = expStart;
                while (expEnd < entry.length() && Character.isDigit(entry.charAt(expEnd))) expEnd++;
                long expires = Long.parseLong(entry.substring(expStart, expEnd));
                tokens.put(token, new TokenEntry(uuid, expires));
            }
        } catch (IOException ex) {
            getLogger().warning("Impossible de lire tokens: " + ex.getMessage());
        }
    }

    static class TokenEntry {
        String uuid;
        long expiresAt;

        TokenEntry(String uuid, long expiresAt) {
            this.uuid = uuid;
            this.expiresAt = expiresAt;
        }
    }
}
