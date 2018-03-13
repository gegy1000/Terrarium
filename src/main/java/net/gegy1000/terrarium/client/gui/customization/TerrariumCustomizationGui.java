package net.gegy1000.terrarium.client.gui.customization;

import com.google.common.base.Strings;
import net.gegy1000.terrarium.client.gui.widget.ActionButtonWidget;
import net.gegy1000.terrarium.client.gui.widget.CustomizationList;
import net.gegy1000.terrarium.client.preview.PreviewController;
import net.gegy1000.terrarium.client.preview.PreviewRenderer;
import net.gegy1000.terrarium.client.preview.WorldPreview;
import net.gegy1000.terrarium.server.world.coordinate.SpawnpointDefinition;
import net.gegy1000.terrarium.server.world.generator.TerrariumGenerator;
import net.gegy1000.terrarium.server.world.generator.customization.CustomizationCategory;
import net.gegy1000.terrarium.server.world.generator.customization.CustomizationWidget;
import net.gegy1000.terrarium.server.world.generator.customization.GenerationSettings;
import net.gegy1000.terrarium.server.world.generator.customization.PropertyContainer;
import net.gegy1000.terrarium.server.world.generator.customization.TerrariumPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class TerrariumCustomizationGui extends GuiScreen {
    private static final int DONE_BUTTON = 0;
    private static final int CANCEL_BUTTON = 1;
    private static final int PREVIEW_BUTTON = 2;

    private static final int PRESET_BUTTON = 3;
    private static final int SPAWNPOINT_BUTTON = 4;

    private static final int TOP_OFFSET = 32;
    private static final int BOTTOM_OFFSET = 64;
    private static final int PADDING_X = 5;

    private final GuiCreateWorld parent;

    private final WorldType worldType;
    private final TerrariumGenerator generator;

    private GenerationSettings settings;

    private CustomizationList activeList;
    private CustomizationList categoryList;

    private PreviewRenderer renderer;
    private PreviewController controller;

    private WorldPreview preview = null;

    private boolean freeze;

    public TerrariumCustomizationGui(GuiCreateWorld parent, WorldType worldType, TerrariumGenerator generator, TerrariumPreset defaultPreset) {
        this.parent = parent;
        this.worldType = worldType;
        this.generator = generator;
        if (defaultPreset.getGenerator() != generator) {
            throw new IllegalArgumentException("Cannot customize world with preset of wrong generator type");
        }
        if (Strings.isNullOrEmpty(parent.chunkProviderSettingsJson)) {
            this.setSettings(defaultPreset.createSettings());
        } else {
            this.setSettings(GenerationSettings.deserialize(parent.chunkProviderSettingsJson));
        }
    }

    @Override
    public void initGui() {
        int previewWidth = this.width / 2 - PADDING_X * 2;
        int previewHeight = this.height - TOP_OFFSET - BOTTOM_OFFSET;
        int previewX = this.width / 2 + PADDING_X;
        int previewY = TOP_OFFSET;

        this.renderer = new PreviewRenderer(this, this.width / 2 + PADDING_X, previewY, previewWidth, previewHeight);
        this.controller = new PreviewController(this.renderer, 0.3F, 1.0F);

        this.buttonList.clear();
        this.addButton(new GuiButton(DONE_BUTTON, this.width / 2 - 154, this.height - 28, 150, 20, I18n.format("gui.done")));
        this.addButton(new GuiButton(CANCEL_BUTTON, this.width / 2 + 4, this.height - 28, 150, 20, I18n.format("gui.cancel")));
        this.addButton(new GuiButton(PREVIEW_BUTTON, previewX + previewWidth - 20, previewY, 20, 20, "..."));

        this.addButton(new GuiButton(PRESET_BUTTON, this.width / 2 - 154, this.height - 52, 150, 20, I18n.format("gui.terrarium.preset")));
        this.addButton(new GuiButton(SPAWNPOINT_BUTTON, this.width / 2 + 4, this.height - 52, 150, 20, I18n.format("gui.terrarium.spawnpoint")));

        ActionButtonWidget upLevelButton = new ActionButtonWidget(0, 0, 0, "<<") {
            @Override
            protected void handlePress(Minecraft mc) {
                TerrariumCustomizationGui.this.activeList = TerrariumCustomizationGui.this.categoryList;
            }
        };

        List<GuiButton> categoryListWidgets = new ArrayList<>();

        List<CustomizationCategory> categories = this.settings.getGenerator().getCategories();
        for (CustomizationCategory category : categories) {
            List<GuiButton> currentWidgets = new ArrayList<>();
            currentWidgets.add(upLevelButton);
            for (CustomizationWidget widget : category.getWidgets()) {
                currentWidgets.add(widget.createWidget(this.settings, 0, 0, 0));
            }

            CustomizationList currentList = new CustomizationList(this.mc, this, PADDING_X, TOP_OFFSET, previewWidth, previewHeight, currentWidgets);

            categoryListWidgets.add(new ActionButtonWidget(0, 0, 0, category.getLocalizedName()) {
                @Override
                protected void handlePress(Minecraft mc) {
                    TerrariumCustomizationGui.this.activeList = currentList;
                }
            });
        }

        this.categoryList = new CustomizationList(this.mc, this, PADDING_X, TOP_OFFSET, previewWidth, previewHeight, categoryListWidgets);

        this.activeList = this.categoryList;

        if (!this.freeze) {
            this.rebuildState();
        }

        this.freeze = false;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled && button.visible) {
            switch (button.id) {
                case DONE_BUTTON:
                    this.parent.chunkProviderSettingsJson = this.settings.serializeString();
                    this.mc.displayGuiScreen(this.parent);
                    break;
                case CANCEL_BUTTON:
                    this.mc.displayGuiScreen(this.parent);
                    break;
                case PREVIEW_BUTTON:
                    this.previewLarge();
                    break;
                case PRESET_BUTTON:
                    this.freeze = true;
                    this.mc.displayGuiScreen(new SelectPresetGui(this));
                    break;
                case SPAWNPOINT_BUTTON:
                    this.freeze = true;
                    this.mc.displayGuiScreen(new SelectSpawnpointGui(this));
                    break;
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        this.controller.update();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        this.controller.mouseClicked(mouseX, mouseY, mouseButton);
        this.activeList.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);

        this.controller.mouseReleased(mouseX, mouseY, mouseButton);
        this.activeList.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceLastClick);

        this.controller.mouseDragged(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.activeList.handleMouseInput();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.controller.updateMouse(mouseX, mouseY);

        this.drawDefaultBackground();

        float zoom = this.controller.getZoom(partialTicks);
        float rotationX = this.controller.getRotationX(partialTicks);
        float rotationY = this.controller.getRotationY(partialTicks);
        this.renderer.render(this.preview, zoom, rotationX, rotationY);

        this.activeList.drawScreen(mouseX, mouseY, partialTicks);

        String title = I18n.format("options.terrarium.customize_world_title.name");
        this.drawCenteredString(this.fontRenderer, title, this.width / 2, 20, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        if (!this.freeze) {
            super.onGuiClosed();

            this.deletePreview();
        }
    }

    public void applyPreset(TerrariumPreset preset) {
        this.setSettings(preset.createSettings());
        this.rebuildState();
    }

    public void applySpawnpoint(double spawnpointX, double spawnpointZ) {
        PropertyContainer properties = this.settings.getProperties();
        SpawnpointDefinition spawnpointDefinition = this.generator.getSpawnpointDefinition();
        properties.setDouble(spawnpointDefinition.getPropertyX(), spawnpointX);
        properties.setDouble(spawnpointDefinition.getPropertyZ(), spawnpointZ);
        this.rebuildState();
    }

    private void rebuildState() {
        this.deletePreview();

        BufferBuilder[] builders = new BufferBuilder[8];
        for (int i = 0; i < builders.length; i++) {
            builders[i] = new BufferBuilder(0x4000);
        }
        this.preview = new WorldPreview(this.worldType, this.settings, builders);
    }

    private void previewLarge() {
        if (this.preview != null) {
            this.freeze = true;
            this.mc.displayGuiScreen(new PreviewWorldGui(this.preview, this));
        }
    }

    private void deletePreview() {
        WorldPreview preview = this.preview;
        if (preview != null) {
            preview.delete();
        }
    }

    private void setSettings(GenerationSettings settings) {
        if (settings.getGenerator() != this.generator) {
            throw new IllegalArgumentException("Cannot use settings from another generator!");
        }
        this.settings = settings;
    }

    public GenerationSettings getSettings() {
        return this.settings;
    }
}