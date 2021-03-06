package li.cil.oc

import java.io._
import java.net.Inet4Address
import java.net.InetAddress
import java.util.UUID

import com.google.common.net.InetAddresses
import com.mojang.authlib.GameProfile
import com.typesafe.config._
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.versioning.DefaultArtifactVersion
import cpw.mods.fml.common.versioning.VersionRange
import li.cil.oc.api.component.TextBuffer.ColorDepth
import li.cil.oc.integration.Mods
import org.apache.commons.lang3.StringEscapeUtils

import scala.collection.convert.WrapAsScala._
import scala.io.Codec
import scala.io.Source
import scala.util.matching.Regex

class Settings(val config: Config) {
  // ----------------------------------------------------------------------- //
  // client
  val screenTextFadeStartDistance = config.getDouble("client.screenTextFadeStartDistance")
  val maxScreenTextRenderDistance = config.getDouble("client.maxScreenTextRenderDistance")
  val textLinearFiltering = config.getBoolean("client.textLinearFiltering")
  val textAntiAlias = config.getBoolean("client.textAntiAlias")
  val robotLabels = config.getBoolean("client.robotLabels")
  val soundVolume = config.getDouble("client.soundVolume").toFloat max 0 min 2
  val fontCharScale = config.getDouble("client.fontCharScale") max 0.5 min 2
  val hologramFadeStartDistance = config.getDouble("client.hologramFadeStartDistance") max 0
  val hologramRenderDistance = config.getDouble("client.hologramRenderDistance") max 0
  val hologramFlickerFrequency = config.getDouble("client.hologramFlickerFrequency") max 0
  val hologramMaxScaleByTier = Array(config.getDoubleList("client.hologramMaxScale"): _*) match {
    case Array(tier1, tier2) =>
      Array((tier1: Double) max 1.0, (tier2: Double) max 1.0)
    case _ =>
      OpenComputers.log.warn("Bad number of hologram max scales, ignoring.")
      Array(3.0, 4.0)
  }
  val hologramMaxTranslationByTier = Array(config.getDoubleList("client.hologramMaxTranslation"): _*) match {
    case Array(tier1, tier2) =>
      Array((tier1: Double) max 0.0, (tier2: Double) max 0.0)
    case _ =>
      OpenComputers.log.warn("Bad number of hologram max translations, ignoring.")
      Array(0.25, 0.5)
  }
  val monochromeColor = Integer.decode(config.getString("client.monochromeColor"))
  val fontRenderer = config.getString("client.fontRenderer")
  val beepSampleRate = config.getInt("client.beepSampleRate")
  val beepAmplitude = config.getInt("client.beepVolume") max 0 min Byte.MaxValue

  // ----------------------------------------------------------------------- //
  // computer
  val threads = config.getInt("computer.threads") max 1
  val timeout = config.getDouble("computer.timeout") max 0
  val startupDelay = config.getDouble("computer.startupDelay") max 0.05
  val eepromSize = config.getInt("computer.eepromSize") max 0
  val ramSizes = Array(config.getIntList("computer.ramSizes"): _*) match {
    case Array(tier1, tier2, tier3, tier4, tier5, tier6) =>
      Array(tier1: Int, tier2: Int, tier3: Int, tier4: Int, tier5: Int, tier6: Int)
    case _ =>
      OpenComputers.log.warn("Bad number of RAM sizes, ignoring.")
      Array(192, 256, 384, 512, 768, 1024)
  }
  val ramScaleFor64Bit = config.getDouble("computer.ramScaleFor64Bit") max 1
  val cpuComponentSupport = Array(config.getIntList("computer.cpuComponentCount"): _*) match {
    case Array(tier1, tier2, tier3) =>
      Array(tier1: Int, tier2: Int, tier3: Int)
    case _ =>
      OpenComputers.log.warn("Bad number of CPU component counts, ignoring.")
      Array(8, 12, 16)
  }
  val callBudgets = Array(config.getDoubleList("computer.callBudgets"): _*) match {
    case Array(tier1, tier2, tier3) =>
      Array(tier1: Double, tier2: Double, tier3: Double)
    case _ =>
      OpenComputers.log.warn("Bad number of CPU call budgets, ignoring.")
      Array(0.5, 1.0, 1.5)
  }
  val canComputersBeOwned = config.getBoolean("computer.canComputersBeOwned")
  val maxUsers = config.getInt("computer.maxUsers") max 0
  val maxUsernameLength = config.getInt("computer.maxUsernameLength") max 0
  val eraseTmpOnReboot = config.getBoolean("computer.eraseTmpOnReboot")
  val executionDelay = config.getInt("computer.executionDelay") max 0

