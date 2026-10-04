package com.example

import android.app.WallpaperManager
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.app.ActivityOptions
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent as NativeKeyEvent
import android.view.KeyCharacterMap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import android.widget.Toast
import coil.compose.AsyncImage
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost

    companion object {
        const val APPWIDGET_HOST_ID = 1024
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        try {
            appWidgetManager = AppWidgetManager.getInstance(this)
            appWidgetHost = AppWidgetHost(this, APPWIDGET_HOST_ID)
            appWidgetHost.startListening()
        } catch (e: Exception) {
            Log.e("MainActivity", "Initial setup failed", e)
        }

        setContent {
            MyApplicationTheme {
                UnibloxOSApp(appWidgetHost, appWidgetManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (::appWidgetHost.isInitialized) {
                appWidgetHost.startListening()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting AppWidgetHost", e)
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::appWidgetHost.isInitialized) {
                appWidgetHost.stopListening()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping AppWidgetHost", e)
        }
    }
}

data class AppEntry(
    val label: String,
    val packageName: String,
    val icon: Drawable? = null
)

data class TaskbarAppItem(
    val id: String = UUID.randomUUID().toString(),
    val app: App = App.SystemApp,
    val packageName: String = "",
    val label: String,
    val icon: Any? = null,
    val isPinned: Boolean = true
)

data class DesktopAppItem(
    val id: String,
    val app: App = App.SystemApp,
    val packageName: String = "",
    val label: String,
    val icon: Any? = null,
    val color: Color = Color.White
)

data class WidgetItem(
    val id: String = UUID.randomUUID().toString(),
    val appWidgetId: Int,
    var position: IntOffset = IntOffset(200, 200),
    var size: DpSize = DpSize(250.dp, 150.dp)
)

@Composable
fun rememberInstalledApps(): List<AppEntry> {
    val context = LocalContext.current
    val apps = remember { mutableStateListOf<AppEntry>() }
    val scope = rememberCoroutineScope()

    val refreshApps = {
        scope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            // Switching to getInstalledApplications as requested (query_package logic)
            val appInfos = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val sortedApps = appInfos.mapNotNull { appInfo ->
                // Only include apps that have a launch intent (launcher apps)
                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) {
                    AppEntry(
                        label = appInfo.loadLabel(pm).toString(),
                        packageName = appInfo.packageName,
                        icon = appInfo.loadIcon(pm)
                    )
                } else null
            }.sortedBy { it.label.lowercase() }
            
            launch(Dispatchers.Main) {
                apps.clear()
                apps.addAll(sortedApps)
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshApps()
    }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                refreshApps()
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    return apps
}

enum class Screen {
    Welcome,
    Desktop
}

enum class App {
    None,
    Browser,
    GameEngine,
    Messages,
    Settings,
    Terminal,
    Keyboard,
    SystemApp,
    AppStore,
    WebViewApp
}

data class WindowState(
    val id: String = UUID.randomUUID().toString(),
    val app: App,
    val packageName: String = "",
    val label: String = "",
    val icon: Drawable? = null,
    val isMinimized: Boolean = false,
    val isFullScreen: Boolean = false,
    val position: IntOffset = IntOffset(100, 100),
    val size: IntOffset = IntOffset(0, 0) // 0 means default
)

enum class OSTheme {
    Aero, Metro, Macnostich, Developer, School, Colors
}

@Composable
fun UnibloxOSApp(appWidgetHost: AppWidgetHost, appWidgetManager: AppWidgetManager) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.Welcome) }
    var currentTheme by remember { mutableStateOf(OSTheme.Aero) }
    val windowStack = remember { mutableStateListOf<WindowState>() }
    val runningSystemApps = remember { mutableStateListOf<AppEntry>() }
    val pinnedApps = remember { mutableStateListOf<TaskbarAppItem>() }
    var showStartMenu by remember { mutableStateOf(false) }
    var showRecentApps by remember { mutableStateOf(false) }
    var defaultLaunchFullscreen by remember { mutableStateOf(false) }
    var showTaskbarBackground by remember { mutableStateOf(false) }
    var focusedWindowId by remember { mutableStateOf<String?>(null) }
    var keyboardInputEvent by remember { mutableStateOf<NativeKeyEvent?>(null) }
    var showRunDialog by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("uniblox_settings", Context.MODE_PRIVATE) }
    var useOverlayKeyboard by remember { mutableStateOf(prefs.getBoolean("use_overlay_keyboard", false)) }

    val wallpapers = listOf(
        R.drawable.img_wallpaper_1,
        R.drawable.img_wallpaper_2,
        R.drawable.img_wallpaper_3,
        R.drawable.img_wallpaper_4,
        R.drawable.img_wallpaper_5,
        R.drawable.img_wallpaper_6,
        R.drawable.img_wallpaper_7,
        R.drawable.img_wallpaper_8,
        R.drawable.img_wallpaper_9,
        R.drawable.img_wallpaper_10
    )

    val dailyWallpaper = remember {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        wallpapers[dayOfYear % wallpapers.size]
    }

    LaunchedEffect(dailyWallpaper) {
        withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                wallpaperManager.setResource(dailyWallpaper)
            } catch (e: Exception) {
                Log.e("UnibloxOS", "Failed to set system wallpaper", e)
            }
        }
    }

    // Extract temporary system files from ezyZip.tar.gz on launch
    LaunchedEffect(Unit) {
        SystemFilesManager.ensureExtracted(context)
    }

    val installedApps = rememberInstalledApps()

    // Automatically seed pinned apps on startup (ensure 10 apps)
    // Use installedApps.size to trigger when apps are loaded
    LaunchedEffect(installedApps.size) {
        if (installedApps.isNotEmpty() && pinnedApps.size < 10) {
            val existingPackages = pinnedApps.map { it.packageName }.toSet()

            // Add Terminal if not already there
            if (pinnedApps.none { it.app == App.Terminal }) {
                pinnedApps.add(
                    TaskbarAppItem(
                        id = "pinned_terminal",
                        app = App.Terminal,
                        label = "Terminal",
                        icon = Icons.Default.Terminal,
                        isPinned = true
                    )
                )
            }

            // Add Settings if not already there
            if (pinnedApps.none { it.app == App.Settings }) {
                pinnedApps.add(
                    TaskbarAppItem(
                        id = "pinned_settings",
                        app = App.Settings,
                        label = "Settings",
                        icon = Icons.Default.Settings,
                        isPinned = true
                    )
                )
            }

            val appsToAdd = installedApps
                .filter { it.packageName.isNotEmpty() && it.packageName !in existingPackages }
                .take(10 - pinnedApps.size)
            
            appsToAdd.forEach { app ->
                pinnedApps.add(
                    TaskbarAppItem(
                        id = "pinned_${app.packageName}",
                        app = App.SystemApp,
                        packageName = app.packageName,
                        label = app.label,
                        icon = app.icon,
                        isPinned = true
                    )
                )
            }
        }
    }

    val onPinApp: (TaskbarAppItem) -> Unit = { item ->
        val alreadyPinned = pinnedApps.any { 
            (it.packageName.isNotEmpty() && it.packageName == item.packageName) || 
            (it.app != App.SystemApp && it.app == item.app) 
        }
        if (!alreadyPinned) {
            pinnedApps.add(item.copy(isPinned = true))
            Toast.makeText(context, "📌 Pinned ${item.label} to Taskbar", Toast.LENGTH_SHORT).show()
        }
    }

    val onUnpinApp: (String) -> Unit = { id ->
        pinnedApps.removeAll { it.id == id }
        Toast.makeText(context, "Unpinned from Taskbar", Toast.LENGTH_SHORT).show()
    }
    val widgets = remember { mutableStateListOf<WidgetItem>() }
    var pendingWidgetId by remember { mutableStateOf(-1) }
    
    val configWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && appWidgetId != -1) {
            widgets.add(WidgetItem(appWidgetId = appWidgetId))
            android.widget.Toast.makeText(context, "Widget added successfully", android.widget.Toast.LENGTH_SHORT).show()
        } else if (appWidgetId != -1) {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        }
        pendingWidgetId = -1
    }

    val pickWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (appWidgetId != -1) {
                val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
                if (appWidgetInfo?.configure != null) {
                    pendingWidgetId = appWidgetId
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = appWidgetInfo.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    configWidgetLauncher.launch(intent)
                } else {
                    widgets.add(WidgetItem(appWidgetId = appWidgetId))
                    android.widget.Toast.makeText(context, "Widget added successfully", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val onAppOpenWithMode: (App, String?, String?, Drawable?, Boolean) -> Unit = { app, pkg, label, icon, forceFullscreen -> 
        if (app == App.SystemApp) {
            if (pkg != null && runningSystemApps.none { it.packageName == pkg }) {
                runningSystemApps.add(AppEntry(label ?: "", pkg, icon))
            }
            val existingIndex = windowStack.indexOfFirst { it.app == App.SystemApp && it.packageName == pkg }
            if (existingIndex != -1) {
                val win = windowStack[existingIndex]
                windowStack.removeAt(existingIndex)
                val newWin = win.copy(isMinimized = false, isFullScreen = forceFullscreen)
                windowStack.add(newWin)
                focusedWindowId = newWin.id
            } else {
                val newWin = WindowState(
                    app = app, 
                    packageName = pkg ?: "", 
                    label = label ?: "", 
                    icon = icon,
                    isFullScreen = forceFullscreen,
                    position = IntOffset(50 + (windowStack.size * 30), 50 + (windowStack.size * 30))
                )
                windowStack.add(newWin)
                focusedWindowId = newWin.id
            }
        } else if (app == App.WebViewApp) {
            val existingIndex = windowStack.indexOfFirst { it.app == App.WebViewApp && it.packageName == pkg }
            if (existingIndex != -1) {
                val win = windowStack[existingIndex]
                windowStack.removeAt(existingIndex)
                val newWin = win.copy(isMinimized = false, isFullScreen = forceFullscreen)
                windowStack.add(newWin)
                focusedWindowId = newWin.id
            } else {
                val newWin = WindowState(
                    app = app, 
                    packageName = pkg ?: "", 
                    label = label ?: "", 
                    icon = icon,
                    isFullScreen = forceFullscreen,
                    position = IntOffset(50 + (windowStack.size * 40), 50 + (windowStack.size * 40))
                )
                windowStack.add(newWin)
                focusedWindowId = newWin.id
            }
        } else {
            val existingIndex = windowStack.indexOfFirst { it.app == app }
            if (existingIndex != -1) {
                val win = windowStack[existingIndex]
                windowStack.removeAt(existingIndex)
                val newWin = win.copy(isMinimized = false, isFullScreen = forceFullscreen)
                windowStack.add(newWin)
                focusedWindowId = newWin.id
            } else {
                val newWin = WindowState(
                    app = app, 
                    label = label ?: "", 
                    icon = icon,
                    isFullScreen = forceFullscreen,
                    position = IntOffset(50 + (windowStack.size * 40), 50 + (windowStack.size * 40))
                )
                windowStack.add(newWin)
                focusedWindowId = newWin.id
            }
        }
        showStartMenu = false
    }

    val onToggleWindowFullscreen: (String) -> Unit = { id ->
        val index = windowStack.indexOfFirst { it.id == id }
        if (index != -1) {
            val cur = windowStack[index]
            windowStack[index] = cur.copy(isFullScreen = !cur.isFullScreen)
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(animationSpec = tween(1000)) togetherWith fadeOut(animationSpec = tween(1000))
        },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            Screen.Welcome -> WelcomeScreen { currentScreen = Screen.Desktop }
            Screen.Desktop -> DesktopScreen(
                wallpaperRes = dailyWallpaper,
                windowStack = windowStack,
                runningSystemApps = runningSystemApps,
                pinnedApps = pinnedApps,
                installedApps = installedApps,
                currentTheme = currentTheme,
                onThemeChange = { currentTheme = it },
                onAppOpen = { app, pkg, label, icon -> 
                    onAppOpenWithMode(app, pkg, label, icon, defaultLaunchFullscreen)
                },
                onAppOpenWithMode = onAppOpenWithMode,
                onToggleWindowFullscreen = onToggleWindowFullscreen,
                defaultLaunchFullscreen = defaultLaunchFullscreen,
                onToggleDefaultLaunchFullscreen = { defaultLaunchFullscreen = !defaultLaunchFullscreen },
                onAppClose = { id -> 
                    windowStack.removeAll { it.id == id }
                },
                onAppMinimize = { id ->
                    val index = windowStack.indexOfFirst { it.id == id }
                    if (index != -1) {
                        windowStack[index] = windowStack[index].copy(isMinimized = true)
                    }
                },
                onWindowMove = { id, delta ->
                    val index = windowStack.indexOfFirst { it.id == id }
                    if (index != -1) {
                        val win = windowStack[index].copy(
                            position = IntOffset(
                                windowStack[index].position.x + delta.x.toInt(),
                                windowStack[index].position.y + delta.y.toInt()
                            )
                        )
                        if (index != windowStack.size - 1) {
                            windowStack.removeAt(index)
                            windowStack.add(win)
                        } else {
                            windowStack[index] = win
                        }
                    }
                },
                onWindowFocus = { id ->
                    try {
                        val index = windowStack.indexOfFirst { it.id == id }
                        if (index != -1) {
                            focusedWindowId = id
                            if (index != windowStack.size - 1) {
                                val win = windowStack.removeAt(index)
                                windowStack.add(win)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("UnibloxOS", "Focus change failed", e)
                    }
                },
                onRemoveRunningSystemApp = { pkg ->
                    runningSystemApps.removeAll { it.packageName == pkg }
                },
                onPinApp = onPinApp,
                onUnpinApp = onUnpinApp,
                showStartMenu = showStartMenu,
                onToggleStartMenu = { 
                    showStartMenu = !showStartMenu
                    if (showStartMenu) showRecentApps = false
                },
                showRecentApps = showRecentApps,
                onToggleRecentApps = {
                    showRecentApps = !showRecentApps
                    if (showRecentApps) showStartMenu = false
                },
                appWidgetHost = appWidgetHost,
                appWidgetManager = appWidgetManager,
                widgets = widgets,
                onAddWidget = {
                    try {
                        val appWidgetId = appWidgetHost.allocateAppWidgetId()
                        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        pickWidgetLauncher.launch(pickIntent)
                    } catch (e: Exception) {
                        Log.e("UnibloxOS", "Failed to launch widget picker", e)
                    }
                },
                onRemoveWidget = { id ->
                    val widget = widgets.find { it.id == id }
                    if (widget != null) {
                        appWidgetHost.deleteAppWidgetId(widget.appWidgetId)
                        widgets.remove(widget)
                    }
                },
                showTaskbarBackground = showTaskbarBackground,
                onToggleTaskbarBackground = { showTaskbarBackground = !showTaskbarBackground },
                focusedWindowId = focusedWindowId,
                onKeyboardEvent = { keyboardInputEvent = it },
                keyboardInputEvent = keyboardInputEvent,
                useOverlayKeyboard = useOverlayKeyboard,
                onToggleOverlayKeyboard = { useOverlayKeyboard = it },
                onRunClick = { showRunDialog = true }
            )
        }
    }

    BackHandler(enabled = windowStack.isNotEmpty() || showStartMenu || showRecentApps) {
        if (showStartMenu) {
            showStartMenu = false
        } else if (showRecentApps) {
            showRecentApps = false
        } else {
            if (windowStack.isNotEmpty()) {
                val top = windowStack.last()
                windowStack.remove(top)
            }
        }
    }

    if (showRunDialog) {
        RunDialog(
            onDismiss = { showRunDialog = false },
            onRun = { target ->
                val trimmed = target.trim()
                when (trimmed.lowercase()) {
                    "cmd", "terminal", "system32", "temp" -> onAppOpenWithMode(App.Terminal, "", "Terminal", null, defaultLaunchFullscreen)
                    "store", "appstore" -> onAppOpenWithMode(App.AppStore, "", "App Store", null, defaultLaunchFullscreen)
                    "settings" -> onAppOpenWithMode(App.Settings, "", "Settings", null, defaultLaunchFullscreen)
                    "keyboard" -> onAppOpenWithMode(App.Keyboard, "", "Keyboard", null, defaultLaunchFullscreen)
                    else -> onAppOpenWithMode(App.SystemApp, trimmed, trimmed, null, defaultLaunchFullscreen)
                }
                showRunDialog = false
            }
        )
    }

    // Global Overlay Keyboard (Computer-style inputs on top of everything)
    if (useOverlayKeyboard) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(5000f), // Top-most layer
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* Block pass-through */ },
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color.Black.copy(alpha = 0.95f),
                shadowElevation = 30.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column {
                    // Handle and Close Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.width(32.dp))
                        Box(modifier = Modifier.size(40.dp, 4.dp).background(Color.White.copy(alpha = 0.3f), CircleShape))
                        IconButton(
                            onClick = { useOverlayKeyboard = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Keyboard",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    KeyboardView(onKeyboardEvent = { keyboardInputEvent = it })
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(onFinished: () -> Unit) {
    val helloTexts = listOf("Hello", "Hola", "Bonjour", "Ciao", "Olá", "Hallo", "नमस्ते")
    var index by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repeat(helloTexts.size) {
            visible = true
            delay(800)
            if (it < helloTexts.size - 1) {
                visible = false
                delay(200)
                index++
            }
        }
        delay(500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFE6F3FF),
                        Color(0xFF00A2E8).copy(alpha = 0.5f),
                        Color(0xFFB5E61D).copy(alpha = 0.2f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.8f),
            exit = fadeOut(tween(500)) + scaleOut(tween(500), targetScale = 1.2f)
        ) {
            Text(
                text = helloTexts[index],
                color = Color(0xFF0054A6),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class OSThemeColors(
    val primary: Color,
    val accent: Color,
    val background: Color,
    val text: Color = Color(0xFF333333),
    val isGlass: Boolean = false,
    val isMetro: Boolean = false,
    val isRounded: Boolean = false,
    val isDeveloper: Boolean = false,
    val isColors: Boolean = false
)

@Composable
fun getOSThemeColors(theme: OSTheme): OSThemeColors {
    return when (theme) {
        OSTheme.Aero -> OSThemeColors(primary = Color(0xFF00A2E8), accent = Color(0xFFB5E61D), background = Color.White.copy(alpha = 0.45f), isGlass = true)
        OSTheme.Metro -> OSThemeColors(primary = Color(0xFF0078D7), accent = Color(0xFFFFFFFF), background = Color(0xFF0078D7), text = Color.White, isMetro = true)
        OSTheme.Macnostich -> OSThemeColors(primary = Color(0xFF333333), accent = Color(0xFF888888), background = Color.White.copy(alpha = 0.95f), isGlass = true, isRounded = true)
        OSTheme.Developer -> OSThemeColors(primary = Color(0xFF00FF00), accent = Color(0xFF00FF00), background = Color(0xFF1E1E1E), text = Color(0xFF00FF00), isDeveloper = true)
        OSTheme.School -> OSThemeColors(primary = Color(0xFFFFD700), accent = Color(0xFFFF5722), background = Color(0xFFFFF9C4))
        OSTheme.Colors -> OSThemeColors(primary = Color.Magenta, accent = Color.Cyan, background = Color.White.copy(alpha = 0.5f), isGlass = true, isColors = true)
    }
}

@Composable
fun BatteryMonitor(onBatteryLow: (Boolean) -> Unit) {
    val context = LocalContext.current
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level != -1 && scale != -1) {
                    val batteryPct = level * 100 / scale.toFloat()
                    onBatteryLow(batteryPct <= 15)
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }
}

@Composable
fun DesktopScreen(
    wallpaperRes: Int,
    windowStack: List<WindowState>,
    runningSystemApps: List<AppEntry>,
    pinnedApps: List<TaskbarAppItem>,
    installedApps: List<AppEntry>,
    currentTheme: OSTheme,
    onThemeChange: (OSTheme) -> Unit,
    onAppOpen: (App, String?, String?, Drawable?) -> Unit,
    onAppClose: (String) -> Unit,
    onAppMinimize: (String) -> Unit,
    onWindowMove: (String, androidx.compose.ui.geometry.Offset) -> Unit,
    onWindowFocus: (String) -> Unit,
    onRemoveRunningSystemApp: (String) -> Unit,
    onPinApp: (TaskbarAppItem) -> Unit,
    onUnpinApp: (String) -> Unit,
    showStartMenu: Boolean,
    onToggleStartMenu: () -> Unit,
    showRecentApps: Boolean,
    onToggleRecentApps: () -> Unit,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    widgets: MutableList<WidgetItem>,
    onAddWidget: () -> Unit,
    onRemoveWidget: (String) -> Unit,
    onToggleWindowFullscreen: (String) -> Unit = {},
    onAppOpenWithMode: (App, String?, String?, Drawable?, Boolean) -> Unit = { app, pkg, label, icon, _ -> onAppOpen(app, pkg, label, icon) },
    defaultLaunchFullscreen: Boolean = false,
    onToggleDefaultLaunchFullscreen: () -> Unit = {},
    showTaskbarBackground: Boolean = false,
    onToggleTaskbarBackground: () -> Unit = {},
    focusedWindowId: String? = null,
    onKeyboardEvent: (NativeKeyEvent) -> Unit = {},
    keyboardInputEvent: NativeKeyEvent? = null,
    useOverlayKeyboard: Boolean = false,
    onToggleOverlayKeyboard: (Boolean) -> Unit = {},
    onRunClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var showDesktopMenu by remember { mutableStateOf<Offset?>(null) }
    var selectedWidgetId by remember { mutableStateOf<String?>(null) }

    val activeFullscreenWindow = windowStack.lastOrNull { !it.isMinimized && it.isFullScreen }
    val isAnyAppFullscreen = activeFullscreenWindow != null

    // System Back Handler for fullscreen apps
    BackHandler(enabled = isAnyAppFullscreen) {
        activeFullscreenWindow?.let { win ->
            onAppClose(win.id)
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    
    var wallpaperClickCount by remember { mutableIntStateOf(0) }
    var showEasterEggClock by remember { mutableStateOf(false) }
    var showFlappyBird by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val sharedPrefs = remember { context.getSharedPreferences("app_store_prefs", Context.MODE_PRIVATE) }
    val installedAppIds = remember { mutableStateListOf<String>().apply {
        addAll(sharedPrefs.getStringSet("installed_apps", emptySet()) ?: emptySet())
    }}

    val appStoreApps = remember {
        listOf(
            WebApp("yt_web", "YouTube", "https://yt.be/", "📺", "Watch popular videos, music, and streams.", R.drawable.img_youtube_aero),
            WebApp("pocket_web", "Uniblox Pocket", "https://uniblox-fun.lovable.app/pocket", "🧱", "Classic sandbox block-building game in pocket edition.", R.drawable.img_uniblox_pocket),
            WebApp("vscode_web", "VS Code", "https://vscode.dev/", "💻", "Code on the go in a full-featured online development environment."),
            WebApp("minecraft_web", "Minecraft", "https://enchanting-dasik-c072d6.netlify.app/", "⛏️", "Minecraft browser edition with block placing and world building.", R.drawable.img_minecraft_aero),
            WebApp("bing_web", "Bing", "https://bing.com/", "🔍", "Search with Bing's smart AI features."),
            WebApp("scratch_web", "Scratch", "https://scratch.mit.edu", "🐈", "Create interactive games, animations, and stories."),
            WebApp("bloxd_web", "Bloxd", "https://bloxd.io/", "🧱", "Bloxd.io multiplayer block builder and mini-games."),
            WebApp("cuberealm_web", "Cube Realm", "https://cuberealm.io/", "🌍", "Explore and build inside a vast cubic online sandbox."),
            WebApp("audilos_web", "Uniblox Audilos", "https://uniblox-audilos.ai.studio/", "🎵", "Explore audio visualization and soundscapes on AI Studio."),
            WebApp("drive_web", "Google Drive", "https://drive.google.com/", "📁", "Access and share your Google Drive files in cloud storage."),
            WebApp("spotify_web", "Spotify", "https://spotify.com/", "🎵", "Listen to millions of songs, playlists, and podcasts."),
            WebApp("github_web", "GitHub", "https://GitHub.com/", "🐙", "Manage, review, and commit code with GitHub on Uniblox OS."),
            WebApp("gemini_web", "Gemini", "https://gemini.google.com/", "✨", "Supercharge your productivity with Google's advanced Gemini AI model.", R.drawable.img_gemini_aero),
            WebApp("poxel_web", "Poxel", "https://poxel.io/", "🖼️", "Poxel.io interactive drawing, painting, and art space."),
            WebApp("slither_web", "Slither.io", "http://slither.com/io", "🐍", "Grow as big as you can in the classic multiplayer snake battle arena."),
            WebApp("flappy_web", "Flappy Bird", "https://flappybird.io/", "🐦", "Tempt your patience with the addictive Flappy Bird arcade game."),
            WebApp("2048_web", "2048", "https://play2048.co/", "🔢", "Merge the numbered tiles to reach the 2048 block."),
            WebApp("crazygames_web", "CrazyGames", "https://crazygames.com/", "🎮", "Instantly play thousands of high-quality online games."),
            WebApp("lovable_web", "Lovable", "https://lovable.dev/", "❤️", "Design, build, and deploy apps in natural language."),
            WebApp("turbowarp_web", "TurboWarp Embed", "https://turbowarp.org/1109852074/embed", "🚀", "Run Scratch games up to 20x faster with enhanced embeds."),
            WebApp("speedtest_web", "Speedtest", "https://speedtest.com/", "⚡", "Instantly check your network download and upload speeds.")
        )
    }

    LaunchedEffect(installedAppIds.size) {
        installedAppIds.forEach { id ->
            val webApp = appStoreApps.find { it.id == id }
            if (webApp != null && pinnedApps.none { it.id == "pinned_web_${webApp.id}" }) {
                onPinApp(
                    TaskbarAppItem(
                        id = "pinned_web_${webApp.id}",
                        app = App.WebViewApp,
                        packageName = webApp.url,
                        label = webApp.name,
                        icon = webApp.iconRes ?: webApp.iconEmoji,
                        isPinned = true
                    )
                )
            }
        }
    }

    // Build the list of desktop apps (only use device apps!)
    val desktopItems = remember(installedApps, installedAppIds.size) {
        val list = mutableListOf<DesktopAppItem>()
        // 1. Add App Store (Persistent icon)
        list.add(DesktopAppItem("app_store_sys", App.AppStore, "", "App Store", R.drawable.img_app_store_aero, Color(0xFF4CAF50)))
        // 2. Add System Terminal
        list.add(DesktopAppItem("terminal_sys", App.Terminal, "", "Terminal", Icons.Default.Terminal, Color(0xFF2D2D2D)))
        
        // 3. Add Installed Web Apps
        appStoreApps.forEach { webApp ->
            if (installedAppIds.contains(webApp.id)) {
                list.add(DesktopAppItem(webApp.id, App.WebViewApp, webApp.url, webApp.name, webApp.iconRes ?: webApp.iconEmoji, Color.White))
            }
        }

        // 4. Add device applications
        installedApps.take(11).forEach { app ->
            list.add(DesktopAppItem(app.packageName, App.SystemApp, app.packageName, app.label, app.icon, Color.White))
        }
        list
    }

    val desktopPositions = remember { mutableStateMapOf<String, IntOffset>() }
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var contextMenuItem by remember { mutableStateOf<Pair<DesktopAppItem, IntOffset>?>(null) }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val startY = with(density) { (statusBarTop + 16.dp).roundToPx() }
    val startX = with(density) { 20.dp.roundToPx() }
    val colWidth = with(density) { 82.dp.roundToPx() }
    val rowHeight = with(density) { 86.dp.roundToPx() }

    val isBatteryLow = remember { mutableStateOf(false) }
    var dismissedBatteryWarning by remember { mutableStateOf(false) }
    BatteryMonitor(onBatteryLow = { low ->
        if (!low) dismissedBatteryWarning = false
        isBatteryLow.value = low
    })

    if (isBatteryLow.value && !dismissedBatteryWarning) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { dismissedBatteryWarning = true }
                .zIndex(9999f),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(0.85f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 24.dp,
                border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with title and X close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = "Low Battery",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Low Battery!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF212121)
                            )
                        }

                        // X Button
                        IconButton(
                            onClick = { dismissedBatteryWarning = true },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.06f))
                                .testTag("battery_warning_close_button")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close battery warning",
                                tint = Color(0xFF616161),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Battery level is low (≤ 15%). Please connect your device to a charger to keep Uniblox OS running.",
                        fontSize = 13.sp,
                        color = Color(0xFF555555),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { dismissedBatteryWarning = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("battery_warning_dismiss_button")
                    ) {
                        Text("Dismiss", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                onKeyboardEvent(keyEvent.nativeKeyEvent)
                if (keyEvent.type == KeyEventType.KeyUp && (keyEvent.key == Key.MetaLeft || keyEvent.key == Key.MetaRight)) {
                    onToggleStartMenu()
                    true
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        showDesktopMenu = offset
                    },
                    onTap = {
                        showDesktopMenu = null
                        selectedWidgetId = null
                        contextMenuItem = null
                    }
                )
            }
    ) {
        // Display high-resolution desktop wallpaper
        Image(
            painter = painterResource(wallpaperRes),
            contentDescription = "Desktop Wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Interactive transparent layer for Colors theme Easter Egg
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (currentTheme == OSTheme.Colors) {
                        wallpaperClickCount++
                        if (wallpaperClickCount >= 7) {
                            showEasterEggClock = true
                            wallpaperClickCount = 0
                        }
                    }
                }
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.05f)))

        // Draggable Desktop Icons
        desktopItems.forEachIndexed { index, item ->
            key(item.id) {
                val col = index / 6
                val row = index % 6
                val defaultX = startX + (col * colWidth)
                val defaultY = startY + (row * rowHeight)
                val currentPos = desktopPositions[item.id] ?: IntOffset(defaultX, defaultY)
                val isPinned = pinnedApps.any {
                    (it.packageName.isNotEmpty() && it.packageName == item.packageName) ||
                    (it.app != App.SystemApp && it.app == item.app)
                }

                val screenWidthPx = context.resources.displayMetrics.widthPixels
                val screenHeightPx = context.resources.displayMetrics.heightPixels
                val iconWidthPx = with(density) { 72.dp.roundToPx() }
                val minX = with(density) { 8.dp.roundToPx() }
                val maxX = (screenWidthPx - iconWidthPx - minX).coerceAtLeast(minX)
                val minY = startY
                val maxY = (screenHeightPx - with(density) { 100.dp.roundToPx() }).coerceAtLeast(minY)

                DraggableDesktopIcon(
                    item = item,
                    position = currentPos,
                    isDragging = draggingItemId == item.id,
                    isPinned = isPinned,
                    onMove = { dragDelta ->
                        val cur = desktopPositions[item.id] ?: IntOffset(defaultX, defaultY)
                        desktopPositions[item.id] = IntOffset(
                            (cur.x + dragDelta.x.toInt()).coerceIn(minX, maxX),
                            (cur.y + dragDelta.y.toInt()).coerceIn(minY, maxY)
                        )
                    },
                    onDragStart = { draggingItemId = item.id },
                    onDragEnd = {
                        draggingItemId = null
                        // Check if dropped near the bottom taskbar (drop to pin!)
                        val dropY = desktopPositions[item.id]?.y ?: defaultY
                        if (dropY > screenHeightPx - with(density) { 150.dp.roundToPx() }) {
                            onPinApp(
                                TaskbarAppItem(
                                    id = "pinned_${item.id}",
                                    app = item.app,
                                    packageName = item.packageName,
                                    label = item.label,
                                    icon = item.icon,
                                    isPinned = true
                                )
                            )
                        }
                    },
                    onClick = {
                        onAppOpenWithMode(item.app, item.packageName, item.label, item.icon as? Drawable, defaultLaunchFullscreen)
                    },
                    onLongClick = {
                        contextMenuItem = Pair(item, currentPos)
                    }
                )
            }
        }

        // Desktop App Context Menu (Open, Pin/Unpin, Reset Position)
        contextMenuItem?.let { (item, pos) ->
            val isPinned = pinnedApps.any {
                (it.packageName.isNotEmpty() && it.packageName == item.packageName) ||
                (it.app != App.SystemApp && it.app == item.app)
            }
            Box(
                modifier = Modifier
                    .offset { IntOffset(pos.x + with(density) { 36.dp.roundToPx() }, pos.y + with(density) { 20.dp.roundToPx() }) }
                    .zIndex(25f)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    shadowElevation = 10.dp,
                    modifier = Modifier.width(200.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = item.label,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Divider(color = Color.White.copy(alpha = 0.15f))
                        DesktopMenuItem(
                            text = "Open in Freeform",
                            icon = Icons.Default.OpenInNew
                        ) {
                            contextMenuItem = null
                            onAppOpenWithMode(item.app, item.packageName, item.label, item.icon as? Drawable, false)
                        }
                        DesktopMenuItem(
                            text = "Open in Fullscreen",
                            icon = Icons.Default.Fullscreen
                        ) {
                            contextMenuItem = null
                            onAppOpenWithMode(item.app, item.packageName, item.label, item.icon as? Drawable, true)
                        }
                        DesktopMenuItem(
                            text = if (isPinned) "Unpin from Taskbar" else "📌 Pin to Taskbar",
                            icon = if (isPinned) Icons.Default.PushPin else Icons.Default.Add
                        ) {
                            if (isPinned) {
                                val found = pinnedApps.find {
                                    (it.packageName.isNotEmpty() && it.packageName == item.packageName) ||
                                    (it.app != App.SystemApp && it.app == item.app)
                                }
                                if (found != null) onUnpinApp(found.id)
                            } else {
                                onPinApp(
                                    TaskbarAppItem(
                                        id = "pinned_${item.id}",
                                        app = item.app,
                                        packageName = item.packageName,
                                        label = item.label,
                                        icon = item.icon,
                                        isPinned = true
                                    )
                                )
                            }
                            contextMenuItem = null
                        }
                        DesktopMenuItem(
                            text = "Reset Position",
                            icon = Icons.Default.Refresh
                        ) {
                            desktopPositions.remove(item.id)
                            contextMenuItem = null
                        }
                    }
                }
            }
        }

        widgets.forEach { widget ->
            key(widget.id) {
                var wPos by remember { mutableStateOf(widget.position) }
                var wSize by remember { mutableStateOf(widget.size) }

                Box(
                    modifier = Modifier
                        .offset { wPos }
                        .size(wSize)
                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    wPos =
                                        IntOffset(wPos.x + dragAmount.x.toInt(), wPos.y + dragAmount.y.toInt())
                                    widget.position = wPos
                                }
                            )
                        }
                        .clickable { selectedWidgetId = widget.id }
                ) {
                    // Border Frame (Behind the widget)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                    )

                    AndroidWidgetHostView(
                        appWidgetId = widget.appWidgetId,
                        appWidgetHost = appWidgetHost,
                        appWidgetManager = appWidgetManager
                    )

                    if (selectedWidgetId == widget.id) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .background(
                                    Color.White.copy(alpha = 0.5f),
                                    RoundedCornerShape(topStart = 12.dp)
                                )
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        wSize = DpSize(
                                            (wSize.width + with(density) { dragAmount.x.toDp() }).coerceAtLeast(
                                                100.dp
                                            ),
                                            (wSize.height + with(density) { dragAmount.y.toDp() }).coerceAtLeast(
                                                60.dp
                                            )
                                        )
                                        widget.size = wSize
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.OpenInFull,
                                null,
                                tint = Color.Black,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                onRemoveWidget(widget.id)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .background(Color.Red.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        showDesktopMenu?.let { offset ->
            Surface(
                modifier = Modifier
                    .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                    .zIndex(50f),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.width(180.dp).padding(4.dp)) {
                    DesktopMenuItem("Refresh", Icons.Default.Refresh) {
                        try {
                            appWidgetHost.stopListening()
                            appWidgetHost.startListening()
                        } catch (e: Exception) {
                            Log.e("UnibloxOS", "Failed to refresh widget host", e)
                        }
                        showDesktopMenu = null
                    }
                    DesktopMenuItem("Add Widget", Icons.Default.AddBox) {
                        onAddWidget()
                        showDesktopMenu = null
                    }
                }
            }
        }

        // Bottom Right Buttons (System Controls)
        // Active App Windows
        windowStack.forEach { win ->
            AnimatedVisibility(
                visible = !win.isMinimized,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f)
            ) {
                AppWindow(
                    windowState = win,
                    onClose = { onAppClose(win.id) },
                    onMinimize = { onAppMinimize(win.id) },
                    onToggleFullscreen = { onToggleWindowFullscreen(win.id) },
                    onFocus = { 
                        onWindowFocus(win.id)
                    },
                    onMove = { onWindowMove(win.id, it) },
                    defaultLaunchFullscreen = defaultLaunchFullscreen,
                    onToggleDefaultLaunchFullscreen = onToggleDefaultLaunchFullscreen,
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange,
                    showTaskbarBackground = showTaskbarBackground,
                    onToggleTaskbarBackground = onToggleTaskbarBackground,
                    onKeyboardEvent = { event ->
                        onKeyboardEvent(event)
                    },
                    keyboardInputEvent = keyboardInputEvent,
                    focusedWindowId = focusedWindowId,
                    useOverlayKeyboard = useOverlayKeyboard,
                    onToggleOverlayKeyboard = onToggleOverlayKeyboard,
                    installedAppIds = installedAppIds,
                    onInstallApp = { id ->
                        if (!installedAppIds.contains(id)) {
                            installedAppIds.add(id)
                            sharedPrefs.edit().putStringSet("installed_apps", installedAppIds.toSet()).apply()
                            val webApp = appStoreApps.find { it.id == id }
                            if (webApp != null && pinnedApps.none { it.id == "pinned_web_${webApp.id}" }) {
                                onPinApp(
                                    TaskbarAppItem(
                                        id = "pinned_web_${webApp.id}",
                                        app = App.WebViewApp,
                                        packageName = webApp.url,
                                        label = webApp.name,
                                        icon = webApp.iconRes ?: webApp.iconEmoji,
                                        isPinned = true
                                    )
                                )
                            }
                        }
                    },
                    onUninstallApp = { id ->
                        installedAppIds.remove(id)
                        sharedPrefs.edit().putStringSet("installed_apps", installedAppIds.toSet()).apply()
                        onUnpinApp("pinned_web_$id")
                    },
                    onAppOpenWithMode = onAppOpenWithMode,
                    wallpaperRes = wallpaperRes
                )
            }
        }

        // Outside tap dismisser for Start Menu
        if (showStartMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1400f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onToggleStartMenu()
                    }
            )
        }

        // Start Menu
        AnimatedVisibility(
            visible = showStartMenu,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = if (isAnyAppFullscreen) 56.dp else 76.dp)
                .zIndex(1500f)
        ) {
            StartMenu(
                installedApps = installedApps,
                pinnedApps = pinnedApps,
                onTogglePin = { app ->
                    val isPinned = pinnedApps.any { it.packageName == app.packageName }
                    if (isPinned) {
                        val pinned = pinnedApps.find { it.packageName == app.packageName }
                        if (pinned != null) onUnpinApp(pinned.id)
                    } else {
                        onPinApp(
                            TaskbarAppItem(
                                id = "pinned_${app.packageName}",
                                app = App.SystemApp,
                                packageName = app.packageName,
                                label = app.label,
                                icon = app.icon,
                                isPinned = true
                            )
                        )
                    }
                },
                onAppOpen = { app, pkg, label, icon ->
                    onAppOpenWithMode(app, pkg, label, icon, defaultLaunchFullscreen)
                },
                currentTheme = currentTheme,
                installedAppIds = installedAppIds,
                appStoreApps = appStoreApps,
                wallpaperRes = wallpaperRes
            )
        }

        // Recent Apps (Task View)
        AnimatedVisibility(
            visible = showRecentApps,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = if (isAnyAppFullscreen) 60.dp else 86.dp)
                .zIndex(1050f)
        ) {
            RecentAppsMenu(runningSystemApps, windowStack, onAppOpen, onRemoveRunningSystemApp, onAppClose, wallpaperRes)
        }

        // Conditional Taskbar Visibility:
        // "If the app is opened in fullscreen instead of freeform, add this taskbar in the bottom ONLY IF APP IS FULLSCREEN"
        if (isAnyAppFullscreen) {
            FullscreenTaskbar(
                pinnedApps = pinnedApps,
                windowStack = windowStack,
                activeApp = activeFullscreenWindow?.app,
                activePackageName = activeFullscreenWindow?.packageName,
                onAppDrawerClick = onToggleStartMenu,
                onBackClick = {
                    activeFullscreenWindow?.let { win ->
                        onAppClose(win.id)
                    }
                },
                onHomeClick = {
                    activeFullscreenWindow?.let { win ->
                        onAppMinimize(win.id)
                    }
                },
                onRecentsClick = onToggleRecentApps,
                onAppOpen = { app, pkg, label, icon ->
                    onAppOpenWithMode(app, pkg, label, icon, true)
                },
                onPinApp = onPinApp,
                onUnpinApp = onUnpinApp,
                onAppClose = onAppClose,
                currentTheme = currentTheme,
                showBackground = showTaskbarBackground,
                onRunClick = onRunClick,
                wallpaperRes = wallpaperRes,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .zIndex(1000f)
            )
        } else {
            // Floating Taskbar (when on Desktop or when apps are in Freeform mode)
            Taskbar(
                pinnedApps = pinnedApps,
                windowStack = windowStack,
                runningSystemApps = runningSystemApps,
                onToggleStartMenu = onToggleStartMenu,
                onToggleRecentApps = onToggleRecentApps,
                onAppOpen = onAppOpen,
                onPinApp = onPinApp,
                onUnpinApp = onUnpinApp,
                onAppClose = onAppClose,
                currentTheme = currentTheme,
                showBackground = showTaskbarBackground,
                onRunClick = onRunClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 16.dp)
                    .zIndex(1000f)
            )
        }

        if (showEasterEggClock) {
            EasterEggClock(
                onConfirm = { h, m ->
                    if (h == 15 && m == 0) {
                        showFlappyBird = true
                    }
                    showEasterEggClock = false
                },
                onCancel = { showEasterEggClock = false }
            )
        }

        if (showFlappyBird) {
            Dialog(
                onDismissRequest = { showFlappyBird = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    FlappyBirdGame(onClose = { showFlappyBird = false })
                }
            }
        }
    }
}

