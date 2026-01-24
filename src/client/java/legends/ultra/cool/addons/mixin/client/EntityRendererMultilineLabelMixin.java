package legends.ultra.cool.addons.mixin.client;

import legends.ultra.cool.addons.data.WidgetConfigManager;
import legends.ultra.cool.addons.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
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

    // IMPORTANT: make this match whatever the editor writes under (usually widget.getName()).
    private static final String CFG = "Nameplates";

    private static final Identifier NAMEPLATE_BORDER =
            Identifier.of("legends-addon", "textures/gui/nameplate_border_grayscaled.png");

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void legends$renderMultiline(
            S state, Text text, MatrixStack matrices,
            VertexConsumerProvider consumers, int light, CallbackInfo ci
    ) {
        Vec3d pos = state.nameLabelPos;
        if (pos == null) return;

        String raw = text.getString();
        if (!raw.contains("\n")) return; // let vanilla handle single-line

        String[] lines = raw.split("\\R");
        if (lines.length == 0) return;

        ci.cancel();

        float yOffset = WidgetConfigManager.getFloat(CFG, "yOffset", 1f);
        float scale = WidgetConfigManager.getFloat(CFG, "scale", 1.0f);

        // This is the configurable tint color (now reading from SAME category)
        int argb = WidgetConfigManager.getInt(CFG, "bgColor", 0xFF520016);

        argb = ColorUtil.forceValueToMax(argb);

        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>>  8) & 0xFF) / 255f;
        float b = ( argb         & 0xFF) / 255f;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        matrices.push();
        matrices.translate(pos.x, pos.y + yOffset, pos.z);
        matrices.multiply(client.getEntityRenderDispatcher().getRotation());
        matrices.scale(0.025f * scale, -0.025f * scale, 0.025f * scale);

        Matrix4f mat = matrices.peek().getPositionMatrix();

        int lineH = tr.fontHeight + 1;

        // Compute dimensions (in nameplate pixel space)
        int maxW = 0;
        for (String s : lines) maxW = Math.max(maxW, tr.getWidth(s));
        int totalH = lines.length * lineH;

        int padX = 4;
        int padY = 3;

        float panelW = maxW + padX * 2f;
        float panelH = totalH + padY * 2f;

        float panelX = -panelW / 2f;
        float panelY = -padY - (panelH / 3);

        // Draw border/background WITH WORLD LIGHTING (no GUI layer)
        drawNineSlice(
                consumers,
                mat,
                panelX, panelY,
                panelW, panelH,
                9, 9, 4,
                r, g, b, 1f,
                light
        );

        // Draw the lines
        float startY = (-(lines.length - 1) * lineH * 0.5f);

        for (int i = 0; i < lines.length; i++) {
            String s = lines[i];
            float x = -tr.getWidth(s) / 2.0f;
            float y = (startY + i * lineH);

            // Use the same light as the renderer gave us (no forced emission)
            tr.draw(
                    s, x, y,
                    0xFFFFFFFF,
                    false,
                    mat,
                    consumers,
                    TextRenderer.TextLayerType.NORMAL,
                    0,
                    light
            );
        }

        matrices.pop();
    }

    private static void putVertex(
            VertexConsumer vc,
            Matrix4f mat,
            float x, float y, float z,
            float u, float v,
            float r, float g, float b, float a,
            int light
    ) {
        vc.vertex(mat, x, y, z);
        vc.color(r, g, b, a);                 // or vc.colorRGB(...) if that’s what yours wants
        vc.texture(u, v);
        vc.overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV);
        vc.light(light);
        vc.normal(0f, 1f, 0f);
    }

    private static void drawQuad(
            VertexConsumer vc,
            Matrix4f mat,
            float x1, float y1, float x2, float y2,
            float u1, float v1, float u2, float v2,
            float r, float g, float b, float a,
            int light
    ) {
        float z = -0.01f;

        // (x1,y1) is top-left, (x2,y2) bottom-right in your nameplate space
        putVertex(vc, mat, x1, y2, z, u1, v2, r, g, b, a, light);
        putVertex(vc, mat, x2, y2, z, u2, v2, r, g, b, a, light);
        putVertex(vc, mat, x2, y1, z, u2, v1, r, g, b, a, light);
        putVertex(vc, mat, x1, y1, z, u1, v1, r, g, b, a, light);
    }


    private static void drawNineSlice(
            VertexConsumerProvider consumers,
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

        // Use a WORLD layer, not GUI.
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityTranslucent(NAMEPLATE_BORDER));

        // corners
        drawQuad(vc, mat, x0, y0, x1, y1, 0f, 0f, cU, cV, r, g, b, a, light);
        drawQuad(vc, mat, x2, y0, x3, y1, 1f - cU, 0f, 1f, cV, r, g, b, a, light);
        drawQuad(vc, mat, x0, y2, x1, y3, 0f, 1f - cV, cU, 1f, r, g, b, a, light);
        drawQuad(vc, mat, x2, y2, x3, y3, 1f - cU, 1f - cV, 1f, 1f, r, g, b, a, light);

        // edges
        drawQuad(vc, mat, x1, y0, x2, y1, cU, 0f, 1f - cU, cV, r, g, b, a, light);
        drawQuad(vc, mat, x1, y2, x2, y3, cU, 1f - cV, 1f - cU, 1f, r, g, b, a, light);
        drawQuad(vc, mat, x0, y1, x1, y2, 0f, cV, cU, 1f - cV, r, g, b, a, light);
        drawQuad(vc, mat, x2, y1, x3, y2, 1f - cU, cV, 1f, 1f - cV, r, g, b, a, light);

        // center
        drawQuad(vc, mat, x1, y1, x2, y2, cU, cV, 1f - cU, 1f - cV, r, g, b, a, light);
    }
}
