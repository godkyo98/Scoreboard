package vn.kyo.scoreboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KyoConfig {

  private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
  private static final Path CONFIG_FILE = CONFIG_DIR.resolve("kyo_scoreboard.json");
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  // Danh sách Khung hình (Frames) tạo hiệu ứng ánh sáng quét (RGB / Shine)
  public static List<String> ANIMATED_TITLE = List.of(
      "<bold><gold>TEA SERVER</gold></bold>",
      "<bold><yellow>T</yellow><gold>EA SERVER</gold></bold>",
      "<bold><gold>T</gold><yellow>E</yellow><gold>A SERVER</gold></bold>",
      "<bold><gold>TE</gold><yellow>A</yellow><gold> SERVER</gold></bold>",
      "<bold><gold>TEA </gold><yellow>S</yellow><gold>ERVER</gold></bold>",
      "<bold><gold>TEA S</gold><yellow>E</yellow><gold>RVER</gold></bold>",
      "<bold><gold>TEA SE</gold><yellow>R</yellow><gold>VER</gold></bold>",
      "<bold><gold>TEA SER</gold><yellow>V</yellow><gold>ER</gold></bold>",
      "<bold><gold>TEA SERV</gold><yellow>E</yellow><gold>R</gold></bold>",
      "<bold><gold>TEA SERVE</gold><yellow>R</yellow></bold>",
      "<bold><gold>TEA SERVER</gold></bold>",
      "<bold><gold>TEA SERVER</gold></bold>" // Dừng lại 1 nhịp ở cuối để người chơi dễ đọc
  );

  // Khung hình Animation cho Header của TabList
  public static List<String> ANIMATED_TAB_HEADER = List.of(
      "<bold><#FF3300>❖ <#FF4D00>T<#FF6600>E<#FF8000>A <#FF9900>S<#FFB300>E<#FFCC00>R<#FFE600>V<#FFFF00>E<#FFFF00>R <#FFFF00>❖</bold>"
  );

  // Footer tĩnh (Hoặc sếp có thể biến nó thành List để làm animation luôn cũng được)
  public static String TAB_FOOTER = "<gray>Nguoi choi: <green>%server:online%</green></gray>\n<yellow>Tham gia Discord: discord.gg/thanh-long</yellow>";

  // TÍNH NĂNG MỚI: Tự do định dạng dòng hiển thị của từng member trong TabList
  // Rút gọn: Chỉ lo định dạng khoảng cách và thông số phía sau tên người chơi
  // Gắn Danh hiệu PvP cực ngầu ngay trước Tên người chơi!
  public static String TAB_PLAYER_FORMAT = "%kyoscoreboard:kill_title% <white>%player:name%</white>  <gray>Kills:</gray> %kyoscoreboard:kills% <dark_gray>|</dark_gray> <gray>Ping:</gray> %kyoscoreboard:ping%";

  public static List<String> BOARD_LINES = Arrays.asList(
      " ",
      "  <aqua>%kyo:lang kyoscoreboard.player%:</aqua> <white>%player:name%</white>",
      "  \uE001 <green>%kyo:lang kyoscoreboard.ping%:</green> %kyoscoreboard:ping%",
      "  \uE006 <yellow>%kyo:lang kyoscoreboard.balance%:</yellow> <green>%kyoeconomy:balance%</green>",
      "  \uE003 <red>%kyo:lang kyoscoreboard.kills%:</red> %kyoscoreboard:kills%",
      "  \uE004 <gray>%kyo:lang kyoscoreboard.deaths%:</gray> %kyoscoreboard:deaths%",
      "  \uE002 <aqua>%kyo:lang kyoscoreboard.played%:</aqua> %kyoscoreboard:playtime%",
      "  <gold>%kyo:lang kyoscoreboard.fishing_level%:</gold> %kyofishing:level%",
      "  <gold>%kyo:lang kyoscoreboard.fishing_caught%:</gold> %kyofishing:caught%",
      " "
  );

  public static void loadConfig() {
    try {
      if (Files.exists(CONFIG_FILE)) {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
          ConfigData data = GSON.fromJson(reader, ConfigData.class);
          if (data != null) {
            // 2. GÁN LẠI DỮ LIỆU TỪ JSON
            if (data.animatedTitle != null && !data.animatedTitle.isEmpty()) {
              ANIMATED_TITLE = data.animatedTitle;
            }
            // Nạp dữ liệu TabList Header (Đã sửa lỗi gán nhầm)
            if (data.animatedTabHeader != null && !data.animatedTabHeader.isEmpty()) {
              ANIMATED_TAB_HEADER = data.animatedTabHeader;
            }
            if (data.tabFooter != null) TAB_FOOTER = data.tabFooter;
            if (data.tabPlayerFormat != null) TAB_PLAYER_FORMAT = data.tabPlayerFormat; // Đọc dữ liệu format
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
      data.animatedTitle = ANIMATED_TITLE;
      data.animatedTabHeader = ANIMATED_TAB_HEADER;
      data.tabFooter = TAB_FOOTER;
      data.tabPlayerFormat = TAB_PLAYER_FORMAT; // Ghi dữ liệu format
      data.boardLines = BOARD_LINES;
      try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
        GSON.toJson(data, writer);
      }
    } catch (Exception e) {}
  }

  private static class ConfigData {
    public List<String> animatedTitle = ANIMATED_TITLE; // <--- CHÚ Ý: Đổi từ String sang List<String>
    public List<String> animatedTabHeader = ANIMATED_TAB_HEADER;
    public String tabFooter;
    public String tabPlayerFormat;
    public List<String> boardLines;
  }
}