@Composable
fun DesktopMenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
fun AndroidWidgetHostView(
    appWidgetId: Int,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager
) {
    val appWidgetInfo = remember(appWidgetId) { appWidgetManager.getAppWidgetInfo(appWidgetId) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    if (appWidgetInfo == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ErrorOutline, null, tint = Color.White.copy(alpha = 0.5f))
                Text("Widget Info Missing", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            }
        }
    } else {
        AndroidView(
            factory = { context ->
                appWidgetHost.createView(context, appWidgetId, appWidgetInfo).apply {
                    setAppWidget(appWidgetId, appWidgetInfo)
                }
            },
            update = { view ->
                // Update widget options for better scaling with safe defaults
                val width = with(density) { 
                    val wPx = if (view.width > 0) view.width.toFloat() else 250.dp.toPx()
                    wPx.toDp().value.toInt()
                }
                val height = with(density) { 
                    val hPx = if (view.height > 0) view.height.toFloat() else 150.dp.toPx()
                    hPx.toDp().value.toInt()
                }
                
                val options = android.os.Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
                }
                try {
                    appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
                } catch (e: Exception) {
                    Log.e("UnibloxOS", "Failed to update widget options", e)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun DraggableDesktopIcon(
    item: DesktopAppItem,
    position: IntOffset,
    isDragging: Boolean,
    isPinned: Boolean,
    onMove: (Offset) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .offset { position }
            .zIndex(if (isDragging) 10f else 0f)
            .scale(if (isDragging) 1.12f else 1f)
            .pointerInput(item.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        totalDragDistance = 0f
                        onDragStart()
                    },
                    onDragEnd = {
                        onDragEnd()
                        if (totalDragDistance < 12f) {
                            onLongClick()
                        }
                    },
                    onDragCancel = {
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragDistance += dragAmount.getDistance()
                        onMove(dragAmount)
                    }
                )
            }
            .clickable { onClick() }
            .width(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(2.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(if (isDragging) 10.dp else 2.dp, CircleShape),
                shape = CircleShape,
                color = Color.White.copy(alpha = if (isDragging) 0.96f else 0.92f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when (val icon = item.icon) {
                        is Drawable -> {
                            AsyncImage(
                                model = icon,
                                contentDescription = item.label,
                                modifier = Modifier.padding(6.dp).fillMaxSize()
                            )
                        }
                        is Int -> {
                            AsyncImage(
                                model = icon,
                                contentDescription = item.label,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        is String -> {
                            Text(
                                text = icon,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        is ImageVector -> {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = item.color
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = item.label,
                                    tint = Color.White,
                                    modifier = Modifier.padding(8.dp).fillMaxSize()
                                )
                            }
                        }
                        else -> {
                            Icon(
                                Icons.Default.Android,
                                contentDescription = null,
                                tint = Color(0xFF333333),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Taskbar(
    pinnedApps: List<TaskbarAppItem>,
    windowStack: List<WindowState>,
    runningSystemApps: List<AppEntry>,
    onToggleStartMenu: () -> Unit,
    onToggleRecentApps: () -> Unit,
    onAppOpen: (App, String?, String?, Drawable?) -> Unit,
    onPinApp: (TaskbarAppItem) -> Unit,
    onUnpinApp: (String) -> Unit,
    onAppClose: (String) -> Unit,
    currentTheme: OSTheme,
    showBackground: Boolean = false,
    onRunClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var taskbarMenuApp by remember { mutableStateOf<TaskbarAppItem?>(null) }

    val themeColors = getOSThemeColors(currentTheme)

    val allTaskbarApps = remember(pinnedApps, windowStack, runningSystemApps) {
        val list = mutableListOf<TaskbarAppItem>()
        // 1. All pinned apps
        list.addAll(pinnedApps)
        
        // 2. Add running internal windows not already in pinned list
        windowStack.forEach { win ->
            val alreadyPresent = list.any {
                (it.app != App.SystemApp && it.app == win.app) ||
                (it.app == App.SystemApp && it.packageName.isNotEmpty() && it.packageName == win.packageName)
            }
            if (!alreadyPresent) {
                list.add(
                    TaskbarAppItem(
                        id = "win_${win.id}",
                        app = win.app,
                        packageName = win.packageName,
                        label = win.label.ifEmpty { win.app.name },
                        icon = win.icon ?: when (win.app) {
                            App.Browser -> Icons.Default.Public
                            App.Terminal -> Icons.Default.Terminal
                            App.Settings -> Icons.Default.Settings
                            else -> Icons.Default.Window
                        },
                        isPinned = false
                    )
                )
            }
        }
        
        // 3. Add running system apps not already in pinned list
        runningSystemApps.forEach { app ->
            val alreadyPresent = list.any { it.packageName == app.packageName }
            if (!alreadyPresent) {
                list.add(
                    TaskbarAppItem(
                        id = "sys_${app.packageName}",
                        app = App.SystemApp,
                        packageName = app.packageName,
                        label = app.label,
                        icon = app.icon,
                        isPinned = false
                    )
                )
            }
        }
        list
    }

    Box(
        modifier = modifier
            .height(64.dp)
            .fillMaxWidth(if (showBackground) 1f else 0.9f)
            .clip(if (showBackground) RectangleShape else RoundedCornerShape(32.dp))
    ) {

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 12.dp)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                // Start Button (4 dots)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(if (themeColors.isMetro) RoundedCornerShape(0.dp) else CircleShape)
                        .background(if (themeColors.isMetro) themeColors.primary else Color.Transparent)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onToggleStartMenu() },
                                onLongPress = { onRunClick() }
                            )
                        }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(modifier = Modifier.size(7.dp).background(Color.White, CircleShape))
                            Box(modifier = Modifier.size(7.dp).background(Color.White, CircleShape))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(modifier = Modifier.size(7.dp).background(Color.White, CircleShape))
                            Box(modifier = Modifier.size(7.dp).background(Color.White, CircleShape))
                        }
                    }
                }

                // Search Bar
                Surface(
                    modifier = Modifier
                        .height(40.dp)
                        .width(120.dp)
                        .clickable { onToggleStartMenu() },
                    shape = if (themeColors.isMetro) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                    color = if (themeColors.isMetro) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.5f),
                    border = if (themeColors.isMetro) BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = if (themeColors.isMetro) Color.White else Color(0xFF333333), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search", color = if (themeColors.isMetro) Color.White.copy(alpha = 0.8f) else Color(0xFF333333).copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color(0xFF333333).copy(alpha = 0.25f))
                )

                // Taskbar Apps (pinned and running) - Scrollable Row
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayCount = 10
                    val displayApps = allTaskbarApps.take(displayCount)
                    
                    if (displayApps.isEmpty() && pinnedApps.isNotEmpty()) {
                        // Fallback if allTaskbarApps logic failed
                        pinnedApps.take(10).forEach { appItem ->
                             TaskbarIconItem(
                                item = appItem,
                                isRunning = false,
                                onClick = { onAppOpen(appItem.app, appItem.packageName, appItem.label, appItem.icon as? Drawable) },
                                onLongClick = { taskbarMenuApp = appItem },
                                currentTheme = currentTheme
                            )
                        }
                    } else {
                        displayApps.forEach { appItem ->
                            val isRunning = windowStack.any {
                                (it.app != App.SystemApp && it.app == appItem.app) ||
                                (it.app == App.SystemApp && it.packageName.isNotEmpty() && it.packageName == appItem.packageName)
                            } || runningSystemApps.any { it.packageName == appItem.packageName }

                            TaskbarIconItem(
                                item = appItem,
                                isRunning = isRunning,
                                onClick = {
                                    onAppOpen(appItem.app, appItem.packageName, appItem.label, appItem.icon as? Drawable)
                                },
                                onLongClick = {
                                    taskbarMenuApp = appItem
                                },
                                currentTheme = currentTheme
                            )
                        }
                    }
                }
            }
        }

        // Context menu dialog for taskbar items (Pin/Unpin, Close)
        taskbarMenuApp?.let { appItem ->
            val isPinned = pinnedApps.any {
                (it.packageName.isNotEmpty() && it.packageName == appItem.packageName) ||
                (it.app != App.SystemApp && it.app == appItem.app)
            }
            val openWin = windowStack.find {
                (it.app != App.SystemApp && it.app == appItem.app) ||
                (it.app == App.SystemApp && it.packageName.isNotEmpty() && it.packageName == appItem.packageName)
            }

            Dialog(
                onDismissRequest = { taskbarMenuApp = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { taskbarMenuApp = null },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 86.dp)
                            .width(220.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = appItem.label,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            Divider(color = Color.White.copy(alpha = 0.15f))
                            
                            DesktopMenuItem(
                                text = if (isPinned) "Unpin from Taskbar" else "📌 Pin to Taskbar",
                                icon = if (isPinned) Icons.Default.PushPin else Icons.Default.Add
                            ) {
                                if (isPinned) {
                                    val pinned = pinnedApps.find {
                                        (it.packageName.isNotEmpty() && it.packageName == appItem.packageName) ||
                                        (it.app != App.SystemApp && it.app == appItem.app)
                                    }
                                    if (pinned != null) onUnpinApp(pinned.id)
                                } else {
                                    onPinApp(appItem.copy(isPinned = true))
                                }
                                taskbarMenuApp = null
                            }

                            if (openWin != null) {
                                DesktopMenuItem(
                                    text = "Close Window",
                                    icon = Icons.Default.Close
                                ) {
                                    onAppClose(openWin.id)
                                    taskbarMenuApp = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }
@Composable
fun TaskbarIconItem(
    item: TaskbarAppItem,
    isRunning: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    currentTheme: OSTheme
) {
    val themeColors = getOSThemeColors(currentTheme)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(vertical = 4.dp)
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = if (themeColors.isMetro) RoundedCornerShape(0.dp) else CircleShape,
            color = if (themeColors.isMetro) themeColors.primary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.92f),
            shadowElevation = if (themeColors.isMetro) 0.dp else 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                when (val icon = item.icon) {
                    is Drawable -> {
                        AsyncImage(
                            model = icon,
                            contentDescription = item.label,
                            modifier = Modifier.padding(7.dp).fillMaxSize()
                        )
                    }
                    is ImageVector -> {
                        Icon(
                            imageVector = icon,
                            contentDescription = item.label,
                            tint = Color(0xFF333333),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.Android,
                            contentDescription = null,
                            tint = Color(0xFF333333),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Active indicator dot
        if (isRunning) {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 3.dp)
                    .background(Color(0xFF2E7D32), RoundedCornerShape(2.dp))
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullscreenTaskbar(
    pinnedApps: List<TaskbarAppItem>,
    windowStack: List<WindowState>,
    activeApp: App?,
    activePackageName: String?,
    onAppDrawerClick: () -> Unit,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onAppOpen: (App, String?, String?, Drawable?) -> Unit,
    onPinApp: (TaskbarAppItem) -> Unit,
    onUnpinApp: (String) -> Unit,
    onAppClose: (String) -> Unit,
    currentTheme: OSTheme,
    showBackground: Boolean = false,
    onRunClick: () -> Unit = {},
    wallpaperRes: Int? = null,
    modifier: Modifier = Modifier
) {
    var taskbarMenuApp by remember { mutableStateOf<TaskbarAppItem?>(null) }

    val displayedApps = remember(pinnedApps, windowStack) {
        val list = mutableListOf<TaskbarAppItem>()
        list.addAll(pinnedApps)
        windowStack.forEach { win ->
            val alreadyPresent = list.any {
                (it.app != App.SystemApp && it.app == win.app) ||
                (it.app == App.SystemApp && it.packageName.isNotEmpty() && it.packageName == win.packageName)
            }
            if (!alreadyPresent) {
                list.add(
                    TaskbarAppItem(
                        id = "win_${win.id}",
                        app = win.app,
                        packageName = win.packageName,
                        label = win.label.ifEmpty { win.app.name },
                        icon = win.icon ?: when (win.app) {
                            App.Browser -> Icons.Default.Public
                            App.Terminal -> Icons.Default.Terminal
                            App.Settings -> Icons.Default.Settings
                            else -> Icons.Default.Window
                        },
                        isPinned = false
                    )
                )
            }
        }
        list
    }

    val themeColors = getOSThemeColors(currentTheme)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (themeColors.isGlass) {
            // Blurred wallpaper slice at the bottom of the screen
            if (wallpaperRes != null) {
                Image(
                    painter = painterResource(wallpaperRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(11.dp)
                )
            }
            // Aero frosted translucent gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.50f),
                                Color.White.copy(alpha = 0.28f),
                                Color(0xFFCCE8FF).copy(alpha = 0.38f)
                            )
                        )
                    )
            )
            // Specular gloss reflection line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.95f),
                                Color.White.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
        }
        // Subtle top divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(if (themeColors.isGlass) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.3f))
                .align(Alignment.TopCenter)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LEFT: App Drawer Button + Divider + App Icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onAppDrawerClick() },
                                onLongPress = { onRunClick() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AppDrawerSearchIcon(currentTheme)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color(0xFFE0E0E0))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // App icons row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayCount = 10
                    displayedApps.take(displayCount).forEach { item ->
                        val isRunning = windowStack.any {
                            (item.app != App.SystemApp && it.app == item.app) ||
                            (item.app == App.SystemApp && it.packageName == item.packageName)
                        }
                        val isCurrentActive = (item.app == activeApp && (item.app != App.SystemApp || item.packageName == activePackageName))

                        FullscreenTaskbarAppIcon(
                            item = item,
                            isActive = isCurrentActive,
                            isRunning = isRunning,
                            onClick = {
                                onAppOpen(item.app, item.packageName, item.label, item.icon as? Drawable)
                            },
                            onLongClick = {
                                taskbarMenuApp = item
                            },
                            currentTheme = currentTheme
                        )
                    }
                }
            }

            // RIGHT: 3-Button Navigation Bar (Back, Home, Recents)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Back (Triangle pointing left)
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Canvas(modifier = Modifier.size(15.dp)) {
                        val path = Path().apply {
                            moveTo(size.width, 0f)
                            lineTo(0f, size.height / 2f)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(path, color = Color.White)
                    }
                }

                // Home (Circle)
                IconButton(
                    onClick = onHomeClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Canvas(modifier = Modifier.size(15.dp)) {
                        drawCircle(color = Color.White, radius = size.minDimension / 2f)
                    }
                }

                // Recents (Square)
                IconButton(
                    onClick = onRecentsClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        drawRoundRect(
                            color = Color.White,
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // Context Menu for taskbar items
        taskbarMenuApp?.let { menuApp ->
            val openWin = windowStack.find {
                (menuApp.app != App.SystemApp && it.app == menuApp.app) ||
                (menuApp.app == App.SystemApp && it.packageName == menuApp.packageName)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 60.dp, bottom = 56.dp)
                    .zIndex(1200f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shadowElevation = 12.dp,
                    modifier = Modifier.width(180.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = menuApp.label,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Divider(color = Color.White.copy(alpha = 0.15f))
                        if (menuApp.isPinned) {
                            DesktopMenuItem(text = "Unpin from Taskbar", icon = Icons.Default.PushPin) {
                                onUnpinApp(menuApp.id)
                                taskbarMenuApp = null
                            }
                        } else {
                            DesktopMenuItem(text = "Pin to Taskbar", icon = Icons.Default.Add) {
                                onPinApp(menuApp.copy(isPinned = true))
                                taskbarMenuApp = null
                            }
                        }
                        if (openWin != null) {
                            DesktopMenuItem(text = "Close App", icon = Icons.Default.Close) {
                                onAppClose(openWin.id)
                                taskbarMenuApp = null
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppDrawerSearchIcon(currentTheme: OSTheme) {
    val themeColors = getOSThemeColors(currentTheme)
    Surface(
        modifier = Modifier.size(36.dp),
        shape = if (themeColors.isMetro) RoundedCornerShape(0.dp) else CircleShape,
        color = if (themeColors.isMetro) themeColors.primary else Color.White.copy(alpha = 0.2f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(modifier = Modifier.size(5.dp).background(if (themeColors.isMetro) Color.White else Color.White, CircleShape))
                    Box(modifier = Modifier.size(5.dp).background(if (themeColors.isMetro) Color.White else Color.White, CircleShape))
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(modifier = Modifier.size(5.dp).background(if (themeColors.isMetro) Color.White else Color.White, CircleShape))
                    Box(modifier = Modifier.size(5.dp).background(if (themeColors.isMetro) Color.White else Color.White, CircleShape))
                }
            }
        }
    }
}

@Composable
fun FullscreenTaskbarAppIcon(
    item: TaskbarAppItem,
    isActive: Boolean,
    isRunning: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    currentTheme: OSTheme
) {
    val themeColors = getOSThemeColors(currentTheme)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(if (themeColors.isMetro) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp))
            .background(if (isActive) Color.White.copy(alpha = 0.3f) else Color.Transparent)
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
    ) {
        if (isActive) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE8F0FE)
            ) {}
        }

        when (val icon = item.icon) {
            is Drawable -> {
                AsyncImage(
                    model = icon,
                    contentDescription = item.label,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
            }
            is ImageVector -> {
                Icon(
                    imageVector = icon,
                    contentDescription = item.label,
                    tint = Color(0xFF333333),
                    modifier = Modifier.size(24.dp)
                )
            }
            else -> {
                Icon(
                    Icons.Default.Android,
                    contentDescription = null,
                    tint = Color(0xFF333333),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (isRunning && !isActive) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(Color(0xFF757575), CircleShape)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

enum class StartMenuTab {
    Pinned,
    AppDrawer
}

@Composable
fun StartMenu(
    installedApps: List<AppEntry>,
    pinnedApps: List<TaskbarAppItem>,
    onTogglePin: (AppEntry) -> Unit,
    onAppOpen: (App, String?, String?, Drawable?) -> Unit,
    currentTheme: OSTheme,
    installedAppIds: List<String> = emptyList(),
    appStoreApps: List<WebApp> = emptyList(),
    wallpaperRes: Int? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(StartMenuTab.Pinned) }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { 
                it.label.contains(searchQuery, ignoreCase = true) || 
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredWebApps = remember(searchQuery, installedAppIds.size) {
        val list = appStoreApps.filter { installedAppIds.contains(it.id) }
        if (searchQuery.isBlank()) list
        else list.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val themeColors = getOSThemeColors(currentTheme)

    Surface(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth(0.94f)
            .heightIn(max = 580.dp)
            .fillMaxHeight(0.82f),
        shape = when {
            themeColors.isMetro -> RoundedCornerShape(0.dp)
            themeColors.isRounded -> RoundedCornerShape(28.dp)
            else -> RoundedCornerShape(24.dp)
        },
        color = if (themeColors.isGlass) Color.Transparent else themeColors.background,
        border = if (themeColors.isGlass) {
            BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.30f),
                        Color(0x6600A2E8)
                    )
                )
            )
        } else if (themeColors.isMetro) {
            BorderStroke(2.dp, Color.White.copy(alpha = 0.2f))
        } else null,
        tonalElevation = if (themeColors.isMetro) 0.dp else 16.dp
    ) {
        // Theme background effect with fixed glass blur
        Box(modifier = Modifier.fillMaxSize()) {
            if (themeColors.isGlass) {
                // 1. Real blurred wallpaper background layer
                if (wallpaperRes != null) {
                    Image(
                        painter = painterResource(wallpaperRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(14.dp)
                    )
                }

                // 2. Translucent frosted glass tint + Aero color tint
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (themeColors.isColors) {
                                Brush.linearGradient(
                                    listOf(
                                        Color.Red.copy(0.18f),
                                        Color.Yellow.copy(0.15f),
                                        Color.Green.copy(0.15f),
                                        Color.Blue.copy(0.18f),
                                        Color.Magenta.copy(0.20f)
                                    )
                                )
                            } else if (themeColors.isRounded) {
                                SolidColor(Color.White.copy(alpha = 0.88f))
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.60f),
                                        Color.White.copy(alpha = 0.38f),
                                        Color(0xFFD6EEFF).copy(alpha = 0.45f)
                                    )
                                )
                            }
                        )
                )

                // 3. Specular gloss reflection (Windows 7 Aero trademark shine)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.50f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 4. Top highlight border line for authentic glass shine
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.95f),
                                    Color.White.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
            } else if (themeColors.isDeveloper) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)))
            } else if (themeColors.isMetro) {
                Box(modifier = Modifier.fillMaxSize().background(themeColors.background))
            } else {
                Box(modifier = Modifier.fillMaxSize().background(themeColors.background))
            }

            Column(modifier = Modifier.padding(22.dp)) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(themeColors.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = themeColors.primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Administrator", color = themeColors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Uniblox OS 7 Professional", color = themeColors.primary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(themeColors.primary.copy(alpha = 0.1f), CircleShape)
                            .clickable { searchQuery = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = themeColors.primary, modifier = Modifier.size(16.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search all applications...", color = themeColors.text.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = themeColors.primary.copy(alpha = 0.7f)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = themeColors.text.copy(alpha = 0.6f))
                            }
                        }
                    },
                    singleLine = true,
                    shape = if (themeColors.isMetro) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColors.text,
                        unfocusedTextColor = themeColors.text,
                        focusedContainerColor = Color.White.copy(alpha = 0.55f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.35f),
                        focusedBorderColor = themeColors.primary.copy(alpha = 0.6f),
                        unfocusedBorderColor = themeColors.primary.copy(alpha = 0.25f)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (searchQuery.isNotBlank()) {
                    // Search Mode
                    Text(
                        text = "SEARCH RESULTS (${filteredApps.size + filteredWebApps.size})",
                        color = themeColors.primary.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredApps.isEmpty() && filteredWebApps.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No applications found matching \"$searchQuery\"", color = themeColors.text.copy(alpha = 0.5f))
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(80.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredWebApps) { webApp ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onAppOpen(App.WebViewApp, webApp.url, webApp.name, null) }
                                        .padding(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (webApp.iconRes != null) {
                                            AsyncImage(model = webApp.iconRes, contentDescription = webApp.name, modifier = Modifier.fillMaxSize())
                                        } else {
                                            Text(text = webApp.iconEmoji, fontSize = 24.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = webApp.name, color = themeColors.text, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            items(filteredApps) { app ->
                                val isPinned = pinnedApps.any { it.packageName == app.packageName }
                                AppGridItem(
                                    app = app,
                                    isPinned = isPinned,
                                    onTogglePin = { onTogglePin(app) },
                                    onClick = { onAppOpen(App.SystemApp, app.packageName, app.label, app.icon) },
                                    currentTheme = currentTheme
                                )
                            }
                        }
                    }
                } else {
                    // Segmented Tabs: Pinned & Quick vs App Drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.28f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Pinned Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedTab == StartMenuTab.Pinned) themeColors.primary.copy(alpha = 0.25f) else Color.Transparent
                                )
                                .clickable { selectedTab = StartMenuTab.Pinned }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PushPin, contentDescription = null, tint = themeColors.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Pinned & Quick",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == StartMenuTab.Pinned) FontWeight.Bold else FontWeight.Medium,
                                    color = themeColors.text
                                )
                            }
                        }

                        // App Drawer Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedTab == StartMenuTab.AppDrawer) themeColors.primary.copy(alpha = 0.25f) else Color.Transparent
                                )
                                .clickable { selectedTab = StartMenuTab.AppDrawer }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Apps, contentDescription = null, tint = themeColors.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "App Drawer (${installedApps.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == StartMenuTab.AppDrawer) FontWeight.Bold else FontWeight.Medium,
                                    color = themeColors.text
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedTab == StartMenuTab.Pinned) {
                        // Pinned & System Overview - Uncramped, clean layout!
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "SYSTEM UTILITIES",
                                color = themeColors.primary.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                StartSystemItem("App Store", R.drawable.img_app_store_aero, Color(0xFF4CAF50), currentTheme) { onAppOpen(App.AppStore, null, "App Store", null) }
                                StartSystemItem("Terminal", Icons.Default.Terminal, Color(0xFF2D2D2D), currentTheme) { onAppOpen(App.Terminal, null, null, null) }
                                StartSystemItem("Keyboard", Icons.Default.Keyboard, Color(0xFF9B59B6), currentTheme) { onAppOpen(App.Keyboard, null, "Keyboard", null) }
                                StartSystemItem("Settings", Icons.Default.Settings, Color(0xFF7F8C8D), currentTheme) { onAppOpen(App.Settings, null, null, null) }
                            }

                            // Pinned Applications (Includes installed Web Apps and pinned device applications together)
                            val userWebApps = remember(installedAppIds.size) {
                                appStoreApps.filter { installedAppIds.contains(it.id) }
                            }
                            val userPinnedApps = remember(pinnedApps, installedApps) {
                                installedApps.filter { app -> pinnedApps.any { it.packageName == app.packageName } }
                            }
                            val hasAnyPinned = userWebApps.isNotEmpty() || userPinnedApps.isNotEmpty()

                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "PINNED APPLICATIONS",
                                color = themeColors.primary.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (!hasAnyPinned) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No pinned applications yet",
                                        color = themeColors.text.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(76.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.heightIn(max = 240.dp)
                                ) {
                                    // 1. Installed Web Apps in Pinned Applications
                                    items(userWebApps) { webApp ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(76.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onAppOpen(App.WebViewApp, webApp.url, webApp.name, null) }
                                                .padding(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (webApp.iconRes != null) {
                                                    AsyncImage(model = webApp.iconRes, contentDescription = webApp.name, modifier = Modifier.fillMaxSize())
                                                } else {
                                                    Text(text = webApp.iconEmoji, fontSize = 22.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = webApp.name,
                                                color = themeColors.text,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // 2. Pinned Device Apps in Pinned Applications
                                    items(userPinnedApps) { app ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(76.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onAppOpen(App.SystemApp, app.packageName, app.label, app.icon) }
                                                .padding(6.dp)
                                        ) {
                                            AsyncImage(
                                                model = app.icon,
                                                contentDescription = app.label,
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = app.label,
                                                color = themeColors.text,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Prominent "All Applications (App Drawer)" Navigation Bar
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTab = StartMenuTab.AppDrawer },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(themeColors.primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Apps, contentDescription = null, tint = themeColors.primary, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("All Applications", fontWeight = FontWeight.Bold, color = themeColors.text, fontSize = 13.sp)
                                        Text("${installedApps.size} device apps • Open App Drawer", color = themeColors.text.copy(alpha = 0.7f), fontSize = 11.sp)
                                    }
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Go to App Drawer", tint = themeColors.primary, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    } else {
                        // App Drawer View - Dedicated spacious layout for all installed apps!
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedTab = StartMenuTab.Pinned }
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = themeColors.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Back", color = themeColors.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    text = "ALL APPS (${installedApps.size})",
                                    color = themeColors.primary.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(80.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(installedApps) { app ->
                                    val isPinned = pinnedApps.any { it.packageName == app.packageName }
                                    AppGridItem(
                                        app = app,
                                        isPinned = isPinned,
                                        onTogglePin = { onTogglePin(app) },
                                        onClick = {
                                            onAppOpen(App.SystemApp, app.packageName, app.label, app.icon)
                                        },
                                        currentTheme = currentTheme
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StartSystemItem(
    label: String,
    icon: Any,
    color: Color,
    currentTheme: OSTheme,
    onClick: () -> Unit
) {
    val themeColors = getOSThemeColors(currentTheme)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            when (icon) {
                is ImageVector -> {
                    Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
                }
                is Int -> {
                    AsyncImage(model = icon, contentDescription = label, modifier = Modifier.fillMaxSize().padding(4.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label, 
            color = themeColors.text, 
            fontSize = 11.sp, 
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AppGridItem(
    app: AppEntry,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onClick: () -> Unit,
    currentTheme: OSTheme
) {
    val themeColors = getOSThemeColors(currentTheme)
    Box(contentAlignment = Alignment.TopEnd) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(if (themeColors.isMetro) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
                .background(if (themeColors.isMetro) themeColors.primary.copy(alpha = 0.1f) else Color.Transparent)
                .clickable { onClick() }
                .padding(8.dp)
        ) {
            AsyncImage(
                model = app.icon,
                contentDescription = app.label,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.label,
                color = themeColors.text,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Quick Pin toggle icon
        IconButton(
            onClick = onTogglePin,
            modifier = Modifier
                .size(24.dp)
                .padding(2.dp)
        ) {
            Icon(
                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Default.Add,
                contentDescription = if (isPinned) "Unpin from taskbar" else "Pin to taskbar",
                tint = if (isPinned) themeColors.primary else themeColors.text.copy(alpha = 0.45f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    blurRadius: Float = 20f,
    content: @Composable BoxScope.() -> Unit = {
        // Place your content here
        Text(
            text = "Frosted Glass",
            modifier = Modifier.padding(16.dp),
            color = Color.Black
        )
    }
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    renderEffect = RenderEffect.createBlurEffect(
                        blurRadius,
                        blurRadius,
                        Shader.TileMode.CLAMP
                    ).asComposeRenderEffect()
                }
            }
            .background(Color.White.copy(alpha = 0.3f)) // semi-transparent overlay
            .clip(RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
fun AppWindow(
    windowState: WindowState,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onFocus: () -> Unit,
    onMove: (androidx.compose.ui.geometry.Offset) -> Unit,
    defaultLaunchFullscreen: Boolean = false,
    onToggleDefaultLaunchFullscreen: () -> Unit = {},
    currentTheme: OSTheme,
    onThemeChange: (OSTheme) -> Unit,
    showTaskbarBackground: Boolean = false,
    onToggleTaskbarBackground: () -> Unit = {},
    onKeyboardEvent: (NativeKeyEvent) -> Unit = {},
    keyboardInputEvent: NativeKeyEvent? = null,
    focusedWindowId: String? = null,
    useOverlayKeyboard: Boolean = false,
    onToggleOverlayKeyboard: (Boolean) -> Unit = {},
    installedAppIds: List<String> = emptyList(),
    onInstallApp: (String) -> Unit = {},
    onUninstallApp: (String) -> Unit = {},
    onAppOpenWithMode: (App, String?, String?, Drawable?, Boolean) -> Unit = { _, _, _, _, _ -> },
    wallpaperRes: Int? = null
) {
    val themeColors = getOSThemeColors(currentTheme)
    val context = LocalContext.current
    val app = windowState.app
    val packageName = windowState.packageName
    val label = windowState.label
    val isFocused = focusedWindowId == windowState.id
    
    val animatedPaddingBottom by animateDpAsState(
        targetValue = if (windowState.isFullScreen) 52.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "fullscreenPadding"
    )
    val animatedShape by animateDpAsState(
        targetValue = if (windowState.isFullScreen) 0.dp else 12.dp,
        animationSpec = tween(durationMillis = 400),
        label = "fullscreenShape"
    )
    val animatedElevation by animateDpAsState(
        targetValue = if (windowState.isFullScreen) 0.dp else 16.dp,
        animationSpec = tween(400),
        label = "fullscreenElevation"
    )

    Surface(
        modifier = (if (windowState.isFullScreen) {
            Modifier
                .fillMaxSize()
                .padding(bottom = animatedPaddingBottom)
        } else {
            Modifier
                .offset { windowState.position }
                .size(
                    width = when(app) {
                        App.SystemApp -> 350.dp
                        App.Keyboard -> 600.dp
                        else -> 711.dp
                    },
                    height = when(app) {
                        App.SystemApp -> 260.dp
                        App.Keyboard -> 240.dp
                        else -> 520.dp
                    }
                )
                .padding(2.dp)
        }).zIndex(if (isFocused) 500f else 100f)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFocus
            ),
        shape = if (windowState.isFullScreen) RectangleShape else RoundedCornerShape(animatedShape),
        color = Color.Transparent,
        shadowElevation = animatedElevation,
        border = if (windowState.isFullScreen) null else BorderStroke(1.5.dp, if (themeColors.isGlass) Color.White.copy(alpha = 0.5f) else Color(0xFF00A2E8).copy(alpha = if (isFocused) 0.6f else 0.2f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Window Title Bar (Draggable if not fullscreen) - Transparent topbar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        if (windowState.isFullScreen) RectangleShape
                        else RoundedCornerShape(topStart = animatedShape, topEnd = animatedShape)
                    )
                    .then(
                        if (!windowState.isFullScreen) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onMove(dragAmount)
                                }
                            }
                        } else Modifier
                    )
            ) {
                // Glass overlay without wallpaper backdrop layer
                if (themeColors.isGlass) {
                    // Translucent frosted glass tint + Aero color tint (transparent see-through)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                if (themeColors.isColors) {
                                    Brush.linearGradient(
                                        listOf(
                                            Color.Red.copy(0.15f),
                                            Color.Yellow.copy(0.12f),
                                            Color.Green.copy(0.12f),
                                            Color.Blue.copy(0.15f),
                                            Color.Magenta.copy(0.15f)
                                        )
                                    )
                                } else if (themeColors.isRounded) {
                                    SolidColor(Color.White.copy(alpha = 0.35f))
                                } else {
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.30f),
                                            Color.White.copy(alpha = 0.15f),
                                            Color(0xFFD6EEFF).copy(alpha = 0.20f)
                                        )
                                    )
                                }
                            )
                    )

                    // 3. Specular gloss reflection (Windows 7 Aero trademark shine)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.50f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // 4. Top highlight border line for authentic glass shine
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.3f),
                                        Color.White.copy(alpha = 0.95f),
                                        Color.White.copy(alpha = 0.3f)
                                    )
                                )
                            )
                    )
                } else if (themeColors.isDeveloper) {
                    Box(modifier = Modifier.matchParentSize().background(Color(0xFF1E1E1E)))
                } else if (themeColors.isMetro) {
                    Box(modifier = Modifier.matchParentSize().background(themeColors.background))
                } else {
                    Box(modifier = Modifier.matchParentSize().background(themeColors.background))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when(app) {
                            App.Browser -> "UNIBLOX browser + infinitycursor"
                            App.GameEngine -> "Pocket Engine"
                            App.Messages -> "Uniblox Messages"
                            App.Settings -> "System Settings"
                            App.Terminal -> "Uniblox Terminal"
                            App.SystemApp -> label
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                blurRadius = 4f
                            )
                        ),
                        fontWeight = FontWeight.Bold,
                        color = if (themeColors.isGlass) Color.White else if (themeColors.isMetro) Color.White else Color(0xFF0054A6)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onMinimize, 
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Remove, 
                            contentDescription = "Minimize", 
                            tint = if (themeColors.isGlass) Color.White else if (themeColors.isMetro) Color.White else Color(0xFF0054A6), 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onToggleFullscreen, 
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (windowState.isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, 
                            contentDescription = if (windowState.isFullScreen) "Restore to Freeform" else "Maximize to Fullscreen", 
                            tint = if (themeColors.isGlass) Color.White else if (themeColors.isMetro) Color.White else Color(0xFF0054A6), 
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onClose, 
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFE81123).copy(alpha = 0.85f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Close", 
                            tint = Color.White, 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Subtle divider line at the bottom of the topbar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            if (themeColors.isGlass) Color.White.copy(alpha = 0.35f)
                            else Color.LightGray.copy(alpha = 0.5f)
                        )
                )
            }

            // Window Content
            val isFocused = focusedWindowId == windowState.id
            val currentEvent = if (isFocused) keyboardInputEvent else null
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(
                        if (windowState.isFullScreen) RectangleShape
                        else RoundedCornerShape(bottomStart = animatedShape, bottomEnd = animatedShape)
                    )
                    .background(
                        if (themeColors.isMetro) themeColors.background
                        else Color.White
                    )
            ) {
                when (app) {
                    App.Browser -> BrowserView(currentEvent)
                    App.GameEngine -> GameEngineView()
                    App.Messages -> MessagesView()
                    App.Settings -> SettingsView(
                        defaultLaunchFullscreen = defaultLaunchFullscreen,
                        onToggleDefaultLaunchFullscreen = onToggleDefaultLaunchFullscreen,
                        currentTheme = currentTheme,
                        onThemeChange = onThemeChange,
                        showTaskbarBackground = showTaskbarBackground,
                        onToggleTaskbarBackground = onToggleTaskbarBackground,
                        useOverlayKeyboard = useOverlayKeyboard,
                        onToggleOverlayKeyboard = onToggleOverlayKeyboard
                    )
                    App.Terminal -> TerminalView(currentEvent)
                    App.Keyboard -> KeyboardView(onKeyboardEvent)
                    App.SystemApp -> {
                        LaunchedEffect(packageName, windowState.isFullScreen) {
                            if (packageName.isNotEmpty()) {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                                if (launchIntent != null) {
                                    try {
                                        if (!windowState.isFullScreen) {
                                            val options = ActivityOptions.makeBasic()
                                            val displayMetrics = context.resources.displayMetrics
                                            val w = displayMetrics.widthPixels
                                            val h = displayMetrics.heightPixels
                                            
                                            // Set initial bounds for the freeform window
                                            options.launchBounds = Rect(w / 4, h / 4, w * 3 / 4, h * 3 / 4)
                                            
                                            // Add flags to encourage freeform/multitasking behavior
                                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)

                                            // Simplified reflection thanks to HiddenApiBypass
                                            try {
                                                HiddenApiBypass.invoke(
                                                    ActivityOptions::class.java,
                                                    options,
                                                    "setLaunchWindowingMode",
                                                    5
                                                )
                                            } catch (e: Exception) {
                                                Log.e("UnibloxOS", "Reflection failed", e)
                                            }

                                            context.startActivity(launchIntent, options.toBundle())
                                        } else {
                                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(launchIntent)
                                        }
                                        onClose()
                                    } catch (e: Exception) {
                                        Log.e("UnibloxOS", "Launch failed", e)
                                        context.startActivity(launchIntent)
                                        onClose()
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("Launching $label", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (windowState.isFullScreen) "Opening in Fullscreen Mode..." else "Initializing Freeform Desktop Mode...", 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    App.AppStore -> {
                        AppStoreView(
                            installedAppIds = installedAppIds,
                            onInstallApp = onInstallApp,
                            onUninstallApp = onUninstallApp,
                            onOpenApp = { url, name ->
                                onAppOpenWithMode(App.WebViewApp, url, name, null, defaultLaunchFullscreen)
                            }
                        )
                    }
                    App.WebViewApp -> {
                        WebViewAppView(url = packageName, externalKeyboardEvent = currentEvent)
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun RecentAppsMenu(
    runningSystemApps: List<AppEntry>,
    windowStack: List<WindowState>,
    onAppOpen: (App, String?, String?, Drawable?) -> Unit,
    onRemoveRunningSystemApp: (String) -> Unit,
    onAppClose: (String) -> Unit,
    wallpaperRes: Int? = null
) {
    Surface(
        modifier = Modifier
            .width(400.dp)
            .height(500.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.5.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.30f),
                    Color(0x6600A2E8)
                )
            )
        ),
        tonalElevation = 16.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (wallpaperRes != null) {
                Image(
                    painter = painterResource(wallpaperRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .blur(13.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
            )
            // Top specular gloss line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.95f),
                                Color.White.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
            Column(modifier = Modifier.padding(20.dp)) {
            Text("RECENT TASKS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Internal Windows
                items(windowStack) { win ->
                    RecentAppItem(
                        label = if (win.app == App.SystemApp) win.label else win.app.name,
                        icon = win.icon ?: Icons.Default.Window,
                        onOpen = { onAppOpen(win.app, win.packageName, win.label, win.icon) },
                        onClose = { onAppClose(win.id) }
                    )
                }
                
                // System Apps
                items(runningSystemApps) { app ->
                    RecentAppItem(
                        label = app.label,
                        icon = app.icon ?: Icons.Default.Android,
                        onOpen = { onAppOpen(App.SystemApp, app.packageName, app.label, app.icon) },
                        onClose = { onRemoveRunningSystemApp(app.packageName) }
                    )
                }
            }
            
            if (windowStack.isEmpty() && runningSystemApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active tasks", color = Color.White.copy(alpha = 0.4f))
                }
            }
        }
        }
    }
}

@Composable
fun RecentAppItem(label: String, icon: Any, onOpen: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { onOpen() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon is Drawable) {
            AsyncImage(model = icon, contentDescription = null, modifier = Modifier.size(32.dp))
        } else if (icon is ImageVector) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewAppView(url: String, externalKeyboardEvent: NativeKeyEvent? = null) {
    var webView: WebView? by remember { mutableStateOf(null) }

    LaunchedEffect(externalKeyboardEvent) {
        externalKeyboardEvent?.let { event ->
            try {
                if (event.keyCode != NativeKeyEvent.KEYCODE_BACK && 
                    event.keyCode != NativeKeyEvent.KEYCODE_HOME) {
                    webView?.dispatchKeyEvent(event)
                }
            } catch (e: Throwable) {
                Log.e("WebViewAppView", "Error dispatching key event", e)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        AndroidView(
            factory = { context ->
                try {
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        webView = this
                        loadUrl(url)
                    }
                } catch (e: Exception) {
                    Log.e("WebViewAppView", "WebView creation failed", e)
                    android.widget.TextView(context).apply { text = "App Error: ${e.message}" }
                }
            },
            update = { view ->
                if (view is WebView && view.url != url) {
                    view.loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

data class WebApp(
    val id: String,
    val name: String,
    val url: String,
    val iconEmoji: String,
    val description: String,
    val iconRes: Int? = null
)

@Composable
fun AppStoreView(
    installedAppIds: List<String>,
    onInstallApp: (String) -> Unit,
    onUninstallApp: (String) -> Unit,
    onOpenApp: (String, String) -> Unit
) {
    val appStoreApps = remember {
        listOf(
            WebApp("yt_web", "YouTube", "https://yt.be/", "📺", "Watch popular videos, music, and streams.", R.drawable.img_youtube_aero),
            WebApp("pocket_web", "Uniblox Pocket", "https://uniblox-fun.lovable.app/pocket", "🧱", "Classic sandbox block-building game in pocket edition.", R.drawable.img_uniblox_pocket),
            WebApp("vscode_web", "VS Code", "https://vscode.dev/", "💻", "Code on the go in a full-featured online development environment."),
            WebApp("minecraft_web", "Minecraft", "https://enchanting-dasik-c072d6.netlify.app/", "⛏️", "Minecraft browser edition with block placing and world building.", R.drawable.img_minecraft_aero),
            WebApp("bing_web", "Bing", "https://bing.com/", "🔍", "Search with Bing's smart AI features."),
            WebApp("scratch_web", "Scratch", "https://scratch.mit.edu", "🐈", "Create interactive games, animations, and stories."),
            WebApp("bloxd_web", "Bloxd", "https://bloxd.io/", "🧱", "Bloxd.io multiplayer block builder and mini-games."),
            WebApp("cuberealm_web", "Cube Realm", "https://cuberealm.io/", "🌍", "Explore and build inside a vast cubic online sandbox."),
            WebApp("audilos_web", "Uniblox Audilos", "https://uniblox-audilos.ai.studio/", "🎵", "Explore audio visualization and soundscapes on AI Studio."),
            WebApp("drive_web", "Google Drive", "https://drive.google.com/", "📁", "Access and share your Google Drive files in cloud storage."),
            WebApp("spotify_web", "Spotify", "https://spotify.com/", "🎵", "Listen to millions of songs, playlists, and podcasts."),
            WebApp("github_web", "GitHub", "https://GitHub.com/", "🐙", "Manage, review, and commit code with GitHub on Uniblox OS."),
            WebApp("gemini_web", "Gemini", "https://gemini.google.com/", "✨", "Supercharge your productivity with Google's advanced Gemini AI model.", R.drawable.img_gemini_aero),
            WebApp("poxel_web", "Poxel", "https://poxel.io/", "🖼️", "Poxel.io interactive drawing, painting, and art space."),
            WebApp("slither_web", "Slither.io", "http://slither.com/io", "🐍", "Grow as big as you can in the classic multiplayer snake battle arena."),
            WebApp("flappy_web", "Flappy Bird", "https://flappybird.io/", "🐦", "Tempt your patience with the addictive Flappy Bird arcade game."),
            WebApp("2048_web", "2048", "https://play2048.co/", "🔢", "Merge the numbered tiles to reach the 2048 block."),
            WebApp("crazygames_web", "CrazyGames", "https://crazygames.com/", "🎮", "Instantly play thousands of high-quality online games."),
            WebApp("lovable_web", "Lovable", "https://lovable.dev/", "❤️", "Design, build, and deploy apps in natural language."),
            WebApp("turbowarp_web", "TurboWarp Embed", "https://turbowarp.org/1109852074/embed", "🚀", "Run Scratch games up to 20x faster with enhanced embeds."),
            WebApp("speedtest_web", "Speedtest", "https://speedtest.com/", "⚡", "Instantly check your network download and upload speeds.")
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(searchQuery) {
        if (searchQuery.isEmpty()) appStoreApps
        else appStoreApps.filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F2F8))
            .padding(16.dp)
    ) {
        // App Store Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFF9C27B0))))
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = "Uniblox App Store",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Discover & install instant web-powered applications",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search apps...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF2196F3)
            ),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Grid of Apps
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredApps) { app ->
                val isInstalled = installedAppIds.contains(app.id)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Emoji Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE3F2FD)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (app.iconRes != null) {
                                AsyncImage(
                                    model = app.iconRes,
                                    contentDescription = app.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(text = app.iconEmoji, fontSize = 28.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = app.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = app.description,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.height(34.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isInstalled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { onOpenApp(app.url, app.name) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Open", fontSize = 11.sp, color = Color.White)
                                }

                                Button(
                                    onClick = { onUninstallApp(app.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Remove", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onInstallApp(app.id) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Install", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrowserView(externalKeyboardEvent: NativeKeyEvent? = null) {
    var url by remember { mutableStateOf("https://www.google.com") }
    var inputUrl by remember { mutableStateOf("https://www.google.com") }
    var webView: WebView? by remember { mutableStateOf(null) }

    LaunchedEffect(externalKeyboardEvent) {
        externalKeyboardEvent?.let { event ->
            try {
                // Only dispatch if it's not a direct system intent key
                if (event.keyCode != NativeKeyEvent.KEYCODE_BACK && 
                    event.keyCode != NativeKeyEvent.KEYCODE_HOME) {
                    webView?.dispatchKeyEvent(event)
                }
            } catch (e: Throwable) {
                Log.e("BrowserView", "Error dispatching key event", e)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Address Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEDE7F6))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                modifier = Modifier.weight(1f).background(Color.White).padding(8.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { url = inputUrl })
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { url = inputUrl }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text("Go", fontSize = 12.sp)
            }
        }

        AndroidView(
            factory = { context ->
                try {
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        webView = this
                        loadUrl(url)
                    }
                } catch (e: Exception) {
                    Log.e("BrowserView", "WebView creation failed", e)
                    android.widget.TextView(context).apply { text = "Browser Error: ${e.message}" }
                }
            },
            update = { view ->
                if (view is WebView && view.url != url) {
                    view.loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AiDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("InfinityCursor Intelligence", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Analyzing current page context using server-side Gemini intelligence...",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GameEngineView() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadUrl("https://uniblox-fun.lovable.app/pocket")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun MessagesView() {
    val messages = remember {
        listOf(
            Message("System", "Welcome to Uniblox OS 7!", "10:00 AM"),
            Message("CyberCode Team", "We hope you enjoy the desktop experience.", "10:05 AM"),
            Message("AI Bot", "InfinityCursor is active and ready to help.", "10:10 AM"),
            Message("Update", "Daily wallpaper has been refreshed.", "11:00 AM"),
            Message("Dev", "Server-side rendering is simulated for this demo.", "11:30 AM")
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Inbox", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(messages) { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                            Text(message.sender.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(message.sender, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(message.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(message.content, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

data class Message(val sender: String, val content: String, val time: String)

data class UnibloxAPI(val type: String, val config: String, val key: String)

@Composable
fun TerminalView(externalKeyboardEvent: NativeKeyEvent? = null) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    val output = remember { mutableStateListOf("Uniblox Terminal [Version 7.0.24]", "Copyright (c) Uniblox Corporation. All rights reserved.", "Temporary system files: MOUNTED & ACCESSIBLE (ezyZip.tar.gz)", "") }
    val createdAPIs = remember { mutableStateListOf<UnibloxAPI>() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(externalKeyboardEvent) {
        externalKeyboardEvent?.let { event ->
            try {
                if (event.action == NativeKeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        NativeKeyEvent.KEYCODE_ENTER -> {
                            if (input.isNotBlank()) {
                                val cmd = input.trim()
                                output.add("C:\\Users\\Administrator> $cmd")
                                processCommand(cmd, output, createdAPIs, context)
                                input = ""
                                scope.launch {
                                    delay(50)
                                    if (scrollState.maxValue > 0) {
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }
                                }
                            }
                        }
                        NativeKeyEvent.KEYCODE_DEL -> {
                            if (input.isNotEmpty()) input = input.dropLast(1)
                        }
                        else -> {
                            val unicodeChar = event.getUnicodeChar(event.metaState)
                            if (unicodeChar != 0) {
                                val char = unicodeChar.toChar()
                                if (char.isPrintable()) {
                                    input += char
                                }
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e("TerminalView", "Key event processing failed", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp)
            .verticalScroll(scrollState)
    ) {
        output.forEach { line ->
            Text(line, color = Color(0xFF00FF00), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("C:\\Users\\Administrator> ", color = Color(0xFF00FF00), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color(0xFF00FF00), fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                cursorBrush = SolidColor(Color(0xFF00FF00)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (input.isNotBlank()) {
                            val cmd = input.trim()
                            output.add("C:\\Users\\Administrator> $cmd")
                            processCommand(cmd, output, createdAPIs, context)
                            input = ""
                            scope.launch {
                                delay(100)
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        }
                    }
                )
            )
        }
    }
}

fun processCommand(cmd: String, output: MutableList<String>, createdAPIs: MutableList<UnibloxAPI>, context: Context? = null) {
    val parts = cmd.split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return

    when (parts[0].lowercase()) {
        "termux-api", "termux" -> {
            if (parts.size < 2) {
                output.add("Usage: termux-api <api_key> <command> [args]")
                output.add("Example: termux-api YOUR_GENERATED_HEX_KEY_HERE uniblox help")
                return
            }
            val expectedKey = try { 
                BuildConfig.GENERATED_HEX_KEY 
            } catch (e: Exception) { 
                "YOUR_GENERATED_HEX_KEY_HERE" 
            }
            val clientKey = parts[1]
            if (clientKey != expectedKey && expectedKey != "YOUR_GENERATED_HEX_KEY_HERE") {
                output.add("ERROR: Unauthorized. Invalid API Key.")
                return
            }
            if (clientKey == "YOUR_GENERATED_HEX_KEY_HERE" || expectedKey == "YOUR_GENERATED_HEX_KEY_HERE") {
                output.add("[WARNING] Using default placeholder API key. Please configure the secure GENERATED_HEX_KEY in AI Studio.")
            }
            
            output.add("Authentication Successful. Running uniblox...")
            if (parts.size == 2) {
                output.add("No command provided after authentication.")
                return
            }
            
            // Execute the shifted command
            val shiftedCmd = parts.subList(2, parts.size).joinToString(" ")
            processCommand(shiftedCmd, output, createdAPIs, context)
        }
        "termux-toast" -> {
            if (parts.size < 3) {
                output.add("Usage: termux-toast <api_key> <message>")
                return
            }
            val expectedKey = try { BuildConfig.GENERATED_HEX_KEY } catch (e: Exception) { "YOUR_GENERATED_HEX_KEY_HERE" }
            if (parts[1] != expectedKey && expectedKey != "YOUR_GENERATED_HEX_KEY_HERE") {
                output.add("ERROR: Unauthorized. Invalid API Key.")
                return
            }
            val msg = parts.subList(2, parts.size).joinToString(" ")
            output.add("termux-toast: Showed toast: $msg")
        }
        "termux-vibrate" -> {
            if (parts.size < 3) {
                output.add("Usage: termux-vibrate <api_key> <duration_ms>")
                return
            }
            val expectedKey = try { BuildConfig.GENERATED_HEX_KEY } catch (e: Exception) { "YOUR_GENERATED_HEX_KEY_HERE" }
            if (parts[1] != expectedKey && expectedKey != "YOUR_GENERATED_HEX_KEY_HERE") {
                output.add("ERROR: Unauthorized. Invalid API Key.")
                return
            }
            val duration = parts[2].toLongOrNull() ?: 500L
            output.add("termux-vibrate: Vibrating device for ${duration}ms")
        }
        "termux-battery-status" -> {
            if (parts.size < 2) {
                output.add("Usage: termux-battery-status <api_key>")
                return
            }
            val expectedKey = try { BuildConfig.GENERATED_HEX_KEY } catch (e: Exception) { "YOUR_GENERATED_HEX_KEY_HERE" }
            if (parts[1] != expectedKey && expectedKey != "YOUR_GENERATED_HEX_KEY_HERE") {
                output.add("ERROR: Unauthorized. Invalid API Key.")
                return
            }
            output.add("termux-battery-status:")
            output.add("  Health: GOOD")
            output.add("  Percentage: 78%")
            output.add("  Status: DISCHARGING")
            output.add("  Temperature: 28.5 C")
        }
        "uniblox" -> {
            if (parts.size == 1) {
                output.add("Error: 'uniblox' requires arguments. Type 'uniblox help' for usage.")
                return
            }
            when (parts[1].lowercase()) {
                "help" -> {
                    output.add("Available Uniblox Commands:")
                    output.add("  uniblox help                             - Show this help message")
                    output.add("  uniblox status                           - Display temporary storage status")
                    output.add("  uniblox -a open <file>                   - Open and read specified temporary file")
                    output.add("  uniblox run <file>                       - Execute temporary system script")
                    output.add("  uniblox dll -d /a /r /m take_ownership    - Take ownership of system components")
                    output.add("  uniblox install <package>                - Install specified command package")
                    output.add("  uniblox --action . createAPI -- <type> -- <config> #copyAPI")
                    output.add("  termux-api <key> <command> [args]         - Secure Termux API Gateway")
                    output.add("  termux-toast <key> <message>             - Show simulated android toast")
                    output.add("  termux-vibrate <key> <duration_ms>       - Trigger device vibration")
                    output.add("  termux-battery-status <key>              - Check device battery metrics")
                    output.add("  ls [-a|-i|-d] [subfolder]                - List temporary files and directories")
                    output.add("  cat <file>                               - Display temporary file contents")
                    output.add("  api list                                 - List all created API keys")
                    output.add("  api write                                - Write API configuration")
                }
                "status" -> {
                    val tempDir = if (context != null) SystemFilesManager.getTempDir(context) else null
                    output.add("UNIBLOX SYSTEM STATUS:")
                    output.add("  Temporary Storage: MOUNTED & ACCESSIBLE")
                    output.add("  Archive: ezyZip.tar.gz (Extracted)")
                    output.add("  Path: ${tempDir?.absolutePath ?: "uniblox_system_files"}")
                    output.add("  Files: api_read_write.upk, hal.uea, casm/, system32/")
                }
                "-a" -> {
                    if (parts.size > 3 && parts[2] == "open") {
                        val filename = parts.subList(3, parts.size).joinToString(" ").trim()
                        output.add("Opening temporary file: $filename...")
                        val content = if (context != null) SystemFilesManager.readFile(context, filename) else null
                        if (content != null) {
                            output.add("Access granted. Component loaded from temporary storage:")
                            output.add("----------------------------------------")
                            content.lines().take(30).forEach { output.add(it) }
                            if (content.lines().size > 30) {
                                output.add("... [${content.lines().size - 30} more lines]")
                            }
                        } else {
                            output.add("Access granted. Loading component...")
                        }
                    } else {
                        output.add("Usage: uniblox -a open <file>")
                    }
                }
                "run" -> {
                    if (parts.size > 2) {
                        val filename = parts[2]
                        output.add("Executing temporary script: $filename...")
                        val res = if (context != null) {
                            SystemFilesManager.executeScript(context, filename) { _, _ -> }
                        } else "Executed: $filename"
                        res.lines().forEach { output.add(it) }
                    } else {
                        output.add("Usage: uniblox run <file>")
                    }
                }
                "dll" -> {
                    if (cmd.contains("take_ownership")) {
                        output.add("Injecting DLL into kernel memory...")
                        output.add("Privilege escalation: SUCCESS")
                        output.add("Ownership of 'SYSTEM_CORE' has been transferred to Administrator.")
                    } else {
                        output.add("Usage: uniblox dll -d /a /r /m take_ownership")
                    }
                }
                "install" -> {
                    if (parts.size > 2) {
                        output.add("Fetching package '${parts[2]}' from Uniblox repository...")
                        output.add("Extracting binaries...")
                        output.add("Registering command in PATH...")
                        output.add("Installation of '${parts[2]}' complete.")
                    } else {
                        output.add("Usage: uniblox install <package>")
                    }
                }
                "--action" -> {
                    if (cmd.contains("createAPI")) {
                        // Pattern: uniblox --action . createAPI -- <type> -- <config>
                        val apiType = if (cmd.contains("--")) {
                             val sub = cmd.substringAfter("createAPI -- ").substringBefore(" --")
                             if (sub.isNotBlank()) sub else "DEFAULT"
                        } else "DEFAULT"
                        
                        val apiConfig = if (cmd.count { it == '-' } >= 4) {
                             cmd.substringAfterLast("-- ").substringBefore(" #").trim()
                        } else "READ"

                        val key = "UBX-${java.util.UUID.randomUUID().toString().take(8).uppercase()}"
                        val newApi = UnibloxAPI(apiType, apiConfig, key)
                        createdAPIs.add(newApi)

                        output.add("Initializing API Creation Wizard...")
                        output.add("Setting API Type: $apiType")
                        output.add("Configuring Permissions: $apiConfig")
                        output.add("Generating cryptographic key...")
                        output.add("API created successfully. Key: $key")
                        output.add("API state: SAVED TO SYSTEM ENCLAVE")
                    } else {
                        output.add("Usage: uniblox --action . createAPI -- <type> -- <config>")
                    }
                }
                else -> output.add("Unknown uniblox command: ${parts[1]}")
            }
        }
        "ls", "dir" -> {
            val sub = if (parts.size > 1 && !parts[1].startsWith("-")) parts[1].replace("\\", "/") else ""
            val tempDir = if (context != null) SystemFilesManager.getTempDir(context) else null
            output.add("Listing directory: C:\\Uniblox\\TempSystemFiles" + (if (sub.isNotEmpty()) "\\$sub" else ""))
            output.add("  Temporary Storage: MOUNTED (Archive: ezyZip.tar.gz)")
            output.add("  .                   [DIR]")
            output.add("  ..                  [DIR]")
            if (context != null && tempDir != null) {
                val targetDir = if (sub.isNotEmpty()) java.io.File(tempDir, sub) else tempDir
                if (targetDir.exists() && targetDir.isDirectory) {
                    val files = targetDir.listFiles()?.filter { !it.name.startsWith(".") } ?: emptyList()
                    files.sortedWith(compareBy({ !it.isDirectory }, { it.name })).forEach { f ->
                        val typeStr = if (f.isDirectory) "[DIR]" else "${f.length()} B"
                        output.add("  %-20s %s".format(f.name, typeStr))
                    }
                } else {
                    output.add("  Directory not found: $sub")
                }
            } else {
                output.add("  api_read_write.upk  [394 B]")
                output.add("  hal.uea             [744 B]")
                output.add("  casm                [DIR]")
                output.add("  system32            [DIR]")
            }
            if (createdAPIs.isNotEmpty()) {
                output.add("  api_keys.vault      [SECURE_STORAGE]")
            }
        }
        "cat", "type", "head" -> {
            if (parts.size < 2) {
                output.add("Usage: ${parts[0]} <filename>")
                return
            }
            val filename = parts[1].replace("\\", "/")
            val content = if (context != null) SystemFilesManager.readFile(context, filename) else null
            if (content != null) {
                output.add("--- File: $filename (${content.length} bytes) ---")
                content.lines().forEach { output.add(it) }
            } else {
                output.add("Error: File '$filename' not found in temporary system directory.")
            }
        }
        "run" -> {
            if (parts.size < 2) {
                output.add("Usage: run <filename>")
                return
            }
            val filename = parts[1].replace("\\", "/")
            output.add("Executing: $filename...")
            val res = if (context != null) {
                SystemFilesManager.executeScript(context, filename) { _, _ -> }
            } else "Executed: $filename"
            res.lines().forEach { output.add(it) }
        }
        "status" -> {
            val tempDir = if (context != null) SystemFilesManager.getTempDir(context) else null
            output.add("TEMPORARY STORAGE STATUS:")
            output.add("  Extracted Archive: ezyZip.tar.gz")
            output.add("  Access Path: ${tempDir?.absolutePath ?: "/data/cache/uniblox_system_files"}")
            output.add("  State: READ/WRITE TEMPORARY STORAGE READY")
        }
        "api" -> {
            if (parts.size > 1) {
                when (parts[1].lowercase()) {
                    "write" -> {
                        if (createdAPIs.isEmpty()) {
                            output.add("Error: No active APIs found. Create an API first.")
                        } else {
                            output.add("Opening API Stream...")
                            output.add("Target: ${createdAPIs.last().key}")
                            output.add("Writing bytes to config.api...")
                            output.add("Sync complete. API state: ACTIVE")
                        }
                    }
                    "list" -> {
                        if (createdAPIs.isEmpty()) {
                            output.add("No API keys found in system vault.")
                        } else {
                            output.add("ACTIVE UNIBLOX API KEYS:")
                            createdAPIs.forEach { api ->
                                output.add("  - Key: ${api.key} [Type: ${api.type}] [Config: ${api.config}]")
                            }
                        }
                    }
                    else -> output.add("Usage: api [write|list]")
                }
            } else {
                output.add("Usage: api [write|list]")
            }
        }
        "help" -> {
            output.add("Type 'uniblox help' for the official Uniblox OS command reference.")
        }
        "clear" -> {
            output.clear()
            output.add("Uniblox Terminal [Version 7.0.24]")
            output.add("")
        }
        else -> {
            output.add("'$cmd' is not recognized as an internal or external command, operable program or batch file.")
        }
    }
    output.add("")
}

@Composable
fun RunDialog(onDismiss: () -> Unit, onRun: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF3F3F3),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
            modifier = Modifier.width(400.dp).padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0078D7), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Run", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Type the name of a program, folder, document, or Internet resource, and Uniblox will open it for you.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Open:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f).background(Color.White, RoundedCornerShape(2.dp)).border(1.dp, Color.Gray, RoundedCornerShape(2.dp)).padding(4.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onRun(text) })
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = { onRun(text) },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1E1E1), contentColor = Color.Black),
                        border = BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier.width(80.dp)
                    ) {
                        Text("OK", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1E1E1), contentColor = Color.Black),
                        border = BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier.width(80.dp)
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

enum class SettingsCategory {
    Personalization,
    Keyboard,
    System,
    Advanced,
    About
}

@Composable
fun SettingsView(
    defaultLaunchFullscreen: Boolean = false,
    onToggleDefaultLaunchFullscreen: () -> Unit = {},
    currentTheme: OSTheme,
    onThemeChange: (OSTheme) -> Unit,
    showTaskbarBackground: Boolean = false,
    onToggleTaskbarBackground: () -> Unit = {},
    useOverlayKeyboard: Boolean = false,
    onToggleOverlayKeyboard: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(SettingsCategory.Personalization) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar
        Surface(
            modifier = Modifier.width(240.dp).fillMaxHeight(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 20.dp)
                )
                
                SettingsCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { selectedCategory = category },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when(category) {
                                    SettingsCategory.Personalization -> Icons.Default.Palette
                                    SettingsCategory.Keyboard -> Icons.Default.Keyboard
                                    SettingsCategory.System -> Icons.Default.Settings
                                    SettingsCategory.Advanced -> Icons.Default.AutoAwesome
                                    SettingsCategory.About -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

        // Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(32.dp)
        ) {
            Text(
                selectedCategory.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            when (selectedCategory) {
                SettingsCategory.Personalization -> PersonalizationSettings(currentTheme, onThemeChange, showTaskbarBackground, onToggleTaskbarBackground)
                SettingsCategory.Keyboard -> KeyboardSettings(context, useOverlayKeyboard, onToggleOverlayKeyboard)
                SettingsCategory.System -> SystemSettings(context)
                SettingsCategory.Advanced -> AdvancedSettings(defaultLaunchFullscreen, onToggleDefaultLaunchFullscreen)
                SettingsCategory.About -> AboutSettings()
            }
        }
    }
}

@Composable
fun PersonalizationSettings(
    currentTheme: OSTheme,
    onThemeChange: (OSTheme) -> Unit,
    showTaskbarBackground: Boolean,
    onToggleTaskbarBackground: () -> Unit
) {
    Text("Themes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier.height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(OSTheme.entries) { theme ->
                    val isSelected = currentTheme == theme
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable { onThemeChange(theme) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                theme.name,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Text("Taskbar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show Taskbar Background", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Enable glassmorphism effect on the taskbar.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = showTaskbarBackground, onCheckedChange = { onToggleTaskbarBackground() })
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Text("Frosted Glass (GlassCard)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        blurRadius = 20f
    )
}

@Composable
fun KeyboardSettings(
    context: Context,
    useOverlayKeyboard: Boolean,
    onToggleOverlayKeyboard: (Boolean) -> Unit
) {
    val prefs = remember { context.getSharedPreferences("uniblox_settings", Context.MODE_PRIVATE) }
    var forceKeyboard by remember { mutableStateOf(prefs.getBoolean("force_on_screen_keyboard", false)) }

    Text("Uniblox Keyboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Uniblox Keyboard provides desktop-style keys like Ctrl, Alt, and Esc. Essential for Terminal and Web IDEs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("1. Enable", fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.showInputMethodPicker()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("2. Switch", fontSize = 12.sp)
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Force On-Screen Keyboard", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("Always show even if hardware keyboard is connected.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = forceKeyboard,
                    onCheckedChange = { 
                        forceKeyboard = it
                        prefs.edit().putBoolean("force_on_screen_keyboard", it).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Overlay Keyboard Mode", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("Floating keyboard that stays on top of all windows.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = useOverlayKeyboard,
                    onCheckedChange = { 
                        if (it && !android.provider.Settings.canDrawOverlays(context)) {
                            context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                        } else {
                            onToggleOverlayKeyboard(it)
                            prefs.edit().putBoolean("use_overlay_keyboard", it).apply()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SystemSettings(context: Context) {
    Text("System Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val canWrite = android.provider.Settings.System.canWrite(context)
            SettingActionItem(
                title = "Modify System Settings",
                description = if (canWrite) "Permission Granted" else "Required for deep OS integration.",
                icon = if (canWrite) Icons.Default.CheckCircle else Icons.Default.Handyman,
                onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = android.net.Uri.parse("package:" + context.packageName)
                    }
                    context.startActivity(intent)
                }
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            SettingActionItem(
                title = "Usage Access",
                description = "Required for task tracking and performance stats.",
                icon = Icons.Default.Timeline,
                onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Text("Advanced Integrations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingActionItem(
                title = "Google Settings",
                description = "Manage accounts, sync, and privacy.",
                icon = Icons.Default.AccountCircle,
                onClick = {
                    try {
                        context.startActivity(Intent("com.google.android.gms.settings.GOOGLE_SETTINGS"))
                    } catch (e: Exception) {
                        context.startActivity(Intent(android.provider.Settings.ACTION_SYNC_SETTINGS))
                    }
                }
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            SettingActionItem(
                title = "Developer Options",
                description = "Enable USB debugging and advanced tools.",
                icon = Icons.Default.Code,
                onClick = {
                    try {
                        context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    } catch (e: Exception) {
                        context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                    }
                }
            )
        }
    }
}

@Composable
fun AdvancedSettings(defaultLaunchFullscreen: Boolean, onToggleDefaultLaunchFullscreen: () -> Unit) {
    Text("OS Behavior", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Default Fullscreen Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Open apps in fullscreen by default instead of windows.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = defaultLaunchFullscreen, onCheckedChange = { onToggleDefaultLaunchFullscreen() })
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Text("Experimental", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
    SettingsToggle("Force Resizable Activities", "Enable multitasking for all apps.", true)
    SettingsToggle("Glassmorphism Effects", "Advanced GPU blur for windows.", true)
}

@Composable
fun AboutSettings() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Uniblox OS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Pro Edition v7.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Build Number", style = MaterialTheme.typography.bodySmall)
                Text("UNIBLOX-2026.10.03", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kernel Version", style = MaterialTheme.typography.bodySmall)
                Text("Linux 5.15.0-x64", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(start = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingsToggle(title: String, subtitle: String, initialState: Boolean) {
    var checked by remember { mutableStateOf(initialState) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
}

@Composable
fun EasterEggClock(onConfirm: (Int, Int) -> Unit, onCancel: () -> Unit) {
    var hour by remember { mutableIntStateOf(12) }
    var minute by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Set System Time", fontWeight = FontWeight.Bold) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                ClockPicker(value = hour, onValueChange = { hour = it }, range = 0..23)
                Text(":", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                ClockPicker(value = minute, onValueChange = { minute = it }, range = 0..59)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(hour, minute) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ClockPicker(value: Int, onValueChange: (Int) -> Unit, range: IntRange) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { if (value < range.last) onValueChange(value + 1) else onValueChange(range.first) }) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
        }
        Text(value.toString().padStart(2, '0'), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = { if (value > range.first) onValueChange(value - 1) else onValueChange(range.last) }) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }
    }
}

data class Pipe(val x: Float, val gapTop: Float)

@Composable
fun FlappyBirdGame(onClose: () -> Unit) {
    var birdY by remember { mutableFloatStateOf(300f) }
    var birdVelocity by remember { mutableFloatStateOf(0f) }
    var gameRunning by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    val gravity = 0.6f
    val jumpStrength = -10f

    val pipes = remember { mutableStateListOf<Pipe>() }
    val density = LocalDensity.current

    LaunchedEffect(gameRunning) {
        if (gameRunning) {
            pipes.clear()
            pipes.add(Pipe(800f, (100..350).random().toFloat()))
            birdY = 300f
            birdVelocity = 0f
            score = 0
            while(gameRunning) {
                birdVelocity += gravity
                birdY += birdVelocity
                
                // Move pipes
                for(i in pipes.indices) {
                    pipes[i] = pipes[i].copy(x = pipes[i].x - 6f)
                }
                
                // Add new pipe
                if (pipes.last().x < 450f) {
                    pipes.add(Pipe(800f, (100..350).random().toFloat()))
                }
                
                // Remove old pipe
                if (pipes.first().x < -100f) {
                    pipes.removeAt(0)
                    score++
                }
                
                // Collision
                if (birdY < 0 || birdY > 600) gameRunning = false
                pipes.forEach { pipe ->
                    if (pipe.x < 180f && pipe.x > 100f) {
                        if (birdY < pipe.gapTop || birdY > pipe.gapTop + 160f) {
                            gameRunning = false
                        }
                    }
                }
                
                delay(16)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4FC3F7))
            .clickable(enabled = gameRunning) { birdVelocity = jumpStrength },
        contentAlignment = Alignment.Center
    ) {
        // Bird
        Box(
            modifier = Modifier
                .offset(y = (birdY - 300f).dp, x = (-100).dp)
                .size(36.dp)
                .background(Color.Yellow, CircleShape)
                .border(2.dp, Color.Black, CircleShape)
        ) {
            Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape).align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp))
        }
        
        // Pipes
        pipes.forEach { pipe ->
            // Top pipe
            Box(
                modifier = Modifier
                    .offset(x = (pipe.x - 400f).dp, y = (pipe.gapTop / 2f - 300f).dp)
                    .width(60.dp)
                    .height(pipe.gapTop.dp)
                    .background(Color(0xFF4CAF50))
                    .border(3.dp, Color.Black)
            )
            // Bottom pipe
            val bottomPipeHeight = 600f - (pipe.gapTop + 160f)
            Box(
                modifier = Modifier
                    .offset(x = (pipe.x - 400f).dp, y = (pipe.gapTop + 160f + bottomPipeHeight / 2f - 300f).dp)
                    .width(60.dp)
                    .height(bottomPipeHeight.dp)
                    .background(Color(0xFF4CAF50))
                    .border(3.dp, Color.Black)
            )
        }
        
        Text(
            text = "Score: $score", 
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp), 
            color = Color.White, 
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)
            )
        )
        
        if (!gameRunning) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxSize(),
                contentColor = Color.White
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (score == 0) "FLAPPY UNIBLOX" else "GAME OVER", 
                        style = MaterialTheme.typography.displayMedium, 
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (score > 0) Text("FINAL SCORE: $score", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { gameRunning = true },
                        modifier = Modifier.width(200.dp).height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("START GAME", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onClose) {
                        Text("EXIT TO DESKTOP", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

fun Char.isPrintable(): Boolean {
    return !Character.isISOControl(this) && this != NativeKeyEvent.KEYCODE_UNKNOWN.toChar()
}

@Composable
fun KeyboardView(onKeyboardEvent: (NativeKeyEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2D2D2D))
            .padding(4.dp)
    ) {
        var isShifted by remember { mutableStateOf(false) }
        var isCtrlPressed by remember { mutableStateOf(false) }
        var isAltPressed by remember { mutableStateOf(false) }

        val sendKey = { keyCode: Int, action: Int ->
            if (keyCode != NativeKeyEvent.KEYCODE_UNKNOWN) {
                val eventTime = System.currentTimeMillis()
                var meta = 0
                if (isShifted) meta = meta or NativeKeyEvent.META_SHIFT_ON
                if (isCtrlPressed) meta = meta or NativeKeyEvent.META_CTRL_ON
                if (isAltPressed) meta = meta or NativeKeyEvent.META_ALT_ON

                try {
                    onKeyboardEvent(
                        NativeKeyEvent(
                            eventTime, eventTime,
                            action,
                            keyCode, 0, meta,
                            KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                            NativeKeyEvent.FLAG_SOFT_KEYBOARD or NativeKeyEvent.FLAG_KEEP_TOUCH_MODE
                        )
                    )
                } catch (e: Exception) {
                    Log.e("KeyboardView", "Failed to send key", e)
                }
            }
        }

        // Special Top Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            KeyboardWindowKey("ESC", weight = 1f, onAction = { sendKey(NativeKeyEvent.KEYCODE_ESCAPE, it) })
            KeyboardWindowKey("TAB", weight = 1f, onAction = { sendKey(NativeKeyEvent.KEYCODE_TAB, it) })
            KeyboardWindowKey("CTRL", weight = 1f, isPressed = isCtrlPressed, onAction = { if (it == NativeKeyEvent.ACTION_DOWN) isCtrlPressed = !isCtrlPressed })
            KeyboardWindowKey("ALT", weight = 1f, isPressed = isAltPressed, onAction = { if (it == NativeKeyEvent.ACTION_DOWN) isAltPressed = !isAltPressed })
            KeyboardWindowKey(Icons.Default.ArrowUpward, weight = 1f, onAction = { sendKey(NativeKeyEvent.KEYCODE_DPAD_UP, it) })
            KeyboardWindowKey("DEL", weight = 1f, onAction = { sendKey(NativeKeyEvent.KEYCODE_FORWARD_DEL, it) })
        }

        Spacer(modifier = Modifier.height(4.dp))
        
        // Numbers Row
        KeyboardWindowRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), sendKey)
        Spacer(modifier = Modifier.height(4.dp))

        // QWERTY rows
        KeyboardWindowRow(listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"), sendKey)
        KeyboardWindowRow(listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"), sendKey)
        
        Row(modifier = Modifier.fillMaxWidth()) {
            KeyboardWindowKey(Icons.Default.KeyboardArrowUp, weight = 1.5f, isPressed = isShifted, onAction = { if (it == NativeKeyEvent.ACTION_DOWN) isShifted = !isShifted })
            KeyboardWindowRowItems(listOf("Z", "X", "C", "V", "B", "N", "M"), sendKey, weight = 7f)
            KeyboardWindowKey(Icons.Default.Backspace, weight = 1.5f, onAction = { sendKey(NativeKeyEvent.KEYCODE_DEL, it) })
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom Row
        Row(modifier = Modifier.fillMaxWidth()) {
            KeyboardWindowKey("SPACE", weight = 6f, onAction = { sendKey(NativeKeyEvent.KEYCODE_SPACE, it) })
            KeyboardWindowKey(Icons.Default.ArrowBack, weight = 1f, onAction = { sendKey(NativeKeyEvent.KEYCODE_DPAD_LEFT, it) })
            KeyboardWindowKey(Icons.Default.ArrowForward, weight = 1f, onAction = { sendKey(NativeKeyEvent.KEYCODE_DPAD_RIGHT, it) })
            KeyboardWindowKey("ENTER", weight = 2f, color = Color(0xFF3498DB), onAction = { sendKey(NativeKeyEvent.KEYCODE_ENTER, it) })
        }
    }
}

@Composable
fun KeyboardWindowRow(keys: List<String>, sendKey: (Int, Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        keys.forEach { key ->
            val keyCode = NativeKeyEvent.keyCodeFromString("KEYCODE_" + key.uppercase())
            KeyboardWindowKey(
                label = key,
                weight = 1f,
                onAction = { action ->
                    if (keyCode != NativeKeyEvent.KEYCODE_UNKNOWN) {
                        sendKey(keyCode, action)
                    }
                }
            )
        }
    }
}

@Composable
fun RowScope.KeyboardWindowRowItems(keys: List<String>, sendKey: (Int, Int) -> Unit, weight: Float) {
    Row(modifier = Modifier.weight(weight)) {
        keys.forEach { key ->
            val keyCode = NativeKeyEvent.keyCodeFromString("KEYCODE_" + key.uppercase())
            KeyboardWindowKey(
                label = key,
                weight = 1f,
                onAction = { action ->
                    if (keyCode != NativeKeyEvent.KEYCODE_UNKNOWN) {
                        sendKey(keyCode, action)
                    }
                }
            )
        }
    }
}

@Composable
fun RowScope.KeyboardWindowKey(
    label: Any, 
    weight: Float, 
    isPressed: Boolean = false, 
    color: Color = Color(0xFF444444),
    onAction: (Int) -> Unit = {}
) {
    var isCurrentlyPressed by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .weight(weight)
            .height(40.dp)
            .padding(1.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isCurrentlyPressed = true
                        onAction(NativeKeyEvent.ACTION_DOWN)
                        
                        waitForUpOrCancellation()
                        isCurrentlyPressed = false
                        onAction(NativeKeyEvent.ACTION_UP)
                    }
                }
            },
        shape = RoundedCornerShape(4.dp),
        color = if (isPressed || isCurrentlyPressed) Color(0xFF00A2E8) else color,
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (label) {
                is String -> Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                is ImageVector -> Icon(label, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun SettingActionItem(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
