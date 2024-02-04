package io.xiaoyi311.uvs;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerSpawnInfo;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatSessionUpdatePacket;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.ServerClosedEvent;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Unified Verify Server 主程序
 * @author xiaoyi311
 */
public class UVSMain {
    static String jHost;
    static Integer jPort;
    static String jPass;
    static List<String> tipUsers = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("= Unified Verify Server =");
        System.out.println("Version: 1.0.0");
        System.out.println("Powered By BackroomsMC IT Group");
        System.out.println("> Starting UVS Server...");
        System.out.println();

        if(args.length != 4) {
            System.out.println("Args Not Enough!");
            return;
        }

        int port;
        try{
            port = Integer.parseInt(args[0]);
            jPort = Integer.parseInt(args[3]);
        }catch (NumberFormatException e){
            System.out.println("Port must be a number.");
            return;
        }
        jHost = args[1];
        jPass = args[2];

        SessionService sessionService = new SessionService();

        Server server = new TcpServer("0.0.0.0", port, MinecraftProtocol::new);
        server.setGlobalFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
        server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, true);
        server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, (ServerInfoBuilder) session ->
                new ServerStatusInfo(
                        new VersionInfo(MinecraftCodec.CODEC.getMinecraftVersion(), MinecraftCodec.CODEC.getProtocolVersion()),
                        new PlayerInfo(1, 0, new ArrayList<>()),
                        Component.text("= BackroomsMC UVS 正版验证服务器 ="),
                        null,
                        false
                )
        );

        server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, (ServerLoginHandler) session ->
                session.send(new ClientboundLoginPacket(
                        0,
                        false,
                        new String[]{"minecraft:world"},
                        0,
                        1,
                        1,
                        false,
                        false,
                        false,
                        new PlayerSpawnInfo(
                                "minecraft:overworld",
                                "minecraft:world",
                                100,
                                GameMode.SURVIVAL,
                                GameMode.SURVIVAL,
                                false,
                                false,
                                null,
                                100
                        )
                ))
        );

        server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 100);
        server.addListener(new ServerAdapter() {
            @Override
            public void serverClosed(ServerClosedEvent event) {
                System.out.println("UVS 服务器已关闭，请稍后重进..");
            }

            @Override
            public void sessionAdded(SessionAddedEvent event) {
                event.getSession().addListener(new SessionAdapter() {
                    @Override
                    public void packetReceived(Session session, Packet packet) {
                        if (packet instanceof ServerboundChatSessionUpdatePacket) {
                            GameProfile profile = event.getSession().getFlag(MinecraftConstants.PROFILE_KEY);
                            String uuid = profile.getId().toString();
                            if(tipUsers.contains(uuid)) {
                                tipUsers.remove(uuid);
                                try{
                                    session.disconnect(getOkTip(uuid.replaceAll("-", "")));
                                }catch (Exception e){
                                    e.printStackTrace();
                                    session.disconnect(getErrorTip());
                                }
                            }else{
                                tipUsers.add(uuid);
                                session.disconnect(getFirstTip());
                            }
                        }
                    }
                });
            }
        });

        System.out.println("> UVS Server Started!");
        server.bind();
    }

    private static Component getFirstTip(){
        return Component
                .text("[UVS 系统]\n").style(Style.style(NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(
                        Component.text("请注意，你正在尝试获取 UVS 验证码，此验证码将会起到临时代表你正版身份的作用!\n")
                                .style(Style.style(NamedTextColor.RED, TextDecoration.BOLD))
                ).append(
                        Component.text("请不要给任何人提供此验证码，此验证码有效期为 5min，使用后立即失效\n")
                                .style(Style.style(NamedTextColor.AQUA, TextDecoration.BOLD))
                ).append(
                        Component.text("如果你确定生成验证码，请重新进入本服务器!")
                                .style(Style.style(NamedTextColor.GREEN, TextDecoration.ITALIC))
                ).append(
                        Component.text("\n\n= Powered By BackroomsMC IT Group =")
                                .style(Style.style(NamedTextColor.GRAY, TextDecoration.ITALIC))
                );
    }

    private static Component getOkTip(String uuid){
        int code = (int) Math.round((Math.random() * 9 + 1 ) * 100000);
        try(Jedis jedis = new Jedis(jHost, 6379)){
            if(!Objects.equals(jPass, "none")){
                jedis.auth(jPass);
            }

            if(jedis.exists("authCodeP:" + uuid)){
                return Component
                        .text("[UVS 系统]\n").style(Style.style(NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(
                                Component.text("你已经近期获取过验证码了，请不要重复获取! 等待 5min 后再试!")
                                        .style(Style.style(NamedTextColor.RED, TextDecoration.BOLD))
                        ).append(
                                Component.text("\n\n= Powered By BackroomsMC IT Group =")
                                        .style(Style.style(NamedTextColor.GRAY, TextDecoration.ITALIC))
                        );
            }
            jedis.set("authCodeP:" + uuid, "", SetParams.setParams().ex(5L * 60));
            jedis.set("authCode:" + code, uuid, SetParams.setParams().ex(5L * 60));
        }

        System.out.println("Player " + uuid + " Get The Auth Code " + code);
        return Component
                .text("[UVS 系统]\n").style(Style.style(NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(
                        Component.text("UVS 验证码获取成功!\n")
                                .style(Style.style(NamedTextColor.RED, TextDecoration.BOLD))
                ).append(
                        Component.text("请不要给任何人提供此验证码，此验证码有效期为 5min，使用后立即失效\n")
                                .style(Style.style(NamedTextColor.AQUA, TextDecoration.BOLD))
                ).append(
                        Component.text("验证码: ")
                                .style(Style.style(NamedTextColor.GREEN, TextDecoration.ITALIC))
                ).append(
                        Component.text(code)
                                .style(Style.style(NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                ).append(
                        Component.text("\n\n= Powered By BackroomsMC IT Group =")
                                .style(Style.style(NamedTextColor.GRAY, TextDecoration.ITALIC))
                );
    }

    private static Component getErrorTip(){
        return Component
                .text("[UVS 系统]\n").style(Style.style(NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(
                        Component.text("系统发生未知错误! 请联系技术组!\n")
                                .style(Style.style(NamedTextColor.RED, TextDecoration.BOLD))
                ).append(
                        Component.text("错误 ID: " + System.currentTimeMillis())
                                .style(Style.style(NamedTextColor.AQUA, TextDecoration.BOLD))
                ).append(
                        Component.text("\n\n= Powered By BackroomsMC IT Group =")
                                .style(Style.style(NamedTextColor.GRAY, TextDecoration.ITALIC))
                );
    }
}
