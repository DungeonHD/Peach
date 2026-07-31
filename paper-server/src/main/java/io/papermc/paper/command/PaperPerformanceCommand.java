package io.papermc.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.util.Locale;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@NullMarked
public final class PaperPerformanceCommand {

    public static final String DESCRIPTION = "View world performance statistics";

    private PaperPerformanceCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> create() {

        final PaperPerformanceCommand command = new PaperPerformanceCommand();

        return Commands.literal("performance")
            .requires(source ->
                source.getSender().hasPermission("bukkit.command.performance")
            )
            .executes(command::execute)
            .build();
    }


    private int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        sender.sendMessage(
            text("-------------------- ", GRAY)
                .append(text("Performance", GOLD))
                .append(text(" --------------------", GRAY))
        );

        for (World world : Bukkit.getWorlds()) {
            File folder = world.getWorldFolder();
            long size = getFolderSize(folder) / 1024 / 1024;

            sender.sendMessage(
                text(world.getName(), YELLOW)
            );
            sender.sendMessage(
                text(" • File Size: ", GRAY)
                    .append(text(size + " MB", GREEN))
            );
            sender.sendMessage(
                text(" • Entities: ", GRAY)
                    .append(text(
                        String.valueOf(world.getEntities().size()),
                        GREEN
                    ))
            );
            sender.sendMessage(
                text(" • Chunks: ", GRAY)
                    .append(text(
                        String.valueOf(world.getLoadedChunks().length),
                        GREEN
                    ))
            );
            sender.sendMessage(
                text(" • Players: ", GRAY)
                    .append(text(
                        String.valueOf(world.getPlayers().size()),
                        GREEN
                    ))
            );
            //sender.sendMessage(Component.empty());
        }
        sender.sendMessage(text(
            "----------------------------------------------------",
            GRAY
        ));
        return Command.SINGLE_SUCCESS;
    }

    private static long getFolderSize(File folder) {
        if (!folder.exists()) {
            return 0;
        }

        long size = 0;
        File[] files = folder.listFiles();

        if (files == null) {
            return 0;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                size += getFolderSize(file);
            } else {
                size += file.length();
            }
        }
        return size;
    }
}
