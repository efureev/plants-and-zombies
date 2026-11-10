package joshxviii.plantz

import joshxviii.plantz.item.model.PlantIconItemModel
import joshxviii.plantz.model.BalloonModel
import joshxviii.plantz.model.FlagBlockModel
import joshxviii.plantz.model.GnomeArmorModel
import joshxviii.plantz.model.GnomeModel
import joshxviii.plantz.model.blueprint_machines.ElectroTurretModel
import joshxviii.plantz.model.blueprint_machines.LawnMowerModel
import joshxviii.plantz.model.blueprint_machines.ZombieDroneModel
import joshxviii.plantz.model.blueprint_machines.ZombieTurretModel
import joshxviii.plantz.model.plants.*
import joshxviii.plantz.model.projectiles.*
import joshxviii.plantz.model.zombies.*
import joshxviii.plantz.renderer.entity.BalloonRenderer
import joshxviii.plantz.renderer.FlagRenderer
import joshxviii.plantz.renderer.GardenGnomeBlockRenderer
import joshxviii.plantz.renderer.entity.GnomeRenderer
import joshxviii.plantz.renderer.entity.zombie.PazZombieRenderer
import joshxviii.plantz.renderer.PlantPotMinecartRenderer
import joshxviii.plantz.renderer.entity.ProjectileRenderer
import joshxviii.plantz.renderer.SunBatteryRenderer
import joshxviii.plantz.renderer.entity.SunRenderer
import joshxviii.plantz.renderer.MailboxRenderer
import joshxviii.plantz.renderer.TimeMachineRenderer
import joshxviii.plantz.renderer.TimePortalRenderer
import joshxviii.plantz.renderer.entity.zombie.GargantuarRenderer
import joshxviii.plantz.renderer.entity.zombie.PirateCaptainRenderer
import joshxviii.plantz.renderer.entity.*
import joshxviii.plantz.renderer.entity.plant.PlantRenderer
import joshxviii.plantz.renderer.entity.plant.PumpkinRenderer
import joshxviii.plantz.renderer.entity.plant.RoyalSunflowerRenderer
import joshxviii.plantz.renderer.entity.zombie.*
import net.minecraft.util.context.ContextKey
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.client.renderer.entity.ArmorModelSet
import net.minecraft.client.renderer.entity.EntityRenderers
import net.minecraft.client.renderer.entity.ThrownItemRenderer
import net.minecraft.client.renderer.item.ItemModels
import net.minecraft.resources.Identifier

object PazModels {

    @JvmField
    val HAS_HYPNO_KEY: ContextKey<Boolean> = ContextKey(pazResource("hypnotized"))
    @JvmField
    val HAS_CHILL_KEY: ContextKey<Boolean> = ContextKey(pazResource("chill"))
    @JvmField
    val HAS_FROZEN_KEY: ContextKey<Boolean> = ContextKey(pazResource("frozen"))
    @JvmField
    val HAS_BUTTER_KEY: ContextKey<Boolean> = ContextKey(pazResource("buttered"))
    @JvmField
    val HEAD_PLANT_KEY: ContextKey<joshxviii.plantz.renderer.WornPlantRenderData> = ContextKey(pazResource("head_plant"))
    @JvmField
    val PAINT_COLORS_KEY: ContextKey<Map<Int, Int>> = ContextKey(pazResource("painted"))
    @JvmField
    val IS_SKATING: ContextKey<Boolean> = ContextKey(pazResource("skating"))

