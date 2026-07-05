package vn.kyo.scoreboard;

import eu.pb4.placeholders.api.ServerPlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.TagParser;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.level.ServerPlayer;

public class KyoTabList {

  public static void register() {
    // Cập nhật nửa giây 1 lần (10 Ticks) cho hiệu ứng TabList lấp lánh mượt mà!
    ServerTickEvents.END_SERVER_TICK.register(server -> {
      if (server.getTickCount() % 10 == 0) {

        // 1. Tính toán Khung hình Animation 1 LẦN DUY NHẤT cho toàn bộ Server
        int frameIndex = (server.getTickCount() / 10) % KyoConfig.ANIMATED_TAB_HEADER.size();
        String rawHeader = KyoConfig.ANIMATED_TAB_HEADER.get(frameIndex);
        String rawFooter = KyoConfig.TAB_FOOTER;

        // 2. Gửi gói tin cập nhật xuống cho từng người chơi
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
          updateTabListHeaderFooter(player, rawHeader, rawFooter);
        }
      }
    });
  }

  // Hàm xử lý gói tin, nhận thêm tham số rawHeader và rawFooter
  private static void updateTabListHeaderFooter(ServerPlayer player, String rawHeader, String rawFooter) {
    ServerPlaceholderContext context = ServerPlaceholderContext.of(player);

    // Phân giải thẻ màu và biến động cho Header chuẩn V3
    TextNode headerNode = TagParser.DEFAULT.parseNode(rawHeader);
    TextNode parsedHeaderNode = Placeholders.SERVER_PLACEHOLDER_PARSER.parseNode(headerNode);
    Component headerComp = parsedHeaderNode.toComponent(context.asParserContext());

    // Phân giải thẻ màu và biến động cho Footer chuẩn V3
    TextNode footerNode = TagParser.DEFAULT.parseNode(rawFooter);
    TextNode parsedFooterNode = Placeholders.SERVER_PLACEHOLDER_PARSER.parseNode(footerNode);
    Component footerComp = parsedFooterNode.toComponent(context.asParserContext());

    // Bắn gói tin thay đổi TabList xuống người chơi
    player.connection.send(new ClientboundTabListPacket(headerComp, footerComp));
  }
}