  // computer.lua
  val allowBytecode = config.getBoolean("computer.lua.allowBytecode")

  // ----------------------------------------------------------------------- //
  // robot
  val allowActivateBlocks = config.getBoolean("robot.allowActivateBlocks")
  val allowUseItemsWithDuration = config.getBoolean("robot.allowUseItemsWithDuration")
  val canAttackPlayers = config.getBoolean("robot.canAttackPlayers")
  val screwCobwebs = config.getBoolean("robot.notAfraidOfSpiders")
  val swingRange = config.getDouble("robot.swingRange")
  val useAndPlaceRange = config.getDouble("robot.useAndPlaceRange")
  val itemDamageRate = config.getDouble("robot.itemDamageRate") max 0 min 1
  val nameFormat = config.getString("robot.nameFormat")
  val uuidFormat = config.getString("robot.uuidFormat")

  // robot.xp
  val baseXpToLevel = config.getDouble("robot.xp.baseValue") max 0
  val constantXpGrowth = config.getDouble("robot.xp.constantGrowth") max 1
  val exponentialXpGrowth = config.getDouble("robot.xp.exponentialGrowth") max 1
  val robotActionXp = config.getDouble("robot.xp.actionXp") max 0
  val robotExhaustionXpRate = config.getDouble("robot.xp.exhaustionXpRate") max 0
  val robotOreXpRate = config.getDouble("robot.xp.oreXpRate") max 0
  val bufferPerLevel = config.getDouble("robot.xp.bufferPerLevel") max 0
  val toolEfficiencyPerLevel = config.getDouble("robot.xp.toolEfficiencyPerLevel") max 0
  val harvestSpeedBoostPerLevel = config.getDouble("robot.xp.harvestSpeedBoostPerLevel") max 0

  // ----------------------------------------------------------------------- //
  // robot.delays

  // Note: all delays are reduced by one tick to account for the tick they are
  // performed in (since all actions are delegated to the server thread).
  val turnDelay = (config.getDouble("robot.delays.turn") - 0.06) max 0.05
  val moveDelay = (config.getDouble("robot.delays.move") - 0.06) max 0.05
  val swingDelay = (config.getDouble("robot.delays.swing") - 0.06) max 0
  val useDelay = (config.getDouble("robot.delays.use") - 0.06) max 0
  val placeDelay = (config.getDouble("robot.delays.place") - 0.06) max 0
  val dropDelay = (config.getDouble("robot.delays.drop") - 0.06) max 0
  val suckDelay = (config.getDouble("robot.delays.suck") - 0.06) max 0
  val harvestRatio = config.getDouble("robot.delays.harvestRatio") max 0

  // ----------------------------------------------------------------------- //
  // power
  val pureIgnorePower = config.getBoolean("power.ignorePower")
  lazy val ignorePower = pureIgnorePower || !Mods.isPowerProvidingModPresent
  val tickFrequency = config.getDouble("power.tickFrequency") max 1
  val chargeRateExternal = config.getDouble("power.chargerChargeRate")
  val chargeRateTablet = config.getDouble("power.chargerChargeRateTablet")
  val generatorEfficiency = config.getDouble("power.generatorEfficiency")
  val solarGeneratorEfficiency = config.getDouble("power.solarGeneratorEfficiency")
  val assemblerTickAmount = config.getDouble("power.assemblerTickAmount") max 1
  val disassemblerTickAmount = config.getDouble("power.disassemblerTickAmount") max 1
  val powerModBlacklist = config.getStringList("power.modBlacklist")

