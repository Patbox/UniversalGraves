package eu.pb4.graves.other;


import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.graves.GravesMod;
import eu.pb4.graves.config.ConfigManager;
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
                            .executes(Commands::list)

                            .then(literal("player")
                                    .requires(Permissions.require("universal_graves.list_others", 2))
                                    .then(argument("player", GameProfileArgumentType.gameProfile())
                                            .executes(Commands::listOthers)
                                    ))

                            .then(literal("about").executes(Commands::about))

                            .then(literal("reload")
                                    .requires(Permissions.require("universal_graves.reload", 3))
                                    .executes(Commands::reloadConfig)
                            )
            );
        });
    }

    private static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();

        new GraveListGui(player, player.getGameProfile()).open();

        return 0;
    }

    private static int listOthers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        List<GameProfile> profiles = new ArrayList(context.getArgument("player", GameProfileArgumentType.GameProfileArgument.class).getNames(context.getSource()));

        if (profiles.size() == 0) {
            context.getSource().sendFeedback(new LiteralText("This player doesn't exist!"), false);
            return 0;
        } else if (profiles.size() > 1) {
            context.getSource().sendFeedback(new LiteralText("Only one player can be selected!"), false);
            return 0;
        }

        new GraveListGui(player, profiles.get(0)).open();

        return 0;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        if (ConfigManager.loadConfig()) {
            context.getSource().sendFeedback(new LiteralText("Reloaded config!"), false);
        } else {
            context.getSource().sendError(new LiteralText("Error accrued while reloading config!").formatted(Formatting.RED));

        }
        return 1;
    }

    private static int about(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(new LiteralText("Universal Graves")
                .formatted(Formatting.AQUA)
                .append(new LiteralText(" - " + GravesMod.VERSION)
                        .formatted(Formatting.WHITE)
                ), false);

        return 1;
    }
}
