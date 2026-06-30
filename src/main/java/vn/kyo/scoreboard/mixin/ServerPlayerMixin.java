package vn.kyo.scoreboard.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vn.kyo.scoreboard.KyoScoreboard;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

  @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
  private void kyoModifyTabListName(CallbackInfoReturnable<Component> cir) {
    ServerPlayer player = (ServerPlayer) (Object) this;

    // Lấy thông số Ping và số mạng hạ gục từ người chơi thật
    int ping = player.connection.latency();
    int kills = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));

    // Định dạng màu sắc Ping bằng chuẩn ChatFormatting của Vanilla
    ChatFormatting pingColor = ping < 50 ? ChatFormatting.GREEN : (ping < 150 ? ChatFormatting.YELLOW : ChatFormatting.RED);
  // FIX: Thay thế ChatFormatting.RED cố định bằng hàm quét mốc Rank động từ KyoScoreboard
    ChatFormatting killsColor = KyoScoreboard.getKillsChatFormatting(kills);

    // Nối chỉ số vào sau tên của người chơi thật (giúp bảo toàn Skin/Avatar đầu người 100%)
    Component customName = Component.literal(player.getScoreboardName())
        .append(Component.literal("    Kills: ").withStyle(ChatFormatting.GRAY))
        .append(Component.literal(String.valueOf(kills)).withStyle(killsColor))
        .append(Component.literal("   |   ").withStyle(ChatFormatting.DARK_GRAY))
        .append(Component.literal(ping + "ms").withStyle(pingColor));

    // Ép hệ thống sử dụng tên tùy chỉnh này trên Tab List
    cir.setReturnValue(customName);
  }
}