    private val PAINT_OVERLAY_TEXTURE_1: Identifier = pazResource("textures/gui/overlay/paint1.png")
    private val PAINT_OVERLAY_TEXTURE_2: Identifier = pazResource("textures/gui/overlay/paint2.png")
    private val PAINT_OVERLAY_TEXTURE_3: Identifier = pazResource("textures/gui/overlay/paint3.png")
    private val PAINT_OVERLAY_TEXTURE_4: Identifier = pazResource("textures/gui/overlay/paint4.png")
    private val PAINT_OVERLAY_TEXTURE_5: Identifier = pazResource("textures/gui/overlay/paint5.png")
    fun getOverlayTexture(value: Float): Identifier {
        return when {
            value < 0.2 -> PAINT_OVERLAY_TEXTURE_1
            value < 0.4 -> PAINT_OVERLAY_TEXTURE_2
            value < 0.6 -> PAINT_OVERLAY_TEXTURE_3
            value < 0.8 -> PAINT_OVERLAY_TEXTURE_4
            else -> PAINT_OVERLAY_TEXTURE_5
        }
    }

    val EMPTY_LAYER: ModelLayerLocation = ModelLayerLocation(pazResource("empty"), "empty")

    val EMPTY_ARMOR_SET = ArmorModelSet(
        EMPTY_LAYER,
        EMPTY_LAYER,
        EMPTY_LAYER,
        EMPTY_LAYER
    )

    val ARMOR_LAYER_HEAD   = ModelLayerLocation(pazResource("gnome_armor"), "head")
    val ARMOR_LAYER_CHEST  = ModelLayerLocation(pazResource("gnome_armor"), "chest")
    val ARMOR_LAYER_LEGS   = ModelLayerLocation(pazResource("gnome_armor"), "legs")
    val ARMOR_LAYER_FEET   = ModelLayerLocation(pazResource("gnome_armor"), "boots")

    val ARMOR_LAYER_LOCATION = GnomeArmorSet(
        head  = ARMOR_LAYER_HEAD,
        chest = ARMOR_LAYER_CHEST,
        legs  = ARMOR_LAYER_LEGS,
        feet  = ARMOR_LAYER_FEET
    )

    fun registerItemModels(event: RegisterItemModelsEvent) {
        event.register(pazResource("plant_icon"), PlantIconItemModel.Unbaked.MAP_CODEC)
        event.register(pazResource("door_shield_model"), DoorShieldItemModel.Unbaked.MAP_CODEC)
    }

    fun registerLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(EMPTY_LAYER) { LayerDefinition.create(MeshDefinition(), 0, 0) }