  // power.buffer
  val bufferCapacitor = config.getDouble("power.buffer.capacitor") max 0
  val bufferCapacitorAdjacencyBonus = config.getDouble("power.buffer.capacitorAdjacencyBonus") max 0
  val bufferComputer = config.getDouble("power.buffer.computer") max 0
  val bufferRobot = config.getDouble("power.buffer.robot") max 0
  val bufferConverter = config.getDouble("power.buffer.converter") max 0
  val bufferDistributor = config.getDouble("power.buffer.distributor") max 0
  val bufferCapacitorUpgrades = Array(config.getDoubleList("power.buffer.batteryUpgrades"): _*) match {
    case Array(tier1, tier2, tier3) =>
      Array(tier1: Double, tier2: Double, tier3: Double)
    case _ =>
      OpenComputers.log.warn("Bad number of battery upgrade buffer sizes, ignoring.")
      Array(10000.0, 15000.0, 20000.0)
  }
  val bufferTablet = config.getDouble("power.buffer.tablet") max 0
  val bufferAccessPoint = config.getDouble("power.buffer.accessPoint") max 0
  val bufferDrone = config.getDouble("power.buffer.drone") max 0
  val bufferMicrocontroller = config.getDouble("power.buffer.mcu") max 0

  // power.cost
  val computerCost = config.getDouble("power.cost.computer") max 0
  val microcontrollerCost = config.getDouble("power.cost.microcontroller") max 0
  val robotCost = config.getDouble("power.cost.robot") max 0
  val droneCost = config.getDouble("power.cost.drone") max 0
  val sleepCostFactor = config.getDouble("power.cost.sleepFactor") max 0
  val screenCost = config.getDouble("power.cost.screen") max 0
  val hologramCost = config.getDouble("power.cost.hologram") max 0
  val hddReadCost = (config.getDouble("power.cost.hddRead") max 0) / 1024
  val hddWriteCost = (config.getDouble("power.cost.hddWrite") max 0) / 1024
  val gpuSetCost = (config.getDouble("power.cost.gpuSet") max 0) / Settings.basicScreenPixels
  val gpuFillCost = (config.getDouble("power.cost.gpuFill") max 0) / Settings.basicScreenPixels
  val gpuClearCost = (config.getDouble("power.cost.gpuClear") max 0) / Settings.basicScreenPixels
  val gpuCopyCost = (config.getDouble("power.cost.gpuCopy") max 0) / Settings.basicScreenPixels
  val robotTurnCost = config.getDouble("power.cost.robotTurn") max 0
  val robotMoveCost = config.getDouble("power.cost.robotMove") max 0
  val robotExhaustionCost = config.getDouble("power.cost.robotExhaustion") max 0
  val wirelessCostPerRange = config.getDouble("power.cost.wirelessCostPerRange") max 0
  val abstractBusPacketCost = config.getDouble("power.cost.abstractBusPacket") max 0
  val geolyzerScanCost = config.getDouble("power.cost.geolyzerScan") max 0
  val robotBaseCost = config.getDouble("power.cost.robotAssemblyBase") max 0
  val robotComplexityCost = config.getDouble("power.cost.robotAssemblyComplexity") max 0
  val microcontrollerBaseCost = config.getDouble("power.cost.microcontrollerAssemblyBase") max 0
  val microcontrollerComplexityCost = config.getDouble("power.cost.microcontrollerAssemblyComplexity") max 0
  val tabletBaseCost = config.getDouble("power.cost.tabletAssemblyBase") max 0
  val tabletComplexityCost = config.getDouble("power.cost.tabletAssemblyComplexity") max 0
  val droneBaseCost = config.getDouble("power.cost.droneAssemblyBase") max 0
  val droneComplexityCost = config.getDouble("power.cost.droneAssemblyComplexity") max 0
  val disassemblerItemCost = config.getDouble("power.cost.disassemblerPerItem") max 0
  val chunkloaderCost = config.getDouble("power.cost.chunkloaderCost") max 0
  val pistonCost = config.getDouble("power.cost.pistonPush") max 0
  val eepromWriteCost = config.getDouble("power.cost.eepromWrite") max 0

