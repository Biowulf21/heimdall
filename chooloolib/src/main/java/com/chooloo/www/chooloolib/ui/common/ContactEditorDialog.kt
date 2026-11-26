package com.chooloo.www.chooloolib.ui.common

import android.Manifest.permission.WRITE_CONTACTS
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.components.Avatar
import com.chooloo.www.chooloolib.ui.components.ConfirmDialog
import com.chooloo.www.chooloolib.ui.theme.Sizes
import com.chooloo.www.chooloolib.ui.theme.Spacing
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val growSpec = tween<androidx.compose.ui.unit.IntSize>(200, easing = FastOutSlowInEasing)

/**
 * Full-screen editor for a new contact ([contactId] null) or an existing one.
 * [onClose] receives the saved contact's lookup URI, or null when nothing was saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactEditorDialog(
    contactId: Long?,
    onClose: (Uri?) -> Unit,
    prefill: Map<ContactFieldKind, List<String>> = emptyMap(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editing = contactId != null
    var data by remember(contactId) {
        mutableStateOf(if (editing) null else newContact(prefill))
    }
    var drafts by rememberSaveable { mutableStateOf(Bundle()) }
    var extraPhones by rememberSaveable { mutableIntStateOf(0) }
    var extraEmails by rememberSaveable { mutableIntStateOf(0) }
    var focusKey by rememberSaveable { mutableStateOf<String?>(null) }
    var photo by rememberSaveable { mutableStateOf<Uri?>(null) }
    var removePhoto by rememberSaveable { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(contactId) {
        if (contactId == null) return@LaunchedEffect
        try {
            data = withContext(Dispatchers.IO) { loadContactEditor(context, contactId) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = R.string.edit_contact_load_failed
        }
    }

    val writable = data != null && (!editing || data?.rawContactId != null)
    val fields = data?.let { snapshot ->
        val existingPhones = snapshot.fields.count { it.kind == ContactFieldKind.PHONE }
        val existingEmails = snapshot.fields.count { it.kind == ContactFieldKind.EMAIL }
        val base = snapshot.fields.size
        buildList {
            addAll(snapshot.fields)
            if (writable) {
                val phones = extraPhones + if (editing && existingPhones == 0) 1 else 0
                val emails = extraEmails + if (editing && existingEmails == 0) 1 else 0
                repeat(phones) { add(ContactEditorField(ContactFieldKind.PHONE, slot = base + it)) }
                repeat(emails) { add(ContactEditorField(ContactFieldKind.EMAIL, slot = base + it)) }
            }
        }.sortedBy { it.kind.ordinal }
    }.orEmpty()
    val values = fields.associate { field ->
        field.key to (drafts.getString(field.key)?.trim() ?: field.value)
    }
    val photoChange = when {
        photo != null -> PhotoEdit.Set(checkNotNull(photo))
        removePhoto -> PhotoEdit.Remove
        else -> null
    }
    val changed = photoChange != null || fields.any { !it.readOnly && values[it.key] != it.value }
    val canSave = writable && if (editing) changed else values.values.any(String::isNotEmpty)
    val errors = if (attempted) validateContact(fields, values, !editing) else emptyMap()

    fun close() {
        if (saving) return
        if (changed) confirmDiscard = true else onClose(null)
    }

    fun save() {
        val snapshot = data ?: return
        if (saving || !canSave) return
        attempted = true
        // Focusing the first bad field scrolls it into view above the keyboard.
        validateContact(fields, values, !editing).keys.firstOrNull()?.let {
            focusKey = it
            return
        }
        val changes = snapshot.copy(fields = fields)
        saving = true
        error = null
        scope.launch {
            try {
                val uri = withContext(Dispatchers.IO) { changes.save(context, values, photoChange) }
                onClose(uri ?: Uri.EMPTY)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Log.e("ContactEditor", "Saving contact failed", e)
                error = R.string.new_contact_failed
            } finally {
                saving = false
            }
        }
    }

    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) save() else error = R.string.new_contact_permission
        }

    fun requestSave() {
        if (ContextCompat.checkSelfPermission(context, WRITE_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            save()
        } else {
            permission.launch(WRITE_CONTACTS)
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        if (it != null) {
            photo = it
            removePhoto = false
        }
    }

    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(
                                    if (editing) {
                                        R.string.edit_contact_title
                                    } else {
                                        R.string.new_contact_title
                                    },
                                ),
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                        navigationIcon = {
                            IconButton(onClick = ::close, enabled = !saving) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    stringResource(R.string.contact_editor_close),
                                )
                            }
                        },
                        actions = {
                            TextButton(
                                enabled = !saving && canSave,
                                onClick = ::requestSave,
                                modifier = Modifier.padding(end = Spacing.Small),
                            ) {
                                Text(stringResource(R.string.new_contact_save))
                            }
                        },
                    )
                    if (saving || (editing && data == null && error == null)) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    error?.let { Banner(stringResource(it)) }
                    if (editing && data != null && data?.rawContactId == null) {
                        Banner(stringResource(R.string.edit_contact_read_only), isError = false)
                    }
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Large),
                ) {
                    if (data == null) return@Column
                    val firstName = values[
                        fields.firstOrNull { it.kind == ContactFieldKind.FIRST_NAME }?.key,
                    ].orEmpty()
                    val company = values[
                        fields.firstOrNull { it.kind == ContactFieldKind.COMPANY }?.key,
                    ].orEmpty()
                    val shownPhoto = photo?.toString()
                        ?: data?.photoUri?.takeUnless { removePhoto }
                    PhotoHeader(
                        name = firstName.ifEmpty { company },
                        photoUri = shownPhoto,
                        enabled = writable && !saving,
                        onPick = {
                            pickPhoto.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                        onRemove = {
                            photo = null
                            removePhoto = data?.photoUri != null
                        },
                    )

                    val sections = listOf(
                        Icons.Rounded.Person to
                            setOf(ContactFieldKind.FIRST_NAME, ContactFieldKind.LAST_NAME),
                        Icons.Rounded.WorkOutline to setOf(ContactFieldKind.COMPANY),
                        Icons.Rounded.Phone to setOf(ContactFieldKind.PHONE),
                        Icons.Rounded.Email to setOf(ContactFieldKind.EMAIL),
                    )
                    sections.forEach { (icon, kinds) ->
                        val sectionFields = fields.filter { it.kind in kinds }
                        val addKind = kinds.singleOrNull()
                            ?.takeIf {
                                it == ContactFieldKind.PHONE || it == ContactFieldKind.EMAIL
                            }
                        if (sectionFields.isEmpty() && (addKind == null || !writable)) {
                            return@forEach
                        }
                        Section(icon) {
                            sectionFields.forEach { field ->
                                key(field.key) {
                                    ContactField(
                                        field = field,
                                        value = drafts.getString(field.key, field.value).orEmpty(),
                                        error = errors[field.key],
                                        focus = focusKey == field.key,
                                        onFocused = { focusKey = null },
                                        onChange = {
                                            drafts =
                                                Bundle(drafts).apply { putString(field.key, it) }
                                        },
                                        onDone = ::requestSave,
                                        enabled = !saving && !field.readOnly,
                                    )
                                }
                            }
                            if (addKind != null && writable) {
                                val base = data?.fields?.size ?: 0
                                val nextSlot = base + sectionFields.count { it.slot >= base }
                                TextButton(
                                    enabled = !saving,
                                    onClick = {
                                        focusKey = ContactEditorField(addKind, slot = nextSlot).key
                                        if (addKind == ContactFieldKind.PHONE) {
                                            extraPhones++
                                        } else {
                                            extraEmails++
                                        }
                                    },
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        stringResource(
                                            if (addKind == ContactFieldKind.PHONE) {
                                                R.string.edit_contact_add_phone
                                            } else {
                                                R.string.edit_contact_add_email
                                            },
                                        ),
                                        modifier = Modifier.padding(start = Spacing.Small),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.contact_editor_discard_title),
            text = stringResource(R.string.contact_editor_discard_text),
            confirmLabel = stringResource(R.string.contact_editor_discard),
            onConfirm = { onClose(null) },
            onDismiss = { confirmDiscard = false },
        )
    }
}

private fun newContact(prefill: Map<ContactFieldKind, List<String>>): ContactEditorData {
    val first = prefill[ContactFieldKind.FIRST_NAME]?.firstOrNull().orEmpty()
    val last = prefill[ContactFieldKind.LAST_NAME]?.firstOrNull().orEmpty()
    val company = prefill[ContactFieldKind.COMPANY]?.firstOrNull().orEmpty()
    val phones = prefill[ContactFieldKind.PHONE].orEmpty().ifEmpty { listOf("") }
    val emails = prefill[ContactFieldKind.EMAIL].orEmpty().ifEmpty { listOf("") }
    return ContactEditorData(
        buildList {
            add(ContactEditorField(ContactFieldKind.FIRST_NAME, first))
            add(ContactEditorField(ContactFieldKind.LAST_NAME, last))
            add(ContactEditorField(ContactFieldKind.COMPANY, company))
            phones.forEachIndexed { i, it ->
                add(ContactEditorField(ContactFieldKind.PHONE, it, slot = i))
            }
            emails.forEachIndexed { i, it ->
                add(ContactEditorField(ContactFieldKind.EMAIL, it, slot = i))
            }
        },
    )
}

@Composable
private fun PhotoHeader(
    name: String,
    photoUri: String?,
    enabled: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        modifier = Modifier.fillMaxWidth(),
    ) {
        val initial = name.firstOrNull(Char::isLetter)?.toString()
        Crossfade(targetState = initial to photoUri, animationSpec = tween(200), label = "Avatar") {
            Avatar(name = it.first, photoUri = it.second, size = Sizes.AvatarLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            TextButton(onClick = onPick, enabled = enabled) {
                Text(
                    stringResource(
                        if (photoUri == null) {
                            R.string.contact_editor_add_photo
                        } else {
                            R.string.contact_editor_change_photo
                        },
                    ),
                )
            }
            if (photoUri != null) {
                TextButton(onClick = onRemove, enabled = enabled) {
                    Text(stringResource(R.string.contact_editor_remove_photo))
                }
            }
        }
    }
}

@Composable
private fun Banner(text: String, isError: Boolean = true) {
    Surface(
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (isError) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        )
    }
}

@Composable
private fun Section(icon: ImageVector, content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.Medium + Spacing.ExtraSmall),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            modifier = Modifier.weight(1f).animateContentSize(growSpec),
        ) {
            content()
        }
    }
}

@Composable
private fun ContactField(
    field: ContactEditorField,
    value: String,
    error: Int?,
    focus: Boolean,
    onFocused: () -> Unit,
    onChange: (String) -> Unit,
    onDone: () -> Unit,
    enabled: Boolean,
) {
    val kind = field.kind
    val keyboardOptions = when (kind) {
        ContactFieldKind.FIRST_NAME, ContactFieldKind.LAST_NAME, ContactFieldKind.COMPANY ->
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            )

        ContactFieldKind.PHONE -> KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next,
        )

        ContactFieldKind.EMAIL -> KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
        )
    }
    val requester = remember { FocusRequester() }
    val formatPhone = rememberPhoneFormatter()
    val countryIso = rememberPhoneCountryIso()
    var focused by remember { mutableStateOf(false) }
    // A hint, not an error: shown once the user leaves the field, and saving still works.
    val hint = R.string.contact_editor_phone_incomplete.takeIf {
        kind == ContactFieldKind.PHONE && !focused && value.isNotBlank() &&
            !looksLikeCompleteNumber(value, countryIso)
    }
    val message = error ?: hint
    val transformation = remember(kind, formatPhone) {
        if (kind ==
            ContactFieldKind.PHONE
        ) {
            PhoneVisualTransformation(formatPhone)
        } else {
            VisualTransformation.None
        }
    }
    LaunchedEffect(focus) {
        if (focus) {
            requester.requestFocus()
            onFocused()
        }
    }
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = {
                if (kind ==
                    ContactFieldKind.PHONE
                ) {
                    onChange(capPhoneDigits(it))
                } else {
                    onChange(it)
                }
            },
            label = { Text(stringResource(kind.label)) },
            supportingText = field.typeLabel?.let { { Text(it) } },
            isError = error != null,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(requester)
                .onFocusChanged { focused = it.isFocused },
            enabled = enabled,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            visualTransformation = transformation,
        )
        // Keeps the last message while the exit animation runs.
        var shown by remember { mutableStateOf(message) }
        if (message != null) shown = message
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(150)) +
                expandVertically(tween(180, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(100)) + shrinkVertically(tween(150)),
        ) {
            shown?.let {
                Text(
                    stringResource(it),
                    color = if (shown == error) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(start = Spacing.Medium, top = Spacing.ExtraSmall)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}