        // REGISTER MODELS
        event.registerLayerDefinition(PeaShooterModel.LAYER_LOCATION) { PeaShooterModel.createBodyLayer() }
        event.registerLayerDefinition(SunflowerModel.LAYER_LOCATION) { SunflowerModel.createBodyLayer() }
        event.registerLayerDefinition(MarigoldModel.LAYER_LOCATION) { MarigoldModel.createBodyLayer() }
        event.registerLayerDefinition(WallNutModel.LAYER_LOCATION) { WallNutModel.createBodyLayer() }
        event.registerLayerDefinition(ChomperModel.LAYER_LOCATION) { ChomperModel.createBodyLayer() }
        event.registerLayerDefinition(CherryBombModel.LAYER_LOCATION) { CherryBombModel.createBodyLayer() }
        event.registerLayerDefinition(PotatoMineModel.LAYER_LOCATION) { PotatoMineModel.createBodyLayer() }
        event.registerLayerDefinition(RepeaterModel.LAYER_LOCATION) { RepeaterModel.createBodyLayer() }
        event.registerLayerDefinition(IcePeaShooterModel.LAYER_LOCATION) { IcePeaShooterModel.createBodyLayer() }
        event.registerLayerDefinition(FirePeaShooterModel.LAYER_LOCATION) { FirePeaShooterModel.createBodyLayer() }
        event.registerLayerDefinition(ElectricPeaShooterModel.LAYER_LOCATION) { ElectricPeaShooterModel.createBodyLayer() }
        event.registerLayerDefinition(CactusModel.LAYER_LOCATION) { CactusModel.createBodyLayer() }
        event.registerLayerDefinition(LightningReedModel.LAYER_LOCATION) { LightningReedModel.createBodyLayer() }
        event.registerLayerDefinition(CabbagePultModel.LAYER_LOCATION) { CabbagePultModel.createBodyLayer() }
        event.registerLayerDefinition(KernelPultModel.LAYER_LOCATION) { KernelPultModel.createBodyLayer() }
        event.registerLayerDefinition(MelonPultModel.LAYER_LOCATION) { MelonPultModel.createBodyLayer() }
        event.registerLayerDefinition(WinterMelonModel.LAYER_LOCATION) { WinterMelonModel.createBodyLayer() }
        event.registerLayerDefinition(BonkChoyModel.LAYER_LOCATION) { BonkChoyModel.createBodyLayer() }
        event.registerLayerDefinition(TangleKelpModel.LAYER_LOCATION) { TangleKelpModel.createBodyLayer() }
        event.registerLayerDefinition(PuffShroomModel.LAYER_LOCATION) { PuffShroomModel.createBodyLayer() }
        event.registerLayerDefinition(ScaredyShroomModel.LAYER_LOCATION) { ScaredyShroomModel.createBodyLayer() }
        event.registerLayerDefinition(FumeShroomModel.LAYER_LOCATION) { FumeShroomModel.createBodyLayer() }
        event.registerLayerDefinition(SunShroomModel.LAYER_LOCATION) { SunShroomModel.createBodyLayer() }
        event.registerLayerDefinition(SunShroomBabyModel.LAYER_LOCATION) { SunShroomBabyModel.createBodyLayer() }
        event.registerLayerDefinition(HypnoShroomModel.LAYER_LOCATION) { HypnoShroomModel.createBodyLayer() }
        event.registerLayerDefinition(IceShroomModel.LAYER_LOCATION) { IceShroomModel.createBodyLayer() }
        event.registerLayerDefinition(DoomShroomModel.LAYER_LOCATION) { DoomShroomModel.createBodyLayer() }
        event.registerLayerDefinition(SeaShroomModel.LAYER_LOCATION) { SeaShroomModel.createBodyLayer() }
        event.registerLayerDefinition(CoffeeBeanModel.LAYER_LOCATION) { CoffeeBeanModel.createBodyLayer() }
        event.registerLayerDefinition(GraveBusterModel.LAYER_LOCATION) { GraveBusterModel.createBodyLayer() }
        event.registerLayerDefinition(PumpkinModel.LAYER_LOCATION) { PumpkinModel.createBodyLayer() }
        event.registerLayerDefinition(SquashModel.LAYER_LOCATION) { SquashModel.createBodyLayer() }

        event.registerLayerDefinition(CitronModel.LAYER_LOCATION) { CitronModel.createBodyLayer() }
        event.registerLayerDefinition(RoyalSunflowerModel.LAYER_LOCATION) { RoyalSunflowerModel.createBodyLayer() }

        event.registerLayerDefinition(PeaModel.LAYER_LOCATION) { PeaModel.createBodyLayer() }
        event.registerLayerDefinition(NeedleModel.LAYER_LOCATION) { NeedleModel.createBodyLayer() }
        event.registerLayerDefinition(CabbageModel.LAYER_LOCATION) { CabbageModel.createBodyLayer() }
        event.registerLayerDefinition(KernelModel.LAYER_LOCATION) { KernelModel.createBodyLayer() }
        event.registerLayerDefinition(ButterModel.LAYER_LOCATION) { ButterModel.createBodyLayer() }
        event.registerLayerDefinition(MelonModel.LAYER_LOCATION) { MelonModel.createBodyLayer() }
        event.registerLayerDefinition(SmallProjectileModel.LAYER_LOCATION) { SmallProjectileModel.createBodyLayer() }
        event.registerLayerDefinition(LaserBulletModel.LAYER_LOCATION) { LaserBulletModel.createBodyLayer() }
        event.registerLayerDefinition(MissileModel.LAYER_LOCATION) { MissileModel.createBodyLayer() }