  // power.rate
  val accessPointRate = config.getDouble("power.rate.accessPoint") max 0
  val assemblerRate = config.getDouble("power.rate.assembler") max 0
  val caseRate = (Array(config.getDoubleList("power.rate.case"): _*) match {
    case Array(tier1, tier2, tier3) =>
      Array(tier1: Double, tier2: Double, tier3: Double)
    case _ =>
      OpenComputers.log.warn("Bad number of computer case conversion rates, ignoring.")
      Array(5.0, 10.0, 20.0)
  }) ++ Array(9001.0)
  // Creative case.
  val chargerRate = config.getDouble("power.rate.charger") max 0
  val disassemblerRate = config.getDouble("power.rate.disassembler") max 0
  val powerConverterRate = config.getDouble("power.rate.powerConverter") max 0
  val serverRackRate = config.getDouble("power.rate.serverRack") max 0

  // power.value
  private val valueAppliedEnergistics2 = config.getDouble("power.value.AppliedEnergistics2")
  private val valueBuildCraft = config.getDouble("power.value.BuildCraft")
  private val valueFactorization = config.getDouble("power.value.Factorization")
  private val valueGalacticraft = config.getDouble("power.value.Galacticraft")
  private val valueIndustrialCraft2 = config.getDouble("power.value.IndustrialCraft2")
  private val valueMekanism = config.getDouble("power.value.Mekanism")
  private val valueRedstoneFlux = config.getDouble("power.value.RedstoneFlux")
  private val valueResonantEngine = config.getDouble("power.value.ResonantEngine")

  private val valueInternal = valueBuildCraft

  val ratioAppliedEnergistics2 = valueAppliedEnergistics2 / valueInternal
  val ratioBuildCraft = valueBuildCraft / valueInternal
  val ratioFactorization = valueFactorization / valueInternal
  val ratioGalacticraft = valueGalacticraft / valueInternal
  val ratioIndustrialCraft2 = valueIndustrialCraft2 / valueInternal
  val ratioMekanism = valueMekanism / valueInternal
  val ratioRedstoneFlux = valueRedstoneFlux / valueInternal
  val ratioResonantEngine = valueResonantEngine / valueInternal

  // ----------------------------------------------------------------------- //
  // filesystem
  val fileCost = config.getInt("filesystem.fileCost") max 0
  val bufferChanges = config.getBoolean("filesystem.bufferChanges")
  val hddSizes = Array(config.getIntList("filesystem.hddSizes"): _*) match {
    case Array(tier1, tier2, tier3) =>
      Array(tier1: Int, tier2: Int, tier3: Int)
    case _ =>
      OpenComputers.log.warn("Bad number of HDD sizes, ignoring.")
      Array(1024, 2048, 4096)
  }
  val floppySize = config.getInt("filesystem.floppySize") max 0
  val tmpSize = config.getInt("filesystem.tmpSize") max 0
  val maxHandles = config.getInt("filesystem.maxHandles") max 0
  val maxReadBuffer = config.getInt("filesystem.maxReadBuffer") max 0

  // ----------------------------------------------------------------------- //
  // internet
  val httpEnabled = config.getBoolean("internet.enableHttp")
  val tcpEnabled = config.getBoolean("internet.enableTcp")
  val httpHostBlacklist = Array(config.getStringList("internet.blacklist").map(new Settings.AddressValidator(_)): _*)
  val httpHostWhitelist = Array(config.getStringList("internet.whitelist").map(new Settings.AddressValidator(_)): _*)
  val httpTimeout = (config.getInt("internet.requestTimeout") max 0) * 1000
  val httpMaxDownloadSize = config.getInt("internet.requestMaxDownloadSize") max 0
  val maxConnections = config.getInt("internet.maxTcpConnections") max 0
  val internetThreads = config.getInt("internet.threads") max 1

