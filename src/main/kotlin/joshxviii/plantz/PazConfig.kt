package joshxviii.plantz

import com.google.gson.GsonBuilder
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.fml.loading.FMLPaths
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntityType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.reader
import kotlin.io.path.writer
import kotlin.math.pow

data class ServerConfig(
    var coopPlanting: Boolean = true,
    var playerCreditForPlantKills: Boolean = false,
    var seedGrowTime: Int = 12000,
    var extraGrowTimePerSun: Int = 2400,
    var zenPotTimeReduction: Double = 0.25,
    var hydrationSunReduction: Double = 0.5,
    var plantPotDamageReduction: Double = 0.5,
    var poweredUpCooldownReduction: Double = 0.2,
    var coffeeBuffDuration: Int = 48_000,
    var pumpkinArmorAmount: Int = 35,
    var sunCostTamingThreshold: Int = 30,
    var plantCooldownEnabled: Boolean = false,
    var plantCooldownTime: Double = 4.0,
    var plantCooldownTimePerSun: Double = 2.5,
    var solarBatteryMax: Int = 512,
    var defaultPlantEntityLimit: Int = 30,
    var customPlantEntityLimits: MutableMap<String, Int> = mutableMapOf(
        "sproutsquad:melonpult"              to 20,
        "sproutsquad:winter_melon"           to 20,
        "sproutsquad:doomshroom"             to 20,
        "sproutsquad:coffeebean"             to -1,
        "sproutsquad:grave_buster"           to -1,
    ),
    var alloyCosts: MutableMap<String, Int> = mutableMapOf(
        "sproutsquad:zombie_turret"          to 12,
        "sproutsquad:zombie_drone"           to 8,
        "sproutsquad:electro_turret"         to 10,
        "sproutsquad:lawn_mower"             to 14,
    ),
    var sunCosts: MutableMap<String, Int> = mutableMapOf(
        "sproutsquad:sunflower"              to 5,
        "sproutsquad:marigold"               to 5,
        "sproutsquad:peashooter"             to 5,
        "sproutsquad:wallnut"                to 5,
        "sproutsquad:explode_o_nut"          to 5,
        "sproutsquad:chomper"                to 7,
        "sproutsquad:cherrybomb"             to 7,
        "sproutsquad:potatomine"             to 3,
        "sproutsquad:repeater"               to 7,
        "sproutsquad:ice_peashooter"         to 7,
        "sproutsquad:fire_peashooter"        to 7,
        "sproutsquad:electric_peashooter"    to 8,
        "sproutsquad:cactus"                 to 6,
        "sproutsquad:lightning_reed"         to 8,
        "sproutsquad:cabbagepult"            to 5,
        "sproutsquad:kernelpult"             to 6,
        "sproutsquad:melonpult"              to 10,
        "sproutsquad:winter_melon"           to 12,
        "sproutsquad:bonkchoy"               to 4,
        "sproutsquad:tanglekelp"             to 2,
        "sproutsquad:puffshroom"             to 0,
        "sproutsquad:scaredyshroom"          to 3,
        "sproutsquad:fumeshroom"             to 6,
        "sproutsquad:sunshroom"              to 6,
        "sproutsquad:hypnoshroom"            to 7,
        "sproutsquad:iceshroom"              to 5,
        "sproutsquad:doomshroom"             to 14,
        "sproutsquad:seashroom"              to 0,
        "sproutsquad:coffeebean"             to 2,
        "sproutsquad:grave_buster"           to 3,
        "sproutsquad:pumpkin"                to 6,
        "sproutsquad:squash"                 to 4
    ),
    var graveStoneActivationRange: Double = 24.0,
    var enableExperimentalFeatures: Boolean = false
)
data class ClientConfig(
    var showDebugInfo: Boolean = false,
    var splitItemTabs: Boolean = false,
    var showRaidUi: Boolean = true,
    var showSunInfo: Boolean = false
)

object PazConfig {

    const val SERVER_CONFIG_PATH = "sproutsquad/server.json"
    const val CLIENT_CONFIG_PATH = "sproutsquad/client.json"

    val GSON = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    private val serverConfigPath: Path = FMLPaths.CONFIGDIR.get().resolve(SERVER_CONFIG_PATH)
    private val clientConfigPath: Path = FMLPaths.CONFIGDIR.get().resolve(CLIENT_CONFIG_PATH)

    private val defaultServerConfig = ServerConfig()
    var server = defaultServerConfig

    private val defaultClientConfig = ClientConfig()
    var client = defaultClientConfig

