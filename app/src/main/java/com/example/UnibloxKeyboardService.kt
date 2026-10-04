package com.example

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.KeyEvent
import android.view.KeyCharacterMap
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerEventType

class UnibloxKeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val _viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = _viewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this)
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        
        composeView.setContent {
            UnibloxKeyboardUI(this)
        }
        return composeView
    }

    private fun isForceKeyboardEnabled(): Boolean {
        val prefs = getSharedPreferences("uniblox_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("force_on_screen_keyboard", false)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    // Helper to send key events
    fun sendHardwareKeyEvent(keyCode: Int, isDown: Boolean, metaState: Int = 0) {
        val ic = currentInputConnection ?: return
        val eventTime = System.currentTimeMillis()
        ic.sendKeyEvent(
            KeyEvent(
                eventTime, eventTime,
                if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP,
                keyCode, 0, metaState, 
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // This could be used to notify the UI of hardware keyboard changes
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Intercept hardware keyboard events to ensure they are handled as "computer" inputs
        // This is where we can translate mobile-specific behaviors if needed
        return super.onKeyDown(keyCode, event)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false
    override fun onEvaluateInputViewShown(): Boolean = true

    fun sendText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    fun sendBackspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    fun sendEnter() {
        sendHardwareKeyEvent(KeyEvent.KEYCODE_ENTER, true)
        sendHardwareKeyEvent(KeyEvent.KEYCODE_ENTER, false)
    }
}

@Composable
fun UnibloxKeyboardUI(service: UnibloxKeyboardService) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    
    // Read the force setting from shared prefs
    var forceOnScreen by remember { 
        mutableStateOf(context.getSharedPreferences("uniblox_settings", Context.MODE_PRIVATE)
            .getBoolean("force_on_screen_keyboard", false))
    }
    
    val isHardwareConnected = configuration.keyboard != Configuration.KEYBOARD_NOKEYS
    
    if (isHardwareConnected && !forceOnScreen) {
        HardwareKeyboardModeUI(onExpand = { forceOnScreen = true })
    } else {
        OnScreenKeyboardUI(service)
    }
}

@Composable
fun HardwareKeyboardModeUI(onExpand: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color(0xFF1E1E1E))
            .clickable { onExpand() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Keyboard, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hardware Keyboard Connected", color = Color.LightGray, fontSize = 12.sp)
            }
            Text("TAP TO SHOW ON-SCREEN", color = Color(0xFF00A2E8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OnScreenKeyboardUI(service: UnibloxKeyboardService) {
    var isShifted by remember { mutableStateOf(false) }
    var isCtrlPressed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2D2D))
            .padding(4.dp)
    ) {
        var isAltPressed by remember { mutableStateOf(false) }

        val getMeta = {
            var meta = 0
            if (isShifted) meta = meta or KeyEvent.META_SHIFT_ON
            if (isCtrlPressed) meta = meta or KeyEvent.META_CTRL_ON
            if (isAltPressed) meta = meta or KeyEvent.META_ALT_ON
            meta
        }

        // Special Top Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            KeyboardKey("ESC", weight = 1f, onPress = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_ESCAPE, true, getMeta()) }, onRelease = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_ESCAPE, false, getMeta()) })
            KeyboardKey("TAB", weight = 1f, onPress = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_TAB, true, getMeta()) }, onRelease = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_TAB, false, getMeta()) })
            KeyboardKey("CTRL", weight = 1f, isPressed = isCtrlPressed, onPress = { isCtrlPressed = !isCtrlPressed })
            KeyboardKey("ALT", weight = 1f, isPressed = isAltPressed, onPress = { isAltPressed = !isAltPressed })
            KeyboardKey("START", weight = 1f, onPress = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_META_LEFT, true, getMeta()) }, onRelease = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_META_LEFT, false, getMeta()) })
            KeyboardKey(Icons.Default.AutoAwesome, weight = 1f, color = Color(0xFF9B59B6), onPress = { service.sendText("/gemini ") })
            KeyboardKey(Icons.Default.ArrowUpward, weight = 1f, onPress = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_DPAD_UP, true, getMeta()) }, onRelease = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_DPAD_UP, false, getMeta()) })
            KeyboardKey("DEL", weight = 1f, onPress = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_FORWARD_DEL, true, getMeta()) }, onRelease = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_FORWARD_DEL, false, getMeta()) })
        }

        Spacer(modifier = Modifier.height(4.dp))
        
        // Numbers Row
        KeyboardRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), service, getMeta)
        Spacer(modifier = Modifier.height(4.dp))

        // QWERTY rows (simplified for brevity but functional)
        KeyboardRow(listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"), service, getMeta)
        KeyboardRow(listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"), service, getMeta)
        
        Row(modifier = Modifier.fillMaxWidth()) {
            KeyboardKey(Icons.Default.KeyboardArrowUp, weight = 1.5f, isPressed = isShifted, onPress = { isShifted = !isShifted })
            KeyboardRowItems(listOf("Z", "X", "C", "V", "B", "N", "M"), service, getMeta, weight = 7f)
            KeyboardKey(Icons.Default.Backspace, weight = 1.5f, onPress = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_DEL, true, getMeta()) }, onRelease = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_DEL, false, getMeta()) })
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom Row
        Row(modifier = Modifier.fillMaxWidth()) {
            KeyboardKey("123", weight = 1.5f) { /* Switch to symbols */ }
            KeyboardKey(Icons.Default.Language, weight = 1f) { /* Switch language */ }
            KeyboardKey("SPACE", weight = 4f, onPress = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_SPACE, true, getMeta()) }, onRelease = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_SPACE, false, getMeta()) })
            KeyboardKey(Icons.Default.ArrowBack, weight = 1f, onPress = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, true, getMeta()) }, onRelease = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, false, getMeta()) })
            KeyboardKey(Icons.Default.ArrowForward, weight = 1f, onPress = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT, true, getMeta()) }, onRelease = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT, false, getMeta()) })
            KeyboardKey(Icons.Default.KeyboardArrowDown, weight = 1f, onPress = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN, true, getMeta()) }, onRelease = { service.sendHardwareKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN, false, getMeta()) })
            KeyboardKey("ENTER", weight = 1.5f, color = Color(0xFF3498DB), onPress = { service.sendEnter() })
        }
    }
}

