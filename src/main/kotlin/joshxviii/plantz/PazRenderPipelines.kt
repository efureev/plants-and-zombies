package joshxviii.plantz

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.renderpearl.api.pipeline.BlendFunction
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.DepthStencilState
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import net.irisshaders.iris.api.v0.IrisApi
import net.irisshaders.iris.api.v0.IrisProgram
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.neoforged.fml.ModList
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent

object PazRenderPipelines {

    @JvmField
    val PAINT_OVERLAY: RenderPipeline = (
        RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(pazResource("pipeline/paint_overlay"))
            .withFragmentShader(pazResource("core/paint_overlay"))
            .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withShaderDefine("ALPHA_CUTOUT", 0.1f)
            .withCull(false)
            .build()
    )

    //TODO use these two for rendering the image on the HUD
    @JvmField
    val HEAD_MARKER: RenderPipeline = (
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(pazResource("pipeline/head_marker"))
            .build()
    )

    @JvmField
    val HEAD_MARKER_OUTLINE: RenderPipeline = (
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(pazResource("pipeline/head_marker_outline"))
            .withFragmentShader(pazResource("core/head_marker_outline"))
            .build()
    )

    @JvmField
    val TIME_PORTAL: RenderPipeline = (RenderPipeline.builder(*arrayOf(
        RenderPipelines.MATRICES_FOG_SNIPPET,
        RenderPipelines.GLOBALS_SNIPPET))
        .withLocation(pazResource("pipeline/time_portal"))
        .withVertexShader(pazResource("core/time_portal"))
        .withFragmentShader(pazResource("core/time_portal"))
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .build()
    )

    /** Fabric registered these through vanilla RenderPipelines.register; NeoForge uses an event. */
    fun registerPipelines(event: RegisterRenderPipelinesEvent) {
        event.registerPipeline(PAINT_OVERLAY)
        event.registerPipeline(HEAD_MARKER)
        event.registerPipeline(HEAD_MARKER_OUTLINE)
        event.registerPipeline(TIME_PORTAL)
    }

    fun initialize() {
        if (ModList.get().isLoaded("iris")) {
            registerIrisCompat()
        }
    }

    private fun registerIrisCompat() {
        try {
            IrisApi.getInstance().let{
                it.assignPipeline(
                    PAINT_OVERLAY,
                    IrisProgram.ENTITIES
                )
                it.assignPipeline(
                    TIME_PORTAL,
                    IrisProgram.PARTICLES_TRANSLUCENT
                )
            }

        } catch (t: Throwable) {
            PazMain.LOGGER.warn("Iris present but assignPipeline failed", t)
        }
    }
}