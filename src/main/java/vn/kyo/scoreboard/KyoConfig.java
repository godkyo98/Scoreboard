package vn.kyo.scoreboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class KyoConfig {

  private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
  private static final Path CONFIG_FILE = CONFIG_DIR.resolve("kyo_scoreboard.json");
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  public static String SERVER_NAME = "<gold>\uE005 TEA SERVER \uE005</gold>";
  // THÊM MỚI: Cho phép tùy chỉnh tự do Tiêu đề và Chân trang của TabList
  public static String TAB_HEADER = "<gold>⚡ TEA SERVER ⚡</gold>\n<gray>Online:</gray> <green>%server:online%</green>/<green>%server:max_players%</green>";
  public static String TAB_FOOTER = "\n<aqua>%kyo:lang kyoscoreboard.ping%:</aqua> %kyoscoreboard:ping%\n<yellow>Tham gia Discord: discord.gg/thanh-long</yellow>\n<yellow>Chúc bạn chơi game vui vẻ!</yellow>";

  // FIX: Gỡ bỏ thẻ <white> ở Level và Caught để nhường quyền quyết định màu cho mod KyoFishing
  public static List<String> BOARD_LINES = Arrays.asList(
      " ",
      "  <aqua>%kyo:lang kyoscoreboard.player%:</aqua> <white>%player:name%</white>",
      "  \uE006 <yellow>%kyo:lang kyoscoreboard.balance%:</yellow> %kyoeconomy:balance%",
      "  \uE003 <red>%kyo:lang kyoscoreboard.kills%:</red> %kyoscoreboard:kills%",
      "  \uE004 <gray>%kyo:lang kyoscoreboard.deaths%:</gray> %kyoscoreboard:deaths%",
      "  \uE002 <aqua>%kyo:lang kyoscoreboard.played%:</aqua> %kyoscoreboard:playtime%",
      "  <gold>%kyo:lang kyoscoreboard.fishing_level%:</gold> %kyofishing:level%",
      "  <gold>%kyo:lang kyoscoreboard.fishing_caught%:</gold> %kyofishing:caught%",
      "  \uE001 <blue>%kyo:lang kyoscoreboard.ping%:</blue> %kyoscoreboard:ping%",
      " "
  );

  public static void loadConfig() {
    try {
      if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
      if (Files.exists(CONFIG_FILE)) {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
          ConfigData data = GSON.fromJson(reader, ConfigData.class);
          if (data != null) {
            if (data.serverName != null) SERVER_NAME = data.serverName;
            // Đọc thêm 2 cấu hình mới
            if (data.tabHeader != null) TAB_HEADER = data.tabHeader;
            if (data.tabFooter != null) TAB_FOOTER = data.tabFooter;
            if (data.boardLines != null) BOARD_LINES = data.boardLines;
          }
        }
      } else {
        saveConfig();
      }
    } catch (Exception e) {
      System.err.println("[KyoScoreboard] Loi doc file config: " + e.getMessage());
    }
  }

  public static void saveConfig() {
    try {
      ConfigData data = new ConfigData();
      data.serverName = SERVER_NAME;
      data.tabHeader = TAB_HEADER;
      data.tabFooter = TAB_FOOTER;
      data.boardLines = BOARD_LINES;
      try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
        GSON.toJson(data, writer);
      }
    } catch (Exception e) {}
  }

  private static class ConfigData {
    String serverName;
    String tabHeader;
    String tabFooter;
    List<String> boardLines;
  }
}