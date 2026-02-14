package legends.ultra.cool.addons.mixin.client;

import legends.ultra.cool.addons.data.WidgetConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMultilineLabelMixin<S extends EntityRenderState> {

    // Must match the group name you use everywhere else (your widget uses "Nameplates")
    private static final String CFG = "Nameplates";

    private static final Identifier NAMEPLATE_BORDER =
            Identifier.of("legends-addon", "textures/gui/nameplate_border_grayscaled.png");

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void legends$renderMultiline(
            S state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraRenderState,
            CallbackInfo ci
    ) {
        Vec3d pos = state.nameLabelPos;
        if (pos == null) return;

        Text label = state.displayName;
        if (label == null) return;

        String raw = label.getString();
        if (!raw.contains("\n")) return; // vanilla handles single line

        String[] lines = raw.split("\\R");
        if (lines.length == 0) return;

        // We fully replace vanilla
        ci.cancel();

        float yOffset = WidgetConfigManager.getFloat(CFG, "yOffset", 1f);
        float scale = WidgetConfigManager.getFloat(CFG, "scale", 1.0f);

        // IMPORTANT: your code had "Nameplate" (singular) — that will NOT match your widget config.
        int argb = WidgetConfigManager.getInt(CFG, "bgColor", 0xFF520016);

        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>>  8) & 0xFF) / 255f;
        float b = ( argb         & 0xFF) / 255f;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        // Use world lighting (so it doesn't glow in the dark)
        final int light = state.light;

        matrices.push();
        matrices.translate(pos.x, pos.y + yOffset, pos.z);
        matrices.multiply(client.getEntityRenderDispatcher().camera.getRotation());
        matrices.scale(0.025f * scale, -0.025f * scale, 0.025f * scale);

        int lineH = tr.fontHeight + 1;

        // Compute text block size in "nameplate pixel space"
        int maxW = 0;
        for (String s : lines) maxW = Math.max(maxW, tr.getWidth(s));
        int totalH = lines.length * lineH;

        int padX = 4;
        int padY = 3;

        float panelW = maxW + padX * 2f;
        float panelH = totalH + padY * 2f;

        float panelX = -panelW / 2f;
        float panelY = -padY - (panelH / 3f);

        // RenderLayer usage in 1.21.11: use RenderLayers helpers (RenderLayer.of exists, but you don't want it here).
        // This one is good for textured, alpha-blended, lit text-like layers.
        RenderLayer layer = RenderLayers.text(NAMEPLATE_BORDER); // :contentReference[oaicite:3]{index=3}

        // Submit the nine-slice as a custom queued render
        queue.submitCustom(matrices, layer, (entry, vc) -> {
            Matrix4f mat = entry.getPositionMatrix();
            drawNineSlice(
                    vc,
                    mat,
                    panelX, panelY,
                    panelW, panelH,
                    9, 9, 4,     // texW, texH, corner
                    r, g, b, a,
                    light
            );
        });

        // Now submit each line of text through the queue
        float startY = (-(lines.length - 1) * lineH * 0.5f);

        for (int i = 0; i < lines.length; i++) {
            String s = lines[i];
            float x = -tr.getWidth(s) / 2.0f;
            float y = startY + i * lineH;

            OrderedText ordered = Text.literal(s).asOrderedText();

            queue.submitText(
                    matrices,
                    x, y,
                    ordered,
                    false,
                    TextRenderer.TextLayerType.NORMAL,
                    light,
                    0xFFFFFFFF,
                    0x00000000,
                    0x00000000
            ); // :contentReference[oaicite:5]{index=5}
        }

        matrices.pop();
    }

    private static void v(
            VertexConsumer vc,
            Matrix4f mat,
            float x, float y, float z,
            float u, float v,
            float r, float g, float b, float a,
            int light
    ) {
        vc.vertex(mat, x, y, z);
        vc.texture(u, v);
        vc.color(r, g, b, a);
        vc.overlay(OverlayTexture.DEFAULT_UV);
        vc.light(light);
        vc.normal(0f, 1f, 0f);
    }

    private static void quad(
            VertexConsumer vc,
            Matrix4f mat,
            float x1, float y1, float x2, float y2,
            float u1, float v1, float u2, float v2,
            float r, float g, float b, float a,
            int light
    ) {
        float z = -0.01f;
        v(vc, mat, x1, y2, z, u1, v2, r, g, b, a, light);
        v(vc, mat, x2, y2, z, u2, v2, r, g, b, a, light);
        v(vc, mat, x2, y1, z, u2, v1, r, g, b, a, light);
        v(vc, mat, x1, y1, z, u1, v1, r, g, b, a, light);
    }

    private static void drawNineSlice(
            VertexConsumer vc,
            Matrix4f mat,
            float x, float y,
            float w, float h,
            int texW, int texH,
            int corner,
            float r, float g, float b, float a,
            int light
    ) {
        float cU = corner / (float) texW;
        float cV = corner / (float) texH;

        float x0 = x;
        float y0 = y;
        float x1 = x + corner;
        float y1 = y + corner;
        float x2 = x + w - corner;
        float y2 = y + h - corner;
        float x3 = x + w;
        float y3 = y + h;

        if (w < corner * 2f) x2 = x1;
        if (h < corner * 2f) y2 = y1;

        // corners
        quad(vc, mat, x0, y0, x1, y1, 0f, 0f, cU, cV, r, g, b, a, light);
        quad(vc, mat, x2, y0, x3, y1, 1f - cU, 0f, 1f, cV, r, g, b, a, light);
        quad(vc, mat, x0, y2, x1, y3, 0f, 1f - cV, cU, 1f, r, g, b, a, light);
        quad(vc, mat, x2, y2, x3, y3, 1f - cU, 1f - cV, 1f, 1f, r, g, b, a, light);

        // edges
        quad(vc, mat, x1, y0, x2, y1, cU, 0f, 1f - cU, cV, r, g, b, a, light);
        quad(vc, mat, x1, y2, x2, y3, cU, 1f - cV, 1f - cU, 1f, r, g, b, a, light);
        quad(vc, mat, x0, y1, x1, y2, 0f, cV, cU, 1f - cV, r, g, b, a, light);
        quad(vc, mat, x2, y1, x3, y2, 1f - cU, cV, 1f, 1f - cV, r, g, b, a, light);

        // center
        quad(vc, mat, x1, y1, x2, y2, cU, cV, 1f - cU, 1f - cV, r, g, b, a, light);
    }
}
