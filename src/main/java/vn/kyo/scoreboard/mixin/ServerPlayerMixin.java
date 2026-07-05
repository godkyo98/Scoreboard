package vn.kyo.scoreboard.mixin;

import eu.pb4.placeholders.api.ServerPlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vn.kyo.scoreboard.KyoConfig;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

  @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
  private void kyoModifyTabListName(CallbackInfoReturnable<Component> cir) {
    ServerPlayer player = (ServerPlayer) (Object) this;
    ServerPlaceholderContext context = ServerPlaceholderContext.of(player);

    // Lấy toàn bộ cấu trúc hiển thị từ file Config
    String rawFormat = KyoConfig.TAB_PLAYER_FORMAT;

    // Quét trực tiếp Mã Màu và Biến Danh Hiệu
    TextNode colorNode = TagParser.DEFAULT.parseNode(rawFormat);
    TextNode finalNode = Placeholders.SERVER_PLACEHOLDER_PARSER.parseNode(colorNode);
    Component finalTabLine = finalNode.toComponent(context.asParserContext());

    cir.setReturnValue(finalTabLine);
  }
}