  // ----------------------------------------------------------------------- //
  // switch
  val switchDefaultMaxQueueSize = config.getInt("switch.defaultMaxQueueSize") max 1
  val switchQueueSizeUpgrade = config.getInt("switch.queueSizeUpgrade") max 0
  val switchDefaultRelayDelay = config.getInt("switch.defaultRelayDelay") max 1
  val switchRelayDelayUpgrade = config.getInt("switch.relayDelayUpgrade") max 0
  val switchDefaultRelayAmount = config.getInt("switch.defaultRelayAmount") max 1
  val switchRelayAmountUpgrade = config.getInt("switch.relayAmountUpgrade") max 0

  // ----------------------------------------------------------------------- //
  // misc
  val maxScreenWidth = config.getInt("misc.maxScreenWidth") max 1
  val maxScreenHeight = config.getInt("misc.maxScreenHeight") max 1
  val inputUsername = config.getBoolean("misc.inputUsername")
  val maxClipboard = config.getInt("misc.maxClipboard") max 0
  val maxNetworkPacketSize = config.getInt("misc.maxNetworkPacketSize") max 0
  val maxNetworkPacketParts = config.getInt("misc.maxNetworkPacketParts") max 0
  val maxOpenPorts = config.getInt("misc.maxOpenPorts") max 0
  val maxWirelessRange = config.getDouble("misc.maxWirelessRange") max 0
  val rTreeMaxEntries = 10
  val terminalsPerTier = Array(config.getIntList("misc.terminalsPerTier"): _*) match {
    case Array(tier1, tier2, tier3) =>
      Array(math.max(tier1, 1), math.max(tier2, 1), math.max(tier3, 1))
    case _ =>
      OpenComputers.log.warn("Bad number of Remote Terminal counts, ignoring.")
      Array(2, 4, 8)
  }
  val updateCheck = config.getBoolean("misc.updateCheck")
  val lootProbability = config.getInt("misc.lootProbability")
  val geolyzerRange = config.getInt("misc.geolyzerRange")
  val geolyzerNoise = config.getDouble("misc.geolyzerNoise").toFloat max 0
  val disassembleAllTheThings = config.getBoolean("misc.disassembleAllTheThings")
  val disassemblerBreakChance = config.getDouble("misc.disassemblerBreakChance") max 0 min 1
  val hideOwnPet = config.getBoolean("misc.hideOwnSpecial")
  val allowItemStackInspection = config.getBoolean("misc.allowItemStackInspection")
  val databaseEntriesPerTier = Array(9, 25, 81) // Not configurable because of GUI design.
  val presentChance = config.getDouble("misc.presentChance") max 0 min 1
  val assemblerBlacklist = config.getStringList("misc.assemblerBlacklist")

  // ----------------------------------------------------------------------- //
  // integration
  val modBlacklist = config.getStringList("integration.modBlacklist")
  val peripheralBlacklist = config.getStringList("integration.peripheralBlacklist")
  val fakePlayerUuid = config.getString("integration.fakePlayerUuid")
  val fakePlayerName = config.getString("integration.fakePlayerName")
  val fakePlayerProfile = new GameProfile(UUID.fromString(fakePlayerUuid), fakePlayerName)

  // integration.vanilla
  val enableInventoryDriver = config.getBoolean("integration.vanilla.enableInventoryDriver")
  val enableTankDriver = config.getBoolean("integration.vanilla.enableTankDriver")
  val enableCommandBlockDriver = config.getBoolean("integration.vanilla.enableCommandBlockDriver")
  val allowItemStackNBTTags = config.getBoolean("integration.vanilla.allowItemStackNBTTags")

