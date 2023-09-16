package eu.pb4.graves.other;


import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.graves.GenericModInfo;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.ui.AllGraveListGui;
import eu.pb4.graves.ui.GraveListGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
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

                            .then(literal("all")
                                    .requires(Permissions.require("universal_graves.list_others", 3))
                                    .executes((ctx) -> Commands.listAll(ctx, false))
                                    .then(literal("modify")
                                                    .requires(Permissions.require("universal_graves.list_others.modify", 3))
                                                    .executes((ctx) -> Commands.listAll(ctx, true))
                                            )
                            )

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
            new GraveListGui(player, player.getGameProfile(), canModify, Permissions.check(player, "universal_graves.fetch_grave", 3)).open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int listOthers(CommandContext<ServerCommandSource> context, boolean canModify) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        List<GameProfile> profiles = new ArrayList(context.getArgument("player", GameProfileArgumentType.GameProfileArgument.class).getNames(context.getSource()));

        if (profiles.size() == 0) {
            context.getSource().sendFeedback(() -> Text.literal("This player doesn't exist!"), false);
            return 0;
        } else if (profiles.size() > 1) {
            context.getSource().sendFeedback(() -> Text.literal("Only one player can be selected!"), false);
            return 0;
        }
        try {
            new GraveListGui(player, profiles.get(0), canModify, canModify && Permissions.check(player, "universal_graves.fetch_grave.others", 3)).open();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static int listAll(CommandContext<ServerCommandSource> context, boolean canModify) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        try {
            new AllGraveListGui(player, canModify, canModify && Permissions.check(player, "universal_graves.fetch_grave.others", 3)).open();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        if (ConfigManager.loadConfig()) {
            context.getSource().sendFeedback(() -> Text.literal("Reloaded config!"), false);
        } else {
            context.getSource().sendError(Text.literal("Error occurred while reloading config!").formatted(Formatting.RED));
        }
        return 1;
    }

    private static int about(CommandContext<ServerCommandSource> context) {
        for (var text : context.getSource().getEntity() instanceof ServerPlayerEntity ? GenericModInfo.getAboutFull() : GenericModInfo.getAboutConsole()) {
            context.getSource().sendFeedback(() -> text, false);
        }

        return 1;
    }
}
