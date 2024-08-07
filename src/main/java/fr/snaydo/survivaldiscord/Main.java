package fr.snaydo.survivaldiscord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.util.List;

public class Main extends JavaPlugin {
    private static final String DISCORD_BOT_TOKEN = "BOT TOKEN";
    private static final String DISCORD_CHANNEL_ID = "DISCORD CHANNEL ID";
    private JDA jda;
    private TextChannel channel;
    private Message statusMessage;
    private boolean isServerOnline;

    @Override
    public void onEnable() {
        getLogger().info("SurvivalDiscord plugin enabled!");
        isServerOnline = true;
        try {
            jda = JDABuilder.createDefault(DISCORD_BOT_TOKEN).build().awaitReady();
            channel = jda.getTextChannelById(DISCORD_CHANNEL_ID);
            if (channel == null) {
                getLogger().warning("Channel not found!");
            } else {
                jda.getPresence().setActivity(Activity.playing("IP DU SERVEUR"));
                // Try to find the existing status message
                List<Message> messages = channel.getHistory().retrievePast(100).complete();
                for (Message message : messages) {
                    if (message.getAuthor().equals(jda.getSelfUser()) && message.getEmbeds().size() > 0 && message.getEmbeds().get(0).getTitle().equals("Statut du Serveur Minecraft")) {
                        statusMessage = message;
                        break;
                    }
                }

                // If no status message was found, create a new one
                if (statusMessage == null) {
                    EmbedBuilder embedBuilder = createEmbed();
                    statusMessage = channel.sendMessageEmbeds(embedBuilder.build()).complete();
                }

                // Schedule the task to update the embed message every minute
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        getLogger().info("Updating status message...");
                        updateEmbed();
                    }
                }.runTaskTimer(this, 0, 20 * 60); // 20 ticks * 60 seconds = 1 minute
            }
        } catch (InterruptedException e) {
            getLogger().severe("Bot initialization was interrupted");
            e.printStackTrace();
        } catch (Exception e) {
            getLogger().severe("An error occurred while initializing the bot");
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        isServerOnline = false;
        if (jda != null) {
            jda.shutdownNow(); // Use shutdownNow to ensure all resources are released immediately
        }
        getLogger().info("SurvivalDiscord plugin disabled!");
    }

    private EmbedBuilder createEmbed() {
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        String serverStatus;
        if (isServerOnline) {
            serverStatus = Bukkit.hasWhitelist() ? "Maintenance" : "Ouvert";
        } else {
            serverStatus = "Fermé";
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Statut du Serveur Minecraft");
        embedBuilder.setColor(Color.GREEN);
        embedBuilder.addField("Nombre de joueurs en ligne", onlinePlayers + "/" + maxPlayers, false);
        embedBuilder.addField("Statut du serveur", serverStatus, false);
        return embedBuilder;
    }

    private void updateEmbed() {
        if (statusMessage != null) {
            EmbedBuilder embedBuilder = createEmbed();
            statusMessage.editMessageEmbeds(embedBuilder.build()).queue(
                    success -> getLogger().info("Status message updated successfully!"),
                    error -> getLogger().severe("Failed to update status message: " + error.getMessage())
            );
        }
    }
}