  // ----------------------------------------------------------------------- //
  // debug
  val logLuaCallbackErrors = config.getBoolean("debug.logCallbackErrors")
  val forceLuaJ = config.getBoolean("debug.forceLuaJ")
  val allowUserdata = !config.getBoolean("debug.disableUserdata")
  val allowPersistence = !config.getBoolean("debug.disablePersistence")
  val limitMemory = !config.getBoolean("debug.disableMemoryLimit")
  val forceCaseInsensitive = config.getBoolean("debug.forceCaseInsensitiveFS")
  val logFullLibLoadErrors = config.getBoolean("debug.logFullNativeLibLoadErrors")
  val forceNativeLib = config.getString("debug.forceNativeLibWithName")
  val logOpenGLErrors = config.getBoolean("debug.logOpenGLErrors")
  val logUnifontErrors = config.getBoolean("debug.logUnifontErrors")
  val alwaysTryNative = config.getBoolean("debug.alwaysTryNative")
  val debugPersistence = config.getBoolean("debug.verbosePersistenceErrors")
  val nativeInTmpDir = config.getBoolean("debug.nativeInTmpDir")
  val periodicallyForceLightUpdate = config.getBoolean("debug.periodicallyForceLightUpdate")
  val insertIdsInConverters = config.getBoolean("debug.insertIdsInConverters")
  val enableDebugCard = config.getBoolean("debug.enableDebugCard")
  val registerLuaJArchitecture = config.getBoolean("debug.registerLuaJArchitecture")
}

object Settings {
  val resourceDomain = "opencomputers"
  val namespace = "oc:"
  val savePath = "opencomputers/"
  val scriptPath = "/assets/" + resourceDomain + "/lua/"
  val screenResolutionsByTier = Array((50, 16), (80, 25), (160, 50))
  val screenDepthsByTier = Array(ColorDepth.OneBit, ColorDepth.FourBit, ColorDepth.EightBit)
  val deviceComplexityByTier = Array(12, 24, 32, 9001)
  var rTreeDebugRenderer = false
  var blockRenderId = -1

  def basicScreenPixels = screenResolutionsByTier(0)._1 * screenResolutionsByTier(0)._2

  private var settings: Settings = _

  def get = settings

  def load(file: File) = {
    import scala.compat.Platform.EOL
    // typesafe config's internal method for loading the reference.conf file
    // seems to fail on some systems (as does their parseResource method), so
    // we'll have to load the default config manually. This was reported on the
    // Minecraft Forums, I could not reproduce the issue, but this version has
    // reportedly fixed the problem.
    val defaults = {
      val in = classOf[Settings].getResourceAsStream("/application.conf")
      val config = Source.fromInputStream(in)(Codec.UTF8).getLines().mkString("", EOL, EOL)
      in.close()
      ConfigFactory.parseString(config)
    }
    val config =
      try {
        val plain = Source.fromFile(file)(Codec.UTF8).getLines().mkString("", EOL, EOL)
        val config = patchConfig(ConfigFactory.parseString(plain), defaults).withFallback(defaults)
        settings = new Settings(config.getConfig("opencomputers"))
        config
      }
      catch {
        case e: Throwable =>
          if (file.exists()) {
            OpenComputers.log.warn("Failed loading config, using defaults.", e)
          }
          settings = new Settings(defaults.getConfig("opencomputers"))
          defaults
      }
    try {
      val renderSettings = ConfigRenderOptions.defaults.setJson(false).setOriginComments(false)
      val nl = sys.props("line.separator")
      val nle = StringEscapeUtils.escapeJava(nl)
      val out = new PrintWriter(file)
      out.write(config.root.render(renderSettings).lines.
        // Indent two spaces instead of four.
        map(line => """^(\s*)""".r.replaceAllIn(line, m => Regex.quoteReplacement(m.group(1).replace("  ", " ")))).
        // Finalize the string.
        filter(_ != "").mkString(nl).
        // Newline after values.
        replaceAll(s"((?:\\s*#.*$nle)(?:\\s*[^#\\s].*$nle)+)", "$1" + nl))
      out.close()
    }
    catch {
      case e: Throwable =>
        OpenComputers.log.warn("Failed saving config.", e)
    }
  }