        event.registerLayerDefinition(PazZombieModel.LAYER_LOCATION) { PazZombieModel.createBodyLayer() }
        event.registerLayerDefinition(DiggerZombieModel.LAYER_LOCATION) { DiggerZombieModel.createBodyLayer() }
        event.registerLayerDefinition(EngineerZombieModel.LAYER_LOCATION) { EngineerZombieModel.createBodyLayer() }
        event.registerLayerDefinition(ZombieYetiModel.LAYER_LOCATION) { ZombieYetiModel.createBodyLayer() }
        event.registerLayerDefinition(DiscoZombieModel.LAYER_LOCATION) { DiscoZombieModel.createBodyLayer() }
        event.registerLayerDefinition(AllStarModel.LAYER_LOCATION) { AllStarModel.createBodyLayer() }
        event.registerLayerDefinition(GraveDiggerModel.LAYER_LOCATION) { GraveDiggerModel.createBodyLayer() }
        event.registerLayerDefinition(SoldierZombieModel.LAYER_LOCATION) { SoldierZombieModel.createBodyLayer() }
        event.registerLayerDefinition(PirateCaptainModel.LAYER_LOCATION) { PirateCaptainModel.createBodyLayer() }
        event.registerLayerDefinition(PirateCaptainGhostModel.LAYER_LOCATION) { PirateCaptainGhostModel.createBodyLayer() }
        event.registerLayerDefinition(RoboZombieModel.LAYER_LOCATION) { RoboZombieModel.createBodyLayer() }
        event.registerLayerDefinition(SuperBrainzModel.LAYER_LOCATION) { SuperBrainzModel.createBodyLayer() }
        event.registerLayerDefinition(ImpModel.LAYER_LOCATION) { ImpModel.createBodyLayer() }
        event.registerLayerDefinition(GargantuarModel.LAYER_LOCATION) { GargantuarModel.createBodyLayer() }
        event.registerLayerDefinition(BaronVonBatsModel.LAYER_LOCATION) { BaronVonBatsModel.createBodyLayer() }
        event.registerLayerDefinition(ElectricSlideModel.LAYER_LOCATION) { ElectricSlideModel.createBodyLayer() }
        event.registerLayerDefinition(ZombatModel.LAYER_LOCATION) { ZombatModel.createBodyLayer() }
        event.registerLayerDefinition(ZombieGoatModel.LAYER_LOCATION) { ZombieGoatModel.createBodyLayer() }

        event.registerLayerDefinition(ZombieTurretModel.LAYER_LOCATION) { ZombieTurretModel.createBodyLayer() }
        event.registerLayerDefinition(ElectroTurretModel.LAYER_LOCATION) { ElectroTurretModel.createBodyLayer() }
        event.registerLayerDefinition(ZombieDroneModel.LAYER_LOCATION) { ZombieDroneModel.createBodyLayer() }
        event.registerLayerDefinition(LawnMowerModel.LAYER_LOCATION) { LawnMowerModel.createBodyLayer() }

        event.registerLayerDefinition(GnomeModel.LAYER_LOCATION) { GnomeModel.createBodyLayer() }
        event.registerLayerDefinition(ARMOR_LAYER_LOCATION.head)  { GnomeArmorModel.createHeadLayer() }
        event.registerLayerDefinition(ARMOR_LAYER_LOCATION.chest) { GnomeArmorModel.createChestLayer() }
        event.registerLayerDefinition(ARMOR_LAYER_LOCATION.legs)  { GnomeArmorModel.createLegsLayer() }
        event.registerLayerDefinition(ARMOR_LAYER_LOCATION.feet)  { GnomeArmorModel.createBootsLayer() }

