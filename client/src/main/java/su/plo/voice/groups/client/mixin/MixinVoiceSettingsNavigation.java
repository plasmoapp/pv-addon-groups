package su.plo.voice.groups.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.plo.lib.mod.client.gui.components.Button;
import su.plo.lib.mod.client.gui.components.IconButton;
import su.plo.lib.mod.client.render.gui.GuiRenderContext;
import su.plo.slib.api.chat.component.McTextComponent;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.client.config.VoiceClientConfig;
import su.plo.voice.client.gui.settings.VoiceSettingsNavigation;
import su.plo.voice.client.gui.settings.VoiceSettingsScreen;
import su.plo.voice.client.gui.settings.tab.TabWidget;
import su.plo.voice.groups.client.gui.GroupsNavigationState;
import su.plo.voice.groups.client.gui.GroupsTabWidget;
import su.plo.voice.groups.client.service.GroupsStatus;
import su.plo.voice.groups.client.service.GroupsClient;

import java.util.List;

import static su.plo.voice.groups.client.gui.RenderKt.addonIcon;
import static su.plo.voice.groups.client.gui.TextKt.addonStatusTooltip;

@Mixin(value = VoiceSettingsNavigation.class, remap = false)
public abstract class MixinVoiceSettingsNavigation {

    @Unique
    private static final ResourceLocation GROUPS_ICON = addonIcon("groups");

    @Unique
    private static final int GROUPS_BUTTON_OFFSET = 80;

    @Shadow
    @Final
    private PlasmoVoiceClient voiceClient;

    @Shadow
    @Final
    private VoiceSettingsScreen parent;

    @Shadow
    @Final
    private VoiceClientConfig config;

    @Shadow
    @Final
    private List<TabWidget> tabWidgets;

    @Shadow
    private int active;

    @Shadow
    public abstract void openTab(int index);

    @Unique
    private final GroupsNavigationState groupsNavigation = new GroupsNavigationState();

    @Unique
    private IconButton groupsButton;

    @Unique
    private int groupsTabIndex = -1;

    @Unique
    private GroupsStatus groupsStatus;

    @Inject(method = "tick", at = @At("HEAD"))
    private void updateGroupsOnStatusChange(CallbackInfo ci) {
        GroupsStatus status = GroupsClient.getService().getStatus();
        if (status == groupsStatus) return;

        if (groupsButton != null) parent.removeWidget(groupsButton);

        boolean groupsTabActive = groupsTabIndex >= 0 && active == groupsTabIndex;
        if (status != GroupsStatus.AVAILABLE && groupsTabActive) openTab(0);

        addGroups(status);
        parent.repositionElements();
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void addGroupsTab(CallbackInfo ci) {
        // PV keeps the settings screen (and `active`) across servers while rebuilding tabWidgets on each init,
        // so a stale groups index can point past the list or at another tab once availability changes
        boolean groupsTabWasActive = groupsTabIndex >= 0 && active == groupsTabIndex;
        if (groupsTabWasActive) active = 0;
        groupsTabIndex = -1;

        addGroups(GroupsClient.getService().getStatus());
        if (groupsTabWasActive && groupsTabIndex >= 0) active = groupsTabIndex;
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void repositionGroupsButton(CallbackInfo ci) {
        if (groupsButton == null) return;

        groupsButton.setX(parent.getWidth() - GROUPS_BUTTON_OFFSET);
    }

    @Inject(method = "renderButtons", at = @At("TAIL"))
    private void renderGroupsButton(GuiRenderContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (groupsButton == null) return;

        groupsButton.setActive(groupsStatus == GroupsStatus.AVAILABLE && active != groupsTabIndex);
        groupsButton.render(context, mouseX, mouseY, delta);
    }

    @ModifyExpressionValue(method = "isMinimized", at = @At(value = "CONSTANT", args = "intValue=62"))
    private int reserveGroupsButtonWidth(int disableButtonsWidth) {
        if (groupsButton == null) return disableButtonsWidth;
        return disableButtonsWidth + GROUPS_BUTTON_OFFSET - 52;
    }

    @Unique
    private void addGroups(GroupsStatus status) {
        groupsStatus = status;
        groupsButton = null;
        if (status == GroupsStatus.UNAVAILABLE) return;

        boolean available = status == GroupsStatus.AVAILABLE;
        if (available && groupsTabIndex < 0) {
            groupsTabIndex = tabWidgets.size();
            tabWidgets.add(new GroupsTabWidget(parent, voiceClient, config, groupsNavigation));
        }

        int tabIndex = groupsTabIndex;
        McTextComponent tooltip = addonStatusTooltip(status);
        groupsButton = new IconButton(
                parent.getWidth() - GROUPS_BUTTON_OFFSET,
                8,
                20,
                20,
                available ? button -> openTab(tabIndex) : Button.NO_ACTION,
                (button, mouseX, mouseY) -> parent.setTooltip(tooltip, mouseX, mouseY),
                GROUPS_ICON,
                true
        );
        parent.addWidget(groupsButton);
    }
}
