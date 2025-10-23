package com.tinnamchoi.tmtlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tinnamchoi.tmtlauncher.ui.theme.TmtLauncherTheme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.FileObserver
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

data class AppInfo(
    val label: String, val packageName: String, val icon: androidx.compose.ui.graphics.ImageBitmap
)

data class GroupInfo(
    val title: String, val apps: MutableList<AppInfo>
)

sealed interface LauncherItem {
    data class Divider(val id: String) : LauncherItem
    data class Title(val text: String) : LauncherItem
    data class App(val appInfo: AppInfo) : LauncherItem
    data class Spacer(val height: Int, val id: String) : LauncherItem
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TmtLauncherTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TmtLauncher(
                        context = this@MainActivity, modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TmtLauncher(context: Context, modifier: Modifier = Modifier) {
    var launcherItems by remember { mutableStateOf(parseConfig(context)) }

    DisposableEffect(Unit) {
        val file = getPublicConfigFile(context)
        val observer = object : FileObserver(file, FileObserver.CLOSE_WRITE) {
            override fun onEvent(event: Int, path: String?) {
                launcherItems = parseConfig(context)
            }
        }
        observer.startWatching()

        val packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                launcherItems = parseConfig(context!!)
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }

        context.registerReceiver(packageReceiver, filter)

        onDispose {
            observer.stopWatching()
            context.unregisterReceiver(packageReceiver)
        }
    }

    LazyColumn(modifier = modifier) {
        items(items = launcherItems, key = { item ->
            when (item) {
                is LauncherItem.App -> "${item.appInfo.label}_${item.appInfo.packageName}"
                is LauncherItem.Title -> item.text
                is LauncherItem.Divider -> item.id
                is LauncherItem.Spacer -> item.id
            }
        }) { item ->
            when (item) {
                is LauncherItem.Divider -> HorizontalDivider()
                is LauncherItem.Spacer -> Spacer(modifier = Modifier.height(item.height.dp))
                is LauncherItem.Title -> GroupTitle(item.text)
                is LauncherItem.App -> App(item.appInfo)
            }
        }
    }
}

fun parseConfig(context: Context): List<LauncherItem> {
    val pm = context.packageManager
    val apps = pm.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
    ).map { resolveInfo ->
        AppInfo(
            label = resolveInfo.loadLabel(pm).toString(),
            packageName = resolveInfo.activityInfo.packageName,
            icon = resolveInfo.loadIcon(pm).toBitmap().asImageBitmap()
        )
    }.groupBy({ app -> app.label }, { it }).toMutableMap()

    val groups = mutableListOf<GroupInfo>()

    val configFile = getPublicConfigFile(context)

    if (configFile.exists()) {
        configFile.forEachLine { line ->
            if (line.startsWith("# ")) {
                groups.add(GroupInfo(line.substring(2), mutableListOf()))
            } else {
                val trimmed = line.trim()
                apps[trimmed]?.let { appInfos ->
                    if (groups.isNotEmpty()) {
                        groups.last().apps.addAll(appInfos)
                        apps.remove(trimmed)
                    }
                }
            }
        }
    }

    groups.add(
        GroupInfo(
            "Miscellaneous", apps.values.flatten().toMutableList()
        )
    )

    val launcherItems = mutableListOf<LauncherItem>()

    // note: can use forEachIndexed here to enable duplicate group names,
    // but LazyColumn might be slower and Title would use more memory
    // and I just don't see enough utility to include that
    groups.forEach { group ->
        launcherItems.add(LauncherItem.Divider("divider_top_${group.title}"))
        launcherItems.add(LauncherItem.Spacer(32, "spacer_top_${group.title}"))
        launcherItems.add(LauncherItem.Title(group.title))
        launcherItems.add(LauncherItem.Spacer(20, "spacer_mid_${group.title}"))
        launcherItems.addAll(group.apps.sortedBy { it.label }.map { LauncherItem.App(it) }.toList())
        launcherItems.add(LauncherItem.Spacer(24, "spacer_bot_${group.title}"))
        launcherItems.add(LauncherItem.Divider("divider_bot_${group.title}"))
    }

    return launcherItems
}


@Composable
fun GroupTitle(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun App(app: AppInfo) {
    val context = LocalContext.current
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 48.dp)
        .clickable {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        }
        .padding(16.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Image(
            bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(48.dp)
        )
        Text(text = app.label, fontSize = 20.sp)
    }
}
