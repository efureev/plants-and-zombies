package joshxviii.plantz.model.projectiles;

import joshxviii.plantz.renderer.entity.ProjectileRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import org.jetbrains.annotations.NotNull;

import static joshxviii.plantz.UtilsKt.pazResource;

public class MissileModel extends EntityModel<@NotNull ProjectileRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(pazResource("missle"), "main");
    private final ModelPart rocket;

    public MissileModel(ModelPart root) {
        super(root);
        this.rocket = root.getChild("rocket");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition rocket = partdefinition.addOrReplaceChild("rocket", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -5.0F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(12, 0).addBox(-3.0F, -2.0F, 0.0F, 6.0F, 5.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(0, 11).addBox(0.0F, -2.0F, -3.0F, 0.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -1.5708F, 1.5708F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    public void setupAnim(final @NotNull ProjectileRenderState state) {
        super.setupAnim(state);
    }
}