@Composable
fun KeyboardRow(keys: List<String>, service: UnibloxKeyboardService, getMeta: () -> Int) {
    Row(modifier = Modifier.fillMaxWidth()) {
        keys.forEach { key ->
            val keyCode = KeyEvent.keyCodeFromString("KEYCODE_" + key.uppercase())
            KeyboardKey(
                label = key,
                weight = 1f,
                onPress = {
                    val meta = getMeta()
                    val isShifted = (meta and KeyEvent.META_SHIFT_ON) != 0
                    
                    if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                        service.sendHardwareKeyEvent(keyCode, true, meta)
                    } else {
                        // Fallback to text if unknown
                        service.sendText(if (isShifted) key.uppercase() else key.lowercase())
                    }
                },
                onRelease = {
                    if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                        service.sendHardwareKeyEvent(keyCode, false, getMeta())
                    }
                }
            )
        }
    }
}

@Composable
fun RowScope.KeyboardRowItems(keys: List<String>, service: UnibloxKeyboardService, getMeta: () -> Int, weight: Float) {
    Row(modifier = Modifier.weight(weight)) {
        keys.forEach { key ->
            val keyCode = KeyEvent.keyCodeFromString("KEYCODE_" + key.uppercase())
            KeyboardKey(
                label = key,
                weight = 1f,
                onPress = {
                    val meta = getMeta()
                    val isShifted = (meta and KeyEvent.META_SHIFT_ON) != 0
                    
                    if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                        service.sendHardwareKeyEvent(keyCode, true, meta)
                    } else {
                        service.sendText(if (isShifted) key.uppercase() else key.lowercase())
                    }
                },
                onRelease = {
                    if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                        service.sendHardwareKeyEvent(keyCode, false, getMeta())
                    }
                }
            )
        }
    }
}

@Composable
fun RowScope.KeyboardKey(
    label: Any, 
    weight: Float, 
    isPressed: Boolean = false, 
    color: Color = Color(0xFF444444),
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {}
) {
    var isCurrentlyPressed by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .weight(weight)
            .height(50.dp)
            .padding(2.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isCurrentlyPressed = true
                        onPress()
                        
                        waitForUpOrCancellation()
                        isCurrentlyPressed = false
                        onRelease()
                    }
                }
            },
        shape = RoundedCornerShape(6.dp),
        color = if (isPressed || isCurrentlyPressed) Color(0xFF00A2E8) else color,
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (label) {
                is String -> Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                is ImageVector -> Icon(label, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
