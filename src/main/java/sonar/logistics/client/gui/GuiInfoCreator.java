package sonar.logistics.client.gui;

import java.io.IOException;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import sonar.core.SonarCore;
import sonar.core.client.gui.GuiSonar;
import sonar.core.helpers.FontHelper;
import sonar.logistics.common.containers.ContainerEmptySync;
import sonar.logistics.common.handlers.InfoCreatorHandler;

public class GuiInfoCreator extends GuiSonar {

	public static final ResourceLocation bground = new ResourceLocation("PracticalLogistics:textures/gui/signaller.png");
	private GuiTextField subCategory, data;
	public InfoCreatorHandler handler;
	public TileEntity entity;
	
	public GuiInfoCreator(InfoCreatorHandler handler, TileEntity entity) {
		super(new ContainerEmptySync(handler, entity), entity);
		this.handler = handler;
		this.entity= entity;
	}

	@Override
	public void initGui() {
		super.initGui();
		Keyboard.enableRepeatEvents(true);
		this.mc.thePlayer.openContainer = this.inventorySlots;
		this.xSize = 176;
		this.ySize = 80;
		this.guiLeft = (this.width - this.xSize) / 2;
		this.guiTop = (this.height - this.ySize) / 2;
		subCategory = new GuiTextField(0, this.fontRendererObj, 42, 20, 126, 12);
		subCategory.setMaxStringLength(21);
		subCategory.setText(this.getText(0));

		data = new GuiTextField(1, this.fontRendererObj, 42, 40, 126, 12);
		data.setMaxStringLength(21);
		data.setText(this.getText(1));
	}

	@Override
	public void drawGuiContainerForegroundLayer(int x, int y) {
		super.drawGuiContainerForegroundLayer(x, y);
		FontHelper.textCentre("Info Creator", xSize, 6, 0);
		FontHelper.text("Name:", 6, 22, 0);
		FontHelper.text("Data:", 6, 22 + 20, 0);
		subCategory.drawTextBox();
		data.drawTextBox();
	}

	@Override
	protected void mouseClicked(int i, int j, int k) throws IOException {
		super.mouseClicked(i, j, k);
		subCategory.mouseClicked(i - guiLeft, j - guiTop, k);
		data.mouseClicked(i - guiLeft, j - guiTop, k);
	}

	@Override
	protected void keyTyped(char c, int i) throws IOException {
		if (subCategory.isFocused()) {
			if (c == 13 || c == 27) {
				subCategory.setFocused(false);
			} else {
				subCategory.textboxKeyTyped(c, i);
				final String text = subCategory.getText();
				if (text.isEmpty() || text == "" || text == null) {
					setString("", 0);
				} else {
					setString(text, 0);
				}

			}
		}else if (data.isFocused()) {
			if (c == 13 || c == 27) {
				data.setFocused(false);
			} else {
				data.textboxKeyTyped(c, i);
				final String text = data.getText();
				if (text.isEmpty() || text == "" || text == null) {
					setString("", 1);
				} else {
					setString(text, 1);
				}

			}
		}else {
			super.keyTyped(c, i);
		}
	}

	@Override
	public ResourceLocation getBackground() {
		return bground;
	}

	public String getText(int id){
		switch (id) {
		case 1:
			return handler.data.getObject();
		case 0:
			return handler.subCategory.getObject();
		}
		return " ";
	}

	public void setString(String string, int id){
		SonarCore.sendPacketToServer(entity, string, id);
		switch (id) {
		case 0:
			handler.subCategory.setObject(string);
			break;
		case 1:
			handler.data.setObject(string);
			break;
		}
	}

}
