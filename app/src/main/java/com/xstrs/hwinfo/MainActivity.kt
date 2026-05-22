package com.xstrs.hwinfo

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.RandomAccessFile
import java.text.DecimalFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 状态栏与导航栏沉浸式设计
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // 智能检测深浅色主题
        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val bgColor = if (isDark) Color.parseColor("#121212") else Color.parseColor("#F3F4F6")
        val cardBgColor = if (isDark) Color.parseColor("#1C1C1E") else Color.WHITE
        val primaryTextColor = if (isDark) Color.parseColor("#F5F5F7") else Color.parseColor("#1D1D1F")
        val secondaryTextColor = if (isDark) Color.parseColor("#8E8E93") else Color.parseColor("#6E6E73")
        val accentColor = if (isDark) Color.parseColor("#A8C7FA") else Color.parseColor("#0B57D0")
        val dividerColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E7EB")

        // 页面总容器
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dpToPx(20), dpToPx(54), dpToPx(20), dpToPx(24))
        }

        // 顶部大标题与小图标容器
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(4))
        }

        // 模拟 App 的精美圆角 Badge 作为装饰图标
        val appBadge = TextView(this).apply {
            text = "ℹ️"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            background = GradientDrawable().apply {
                setColor(accentColor)
                cornerRadius = dpToPx(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(42)).apply {
                setMargins(0, 0, dpToPx(12), 0)
            }
        }
        headerLayout.addView(appBadge)

        // 标题与副标题文字
        val titleTextLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleView = TextView(this).apply {
            text = "硬件信息"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primaryTextColor)
        }
        titleTextLayout.addView(titleView)

        val subtitleView = TextView(this).apply {
            text = "Hardware Info v1.0 • Material 3"
            textSize = 12f
            setTextColor(secondaryTextColor)
            setTypeface(null, Typeface.BOLD)
        }
        titleTextLayout.addView(subtitleView)

        headerLayout.addView(titleTextLayout)
        rootLayout.addView(headerLayout)

        // 占位空隙
        val space = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(16))
        }
        rootLayout.addView(space)

        // 滚动容器
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            isVerticalScrollBarEnabled = false
        }

        val scrollContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 获取并组装所有硬件分类数据
        val categories = listOf(
            buildDeviceCategory(),
            buildSystemCategory(),
            buildCpuCategory(),
            buildMemoryCategory(),
            buildDisplayCategory(),
            buildBatteryCategory(),
            buildNetworkCategory(),
            buildSensorCategory()
        )

        // 动态循环渲染每个卡片
        for (cat in categories) {
            // 分类标题（带彩色左边小药丸装饰）
            val categoryHeader = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(4), dpToPx(16), 0, dpToPx(8))
            }

            // 装饰小药丸
            val indicator = View(this).apply {
                background = GradientDrawable().apply {
                    setColor(accentColor)
                    cornerRadius = dpToPx(4).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(dpToPx(4), dpToPx(16)).apply {
                    setMargins(0, 0, dpToPx(8), 0)
                }
            }
            categoryHeader.addView(indicator)

            val headerTextView = TextView(this).apply {
                text = cat.title
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(accentColor)
            }
            categoryHeader.addView(headerTextView)
            scrollContainer.addView(categoryHeader)

            // 卡片容器
            val cardLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))

                background = GradientDrawable().apply {
                    setColor(cardBgColor)
                    cornerRadius = dpToPx(16).toFloat()
                    if (!isDark) {
                        setStroke(dpToPx(1), Color.parseColor("#E5E7EB"))
                    }
                }

                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dpToPx(12))
                }
            }

            // 往卡片内填充具体的信息条目
            for (i in cat.items.indices) {
                val item = cat.items[i]
                val itemLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dpToPx(12), 0, dpToPx(12))
                    weightSum = 10f
                }

                val keyView = TextView(this).apply {
                    text = item.key
                    textSize = 14f
                    setTextColor(secondaryTextColor)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 4.5f)
                }

                val valView = TextView(this).apply {
                    text = item.value
                    textSize = 14f
                    setTextColor(primaryTextColor)
                    gravity = Gravity.END
                    setTypeface(null, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 5.5f)
                }

                itemLayout.addView(keyView)
                itemLayout.addView(valView)
                cardLayout.addView(itemLayout)

                // 在项目条目之间添加一条极细的分隔线（最后一项不要分隔线）
                if (i < cat.items.size - 1) {
                    val divider = View(this).apply {
                        setBackgroundColor(dividerColor)
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1))
                    }
                    cardLayout.addView(divider)
                }
            }
            scrollContainer.addView(cardLayout)
        }

        scrollView.addView(scrollContainer)
        rootLayout.addView(scrollView)
        setContentView(rootLayout)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    data class Category(val title: String, val items: List<InfoItem>)
    data class InfoItem(val key: String, val value: String)

    private fun formatSize(bytes: Long): String {
        val df = DecimalFormat("#.##")
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            bytes < kb -> "$bytes B"
            bytes < mb -> "${df.format(bytes / kb)} KB"
            bytes < gb -> "${df.format(bytes / mb)} MB"
            else -> "${df.format(bytes / gb)} GB"
        }
    }

    // --- 数据采集逻辑 ---

    private fun buildDeviceCategory(): Category {
        val items = mutableListOf<InfoItem>()
        items.add(InfoItem("品牌 (Brand)", Build.BRAND))
        items.add(InfoItem("机型 (Model)", Build.MODEL))
        items.add(InfoItem("制造商 (Manufacturer)", Build.MANUFACTURER))
        items.add(InfoItem("主板 (Board)", Build.BOARD))
        items.add(InfoItem("硬件代号 (Hardware)", Build.HARDWARE))
        items.add(InfoItem("设备代号 (Device)", Build.DEVICE))
        return Category("📱 设备基本信息", items)
    }

    private fun buildSystemCategory(): Category {
        val items = mutableListOf<InfoItem>()
        items.add(InfoItem("系统版本 (OS)", "Android ${Build.VERSION.RELEASE}"))
        items.add(InfoItem("API 等级 (SDK)", Build.VERSION.SDK_INT.toString()))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            items.add(InfoItem("安全补丁 (Security)", Build.VERSION.SECURITY_PATCH))
        }
        items.add(InfoItem("基带版本 (Bootloader)", Build.BOOTLOADER))
        items.add(InfoItem("内核版本 (Kernel)", System.getProperty("os.version") ?: "未知"))
        items.add(InfoItem("已运行时间 (Uptime)", formatUptime()))
        return Category("⚙️ 系统信息", items)
    }

    private fun formatUptime(): String {
        val uptimeMs = SystemClock.elapsedRealtime()
        val secs = uptimeMs / 1000
        val mins = secs / 60
        val hours = mins / 60
        val days = hours / 24
        return "${days}天 ${hours % 24}时 ${mins % 60}分 ${secs % 60}秒"
    }

    private fun buildCpuCategory(): Category {
        val items = mutableListOf<InfoItem>()
        items.add(InfoItem("支持架构 (ABIs)", Build.SUPPORTED_ABIS.joinToString(", ")))
        items.add(InfoItem("核心数量 (Cores)", Runtime.getRuntime().availableProcessors().toString()))
        val maxFreq = getCpuMaxFreq()
        items.add(InfoItem("最大频率 (Max Freq)", if (maxFreq != "未知") "$maxFreq MHz" else "未知"))
        items.add(InfoItem("调度策略 (Governor)", getCpuGovernor()))
        return Category("🧠 处理器 (CPU)", items)
    }

    private fun getCpuMaxFreq(): String {
        return try {
            val reader = RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r")
            val line = reader.readLine()
            reader.close()
            val freqKhz = line.toLong()
            (freqKhz / 1000).toString()
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun getCpuGovernor(): String {
        return try {
            val reader = RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "r")
            val line = reader.readLine()
            reader.close()
            line.trim()
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun buildMemoryCategory(): Category {
        val items = mutableListOf<InfoItem>()
        val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        items.add(InfoItem("运行内存总共 (RAM)", formatSize(memInfo.totalMem)))
        items.add(InfoItem("运行内存可用 (Avail)", formatSize(memInfo.availMem)))

        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        items.add(InfoItem("内置存储总共 (Storage)", formatSize(stat.blockCountLong * blockSize)))
        items.add(InfoItem("内置存储可用 (Avail)", formatSize(stat.availableBlocksLong * blockSize)))
        return Category("💾 内存与存储", items)
    }

    private fun buildDisplayCategory(): Category {
        val items = mutableListOf<InfoItem>()
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)
        val width = dm.widthPixels
        val height = dm.heightPixels
        items.add(InfoItem("屏幕分辨率 (Resolution)", "${width} x ${height}"))
        items.add(InfoItem("屏幕密度 (Density)", "${dm.densityDpi} DPI"))

        val refreshRate = windowManager.defaultDisplay.refreshRate
        items.add(InfoItem("刷新率 (Refresh Rate)", "${String.format(Locale.getDefault(), "%.1f", refreshRate)} Hz"))

        val xDpi = if (dm.xdpi > 0) dm.xdpi else 160f
        val yDpi = if (dm.ydpi > 0) dm.ydpi else 160f
        val x = Math.pow((width / xDpi).toDouble(), 2.0)
        val y = Math.pow((height / yDpi).toDouble(), 2.0)
        val screenInches = Math.sqrt(x + y)
        items.add(InfoItem("估算屏幕尺寸 (Size)", "${String.format(Locale.getDefault(), "%.1f", screenInches)} 英寸"))

        // 追加获取 GLES 版本 (GPU 信息)
        val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val glEsVersion = actManager.deviceConfigurationInfo.glEsVersion
        items.add(InfoItem("OpenGL ES 版本", glEsVersion))

        return Category("🖥️ 屏幕与 GPU", items)
    }

    private fun buildBatteryCategory(): Category {
        val items = mutableListOf<InfoItem>()
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent != null) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val pct = level * 100 / scale.toFloat()
            items.add(InfoItem("当前电量", "${pct.toInt()}%"))

            val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
            items.add(InfoItem("电池温度", "$temp °C"))

            val volt = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            items.add(InfoItem("电池电压", "${volt} mV"))

            val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "未知"
            items.add(InfoItem("电池技术", tech))

            val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "优秀 (Good)"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "温度过高"
                BatteryManager.BATTERY_HEALTH_DEAD -> "损坏 (Dead)"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "电压过高"
                else -> "普通"
            }
            items.add(InfoItem("健康状态", health))
        } else {
            items.add(InfoItem("电池状态", "无法获取"))
        }
        return Category("🔋 电池与电源", items)
    }

    private fun buildNetworkCategory(): Category {
        val items = mutableListOf<InfoItem>()
        var type = "未连接"
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNetwork)
            if (caps != null) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    type = "Wi-Fi 无线网"
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    type = "蜂窝移动网络"
                }
            }
        } catch (e: Exception) {
            type = "获取失败"
        }
        items.add(InfoItem("当前网络连接", type))

        val hasNfc = packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
        items.add(InfoItem("NFC 支持", if (hasNfc) "支持" else "不支持"))

        val hasBluetooth = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        items.add(InfoItem("蓝牙支持", if (hasBluetooth) "支持" else "不支持"))
        return Category("🌐 网络与连接", items)
    }

    private fun buildSensorCategory(): Category {
        val items = mutableListOf<InfoItem>()
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sm.getSensorList(Sensor.TYPE_ALL)

        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        val gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
        val light = sm.getDefaultSensor(Sensor.TYPE_LIGHT) != null

        items.add(InfoItem("加速度传感器", if (accel) "支持" else "不支持"))
        items.add(InfoItem("陀螺仪传感器", if (gyro) "支持" else "不支持"))
        items.add(InfoItem("磁力传感器 (罗盘)", if (mag) "支持" else "不支持"))
        items.add(InfoItem("光线传感器", if (light) "支持" else "不支持"))
        items.add(InfoItem("硬件传感器总数", "${sensors.size} 个"))
        return Category("📡 传感器检测", items)
    }
}