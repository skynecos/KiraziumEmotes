package com.kirazium.emotes.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EmoteWheelScreen extends Screen {
    private static final int SLOT_WIDTH = 86;
    private static final int SLOT_HEIGHT = 22;

    private int activeX;
    private int activeY;

    public EmoteWheelScreen() {
        super(Component.translatable("screen.kiraziumemotes_client.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Eight positions around an empty center, matching the radial layout reference.
        addEmoteButton(centerX, centerY, 0, -84, "The Floss", "floss", true);
        addEmoteButton(centerX, centerY, 78, -58, "Yakında", null, false);
        addEmoteButton(centerX, centerY, 104, 0, "Yakında", null, false);
        addEmoteButton(centerX, centerY, 78, 58, "Yakında", null, false);
        addEmoteButton(centerX, centerY, 0, 84, "Yakında", null, false);
        addEmoteButton(centerX, centerY, -78, 58, "Yakında", null, false);
        addEmoteButton(centerX, centerY, -104, 0, "Yakında", null, false);
        addEmoteButton(centerX, centerY, -78, -58, "Yakında", null, false);
    }

    private void addEmoteButton(int centerX, int centerY, int offsetX, int offsetY,
                                String label, String emoteId, boolean active) {
        int x = centerX + offsetX - SLOT_WIDTH / 2;
        int y = centerY + offsetY - SLOT_HEIGHT / 2;

        Button button = Button.builder(Component.literal(label), ignored -> {
            if (active && emoteId != null) {
                playEmote(emoteId);
            }
        }).bounds(x, y, SLOT_WIDTH, SLOT_HEIGHT).build();

        if (!active) {
            button.active = false;
        } else {
            activeX = x;
            activeY = y;
        }
        this.addRenderableWidget(button);
    }

    private void playEmote(String emoteId) {
        if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.connection != null) {
            // ClientPacketListener#sendCommand expects the command without a leading slash.
            this.minecraft.player.connection.sendCommand("emote " + emoteId);
        }
        onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Dark translucent overlay so the radial menu stays readable over the world.
        graphics.fill(0, 0, this.width, this.height, 0x99000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        graphics.fill(centerX - 42, centerY - 26, centerX + 42, centerY + 26, 0xB0181818);
        graphics.outline(centerX - 42, centerY - 26, 84, 52, 0x66FFFFFF);

        // First working emote is emphasized in green, like the selected sector in the reference.
        graphics.outline(activeX - 2, activeY - 2, SLOT_WIDTH + 4, SLOT_HEIGHT + 4, 0xFF55FF55);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String title = "EMOTES";
        graphics.text(this.font, title, centerX - this.font.width(title) / 2,
                centerY - this.font.lineHeight / 2, 0xFFFFFFFF, true);
        graphics.text(this.font, "G", centerX - this.font.width("G") / 2,
                centerY + 12, 0xFFAAAAAA, false);
    }
}
