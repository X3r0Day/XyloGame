package me.xeroday.utils;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DiscordIntegration {

    private static Core core;
    private static Activity activity;
    private static boolean enabled = false;

    public static void init(Long applicationId) {
        if (applicationId == null || applicationId == 0L) {
            System.out.println("Discord RPC disabled (no Application ID)");
            return;
        }

        try {
            loadNative();

            CreateParams params = new CreateParams();
            params.setClientID(applicationId);
            params.setFlags(CreateParams.getDefaultFlags());

            core = new Core(params);
            activity = new Activity();
            enabled = true;

            update("In World", "Playing best game ever");

            System.out.println("Discord Game SDK initialized");
        } catch (Exception e) {
            System.err.println("Discord SDK failed to start, continuing without it");
            e.printStackTrace();
            enabled = false;
        }
    }

    public static void tick() {
        if (enabled && core != null) {
            core.runCallbacks();
        }
    }

    public static void update(String state, String details) {
        if (!enabled || activity == null || core == null) return;

        activity.setState(state);
        activity.setDetails(details);

        activity.assets().setLargeImage("logo");
        activity.assets().setLargeText("XyloGame Engine");

        core.activityManager().updateActivity(activity);
    }

    public static void stop() {
        if (!enabled) return;

        if (activity != null) activity.close();
        if (core != null) core.close();

        activity = null;
        core = null;
        enabled = false;
    }

    private static void loadNative() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        String lib;

        if (os.contains("linux")) lib = "discord_game_sdk.so";
        else if (os.contains("windows")) lib = "discord_game_sdk.dll";
        else if (os.contains("mac")) lib = "discord_game_sdk.dylib";
        else throw new RuntimeException("Unsupported OS");

        File temp = Files.createTempFile("discord-sdk", lib).toFile();
        temp.deleteOnExit();

        try (var in = DiscordIntegration.class.getResourceAsStream("/natives/" + lib)) {
            if (in == null) throw new RuntimeException("Missing native: " + lib);
            Files.copy(in, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        System.load(temp.getAbsolutePath());
    }
}