  private val configPatches = Array(
    // Upgrading to version 1.3, increased lower bounds for default RAM sizes
    // and reworked the way black- and whitelisting works (IP based).
    VersionRange.createFromVersionSpec("[0.0,1.3-alpha)") -> Array(
      "computer.ramSizes",
      "internet.blacklist",
      "internet.whitelist"
    ),
    // Upgrading to version 1.3.3, default power consumption of chunk loader
    // reduced as discussed in #447.
    VersionRange.createFromVersionSpec("[1.3.0,1.3.3)") -> Array(
      "power.cost.chunkloaderCost"
    ),
    // Upgrading to version 1.3.4+, computer.debug category was moved to top
    // level debug category for more flexibility and some other settings merged
    // into that new category.
    VersionRange.createFromVersionSpec("1.3.3") -> Array(
      "computer.debug",
      "misc.alwaysTryNative",
      "misc.verbosePersistenceErrors"
    ),
    // Upgrading to version 1.3.5, added forgotten check for item stack,
    // inspection, patch to true to avoid stuff suddenly breaking.
    VersionRange.createFromVersionSpec("1.3.4") -> Array(
      "misc.allowItemStackInspection"
    ),
    // Upgrading to version 1.4.7, reduce default geolyzer noise.
    VersionRange.createFromVersionSpec("[0.0, 1.4.7)") -> Array(
      "misc.geolyzerNoise"
    )
  )

  // Checks the config version (i.e. the version of the mod the config was
  // created by) against the current version to see if some hard changes
  // were made. If so, the new default values are copied over.
  private def patchConfig(config: Config, defaults: Config) = {
    val mod = Loader.instance.activeModContainer
    val prefix = "opencomputers."
    val configVersion = new DefaultArtifactVersion(if (config.hasPath(prefix + "version")) config.getString(prefix + "version") else "0.0.0")
    var patched = config
    if (configVersion.compareTo(mod.getProcessedVersion) != 0) {
      OpenComputers.log.info(s"Updating config from version '${configVersion.getVersionString}' to '${defaults.getString(prefix + "version")}'.")
      patched = patched.withValue(prefix + "version", defaults.getValue(prefix + "version"))
      for ((version, paths) <- configPatches if version.containsVersion(configVersion)) {
        for (path <- paths) {
          val fullPath = prefix + path
          OpenComputers.log.info(s"Updating setting '$fullPath'. ")
          if (defaults.hasPath(fullPath)) {
            patched = patched.withValue(fullPath, defaults.getValue(fullPath))
          }
          else {
            patched = patched.withoutPath(fullPath)
          }
        }
      }
    }
    patched
  }

  val cidrPattern = """(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(?:/(\d{1,2}))""".r

  class AddressValidator(val value: String) {
    val validator = try cidrPattern.findFirstIn(value) match {
      case Some(cidrPattern(address, prefix)) =>
        val addr = InetAddresses.coerceToInteger(InetAddresses.forString(address))
        val mask = 0xFFFFFFFF << (32 - prefix.toInt)
        val min = addr & mask
        val max = min | ~mask
        (inetAddress: InetAddress, host: String) => inetAddress match {
          case v4: Inet4Address =>
            val numeric = InetAddresses.coerceToInteger(v4)
            min <= numeric && numeric <= max
          case _ => true // Can't check IPv6 addresses so we pass them.
        }
      case _ =>
        val address = InetAddress.getByName(value)
        (inetAddress: InetAddress, host: String) => host == value || inetAddress == address
    } catch {
      case t: Throwable =>
        OpenComputers.log.warn("Invalid entry in internet blacklist / whitelist: " + value, t)
        (inetAddress: InetAddress, host: String) => true
    }

    def apply(inetAddress: InetAddress, host: String) = validator(inetAddress, host)
  }

}