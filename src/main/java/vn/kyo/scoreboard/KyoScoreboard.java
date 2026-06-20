package vn.kyo.scoreboard;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.numbers.BlankFormat;
// 2 IMPORT QUAN TRỌNG NHẤT CỦA BẢN 26.2:
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.FontDescription;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class KyoScoreboard implements ModInitializer {

    private static final ScoreHolder PLAYER_HOLDER = createFakeHolder("tea_player");
    private static final ScoreHolder KILLS_HOLDER = createFakeHolder("tea_kills");
    private static final ScoreHolder DEATH_HOLDER = createFakeHolder("tea_deaths");
    private static final ScoreHolder PLAYTIME_HOLDER = createFakeHolder("tea_playtime");
    private static final ScoreHolder PING_HOLDER = createFakeHolder("tea_ping");

    private static final ScoreHolder SPACER_1 = createFakeHolder("tea_s1");
    private static final ScoreHolder SPACER_2 = createFakeHolder("tea_s2");

    private int tickTimer = 0;

    @Override
    public void onInitialize() {
        // [POLYMER] Đóng gói thư mục assets thành Resource Pack tự động
        PolymerResourcePackUtils.addModAssets("kyoscoreboard");
        PolymerResourcePackUtils.markAsRequired();

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        System.out.println("[KyoScoreboard] Mod (V8.5 - 26.2 Mojang Mappings) da khoi tao!");
    }

    private void onServerTick(MinecraftServer server) {
        if (++tickTimer % 20 != 0) return;
        Scoreboard scoreboard = server.getScoreboard();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayerScoreboard(player, scoreboard);
        }
    }

    private void updatePlayerScoreboard(ServerPlayer player, Scoreboard scoreboard) {
        String objectiveName = "tea_scoreboard";
        Objective objective = scoreboard.getObjective(objectiveName);

        if (objective == null) {
            try {
                objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY, hex(" ☕ TEA SERVER ", 0xFFB700).copy().withStyle(style -> style.withBold(true)), ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
                scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
            } catch (Exception e) {
                return;
            }
        }

        if (objective == null) return;

        // Khai báo 5 icon từ gói tài nguyên của bạn
        Component iconPing = Component.literal("\uE001").withStyle(ChatFormatting.WHITE);
        Component iconTime = Component.literal("\uE002").withStyle(ChatFormatting.WHITE);
        Component iconKill = Component.literal("\uE003").withStyle(ChatFormatting.WHITE);
        Component iconDeath = Component.literal("\uE004").withStyle(ChatFormatting.WHITE);
        Component iconTea = Component.literal("\uE005").withStyle(ChatFormatting.WHITE);

        // ĐỒNG BỘ TIÊU ĐỀ: Đẩy thẳng Icon Tea lên Tiêu đề gốc của bảng - MÀU CAM
        objective.setDisplayName(Component.literal("").append(iconTea).append(hex(" TEA SERVER", 0xFFA500)) // 0xFFA500: Màu cam chuẩn
        );

        // --- BẮT ĐẦU SET CÁC DÒNG NỘI DUNG ---

        // Dòng đệm trên cùng (Điểm 6)
        setLine(scoreboard, objective, SPACER_1, 6, Component.literal(" "));

        // Dòng 1: Tên Người Chơi - Điểm 5 - MÀU VÀNG
        setLine(scoreboard, objective, PLAYER_HOLDER, 5, Component.literal("  ").append(hex("Player: ", 0xAAAAAA)) // Chữ lề màu xám
                .append(hex(player.getName().getString(), 0xFFFF55)) // Tên màu Vàng
        );

        // Dòng 2: Chỉ số Kills - Điểm 4 - MÀU ĐỎ
        int kills = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));
        setLine(scoreboard, objective, KILLS_HOLDER, 4, Component.literal(" ").append(iconKill).append(hex(" Kills: ", 0xAAAAAA)).append(hex(String.valueOf(kills), 0xFF5555)) // 0xFF5555: Màu đỏ rực
        );

        // Dòng 3: Chỉ số Deaths - Điểm 3 - MÀU TRẮNG
        int deaths = player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
        setLine(scoreboard, objective, DEATH_HOLDER, 3, Component.literal(" ").append(iconDeath).append(hex(" Deaths: ", 0xAAAAAA)).append(hex(String.valueOf(deaths), 0xFFFFFF)) // 0xFFFFFF: Màu trắng tinh
        );

        // Dòng 4: Thời gian chơi - Điểm 2 - MÀU VÀNG NHẠT
        int playTicks = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        int playMins = playTicks / 1200;
        String timeStr = (playMins / 60 > 0 ? (playMins / 60) + "h " : "") + (playMins % 60) + "m";
        setLine(scoreboard, objective, PLAYTIME_HOLDER, 2, Component.literal(" ").append(iconTime).append(hex(" Played: ", 0xAAAAAA)).append(hex(timeStr, 0xFEFE33)) // Màu vàng nhạt sang trọng
        );

        // Dòng 5: Chỉ số Ping - Điểm 1 (LUÔN CỐ ĐỊNH Ở DƯỚI CÙNG) - MÀU XANH LÁ
        int ping = player.connection.latency();
        setLine(scoreboard, objective, PING_HOLDER, 1, Component.literal(" ").append(iconPing).append(hex(" Ping: ", 0xAAAAAA)).append(hex(ping + "ms", 0x55FF55)) // 0x55FF55: Màu xanh lá sáng cực mượt
        );

        // Dòng đệm dưới đáy bảng (Điểm 0)
        setLine(scoreboard, objective, SPACER_2, 0, Component.literal("  "));
    }

    private void setLine(Scoreboard scoreboard, Objective objective, ScoreHolder holder, int score, Component text) {
        scoreboard.getOrCreatePlayerScore(holder, objective).set(score);
        scoreboard.getOrCreatePlayerScore(holder, objective).display(text);
    }

    private static ScoreHolder createFakeHolder(String name) {
        return new ScoreHolder() {
            @Override
            public String getScoreboardName() {
                return name;
            }
        };
    }

    private static Component hex(String text, int hexColor) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(hexColor)));
    }
}