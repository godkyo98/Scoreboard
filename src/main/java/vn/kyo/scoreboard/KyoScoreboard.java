package vn.kyo.scoreboard;

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

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      dispatcher.register(Commands.literal("kyoscoreboard")
          .requires(source -> source.getPlayer() == null || source.getServer().getPlayerList().isOp(source.getPlayer().nameAndId()))
          .then(Commands.literal("reload").executes(context -> {
            KyoConfig.loadConfig();
            context.getSource().sendSystemMessage(Component.literal("§a[KyoScoreboard] Đã tải lại Config thành công!"));
            return 1;
          })));
    });

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      ServerPlayer player = handler.player;
      initSidebar(player);
    });

    ServerTickEvents.END_SERVER_TICK.register(server -> {
      if (server.getTickCount() % 20 == 0) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
          updateSidebar(player);
        }
      }
    });
  }

  // Bộ lọc mốc màu PvP thô cho Scoreboard (Dạng chuỗi mã màu Legacy)
  public static String getKillsColorCode(int kills) {
    if (kills >= 10000) return "§4§l"; // Đỏ sậm + In đậm (Quỷ vương PvP)
    if (kills >= 5000)  return "§c";   // Đỏ sáng (Huyền thoại)
    if (kills >= 1000)  return "§6";   // Cam (Chiến thần)
    if (kills >= 500)  return "§e";   // Vàng (Sát thủ)
    if (kills >= 150)   return "§b";   // Xanh lơ (Thợ săn)
    if (kills >= 50)   return "§a";   // Xanh lá (Tập sự)
    return "§f";                      // Trắng (Tân binh)
  }

  // Bộ lọc mốc màu cho mã nguồn Mixin TabList (Dạng ChatFormatting)
  public static ChatFormatting getKillsChatFormatting(int kills) {
    if (kills >= 10000) return ChatFormatting.DARK_RED;
    if (kills >= 5000)  return ChatFormatting.RED;
    if (kills >= 1000)  return ChatFormatting.GOLD;
    if (kills >= 500)  return ChatFormatting.YELLOW;
    if (kills >= 150)   return ChatFormatting.AQUA;
    if (kills >= 50)   return ChatFormatting.GREEN;
    return ChatFormatting.WHITE;
  }

  // Bộ lọc mốc màu cho Thời gian chơi (Tính theo Tổng NGÀY)
  public static String getPlaytimeColorCode(int totalDays) {
    if (totalDays >= 365) return "§d§l"; // Tím hồng in đậm (Huyền thoại 1 Năm)
    if (totalDays >= 100) return "§4§l"; // Đỏ sậm in đậm (Bậc thầy)
    if (totalDays >= 50)  return "§c";   // Đỏ (Chuyên gia)
    if (totalDays >= 30)  return "§6";   // Cam (Gắn bó 1 tháng)
    if (totalDays >= 10)  return "§e";   // Vàng (Thân thiết)
    if (totalDays >= 5)   return "§b";   // Xanh lơ (Quen thuộc)
    if (totalDays >= 1)   return "§a";   // Xanh lá (Công dân chính thức)
    return "§f";                         // Trắng (Tân binh chưa đủ 1 ngày)
  }

  // Bộ lọc mốc màu cho Ping (Độ trễ mạng)
  public static String getPingColorCode(int ping) {
    if (ping < 0) return "§8";      // Xám tối (Đang tải/Lỗi)
    if (ping <= 50) return "§a";    // Xanh lá (Kết nối tuyệt vời)
    if (ping <= 100) return "§e";   // Vàng (Kết nối ổn định)
    if (ping <= 200) return "§6";   // Cam (Kết nối trung bình, hơi giật)
    return "§c";                    // Đỏ (Đường truyền kém, rất lag)
  }

  public static String getDeathsColorCode(int deaths) {
    if (deaths >= 300) return "§7";   // >100 mạng: Xám xịt (Chán chả buồn nói)
    if (deaths >= 150)  return "§c";   // >150 mạng: Đỏ (Chê mạnh / Quá tạ)
    if (deaths >= 50)  return "§6";   // >50 mạng: Cam (Chê nhẹ)
    if (deaths >= 25)   return "§e";   // >25 mạng: Vàng (Bình thường)
    if (deaths >= 10)   return "§a";   // 10 mạng: Xanh lá (Sống dai)
    return "§b§l";                    // 0 mạng: Aqua Đậm (Bất tử - Vinh danh tối cao)
  }

  // Nơi khai sinh ra các biến Placeholder Custom của bạn
  private void registerCustomPlaceholders() {
    // 1. Đăng ký Playtime (Ngày:Giờ:Phút) với Màu Động & Thành Tựu
    Placeholders.registerServer(Identifier.tryParse("kyoscoreboard:playtime"), (context, arg) -> {
      ServerPlayer player = (ServerPlayer) context.player();
      if (player == null) return PlaceholderResult.value("§f0h 0m");

      // Lấy thời gian từ thống kê của game
      int ticks = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
      int totalMinutes = (ticks / 20) / 60;
      int totalHours = totalMinutes / 60;

      // Bóc tách số Ngày, Giờ lẻ, Phút lẻ
      int totalDays = totalHours / 24;
      int hours = totalHours % 24;
      int minutes = totalMinutes % 60;

      // Lấy màu tự động dựa trên tổng số NGÀY
      String color = getPlaytimeColorCode(totalDays);

      // THÀNH TỰU 1 NĂM: Thêm ngôi sao vàng cực ngầu nếu chơi đủ 365 ngày
      String badge = (totalDays >= 365) ? "§e★ " : "";

      // Định dạng chuỗi hiển thị cực kỳ thông minh
      String timeString;
      if (totalDays > 0) {
        // Nếu đã chơi qua 1 ngày: Hiển thị (Ngày d - Giờ h - Phút m)
        timeString = totalDays + "d " + hours + "h " + minutes + "m";
      } else {
        // Nếu chưa đủ 1 ngày: Chỉ hiện (Giờ h - Phút m) cho gọn gàng
        timeString = hours + "h " + minutes + "m";
      }

      // Nối tất cả lại: [Màu sắc] + [Huy Hiệu] + [Thời gian]
      return PlaceholderResult.value(color + badge + timeString);
    });

    // FIX: Tự động bơm mã màu động của Rank PvP vào trước con số
    Placeholders.registerServer(Identifier.tryParse("kyoscoreboard:kills"), (context, arg) -> {
      ServerPlayer player = (ServerPlayer) context.player();
      if (player == null) return PlaceholderResult.value("§f0");
      int kills = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));
      return PlaceholderResult.value(getKillsColorCode(kills) + kills);
    });

    Placeholders.registerServer(Identifier.tryParse("kyoscoreboard:deaths"), (context, arg) -> {
      ServerPlayer player = (ServerPlayer) context.player();
      if (player == null) return PlaceholderResult.value("§b§l0");
      int deaths = player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
      return PlaceholderResult.value(getDeathsColorCode(deaths) + deaths);
    });

    // 4. Hồi sinh hệ thống Đa ngôn ngữ (kyo:lang)
    Placeholders.registerServer(Identifier.tryParse("kyo:lang"), (context, arg) -> {
      if (arg == null || arg.isEmpty()) return PlaceholderResult.value("");
      // Component.translatable sẽ lệnh cho Client tự động đọc file json (en_us.json) của nó để dịch
      return PlaceholderResult.value(Component.translatable(arg));
    });

    // Đăng ký Ping với Màu Động tự động
    Placeholders.registerServer(Identifier.tryParse("kyoscoreboard:ping"), (context, arg) -> {
      ServerPlayer player = (ServerPlayer) context.player();
      if (player == null) return PlaceholderResult.value("§80ms");

      // Lấy chỉ số Ping thực tế từ kết nối của người chơi
      int ping = player.connection.latency();

      // Lấy màu tự động dựa trên số Ping
      String color = getPingColorCode(ping);

      return PlaceholderResult.value(color + ping + " ms");
    });
  }

  private void initSidebar(ServerPlayer player) {
    player.connection.send(new ClientboundSetObjectivePacket(getSidebarObjective(player), 0));
    player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, getSidebarObjective(player)));
  }

  private Objective getSidebarObjective(ServerPlayer player) {
    ServerPlaceholderContext context = ServerPlaceholderContext.of(player);
    TextNode colorNode = TagParser.DEFAULT.parseNode(KyoConfig.SERVER_NAME);
    TextNode finalNode = Placeholders.SERVER_PLACEHOLDER_PARSER.parseNode(colorNode);
    Component title = finalNode.toComponent(context.asParserContext());
    return new Objective(DUMMY_SCOREBOARD, SIDEBAR_OBJECTIVE_NAME, ObjectiveCriteria.DUMMY, title, ObjectiveCriteria.RenderType.INTEGER, false, null);
  }

  private void updateSidebar(ServerPlayer player) {
    player.connection.send(new ClientboundSetObjectivePacket(getSidebarObjective(player), 2));
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

  private void sendLine(ServerPlayer player, String holderName, int score, Component text) {
    ClientboundSetScorePacket packet = new ClientboundSetScorePacket(holderName, SIDEBAR_OBJECTIVE_NAME, score, Optional.of(text), Optional.of(BlankFormat.INSTANCE));
    player.connection.send(packet);
  }
}