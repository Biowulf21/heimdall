package com.chooloo.www.chooloolib.ui.common

import android.Manifest.permission.WRITE_CONTACTS
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.chooloo.www.chooloolib.R
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactEditorDialog(initialNumber: String, contactId: Long?, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editing = contactId != null
    var data by remember(contactId) {
        mutableStateOf(
            if (editing) {
                null
            } else {
                ContactEditorData(
                    listOf(
                        ContactEditorField(ContactFieldKind.FIRST_NAME),
                        ContactEditorField(ContactFieldKind.LAST_NAME),
                        ContactEditorField(ContactFieldKind.COMPANY),
                        ContactEditorField(ContactFieldKind.PHONE, initialNumber),
                        ContactEditorField(ContactFieldKind.EMAIL),
                    ),
                )
            },
        )
    }
    var drafts by rememberSaveable { mutableStateOf(Bundle()) }
    var addPhone by rememberSaveable { mutableStateOf(false) }
    var addEmail by rememberSaveable { mutableStateOf(false) }
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

    val fields = data?.let { snapshot ->
        buildList {
            addAll(snapshot.fields)
            if (editing && snapshot.rawContactId != null) {
                if (addPhone || none { it.kind == ContactFieldKind.PHONE }) {
                    add(ContactEditorField(ContactFieldKind.PHONE))
                }
                if (addEmail || none { it.kind == ContactFieldKind.EMAIL }) {
                    add(ContactEditorField(ContactFieldKind.EMAIL))
                }
            }
        }.sortedBy { it.kind.ordinal }
    }.orEmpty()
    val values = fields.associate { field ->
        field.key to (drafts.getString(field.key)?.trim() ?: field.value)
    }
    val canSave = if (editing) {
        fields.any { !it.readOnly && values[it.key] != it.value }
    } else {
        values.values.any { it.isNotEmpty() }
    }

    fun save() {
        val snapshot = data ?: return
        if (saving || !canSave) return
        val changes = snapshot.copy(fields = fields)
        saving = true
        error = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) { changes.save(context, values) }
                onClose()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
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
    Dialog(
        onDismissRequest = { if (!saving) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
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
                        IconButton(onClick = onClose, enabled = !saving) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                stringResource(R.string.contact_editor_close),
                            )
                        }
                    },
                    actions = {
                        Button(
                            enabled = !saving && canSave,
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, WRITE_CONTACTS) ==
                                    PackageManager.PERMISSION_GRANTED
                                ) {
                                    save()
                                } else {
                                    permission.launch(WRITE_CONTACTS)
                                }
                            },
                            modifier = Modifier.padding(end = 16.dp).heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(R.string.new_contact_save))
                        }
                    },
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(
                    padding,
                ).verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (editing && data == null && error == null) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    if (editing && data != null && data?.rawContactId == null) {
                        Text(stringResource(R.string.edit_contact_read_only))
                    }
                    fields.forEach { field ->
                        key(field.key) {
                            ContactField(
                                field = field,
                                value = drafts.getString(field.key, field.value).orEmpty(),
                                onChange = {
                                    drafts =
                                        Bundle(drafts).apply { putString(field.key, it) }
                                },
                                enabled = !saving && !field.readOnly,
                            )
                        }
                    }
                    if (editing && data?.rawContactId != null) {
                        if (fields.any { it.kind == ContactFieldKind.PHONE && it.id != null } &&
                            !addPhone
                        ) {
                            TextButton(onClick = { addPhone = true }, enabled = !saving) {
                                Text(stringResource(R.string.edit_contact_add_phone))
                            }
                        }
                        if (fields.any { it.kind == ContactFieldKind.EMAIL && it.id != null } &&
                            !addEmail
                        ) {
                            TextButton(onClick = { addEmail = true }, enabled = !saving) {
                                Text(stringResource(R.string.edit_contact_add_email))
                            }
                        }
                    }
                    error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun ContactField(
    field: ContactEditorField,
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
) {
    val kind = field.kind
    val label = when (kind) {
        ContactFieldKind.FIRST_NAME -> R.string.contact_editor_first_name
        ContactFieldKind.LAST_NAME -> R.string.contact_editor_last_name
        ContactFieldKind.COMPANY -> R.string.contact_editor_company
        ContactFieldKind.PHONE -> R.string.new_contact_phone
        ContactFieldKind.EMAIL -> R.string.new_contact_email
    }
    val icon = when (kind) {
        ContactFieldKind.FIRST_NAME, ContactFieldKind.LAST_NAME -> Icons.Rounded.Person
        ContactFieldKind.COMPANY -> Icons.Rounded.WorkOutline
        ContactFieldKind.PHONE -> Icons.Rounded.Phone
        ContactFieldKind.EMAIL -> Icons.Rounded.Email
    }
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
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        supportingText = field.typeLabel?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        keyboardOptions = keyboardOptions,
    )
}
