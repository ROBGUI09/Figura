package org.figuramc.figura.mixin.render.layers.items;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.FiguraItemStackRenderStateExtension;
import org.figuramc.figura.ducks.SkullBlockRendererAccessor;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin<S extends ArmedEntityRenderState, M extends EntityModel<S> & ArmedModel> extends RenderLayer<S, M> {

    @Unique
    private Avatar avatar;

    public ItemInHandLayerMixin(RenderLayerParent<S, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    protected void renderArmWithItemInject(S state, ItemStackRenderState itemStackRenderState, HumanoidArm humanoidArm, PoseStack matrices, MultiBufferSource multiBufferSource, int light, CallbackInfo ci) {
        avatar = AvatarManager.getAvatar(state);

        if (itemStackRenderState.isEmpty())
            return;

        boolean left = humanoidArm == HumanoidArm.LEFT;

        if (!RenderUtils.renderArmItem(avatar, left, ci))
            return;

        // pivot part
        if (avatar.pivotPartRender(left ? ParentType.LeftItemPivot : ParentType.RightItemPivot, stack -> {
            final float s = 16f;
            stack.scale(s, s, s);
            stack.mulPose(Axis.XP.rotationDegrees(-90f));
            // Must do this bs manually
            if (((FiguraItemStackRenderStateExtension)itemStackRenderState).figura$getItemStack().getItem() instanceof BlockItem bl && bl.getBlock() instanceof AbstractSkullBlock) {
                Entity entity = AvatarManager.getEntity(state);
                SkullBlockRendererAccessor.setEntity(entity);
                SkullBlockRendererAccessor.setRenderMode(switch (((FiguraItemStackRenderStateExtension) itemStackRenderState).figura$getDisplayContext()) {
                    case FIRST_PERSON_LEFT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_LEFT_HAND;
                    case FIRST_PERSON_RIGHT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_RIGHT_HAND;
                    case THIRD_PERSON_LEFT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_LEFT_HAND;
                    case THIRD_PERSON_RIGHT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_RIGHT_HAND;
                    default -> left ? SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_LEFT_HAND // should never happen
                            : SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_RIGHT_HAND;
                });
            }

            // sorta have to do this manually otherwise itemRenderEvent isn't called
            ItemTransform transform = itemStackRenderState.transform();
            if (avatar == null || !avatar.itemRenderEvent(ItemStackAPI.verify(((FiguraItemStackRenderStateExtension)itemStackRenderState).figura$getItemStack()), ((FiguraItemStackRenderStateExtension)itemStackRenderState).figura$getDisplayContext().name(), FiguraVec3.fromVec3f(transform.translation), FiguraVec3.of(transform.rotation.z, transform.rotation.y, transform.rotation.x), FiguraVec3.fromVec3f(transform.scale), ((FiguraItemStackRenderStateExtension)itemStackRenderState).figura$isLeftHanded(), stack, multiBufferSource, light, OverlayTexture.NO_OVERLAY))
                itemStackRenderState.render(stack, multiBufferSource, light, OverlayTexture.NO_OVERLAY);
        })) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"))
    private void figuraItemEvent(ItemStackRenderState instance, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Operation<Void> original, @Local(argsOnly = true) S armedState) {
        ItemStack stack = ((FiguraItemStackRenderStateExtension)instance).figura$getItemStack();
        Entity entity = AvatarManager.getEntity(armedState);
        if (avatar != null && stack != null && entity != null && stack.getItem() instanceof BlockItem bl && bl.getBlock() instanceof AbstractSkullBlock sk) {
            SkullBlockRendererAccessor.setEntity(entity);
            SkullBlockRendererAccessor.setRenderMode(switch (((FiguraItemStackRenderStateExtension) instance).figura$getDisplayContext()) {
                case FIRST_PERSON_LEFT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_LEFT_HAND;
                case FIRST_PERSON_RIGHT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_RIGHT_HAND;
                case THIRD_PERSON_LEFT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_LEFT_HAND;
                case THIRD_PERSON_RIGHT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_RIGHT_HAND;
                default -> ((FiguraItemStackRenderStateExtension) instance).figura$isLeftHanded() ? SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_LEFT_HAND // should never happen
                        : SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_RIGHT_HAND;
            });
        }
        ItemTransform transform = instance.transform();
        if (avatar == null || !avatar.itemRenderEvent(ItemStackAPI.verify(stack), ((FiguraItemStackRenderStateExtension)instance).figura$getDisplayContext().name(), FiguraVec3.fromVec3f(transform.translation), FiguraVec3.of(transform.rotation.z, transform.rotation.y, transform.rotation.x), FiguraVec3.fromVec3f(transform.scale), ((FiguraItemStackRenderStateExtension)instance).figura$isLeftHanded(), matrices, vertexConsumers, light, overlay))
            original.call(instance, matrices, vertexConsumers, light, overlay);
    }
}
