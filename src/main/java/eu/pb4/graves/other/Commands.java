package eu.pb4.graves.other;


import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.graves.GenericModInfo;
import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.ui.GraveListGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(
                    literal("graves")
                            .requires(Permissions.require("universal_graves.list", true))
                            .executes((ctx) -> Commands.list(ctx, false))
                            .then(literal("modify")
                                    .requires(Permissions.require("universal_graves.modify", 3))
                                    .executes((ctx) -> Commands.list(ctx, true))
                            )

                            .then(literal("player")
                                    .requires(Permissions.require("universal_graves.list_others", 3))
                                    .then(argument("player", GameProfileArgumentType.gameProfile())
                                            .executes((ctx) -> Commands.listOthers(ctx, false))
                                            .then(literal("modify")
                                                    .requires(Permissions.require("universal_graves.list_others.modify", 3))
                                                    .executes((ctx) -> Commands.listOthers(ctx, true))
                                            )
                                    ))

                            .then(literal("about").executes(Commands::about))

                            .then(literal("reload")
                                    .requires(Permissions.require("universal_graves.reload", 4))
                                    .executes(Commands::reloadConfig)
                            )
            );
        });
    }

    private static int list(CommandContext<ServerCommandSource> context, boolean canModify) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();

        try {
            new GraveListGui(player, player.getGameProfile(), canModify).open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int listOthers(CommandContext<ServerCommandSource> context, boolean canModify) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        List<GameProfile> profiles = new ArrayList(context.getArgument("player", GameProfileArgumentType.GameProfileArgument.class).getNames(context.getSource()));

        if (profiles.size() == 0) {
            context.getSource().sendFeedback(new LiteralText("This player doesn't exist!"), false);
            return 0;
        } else if (profiles.size() > 1) {
            context.getSource().sendFeedback(new LiteralText("Only one player can be selected!"), false);
            return 0;
        }
        try {
            new GraveListGui(player, profiles.get(0), canModify).open();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        if (ConfigManager.loadConfig()) {
            context.getSource().sendFeedback(new LiteralText("Reloaded config!"), false);
            for (var player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
                GraveNetworking.sendConfig(player.networkHandler);
            }

        } else {
            context.getSource().sendError(new LiteralText("Error accrued while reloading config!").formatted(Formatting.RED));

        }
        return 1;
    }

    private static int about(CommandContext<ServerCommandSource> context) {
        /*context.getSource().sendFeedback(new LiteralText("Universal Graves")
                .formatted(Formatting.AQUA)
                .append(new LiteralText(" - " + GravesMod.VERSION)
                        .formatted(Formatting.WHITE)
                ), false);*/

        for (var text : context.getSource().getEntity() instanceof ServerPlayerEntity ? GenericModInfo.getAboutFull() : GenericModInfo.getAboutConsole()) {
            context.getSource().sendFeedback(text, false);
        }

        return 1;
    }
}