    fun load() {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            client = loadConfig(clientConfigPath, ClientConfig())
        }
        server = loadConfig(serverConfigPath, ServerConfig())
    }

    fun saveClient() = saveConfig(clientConfigPath, client)
    fun saveServer() = saveConfig(serverConfigPath, server)

    private inline fun <reified T> loadConfig(path: Path, default: T): T {
        return if (path.exists()) {
            try {
                path.reader().use { GSON.fromJson(it, T::class.java) } ?: default
            } catch (e: Exception) {
                PazMain.LOGGER.error("Failed to load config", e)
                default
            }
        } else default.also { saveConfig(path, it) }
    }

    private inline fun <reified T> saveConfig(path: Path, config: T) {
        try {
            Files.createDirectories(path.parent)
            path.writer().use { GSON.toJson(config, it) }
        } catch (exception: Exception) {
            PazMain.LOGGER.error("Failed to save plantz config.", exception)
        }
    }

    val SHOW_DEBUG_INFO: Boolean
        get() = client.showDebugInfo

    val SPLIT_ITEM_TABS: Boolean
        get() = client.splitItemTabs

    val SHOW_RAID_UI: Boolean
        get() = client.showRaidUi

    val SHOW_SUN_INFO: Boolean
        get() = client.showSunInfo


    val COFFEE_BUFF_DURATION: Int
        get() = server.coffeeBuffDuration.coerceAtLeast(0)

    val PUMPKIN_ARMOR_AMOUNT: Int
        get() = server.pumpkinArmorAmount.coerceAtLeast(0)

    val SUN_BATTERY_MAX: Int
        get() = server.solarBatteryMax.coerceAtLeast(0)

    val COOP_PLANTING: Boolean
        get() = server.coopPlanting

    val PLAYER_CREDIT_FOR_PLANT_KILLS: Boolean
        get() = server.playerCreditForPlantKills

    val HYDRATION_SUN_REDUCTION: Double
        get() = 1f - server.hydrationSunReduction.coerceIn(0.0, 1.0)

    val PLANT_POT_DAMAGE_REDUCTION: Double
        get() = 1f - server.plantPotDamageReduction.coerceIn(0.0, 1.0)

    val POWERED_UP_COOLDOWN_REDUCTION: Double
        get() = 1f - server.poweredUpCooldownReduction.coerceIn(0.0, 1.0)

    val PLANT_COOLDOWN_ENABLED: Boolean
        get() = server.plantCooldownEnabled

    val GRAVESTONE_ACTIVATION_RANGE: Double
        get() = server.graveStoneActivationRange

    val EXPERIMENTAL_FEATURES_ENABLED: Boolean
        get() = server.enableExperimentalFeatures

    fun getGrowTime(sunCost: Int, zenBuff: Boolean): Int {
        val time = server.seedGrowTime.coerceAtLeast(0) + (sunCost * server.extraGrowTimePerSun.coerceAtLeast(0))
        return if (zenBuff) (time * (1f - server.zenPotTimeReduction.coerceIn(0.0, 1.0))).toInt() else time
    }

    fun getCooldownTime(sunCost: Int): Float {
        val time = server.plantCooldownTime + (sunCost * server.plantCooldownTimePerSun)
        return time.toFloat().coerceAtLeast(1f)
    }

    fun getSunCost(type: EntityType<*>?): Int {
        val id = type?.let { BuiltInRegistries.ENTITY_TYPE.getKey(it) }
        if (id == null) return 0
        val key = id.toString()
        val value = server.sunCosts[key]?:// config
        defaultServerConfig.sunCosts[key]?.let {// default
            putDefaultSunCost(id, it)
            it
        }
        ?: -1 // not in id list
        return value.coerceAtLeast(-1)
    }

    fun getAlloyCost(type: EntityType<*>?): Int {
        val id = type?.let { BuiltInRegistries.ENTITY_TYPE.getKey(it) }
        if (id == null) return 0
        val key = id.toString()
        val value = server.alloyCosts[key]?:// config
        defaultServerConfig.alloyCosts[key]?.let {// default
            putDefaultAlloyCost(id, it)
            it
        }
        ?: -1 // not in id list
        return value.coerceAtLeast(-1)
    }

    fun getEntityLimit(type: EntityType<*>?): Int {
        val id = type?.let { BuiltInRegistries.ENTITY_TYPE.getKey(it) }
        val default = server.defaultPlantEntityLimit
        val key = id?.toString()?: return default
        val value = server.customPlantEntityLimits[key]?: default
        return value.coerceAtLeast(-1)
    }

    fun getTameChance(type: EntityType<*>?): Double {
        val a = server.sunCostTamingThreshold
        val sunCost = getSunCost(type).coerceAtMost(a)
        val weight = 3 // higher weight = harder to tame
        val chance = ((a - sunCost).toDouble() / (a + sunCost).toDouble()).pow(weight).coerceIn(0.02, 1.0)
        return chance
    }

    /** Default sun cost for plants registered outside the original mod (Sprout Squad's own characters). */
    fun registerDefaultSunCost(entityId: String, sunCost: Int) {
        defaultServerConfig.sunCosts.putIfAbsent(entityId, sunCost)
    }

    fun putDefaultSunCost(entityId: Identifier, sunCost: Int) {
        server.sunCosts.putIfAbsent(entityId.toString(), sunCost.coerceAtLeast(0))
        saveServer()
    }

    fun putDefaultAlloyCost(entityId: Identifier, alloyCost: Int) {
        server.alloyCosts.putIfAbsent(entityId.toString(), alloyCost.coerceAtLeast(0))
        saveServer()
    }
}