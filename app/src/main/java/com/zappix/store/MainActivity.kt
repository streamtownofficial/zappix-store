package com.zappix.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private val Bg = Color(0xFF050812)
private val CardBg = Color(0xFF111827)
private val Blue = Color(0xFF16C7FF)
private val Violet = Color(0xFF7047FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = CardBg, primary = Blue)) {
                ZappixStore()
            }
        }
    }
}

@Composable
private fun ZappixStore(vm: StoreViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<StoreApp?>(null) }

    if (selected != null) {
        AppDetails(app = selected!!, onBack = { selected = null })
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Bg).padding(horizontal = 54.dp, vertical = 26.dp)
    ) {
        Header(onRefresh = vm::refresh)
        Spacer(Modifier.height(22.dp))

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Blue) }
            state.error != null -> ErrorState(state.error!!, vm::refresh)
            else -> {
                val free = state.apps.filter { it.type == AppType.FREE }
                val subscription = state.apps.filter { it.type == AppType.SUBSCRIPTION }
                StoreSection("Free Apps", free, onOpen = { selected = it })
                Spacer(Modifier.height(30.dp))
                StoreSection("Subscription Apps", subscription, onOpen = { selected = it })
            }
        }
    }
}

@Composable
private fun Header(onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.zappix_icon),
            contentDescription = "Zappix",
            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(15.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(14.dp))
        Text("Zappix", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        FocusButton("Refresh", onClick = onRefresh)
    }
}

@Composable
private fun StoreSection(title: String, apps: List<StoreApp>, onOpen: (StoreApp) -> Unit) {
    Column {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 25.sp)
        Spacer(Modifier.height(14.dp))
        if (apps.isEmpty()) {
            Text("No apps in this section yet.", color = Color(0xFF8693A6), fontSize = 16.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(vertical = 7.dp, horizontal = 3.dp)) {
                items(apps, key = { it.id }) { app -> AppCard(app, onClick = { onOpen(app) }) }
            }
        }
    }
}

@Composable
private fun AppCard(app: StoreApp, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.07f else 1f, label = "scale")
    val border by animateColorAsState(if (focused) Blue else Color.Transparent, label = "border")

    Surface(
        onClick = onClick,
        color = CardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(210.dp)
            .height(262.dp)
            .scale(scale)
            .border(3.dp, border, RoundedCornerShape(16.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
    ) {
        Column(Modifier.padding(15.dp)) {
            AsyncImage(
                model = app.iconUrl,
                contentDescription = app.name,
                modifier = Modifier.size(118.dp).align(Alignment.CenterHorizontally).clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(14.dp))
            Text(app.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(
                if (app.type == AppType.FREE) "FREE" else (app.priceLabel ?: "SUBSCRIPTION"),
                color = if (app.type == AppType.FREE) Blue else Color(0xFF9D87FF),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun AppDetails(app: StoreApp, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val installer = remember { ApkInstaller(context.applicationContext) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)

    Row(
        Modifier.fillMaxSize().background(Bg).padding(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = app.iconUrl,
            contentDescription = app.name,
            modifier = Modifier.size(240.dp).clip(RoundedCornerShape(34.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(48.dp))
        Column(Modifier.weight(1f)) {
            Text(app.name, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                if (app.type == AppType.FREE) "FREE" else (app.priceLabel ?: "SUBSCRIPTION"),
                color = if (app.type == AppType.FREE) Blue else Color(0xFF9D87FF),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Spacer(Modifier.height(24.dp))
            Text(app.description, color = Color(0xFFD7DCE5), fontSize = 19.sp, lineHeight = 28.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(34.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FocusButton(if (downloading) "Downloading $progress%" else "Download / Install", enabled = !downloading) {
                    downloading = true
                    error = null
                    scope.launch {
                        installer.downloadAndOpenInstaller(app) { progress = it }
                            .onFailure { error = it.message ?: "Download failed" }
                        downloading = false
                    }
                }
                FocusButton("Back", onClick = onBack, secondary = true)
            }
            if (error != null) {
                Spacer(Modifier.height(16.dp))
                Text(error!!, color = Color(0xFFFF8C8C), fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun FocusButton(text: String, enabled: Boolean = true, secondary: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        when {
            !enabled -> Color(0xFF273044)
            focused -> if (secondary) Violet else Blue
            else -> if (secondary) Color(0xFF22283A) else Color(0xFF0C6182)
        }, label = "button"
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = bg),
        modifier = Modifier.onFocusChanged { focused = it.isFocused }.focusable(),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 26.dp, vertical = 14.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
private fun ErrorState(message: String, retry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Unable to load Zappix", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(message, color = Color(0xFFA7B1C2), fontSize = 15.sp)
            Spacer(Modifier.height(22.dp))
            FocusButton("Try Again", onClick = retry)
        }
    }
}