        event.registerLayerDefinition(BalloonModel.LAYER_LOCATION) { BalloonModel.createBodyLayer() }
        event.registerLayerDefinition(FlagBlockModel.LAYER_LOCATION) { FlagBlockModel.createBodyLayer() }
    }

    fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        // REGISTER ENTITY RENDERERS
        event.registerEntityRenderer(PazEntities.PEA_SHOOTER) { PlantRenderer(PeaShooterModel(it.bakeLayer(PeaShooterModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.SUNFLOWER) { PlantRenderer(SunflowerModel(it.bakeLayer(SunflowerModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.MARIGOLD) { PlantRenderer(MarigoldModel(it.bakeLayer(MarigoldModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.WALL_NUT) { PlantRenderer(WallNutModel(it.bakeLayer(WallNutModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.EXPLODE_O_NUT) { PlantRenderer(WallNutModel(it.bakeLayer(WallNutModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.CHOMPER) { PlantRenderer(ChomperModel(it.bakeLayer(ChomperModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.CHERRY_BOMB) { PlantRenderer(CherryBombModel(it.bakeLayer(CherryBombModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.POTATO_MINE) { PlantRenderer(PotatoMineModel(it.bakeLayer(PotatoMineModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.REPEATER) { PlantRenderer(RepeaterModel(it.bakeLayer(RepeaterModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.ICE_PEA_SHOOTER) { PlantRenderer(IcePeaShooterModel(it.bakeLayer(IcePeaShooterModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.FIRE_PEA_SHOOTER) { PlantRenderer(FirePeaShooterModel(it.bakeLayer(FirePeaShooterModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.ELECTRIC_PEA_SHOOTER) { PlantRenderer(ElectricPeaShooterModel(it.bakeLayer(ElectricPeaShooterModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.CACTUS) { PlantRenderer(CactusModel(it.bakeLayer(CactusModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.LIGHTNING_REED) { PlantRenderer(LightningReedModel(it.bakeLayer(LightningReedModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.CABBAGE_PULT) { PlantRenderer(CabbagePultModel(it.bakeLayer(CabbagePultModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.KERNEL_PULT) { PlantRenderer(KernelPultModel(it.bakeLayer(KernelPultModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.MELON_PULT) { PlantRenderer(MelonPultModel(it.bakeLayer(MelonPultModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.WINTER_MELON) { PlantRenderer(WinterMelonModel(it.bakeLayer(WinterMelonModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.BONK_CHOY) { PlantRenderer(BonkChoyModel(it.bakeLayer(BonkChoyModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.TANGLE_KELP) { PlantRenderer(TangleKelpModel(it.bakeLayer(TangleKelpModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.PUFF_SHROOM) { PlantRenderer(PuffShroomModel(it.bakeLayer(PuffShroomModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.SCAREDY_SHROOM) { PlantRenderer(ScaredyShroomModel(it.bakeLayer(ScaredyShroomModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.FUME_SHROOM) { PlantRenderer(FumeShroomModel(it.bakeLayer(FumeShroomModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.HYPNOSHROOM) { PlantRenderer(HypnoShroomModel(it.bakeLayer(HypnoShroomModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.ICE_SHROOM) { PlantRenderer(IceShroomModel(it.bakeLayer(IceShroomModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.DOOM_SHROOM) { PlantRenderer(DoomShroomModel(it.bakeLayer(DoomShroomModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.SEA_SHROOM) { PlantRenderer(SeaShroomModel(it.bakeLayer(SeaShroomModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.COFFEE_BEAN) { PlantRenderer(CoffeeBeanModel(it.bakeLayer(CoffeeBeanModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.GRAVE_BUSTER) { PlantRenderer(GraveBusterModel(it.bakeLayer(GraveBusterModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.PUMPKIN) { PumpkinRenderer(it) }
        event.registerEntityRenderer(PazEntities.SQUASH) { PlantRenderer(SquashModel(it.bakeLayer(SquashModel.LAYER_LOCATION)), it) }

        event.registerEntityRenderer(PazEntities.SUN_SHROOM) { PlantRenderer(SunShroomModel(it.bakeLayer(SunShroomModel.LAYER_LOCATION)), it, SunShroomBabyModel(it.bakeLayer(SunShroomBabyModel.LAYER_LOCATION))) }

        event.registerEntityRenderer(PazEntities.CITRON) { PlantRenderer(CitronModel(it.bakeLayer(CitronModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.ROYAL_SUNFLOWER) { RoyalSunflowerRenderer(it) }

        event.registerEntityRenderer(PazEntities.PEA) { ProjectileRenderer(PeaModel(it.bakeLayer(PeaModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.PEA_ICE) { ProjectileRenderer(PeaModel(it.bakeLayer(PeaModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.PEA_FIRE) { ProjectileRenderer(PeaModel(it.bakeLayer(PeaModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.PEA_ELECTRIC) { ProjectileRenderer(PeaModel(it.bakeLayer(PeaModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.SPORE) { ProjectileRenderer(SmallProjectileModel(it.bakeLayer(SmallProjectileModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.WATER_SPORE) { ProjectileRenderer(SmallProjectileModel(it.bakeLayer(SmallProjectileModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.NEEDLE) { ProjectileRenderer(NeedleModel(it.bakeLayer(NeedleModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.CABBAGE) { ProjectileRenderer(CabbageModel(it.bakeLayer(CabbageModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.KERNEL) { ProjectileRenderer(KernelModel(it.bakeLayer(KernelModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.BUTTER) { ProjectileRenderer(ButterModel(it.bakeLayer(ButterModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.MELON) { ProjectileRenderer(MelonModel(it.bakeLayer(MelonModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.FROZEN_MELON) { ProjectileRenderer(MelonModel(it.bakeLayer(MelonModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.PAINT_BALL) { ProjectileRenderer(SmallProjectileModel(it.bakeLayer(SmallProjectileModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.LASER_BULLET) { ProjectileRenderer(LaserBulletModel(it.bakeLayer(LaserBulletModel.LAYER_LOCATION)), it) }
        event.registerEntityRenderer(PazEntities.MISSILE) { ProjectileRenderer(MissileModel(it.bakeLayer(MissileModel.LAYER_LOCATION)), it) }

        event.registerEntityRenderer(PazEntities.BROWN_COAT) { PazZombieRenderer(it) }
        event.registerEntityRenderer(PazEntities.NEWSPAPER_ZOMBIE) { PazZombieRenderer(it) }
        event.registerEntityRenderer(PazEntities.DIGGER_ZOMBIE) { DiggerZombieRenderer(it) }
        event.registerEntityRenderer(PazEntities.ENGINEER_ZOMBIE) { PazZombieRenderer(it, EngineerZombieModel(it.bakeLayer(EngineerZombieModel.LAYER_LOCATION))) }
        event.registerEntityRenderer(PazEntities.ZOMBIE_YETI) { PazZombieRenderer(it, ZombieYetiModel(it.bakeLayer(ZombieYetiModel.LAYER_LOCATION))) }
        event.registerEntityRenderer(PazEntities.BACKUP_DANCER) { PazZombieRenderer(it, DiscoZombieModel(it.bakeLayer(DiscoZombieModel.LAYER_LOCATION))) }
        event.registerEntityRenderer(PazEntities.DISCO_ZOMBIE) { PazZombieRenderer(it, DiscoZombieModel(it.bakeLayer(DiscoZombieModel.LAYER_LOCATION))) }
        event.registerEntityRenderer(PazEntities.ALL_STAR) { PazZombieRenderer(it, AllStarModel(it.bakeLayer(AllStarModel.LAYER_LOCATION)), AllStarModel(it.bakeLayer(AllStarModel.LAYER_LOCATION))) }
        event.registerEntityRenderer(PazEntities.GRAVE_DIGGER) { PazZombieRenderer(it, GraveDiggerModel(it.bakeLayer(GraveDiggerModel.LAYER_LOCATION))) }
        event.registerEntityRenderer(PazEntities.SOLDIER_ZOMBIE) { PazZombieRenderer(it, SoldierZombieModel(it.bakeLayer(SoldierZombieModel.LAYER_LOCATION)), SoldierZombieModel(it.bakeLayer(SoldierZombieModel.LAYER_LOCATION))) }
        event.registerEntityRenderer(PazEntities.PIRATE_CAPTAIN) { PirateCaptainRenderer(it) }
        event.registerEntityRenderer(PazEntities.PIRATE_CAPTAIN_GHOST) { PirateCaptainRenderer(it, PirateCaptainGhostModel(it.bakeLayer(PirateCaptainGhostModel.LAYER_LOCATION))) }
        event.registerEntityRenderer(PazEntities.ROBO_ZOMBIE) { RoboZombieRenderer(it) }
        event.registerEntityRenderer(PazEntities.SUPER_BRAINZ) { SuperBrainzRenderer(it) }
        event.registerEntityRenderer(PazEntities.IMP) { PazZombieRenderer(it, ImpModel(it.bakeLayer(ImpModel.LAYER_LOCATION)), ImpModel(it.bakeLayer(ImpModel.LAYER_LOCATION))) }
        event.registerEntityRenderer(PazEntities.GARGANTUAR) { GargantuarRenderer(it) }
        event.registerEntityRenderer(PazEntities.BARON_VON_BATS) { BaronVonBatsRenderer(it) }
        event.registerEntityRenderer(PazEntities.ELECTRIC_SLIDE) { PazZombieRenderer(it, ElectricSlideModel(it.bakeLayer(ElectricSlideModel.LAYER_LOCATION))) }
        event.registerEntityRenderer(PazEntities.ZOMBAT) { ZombatRenderer(it) }
        event.registerEntityRenderer(PazEntities.ZOMBIE_GOAT) { ZombieGoatRenderer(it) }

        event.registerEntityRenderer(PazEntities.ZOMBIE_TURRET) { BlueprintMachineRenderer(it) }
        event.registerEntityRenderer(PazEntities.ELECTRO_TURRET) { BlueprintMachineRenderer(it, ElectroTurretModel(it.bakeLayer(ElectroTurretModel.LAYER_LOCATION))) }
        event.registerEntityRenderer(PazEntities.ZOMBIE_DRONE) { BlueprintMachineRenderer(it, ZombieDroneModel(it.bakeLayer(ZombieDroneModel.LAYER_LOCATION))) }
        event.registerEntityRenderer(PazEntities.LAWN_MOWER) { BlueprintMachineRenderer(it, LawnMowerModel(it.bakeLayer(LawnMowerModel.LAYER_LOCATION))) }

        event.registerEntityRenderer(PazEntities.GNOME) { GnomeRenderer(it) }

        event.registerEntityRenderer(PazEntities.PLANT_POT_MINECART) { PlantPotMinecartRenderer(it, ModelLayers.MINECART) }
        event.registerEntityRenderer(PazEntities.SUN) { SunRenderer(it) }
        event.registerEntityRenderer(PazEntities.THROWN_SUN_BOTTLE) { ThrownItemRenderer(it) }
        event.registerEntityRenderer(PazEntities.BALLOON) { BalloonRenderer(it) }

        event.registerBlockEntityRenderer(PazBlocks.FLAG_BLOCK_ENTITY) { FlagRenderer(FlagBlockModel(it.bakeLayer(FlagBlockModel.LAYER_LOCATION))) }
        event.registerBlockEntityRenderer(PazBlocks.GARDEN_GNOME_ENTITY) { GardenGnomeBlockRenderer(GnomeModel(it.bakeLayer(GnomeModel.LAYER_LOCATION))) }
        event.registerBlockEntityRenderer(PazBlocks.SUN_BATTERY_BLOCK_ENTITY) { SunBatteryRenderer() }
        event.registerBlockEntityRenderer(PazBlocks.TIME_MACHINE_ENTITY) { TimeMachineRenderer() }
        event.registerBlockEntityRenderer(PazBlocks.TIME_PORTAL_ENTITY) { TimePortalRenderer() }
        event.registerBlockEntityRenderer(PazBlocks.MAILBOX_ENTITY) { MailboxRenderer() }
    }
}