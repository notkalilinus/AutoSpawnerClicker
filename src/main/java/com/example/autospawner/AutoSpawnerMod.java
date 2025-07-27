package com.example.autospawner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.lwjgl.glfw.GLFW;

public class AutoSpawnerMod implements ClientModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static long lastActionTime = 0;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Toggle AutoSpawner",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PAGE_DOWN, // Page Down key
                "AutoSpawnerClicker"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                if (enabled) {
                    mc.player.sendMessage(net.minecraft.text.Text.literal("§a[AutoSpawner] Enabled"), false);
                    lastActionTime = 0; // reset timer
                } else {
                    mc.player.sendMessage(net.minecraft.text.Text.literal("§c[AutoSpawner] Disabled"), false);
                }
            }

            if (!enabled) return;

            long now = System.currentTimeMillis();
            if (now - lastActionTime >= 3600_000) { // every 1 hour
                lastActionTime = now;
                triggerSpawnerClick();
            }
        });
    }

    private void triggerSpawnerClick() {
        if (mc.player == null || mc.world == null) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;

        // Right-click spawner
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);

        // Wait 2 seconds for GUI, then click storage slot
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                mc.execute(this::clickStorageSlot);
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private void clickStorageSlot() {
        if (mc.player == null) return;
        if (mc.currentScreen == null) return;

        int storageSlotId = 13; // middle icon in second row
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                storageSlotId,
                0,
                SlotActionType.PICKUP,
                mc.player
        );

        mc.player.closeHandledScreen(); // Close GUI after clicking
    }
}
