package vn.kyo.scoreboard;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.TagParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.List;
import java.util.Optional;

public class KyoScoreboard implements ModInitializer {

  private static final String SIDEBAR_OBJECTIVE_NAME = "tea_sidebar";
  private static final Scoreboard DUMMY_SCOREBOARD = new Scoreboard();

  @Override
  public void onInitialize() {
    KyoConfig.loadConfig();
    KyoTabList.register();
    // Kích hoạt việc đăng ký Placeholder tự tạo!
    registerCustomPlaceholders();

    // ==========================================
    // ĐĂNG KÝ LỆNH /kyoscoreboard reload
    // ==========================================
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      dispatcher.register(Commands.literal("kyoscoreboard")
          // 1. Kiểm tra quyền OP (Áp dụng chuẩn 26.2 của sếp)
          .requires(source -> {
            if (source.getEntity() instanceof ServerPlayer player) {
              return source.getServer().getPlayerList().isOp(player.nameAndId());
            }
            return true; // Cho phép chạy từ Console
          })
          // 2. Nhánh lệnh con "reload"
          .then(Commands.literal("reload")
              .executes(context -> {
                // Tải lại file JSON
                KyoConfig.loadConfig();

                // Lặp qua toàn bộ người chơi đang online để Xóa và Tạo lại bảng điểm
                for (ServerPlayer p : context.getSource().getServer().getPlayerList().getPlayers()) {
                  removeSidebar(p);
                  initSidebar(p);
                }

                // Gửi thông báo thành công (Áp dụng chuẩn 26.2)
                context.getSource().sendSystemMessage(Component.literal("§a[Kyo Scoreboard] Đã tải lại cấu hình thành công!"));
                return 1;
              })
          )
      );
    });

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      ServerPlayer player = handler.player;
      initSidebar(player);
    });

    ServerTickEvents.END_SERVER_TICK.register(server -> {
      if (server.getTickCount() % 20 == 0) { // Cập nhật 1 giây/lần
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
          updateSidebar(player); // Chỉ cần gọi hàm gốc này của sếp là đủ combo Tiêu đề + Dòng dữ liệu
        }
      }
    });

    // 2. Khi người chơi THOÁT server -> Xóa bảng điểm cho sạch bộ nhớ Client
    ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
      ServerPlayer player = handler.player;
      removeSidebar(player);
    });
  }

  // Hàm của sếp được giữ lại để dùng (Bỏ đoạn String cũ đi cũng được)
  public static ChatFormatting getKillsChatFormatting(int kills) {
    if (kills >= 10000) return ChatFormatting.DARK_RED;
    if (kills >= 5000)  return ChatFormatting.RED;
    if (kills >= 1000)  return ChatFormatting.GOLD;
    if (kills >= 500)   return ChatFormatting.YELLOW;
    if (kills >= 150)   return ChatFormatting.AQUA;
    if (kills >= 50)    return ChatFormatting.GREEN;
    return ChatFormatting.WHITE;
  }

  // ==========================================
  // BỘ LỌC MỐC THỜI GIAN CHƠI (HỆ THỐNG TIER RPG) - ĐÃ ĐỒNG BỘ THẺ V3
  // ==========================================
  public static String getPlaytimeColorCode(int totalDays) {
    // --- MỐC THẦN THOẠI ---
    if (totalDays >= 730) return "<bold><#FF3300>"; // 2 Năm+: Màu Đỏ Cam rực lửa
    if (totalDays >= 365) return "<bold><#00FFFF>"; // 1 Năm: Xanh Lục Bảo Diamond chói sáng
    if (totalDays >= 180) return "<bold><#FF55FF>"; // Nửa năm: Tím Neon chói lóa

    // --- MỐC CAO THỦ (Đã chuyển § sang thẻ STF chuẩn) ---
    if (totalDays >= 90)  return "<bold><dark_red>"; // 3 Tháng: Đỏ sậm in đậm
    if (totalDays >= 60)  return "<red>";           // 2 Tháng: Đỏ tươi
    if (totalDays >= 30)  return "<gold>";          // 1 Tháng: Cam Vàng

    // --- MỐC TÂN BINH & GẮN BÓ ---
    if (totalDays >= 14)  return "<dark_purple>";   // 2 Tuần: Tím
    if (totalDays >= 7)   return "<blue>";          // 1 Tuần: Xanh lam đậm
    if (totalDays >= 3)   return "<aqua>";          // 3 Ngày: Xanh lơ
    if (totalDays >= 1)   return "<green>";         // 1 Ngày: Xanh lá

    return "<white>";                               // Mặc định dưới 1 ngày: Trắng
  }

  // Hàm lấy Icon/Huy hiệu tương ứng với số ngày (Đã đồng bộ thẻ màu)
  public static String getPlaytimeBadge(int totalDays) {
    if (totalDays >= 730) return "<gold>👑</gold> ";
    if (totalDays >= 365) return "<yellow>★</yellow> ";
    if (totalDays >= 180) return "<light_purple>✦</light_purple> ";
    if (totalDays >= 90)  return "<dark_red>⚔</dark_red> "; // Chuyển từ §4
    if (totalDays >= 30)  return "<gold>♦</gold> ";         // Chuyển từ §6
    return "";
  }

  // Nơi khai sinh ra các biến Placeholder Custom
  private void registerCustomPlaceholders() {
    Placeholders.registerServer(Identifier.fromNamespaceAndPath("kyoscoreboard", "playtime"), (context, arg) -> {
      if (!context.hasPlayer() || context.player() == null) {
        return PlaceholderResult.value(Component.literal("0h 1m"));
      }

      ServerPlayer player = (ServerPlayer) context.player();
      int playTimeTicks = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));

      int totalSeconds = playTimeTicks / 20;
      int totalDays = totalSeconds / 86400;
      int hours = (totalSeconds % 86400) / 3600;
      int minutes = ((totalSeconds % 86400) % 3600) / 60;

      int years = totalDays / 365;
      int remDays = totalDays % 365;

      int months = remDays / 30;
      remDays = remDays % 30;

      int weeks = remDays / 7;
      int days = remDays % 7;

      // Lấy chuỗi cấu trúc thẻ màu và huy hiệu
      String color = getPlaytimeColorCode(totalDays);
      String badge = getPlaytimeBadge(totalDays);

      // Cấu trúc chuỗi thời gian thông minh của sếp (Giữ nguyên 100%)
      String timeString;
      if (years > 0) {
        timeString = years + "y " + (months > 0 ? months + "mo" : "");
      } else if (months > 0) {
        timeString = months + "mo " + (weeks > 0 ? weeks + "w " : "") + (days > 0 ? days + "d" : "");
      } else if (weeks > 0) {
        timeString = weeks + "w " + (days > 0 ? days + "d " : "") + (hours > 0 ? hours + "h" : "");
      } else if (days > 0) {
        timeString = days + "d " + (hours > 0 ? hours + "h " : "") + (minutes > 0 ? minutes + "m" : "");
      } else if (hours > 0) {
        timeString = hours + "h " + minutes + "m";
      } else {
        timeString = (minutes == 0 ? 1 : minutes) + "m";
      }

      // Gộp tất cả thành 1 chuỗi thô chứa tag hoàn chỉnh: "<bold><#FF3300>👑 1y 2mo"
      String rawCombined = color + badge + timeString.trim();

      // KHẮC PHỤC LỖI CHÍ MẠNG:
      // Dùng TagParser quét chuỗi thô này để dịch toàn bộ thẻ màu và ép thẳng ra Component xịn của bản V3
      Component parsedComponent = TagParser.DEFAULT.parseNode(rawCombined).toComponent(context.asParserContext());

      return PlaceholderResult.value(parsedComponent);
    });

    // 1. KILLS: NHUỘM MÀU ĐỘNG BẰNG CHAT FORMATTING TỪ LÕI
    Placeholders.registerServer(Identifier.tryParse("kyoscoreboard:kills"), (context, arg) -> {
      ServerPlayer player = (ServerPlayer) context.player();
      if (player == null) return PlaceholderResult.value(Component.literal("0").withStyle(ChatFormatting.WHITE));

      int kills = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));

      // Khôi phục quyền lực cho hàm ChatFormatting của sếp!
      ChatFormatting color = getKillsChatFormatting(kills);
      MutableComponent killComp = Component.literal(String.valueOf(kills)).withStyle(color);

      // Nếu là Quỷ Vương (>= 10000 mạng), buff thêm hiệu ứng IN ĐẬM
      if (kills >= 10000) killComp.withStyle(ChatFormatting.BOLD);

      // Trả về Component có màu -> PlaceholderAPI tự động lắp vào mọi chỗ (Tab/Scoreboard)
      return PlaceholderResult.value(killComp);
    });

    // 4. DANH HIỆU PVP (KILL_TITLE) SẾP VỪA DUYỆT BÊN TRÊN
    Placeholders.registerServer(Identifier.tryParse("kyoscoreboard:kill_title"), (context, arg) -> {
      ServerPlayer player = (ServerPlayer) context.player();
      if (player == null) return PlaceholderResult.value(Component.literal("[Tân Binh]").withStyle(ChatFormatting.WHITE));
      int kills = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));

      Component titleComp;
      if (kills >= 10000) titleComp = Component.literal("[Quỷ Vương]").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
      else if (kills >= 5000)  titleComp = Component.literal("[Huyền Thoại]").withStyle(ChatFormatting.RED);
      else if (kills >= 1000)  titleComp = Component.literal("[Chiến Thần]").withStyle(ChatFormatting.GOLD);
      else if (kills >= 500)  titleComp = Component.literal("[Sát Thủ]").withStyle(ChatFormatting.YELLOW);
      else if (kills >= 150)   titleComp = Component.literal("[Thợ Săn]").withStyle(ChatFormatting.AQUA);
      else if (kills >= 50)   titleComp = Component.literal("[Tập Sự]").withStyle(ChatFormatting.GREEN);
      else titleComp = Component.literal("[Tân Binh]").withStyle(ChatFormatting.WHITE);
      return PlaceholderResult.value(titleComp);
    });

    // 2. DEATHS: NHUỘM MÀU TẤU TẠ CŨNG BẰNG COMPONENT
    Placeholders.registerServer(Identifier.tryParse("kyoscoreboard:deaths"), (context, arg) -> {
      ServerPlayer player = (ServerPlayer) context.player();
      if (player == null) return PlaceholderResult.value(Component.literal("0").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

      int deaths = player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));

      // Sếp có thể tự tạo getDeathsChatFormatting tương tự Kills
      ChatFormatting color;
      if (deaths >= 300) color = ChatFormatting.GRAY;
      else if (deaths >= 150) color = ChatFormatting.RED;
      else if (deaths >= 50) color = ChatFormatting.GOLD;
      else if (deaths >= 25) color = ChatFormatting.YELLOW;
      else if (deaths >= 10) color = ChatFormatting.GREEN;
      else color = ChatFormatting.AQUA;

      MutableComponent deathComp = Component.literal(String.valueOf(deaths)).withStyle(color);
      if (deaths == 0) deathComp.withStyle(ChatFormatting.BOLD); // Vinh danh 0 mạng

      return PlaceholderResult.value(deathComp);
    });

    // 4. Hồi sinh hệ thống Đa ngôn ngữ (kyo:lang)
    Placeholders.registerServer(Identifier.tryParse("kyo:lang"), (context, arg) -> {
      if (arg == null || arg.isEmpty()) return PlaceholderResult.value("");
      // Component.translatable sẽ lệnh cho Client tự động đọc file json (en_us.json) của nó để dịch
      return PlaceholderResult.value(Component.translatable(arg));
    });

    // 3. PING: NHUỘM MÀU MƯỢT MÀ
    Placeholders.registerServer(Identifier.tryParse("kyoscoreboard:ping"), (context, arg) -> {
      ServerPlayer player = (ServerPlayer) context.player();
      if (player == null) return PlaceholderResult.value(Component.literal("0ms").withStyle(ChatFormatting.DARK_GRAY));

      int ping = player.connection.latency();
      ChatFormatting color = ping < 50 ? ChatFormatting.GREEN : (ping < 150 ? ChatFormatting.YELLOW : ChatFormatting.RED);

      return PlaceholderResult.value(Component.literal(ping + "ms").withStyle(color));
    });
  }

  // Hàm ép Client tạo bảng điểm lần đầu
  private void initSidebar(ServerPlayer player) {
    Objective objective = getSidebarObjective(player);
    // Action 0: Khởi tạo bảng (Create)
    player.connection.send(new ClientboundSetObjectivePacket(objective, 0));
    // Đặt bảng điểm vào đúng slot góc phải màn hình
    player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));
  }

  private Objective getSidebarObjective(ServerPlayer player) {
    ServerPlaceholderContext context = ServerPlaceholderContext.of(player);

    // 1. Tự động tính toán số khung hình (Frame Index) dựa trên Tick thực tế của Server
    int tickCount = player.level().getServer().getTickCount();
    int frameIndex = (tickCount / 20) % KyoConfig.ANIMATED_TITLE.size();
    String rawTitle = KyoConfig.ANIMATED_TITLE.get(frameIndex);

    // 2. Ép qua bộ lọc màu STF và dịch Placeholder chuẩn API V3 giống hệt cách sếp làm với boardLines
    TextNode colorNode = TagParser.DEFAULT.parseNode(rawTitle);
    TextNode finalNode = Placeholders.SERVER_PLACEHOLDER_PARSER.parseNode(colorNode);
    Component title = finalNode.toComponent(context.asParserContext());

    // 3. Trả về đúng Objective gốc với BlankFormat để giấu số đỏ Vanilla
    return new Objective(DUMMY_SCOREBOARD, SIDEBAR_OBJECTIVE_NAME, ObjectiveCriteria.DUMMY, title, ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
  }

  private void updateSidebar(ServerPlayer player) {
    // Gửi packet cập nhật tiêu đề (Action số 2)
    player.connection.send(new ClientboundSetObjectivePacket(getSidebarObjective(player), 2));

    // Vẽ lại toàn bộ các dòng (Kinh tế, Ping, Kills...) vào đúng Objective gốc
    ServerPlaceholderContext context = ServerPlaceholderContext.of(player);
    List<String> lines = KyoConfig.BOARD_LINES;
    for (int i = 0; i < lines.size(); i++) {
      String rawLine = lines.get(i);
      TextNode colorNode = TagParser.DEFAULT.parseNode(rawLine);
      TextNode finalNode = Placeholders.SERVER_PLACEHOLDER_PARSER.parseNode(colorNode);
      Component finalComponent = finalNode.toComponent(context.asParserContext());
      sendLine(player, "tea_line_" + i, lines.size() - i, finalComponent);
    }
  }

  // Hàm dọn rác Client khi người chơi thoát
  private void removeSidebar(ServerPlayer player) {
    Objective objective = getSidebarObjective(player);
    // Action 1: Xóa bảng (Remove)
    player.connection.send(new ClientboundSetObjectivePacket(objective, 1));
  }

  private void sendLine(ServerPlayer player, String holderName, int score, Component text) {
    ClientboundSetScorePacket packet = new ClientboundSetScorePacket(holderName, SIDEBAR_OBJECTIVE_NAME, score, Optional.of(text), Optional.of(BlankFormat.INSTANCE));
    player.connection.send(packet);
  }
}