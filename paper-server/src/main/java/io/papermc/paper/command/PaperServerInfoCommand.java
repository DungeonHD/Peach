package io.papermc.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.text.DecimalFormat;
import java.util.Locale;

import com.sun.management.OperatingSystemMXBean;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@NullMarked
public final class PaperServerInfoCommand {

    public static final String DESCRIPTION = "View server and system information";

    private static final long START_TIME = System.currentTimeMillis();

    private static final DecimalFormat DECIMAL_FORMAT =
        new DecimalFormat("0.0");

    private PaperServerInfoCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> create() {
        final PaperServerInfoCommand command = new PaperServerInfoCommand();

        return Commands.literal("serverinfo")
            .requires(source ->
                source.getSender().hasPermission("bukkit.command.serverinfo")
            )
            .executes(command::execute)
            .build();
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        final CommandSender sender = context.getSource().getSender();

        // ------------------------------------------------------------
        // CPU
        // ------------------------------------------------------------

        final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        final String cpuModel = getCpuModel();
        final int cpuCores = Runtime.getRuntime().availableProcessors();

        double cpuUsage = osBean.getCpuLoad();

        if (cpuUsage < 0) {
            cpuUsage = 0;
        }

        cpuUsage *= 100.0;


        // ------------------------------------------------------------
        // RAM
        // ------------------------------------------------------------

        final Runtime runtime = Runtime.getRuntime();

        final long ramMaxBytes = runtime.maxMemory();
        final long ramUsedBytes =
            runtime.totalMemory() - runtime.freeMemory();

        final double ramMax = bytesToGB(ramMaxBytes);
        final double ramUsed = bytesToGB(ramUsedBytes);

        final double ramUsage =
            ramMax <= 0 ? 0.0 : (ramUsed / ramMax) * 100.0;


        // ------------------------------------------------------------
        // Storage
        // ------------------------------------------------------------

        final StorageInfo storage = getStorageInfo();


        // ------------------------------------------------------------
        // Server
        // ------------------------------------------------------------

        final String uptime = formatUptime(
            System.currentTimeMillis() - START_TIME
        );

        final double tps = getTPS();

        final Component tpsComponent = getTPSComponent(tps);

        final int playersOnline = Bukkit.getOnlinePlayers().size();
        final int loadedWorlds = Bukkit.getWorlds().size();


        // ------------------------------------------------------------
        // Output
        // ------------------------------------------------------------

        sender.sendMessage(
            text("============================================================", GRAY)
        );

        sender.sendMessage(
            text("CPU Information:", GOLD)
        );

        sender.sendMessage(
            text(" Model: ", GRAY)
                .append(text(cpuModel, WHITE))
        );

        sender.sendMessage(
            text(" Cores: ", GRAY)
                .append(text(String.valueOf(cpuCores), WHITE))
        );

        sender.sendMessage(
            text(" Usage: ", GRAY)
                .append(text(
                    format(cpuUsage) + "%",
                    WHITE
                ))
        );


        sender.sendMessage(
            text("RAM Information:", GOLD)
        );

        sender.sendMessage(
            text(" Used: ", GRAY)
                .append(text(
                    format(ramUsed) + " GB",
                    GREEN
                ))
        );

        sender.sendMessage(
            text(" Max: ", GRAY)
                .append(text(
                    format(ramMax) + " GB",
                    WHITE
                ))
        );

        sender.sendMessage(
            text(" Usage: ", GRAY)
                .append(text(
                    format(ramUsage) + "%",
                    GREEN
                ))
        );


        sender.sendMessage(
            text("Storage Information:", GOLD)
        );

        sender.sendMessage(
            text(" Used: ", GRAY)
                .append(text(
                    format(storage.usedGB) + " GB",
                    GREEN
                ))
        );

        sender.sendMessage(
            text(" Total: ", GRAY)
                .append(text(
                    format(storage.totalGB) + " GB",
                    WHITE
                ))
        );

        sender.sendMessage(
            text(" Usage: ", GRAY)
                .append(text(
                    format(storage.usage) + "%",
                    GREEN
                ))
        );


        sender.sendMessage(
            text("Server Information:", GOLD)
        );

        sender.sendMessage(
            text(" Uptime: ", GRAY)
                .append(text(uptime, WHITE))
        );

        sender.sendMessage(
            text(" TPS: ", GRAY)
                .append(tpsComponent)
        );

        sender.sendMessage(
            text(" Players: ", GRAY)
                .append(text(
                    String.valueOf(playersOnline),
                    WHITE
                ))
        );

        sender.sendMessage(
            text(" Worlds: ", GRAY)
                .append(text(
                    String.valueOf(loadedWorlds),
                    WHITE
                ))
        );

        sender.sendMessage(
            text("============================================================", GRAY)
        );

        return Command.SINGLE_SUCCESS;
    }


    private static String getCpuModel() {
        final String os = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT);

        // Linux
        if (os.contains("linux")) {
            try {
                final java.nio.file.Path cpuInfo =
                    java.nio.file.Path.of("/proc/cpuinfo");

                if (java.nio.file.Files.exists(cpuInfo)) {
                    for (String line : java.nio.file.Files.readAllLines(cpuInfo)) {
                        if (line.startsWith("model name")) {
                            final int index = line.indexOf(':');

                            if (index >= 0) {
                                return line.substring(index + 1).trim();
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Windows / other systems
        final String processor =
            System.getenv("PROCESSOR_IDENTIFIER");

        if (processor != null && !processor.isBlank()) {
            return processor;
        }

        return System.getProperty(
            "os.arch",
            "Unknown CPU"
        );
    }


    private static StorageInfo getStorageInfo() {
        long total = 0;
        long usable = 0;

        try {
            for (FileStore store : FileSystems.getDefault().getFileStores()) {
                try {
                    total += store.getTotalSpace();
                    usable += store.getUsableSpace();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        if (total <= 0) {
            return new StorageInfo(0, 0, 0);
        }

        final long used = total - usable;

        final double totalGB = bytesToGB(total);
        final double usedGB = bytesToGB(used);

        final double usage =
            (used / (double) total) * 100.0;

        return new StorageInfo(
            usedGB,
            totalGB,
            usage
        );
    }


    private static double getTPS() {
        final double[] tps = Bukkit.getTPS();

        if (tps.length == 0) {
            return 20.0;
        }

        // Paper exposes the 1m TPS as the third value.
        return Math.min(tps[2], 20.0);
    }


    private static Component getTPSComponent(double tps) {
        if (tps >= 18.0) {
            return text(format(tps), GREEN);
        }

        if (tps >= 15.0) {
            return text(format(tps), YELLOW);
        }

        return text(format(tps), RED);
    }


    private static String formatUptime(long milliseconds) {
        long seconds = milliseconds / 1000;

        final long days = seconds / 86400;
        seconds %= 86400;

        final long hours = seconds / 3600;
        seconds %= 3600;

        final long minutes = seconds / 60;
        seconds %= 60;

        if (days > 0) {
            return days + "d "
                + hours + "h "
                + minutes + "m "
                + seconds + "s";
        }

        if (hours > 0) {
            return hours + "h "
                + minutes + "m "
                + seconds + "s";
        }

        if (minutes > 0) {
            return minutes + "m "
                + seconds + "s";
        }
        return seconds + "s";
    }


    private static double bytesToGB(long bytes) {
        return bytes / 1024.0 / 1024.0 / 1024.0;
    }


    private static String format(double value) {
        return DECIMAL_FORMAT.format(value);
    }


    private record StorageInfo(
        double usedGB,
        double totalGB,
        double usage
    ) {
    }
}
