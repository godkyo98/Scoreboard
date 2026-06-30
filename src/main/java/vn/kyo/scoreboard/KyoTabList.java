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
    // Cập nhật nội dung Header và Footer mỗi 1 giây (20 Ticks)
    ServerTickEvents.END_SERVER_TICK.register(server -> {
      if (server.getTickCount() % 20 == 0) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
          updateTabListHeaderFooter(player);
        }
      }
    });
  }

  private static void updateTabListHeaderFooter(ServerPlayer player) {
    ServerPlaceholderContext context = ServerPlaceholderContext.of(player);

    // Kéo trực tiếp dữ liệu từ file JSON, mod giờ đã có thể đem chia sẻ công khai!
    String rawHeader = KyoConfig.TAB_HEADER;
    String rawFooter = KyoConfig.TAB_FOOTER;

    // Phân giải thẻ màu và biến động cho Header
    TextNode headerNode = TagParser.DEFAULT.parseNode(rawHeader);
    TextNode parsedHeaderNode = Placeholders.SERVER_PLACEHOLDER_PARSER.parseNode(headerNode);
    Component headerComp = parsedHeaderNode.toComponent(context.asParserContext());

    // Phân giải thẻ màu và biến động cho Footer
    TextNode footerNode = TagParser.DEFAULT.parseNode(rawFooter);
    TextNode parsedFooterNode = Placeholders.SERVER_PLACEHOLDER_PARSER.parseNode(footerNode);
    Component footerComp = parsedFooterNode.toComponent(context.asParserContext());

    // Gửi gói tin cập nhật chỉ Header và Footer
    player.connection.send(new ClientboundTabListPacket(headerComp, footerComp));
  }
}