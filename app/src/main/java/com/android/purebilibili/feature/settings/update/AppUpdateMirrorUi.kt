package com.android.purebilibili.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppDropdownMenu
import com.android.purebilibili.core.ui.components.AppDropdownMenuItem
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton

@Composable
internal fun rememberAppUpdateMirrorPreference(): AppUpdateMirrorPreference {
    val context = LocalContext.current
    val preference by remember(context) { SettingsManager.getAppUpdateMirrorPreference(context) }
        .collectAsStateWithLifecycle(initialValue = AppUpdateMirrorPreference())
    return preference
}

@Composable
internal fun AppUpdateCustomMirrorDialog(
    initialPrefix: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var prefix by rememberSaveable { mutableStateOf(initialPrefix) }
    val normalizedPrefix = remember(prefix) { normalizeAppUpdateMirrorPrefix(prefix) }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("自定义镜像源") },
        text = {
            AppOutlinedTextField(
                value = prefix,
                onValueChange = { prefix = it },
                modifier = Modifier.fillMaxWidth(),
                labelText = "HTTPS 镜像前缀",
                placeholderText = "https://mirror.example/",
                supportingText = { AppText("下载时会在此前缀后拼接原始 GitHub APK 链接") },
                isError = prefix.isNotBlank() && normalizedPrefix == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
            )
        },
        confirmButton = {
            AppTextButton(
                enabled = normalizedPrefix != null,
                onClick = { normalizedPrefix?.let(onConfirm) },
            ) { AppText("保存") }
        },
        dismissButton = { AppTextButton(onClick = onDismiss) { AppText("取消") } },
    )
}

private enum class AppUpdateReleaseLink(val label: String) {
    STABLE("正式版下载"),
    TEST("测试版下载"),
}

@Composable
internal fun AppUpdateDownloadChannels(state: AppUpdateDialogState, actions: AppUpdateDialogActions) {
    var releaseLink by rememberSaveable { mutableStateOf(AppUpdateReleaseLink.STABLE) }
    val isDownloading = state.downloadState.isActiveDownload()
    val mirrorActionLabel = when {
        isDownloading || state.downloadState.status == AppUpdateDownloadStatus.COMPLETED ->
            appUpdatePrimaryLabel(state.downloadState, state.hasAsset)
        else -> state.mirrorSource.label
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppText("其他下载渠道", color = AppContentColors.secondaryText())
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 320.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppUpdateDownloadChannelButton(
                modifier = Modifier.weight(1f),
                choices = AppUpdateReleaseLink.entries,
                selected = releaseLink,
                label = AppUpdateReleaseLink::label,
                actionLabel = releaseLink.label,
                menuLabel = "选择发布版本",
                onSelect = { releaseLink = it },
                onClick = when (releaseLink) {
                    AppUpdateReleaseLink.STABLE -> actions.onOpenRelease
                    AppUpdateReleaseLink.TEST -> actions.onOpenTestRelease
                },
            )
            AppUpdateDownloadChannelButton(
                modifier = Modifier.weight(1f),
                choices = AppUpdateMirror.entries,
                selected = state.mirrorSource,
                label = AppUpdateMirror::label,
                actionLabel = mirrorActionLabel,
                menuLabel = "选择下载镜像",
                enabled = state.hasAsset && !isDownloading,
                onSelect = actions.onMirrorSelect,
                onClick = actions.onMirrorAction,
            )
        }
    }
}

@Composable
private fun <T> AppUpdateDownloadChannelButton(
    modifier: Modifier,
    choices: List<T>,
    selected: T,
    label: (T) -> String,
    actionLabel: String,
    menuLabel: String,
    enabled: Boolean = true,
    onSelect: (T) -> Unit,
    onClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        AppSurface(
            shape = AppShapes.container(ContainerLevel.Pill),
            color = Color.Transparent,
            border = BorderStroke(
                AppSurfaceTokens.OutlineWidth,
                AppSurfaceTokens.outline().copy(alpha = 0.45f),
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppTextButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    enabled = enabled,
                    contentPadding = PaddingValues(start = 8.dp, end = 0.dp),
                ) {
                    AppText(actionLabel, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
                AppIconButton(onClick = { expanded = true }, enabled = enabled) {
                    AppIcon(Icons.Filled.KeyboardArrowDown, contentDescription = menuLabel)
                }
            }
        }
        AppDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            glassSource = null,
        ) {
            choices.forEach { choice ->
                AppDropdownMenuItem(
                    text = { AppText(label(choice)) },
                    selected = choice == selected,
                    onClick = {
                        expanded = false
                        onSelect(choice)
                    },
                )
            }
        }
    }
}
