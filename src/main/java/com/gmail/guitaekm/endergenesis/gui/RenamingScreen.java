package com.gmail.guitaekm.endergenesis.gui;

import com.gmail.guitaekm.endergenesis.networking.AnswerRenamingRequest;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public class RenamingScreen extends HandledScreen<RenamingScreenHandler> {
    // configure
    public int HEIGHT_FIELD = 100;
    public int HEIGHT_BUTTONS = 150;
    public int HORIZONTAL_DISTANCE = 40;
    public int BUTTON_WIDTH = 60;
    public String currName;
    public RenamingScreen(RenamingScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, new EmptyInventory(), title);
        this.currName = handler.currName;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {

    }
    protected ButtonWidget okButton;
    protected ButtonWidget cancelButton;
    protected ButtonWidget forgetButton;
    protected TextFieldWidget nameField;
    @Override
    protected void init() {
        super.init();
        nameField = new TextFieldWidget(
                MinecraftClient.getInstance().textRenderer,
                BUTTON_WIDTH, HEIGHT_FIELD, 5 * BUTTON_WIDTH, 20, Text.of("")
        );
        // weirdly enough, this can't be done inside the TextFieldWidget-constructor
        nameField.setText(this.currName);
        setInitialFocus(nameField);
        nameField.setSelectionStart(0);
        nameField.setSelectionEnd(this.currName.length());
        this.addDrawableChild(nameField);
        // the 20 is a number tied to minecraft textures of buttons
        this.okButton = new ButtonWidget(
            BUTTON_WIDTH, HEIGHT_BUTTONS, BUTTON_WIDTH, 20, TranslateString.translate("rename_button"), button -> {
                this.handler.sendAnswer(this.nameField.getText(), AnswerRenamingRequest.ButtonPressed.OK);
            }
        );
        this.addDrawableChild(this.okButton);

        this.cancelButton = new ButtonWidget(
            3 * BUTTON_WIDTH, HEIGHT_BUTTONS, BUTTON_WIDTH, 20, TranslateString.translate("cancel_button"), button -> {
                this.handler.sendAnswer(this.nameField.getText(), AnswerRenamingRequest.ButtonPressed.CANCEL);
            }
        );
        this.addDrawableChild(this.cancelButton);
        this.forgetButton = new ButtonWidget(
                5 * BUTTON_WIDTH, HEIGHT_BUTTONS, BUTTON_WIDTH, 20, TranslateString.translate("forget_button"), button -> {
            this.handler.sendAnswer(this.nameField.getText(), AnswerRenamingRequest.ButtonPressed.FORGET);
        }
        );
        this.addDrawableChild(this.forgetButton);
    }
    // from https://github.com/TwelveIterationMods/Waystones/blob/1.18.x/shared/src/main/java/net/blay09/mods/waystones/client/gui/screen/WaystoneSettingsScreen.java
    // I understand this part of the license:
    // - to copy non-substantial portions of the code for use in other projects
    // as it being allowed
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            this.okButton.onPress();
            return true;
        }

        if (this.nameField.keyPressed(keyCode, scanCode, modifiers) || this.nameField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                Objects.requireNonNull(MinecraftClient.getInstance().player).closeHandledScreen();
            }

            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
