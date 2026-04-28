package com.example.customrpc

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

class MainActivity : AppCompatActivity() {

    // View Containers
    private lateinit var viewLogin: View
    private lateinit var viewDashboard: View
    private lateinit var viewSettings: View
    private lateinit var viewAbout: View
    private lateinit var viewLogs: View

    // Login View Elements
    private lateinit var loginTokenInput: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnTokenGuideLogin: Button

    // Dashboard View Elements
    private lateinit var tvDashboardStatus: TextView
    private lateinit var tvDashboardDesc: TextView
    private lateinit var btnToggleConnection: Button
    private lateinit var btnOpenConfig: Button
    private lateinit var btnStopService: Button
    private lateinit var btnLogout: ImageView
    private lateinit var cardPresenceInfo: View
    private lateinit var tvPresenceName: TextView
    private lateinit var tvPresenceDetails: TextView

    // Settings View Elements
    private lateinit var appIdEditText: EditText
    private lateinit var appNameEditText: EditText
    private lateinit var activityTypeSpinner: Spinner
    private lateinit var statusSpinner: Spinner
    private lateinit var streamUrlEditText: EditText
    private lateinit var streamUrlLayout: View
    private lateinit var partyIdEditText: EditText

    // Live preview elements
    private lateinit var previewHeader: TextView
    private lateinit var previewName: TextView
    private lateinit var previewDetails: TextView
    private lateinit var previewState: TextView

    // Advanced action buttons
    private lateinit var btnResetSettings: Button
    private lateinit var btnExportSettings: Button
    private lateinit var btnImportSettings: Button

    private lateinit var detailsEditText: EditText
    private lateinit var stateEditText: EditText
    private lateinit var partySizeEditText: EditText
    private lateinit var partyMaxEditText: EditText
    private lateinit var largeImageKeyEditText: EditText
    private lateinit var largeImageTextEditText: EditText
    private lateinit var smallImageKeyEditText: EditText
    private lateinit var smallImageTextEditText: EditText

    private lateinit var btn1Text: EditText
    private lateinit var btn1Url: EditText
    private lateinit var btn2Text: EditText
    private lateinit var btn2Url: EditText

    private lateinit var timestampSpinner: Spinner
    private lateinit var customTimestampLayout: View
    private lateinit var btnPickStartTime: Button
    private lateinit var tvStartTimeVal: TextView
    private lateinit var btnPickEndTime: Button
    private lateinit var tvEndTimeVal: TextView

    private lateinit var btnSaveApply: Button
    private lateinit var btnCancelConfig: Button

    // Logs View Elements
    private lateinit var tvLogs: TextView
    private lateinit var scrollLogs: ScrollView

    private var customStartTime: Long? = null
    private var customEndTime: Long? = null
    private var isServiceConnected = false

    // Last known presence info for the card
    private var lastPresenceName: String = ""
    private var lastPresenceDetails: String = ""

    // Rotation Mode
    private lateinit var swRotation: com.google.android.material.switchmaterial.SwitchMaterial
    private lateinit var layRotationControls: View
    private lateinit var spinnerRotationInterval: Spinner
    private lateinit var btnManagePresets: com.google.android.material.button.MaterialButton
    private lateinit var tvRotationStatus: TextView
    private lateinit var tvDeviceApp: TextView
    private var rotationEnabled = false
    private val rotationIntervalValues = longArrayOf(30_000L, 60_000L, 120_000L, 300_000L, 600_000L)

    // Device tracking
    private val foregroundPollHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var foregroundPollRunnable: Runnable? = null

    // Web Interface
    inner class WebAppInterface {
        @android.webkit.JavascriptInterface
        fun onTokenReceived(token: String) {
            runOnUiThread {
                val cleanToken = token.replace("\"", "").trim()
                handleSavedToken(cleanToken)
            }
        }
    }

    private fun handleSavedToken(token: String) {
        if (token.isNotEmpty()) {
            loginTokenInput.setText(token)
            saveSettings()
            Toast.makeText(this, getString(R.string.msg_token_saved), Toast.LENGTH_SHORT).show()
            discordLoginDialog?.dismiss()
            showDashboard()
        }
    }

    private var discordLoginDialog: android.app.Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this.application)

        val sharedPref = getSharedPreferences("RpcSettings", Context.MODE_PRIVATE)
        val hasAskedBattery = sharedPref.getBoolean("hasAskedBattery", false)
        val pm = getSystemService(android.os.PowerManager::class.java)

        if (!hasAskedBattery && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(packageName)) {
            sharedPref.edit().putBoolean("hasAskedBattery", true).apply()
            try {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) { }
        }

        setContentView(R.layout.activity_main)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewLogin = findViewById(R.id.view_login)
        viewDashboard = findViewById(R.id.view_dashboard)
        viewSettings = findViewById(R.id.view_settings)
        viewAbout = findViewById(R.id.view_about)
        viewLogs = findViewById(R.id.view_logs)

        bindLoginViews()
        bindDashboardViews()
        bindSettingsViews()
        bindLogsViews()

        loadSettings()

        val savedToken = loginTokenInput.text.toString()
        if (savedToken.isNotBlank()) {
            showDashboard()
        } else {
            showLogin()
        }

        handleIntent(intent)

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    viewLogs.visibility == View.VISIBLE -> showDashboard()
                    viewAbout.visibility == View.VISIBLE -> showDashboard()
                    viewSettings.visibility == View.VISIBLE -> {
                        loadSettings()
                        showDashboard()
                    }
                    viewDashboard.visibility == View.VISIBLE -> moveTaskToBack(true)
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    // ── LOGIN ────────────────────────────────────────────────────────────────

    private fun bindLoginViews() {
        loginTokenInput = findViewById(R.id.login_token_input)
        btnLogin = findViewById(R.id.btn_login)
        btnTokenGuideLogin = findViewById(R.id.btn_token_guide_login)

        btnLogin.setOnClickListener {
            val token = loginTokenInput.text.toString()
            if (token.isNotBlank()) {
                saveSettings()
                showDashboard()
            } else {
                Toast.makeText(this, getString(R.string.msg_enter_token), Toast.LENGTH_SHORT).show()
            }
        }

        btnTokenGuideLogin.setOnClickListener { showTokenGuideDialog() }

        val btnDiscordLogin = findViewById<Button>(R.id.btn_discord_login)
        btnDiscordLogin.setOnClickListener { showDiscordLogin() }
    }

    private fun showDiscordLogin() {
        val container = FrameLayout(this)
        val webView = android.webkit.WebView(this)
        webView.layoutParams = FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile; rv:88.0) Gecko/88.0 Firefox/88.0"
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        webView.webChromeClient = android.webkit.WebChromeClient()

        webView.webViewClient = object : android.webkit.WebViewClient() {
            var sniffingActive = false
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                if (sniffingActive) return
                sniffingActive = true
                val js = """
                    (function() {
                        var originalSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader;
                        XMLHttpRequest.prototype.setRequestHeader = function(key, value) {
                            if (key && key.toLowerCase() === 'authorization') {
                                Android.onTokenReceived(value);
                            }
                            originalSetRequestHeader.apply(this, arguments);
                        };
                        var t = localStorage.getItem('token');
                        if (t) Android.onTokenReceived(t.replace(/"/g, ''));
                    })();
                """.trimIndent()
                view?.evaluateJavascript(js, null)
                Toast.makeText(this@MainActivity, getString(R.string.msg_sniffing), Toast.LENGTH_SHORT).show()
            }
        }

        container.addView(webView)
        webView.loadUrl("https://discord.com/login")

        discordLoginDialog = android.app.Dialog(this, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen)
        discordLoginDialog?.setContentView(container)
        discordLoginDialog?.setCancelable(true)
        discordLoginDialog?.show()
        discordLoginDialog?.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    // ── DASHBOARD ─────────────────────────────────────────────────────────────

    private fun bindDashboardViews() {
        tvDashboardStatus = findViewById(R.id.tv_dashboard_status)
        tvDashboardDesc = findViewById(R.id.tv_dashboard_desc)
        btnToggleConnection = findViewById(R.id.btn_toggle_connection)
        btnOpenConfig = findViewById(R.id.btn_open_config)
        btnStopService = findViewById(R.id.btn_stop_service)
        btnLogout = findViewById(R.id.btn_logout)
        cardPresenceInfo = findViewById(R.id.card_presence_info)
        tvPresenceName = findViewById(R.id.tv_presence_name)
        tvPresenceDetails = findViewById(R.id.tv_presence_details)

        val btnAbout = findViewById<ImageView>(R.id.btn_about)
        val btnLogs = findViewById<ImageView>(R.id.btn_logs)
        val btnTokenOptions = findViewById<ImageView>(R.id.btn_token_options)

        btnLogs.setOnClickListener { showLogs() }
        btnTokenOptions.setOnClickListener { showTokenOptionsDialog() }
        btnAbout.setOnClickListener { showAbout() }

        btnToggleConnection.setOnClickListener {
            if (isServiceConnected) {
                sendDisconnectIntent()
            } else {
                val token = loginTokenInput.text.toString()
                val appId = appIdEditText.text.toString()
                if (appId.isBlank()) {
                    Toast.makeText(this, getString(R.string.msg_no_app_id), Toast.LENGTH_SHORT).show()
                    showSettings()
                    return@setOnClickListener
                }
                if (token.isNotBlank()) {
                    updateDashboardStatus(false, getString(R.string.status_connecting))
                    AppLogger.info("User initiated connection")
                    val serviceIntent = Intent(this, RpcService::class.java).apply {
                        action = RpcService.ACTION_START
                        putExtra("TOKEN", token)
                        putExtra("APP_NAME", appNameEditText.text.toString().ifBlank { getString(R.string.app_name) })
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                } else {
                    showLogin()
                }
            }
        }

        btnStopService.setOnClickListener { sendDisconnectIntent() }
        btnOpenConfig.setOnClickListener { showSettings() }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log out?")
                .setMessage("This will clear your saved token and disconnect the RPC service.")
                .setPositiveButton("Log out") { _, _ ->
                    loginTokenInput.setText("")
                    saveSettings()
                    sendDisconnectIntent()
                    AppLogger.info("User logged out")
                    showLogin()
                }
                .setNegativeButton(getString(R.string.btn_no), null)
                .show()
        }

        // Rotation card
        swRotation = findViewById(R.id.sw_rotation)
        layRotationControls = findViewById(R.id.lay_rotation_controls)
        spinnerRotationInterval = findViewById(R.id.spinner_rotation_interval)
        btnManagePresets = findViewById(R.id.btn_manage_presets)
        tvRotationStatus = findViewById(R.id.tv_rotation_status)
        tvDeviceApp = findViewById(R.id.tv_device_app)

        val intervalLabels = arrayOf("30 s", "1 min", "2 min", "5 min", "10 min")
        val intervalAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervalLabels)
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRotationInterval.adapter = intervalAdapter

        val prefs = getSharedPreferences("RpcSettings", Context.MODE_PRIVATE)
        rotationEnabled = prefs.getBoolean("rotationEnabled", false)
        swRotation.isChecked = rotationEnabled
        spinnerRotationInterval.setSelection(prefs.getInt("rotationIntervalIdx", 1))
        updateRotationCard()

        swRotation.setOnCheckedChangeListener { _, isChecked ->
            rotationEnabled = isChecked
            getSharedPreferences("RpcSettings", Context.MODE_PRIVATE).edit()
                .putBoolean("rotationEnabled", isChecked).apply()
            updateRotationCard()
            if (isChecked && isServiceConnected) {
                startRotationInService()
            } else {
                stopRotationInService()
            }
        }

        spinnerRotationInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                getSharedPreferences("RpcSettings", Context.MODE_PRIVATE).edit()
                    .putInt("rotationIntervalIdx", position).apply()
                if (rotationEnabled && isServiceConnected) startRotationInService()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnManagePresets.setOnClickListener { showManagePresetsDialog() }
    }

    private fun sendDisconnectIntent() {
        val serviceIntent = Intent(this, RpcService::class.java).apply {
            action = RpcService.ACTION_STOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        updateDashboardStatus(false, getString(R.string.status_offline))
    }

    // ── TOKEN OPTIONS ─────────────────────────────────────────────────────────

    private fun showTokenOptionsDialog() {
        val token = loginTokenInput.text.toString()
        if (token.isBlank()) {
            Toast.makeText(this, getString(R.string.msg_token_none), Toast.LENGTH_SHORT).show()
            return
        }

        val masked = if (token.length > 12) token.take(10) + "…" else "•".repeat(token.length)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_token_options_title))
            .setMessage("Saved token: $masked")
            .setPositiveButton(getString(R.string.btn_copy_token)) { _, _ ->
                copyTokenToClipboard(token)
            }
            .setNeutralButton(getString(R.string.btn_revoke_token)) { _, _ ->
                confirmRevokeToken(token)
            }
            .setNegativeButton(getString(R.string.btn_close), null)
            .show()
    }

    private fun copyTokenToClipboard(token: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("Discord token", token))
        AppLogger.info("Token copied to clipboard")
        Toast.makeText(this, getString(R.string.msg_token_copied), Toast.LENGTH_SHORT).show()
    }

    private fun confirmRevokeToken(token: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_revoke_title))
            .setMessage(getString(R.string.dialog_revoke_msg))
            .setPositiveButton(getString(R.string.btn_revoke_confirm)) { _, _ ->
                revokeToken(token)
            }
            .setNegativeButton(getString(R.string.btn_no), null)
            .show()
    }

    private fun revokeToken(token: String) {
        Toast.makeText(this, getString(R.string.msg_revoking), Toast.LENGTH_SHORT).show()
        AppLogger.warn("Revoking token via Discord API…")
        Thread {
            try {
                val client = okhttp3.OkHttpClient()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = "{\"provider\":null,\"voip_provider\":null}".toRequestBody(mediaType)
                val request = okhttp3.Request.Builder()
                    .url("https://discord.com/api/v9/auth/logout")
                    .addHeader("Authorization", token)
                    .post(body)
                    .build()
                val response = client.newCall(request).execute()
                runOnUiThread {
                    if (response.code == 204 || response.code == 200) {
                        AppLogger.info("Token revoked successfully (HTTP ${response.code})")
                        sendDisconnectIntent()
                        loginTokenInput.setText("")
                        saveSettings()
                        Toast.makeText(this, getString(R.string.msg_revoke_success), Toast.LENGTH_LONG).show()
                        showLogin()
                    } else {
                        val msg = "HTTP ${response.code}"
                        AppLogger.error("Revoke failed: $msg")
                        Toast.makeText(this, getString(R.string.msg_revoke_failed, msg), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    AppLogger.error("Revoke exception: ${e.message}")
                    Toast.makeText(this, getString(R.string.msg_revoke_failed, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    // ── SETTINGS ──────────────────────────────────────────────────────────────

    private fun bindSettingsViews() {
        appIdEditText = findViewById(R.id.app_id_edit_text)
        appNameEditText = findViewById(R.id.app_name_edit_text)
        activityTypeSpinner = findViewById(R.id.activity_type_spinner)
        statusSpinner = findViewById(R.id.status_spinner)
        streamUrlEditText = findViewById(R.id.stream_url_edit_text)
        streamUrlLayout = findViewById(R.id.lay_stream_url)
        partyIdEditText = findViewById(R.id.party_id_edit_text)

        previewHeader = findViewById(R.id.preview_header)
        previewName = findViewById(R.id.preview_name)
        previewDetails = findViewById(R.id.preview_details)
        previewState = findViewById(R.id.preview_state)

        btnResetSettings = findViewById(R.id.btn_reset_settings)
        btnExportSettings = findViewById(R.id.btn_export_settings)
        btnImportSettings = findViewById(R.id.btn_import_settings)
        detailsEditText = findViewById(R.id.details_edit_text)
        stateEditText = findViewById(R.id.state_edit_text)
        partySizeEditText = findViewById(R.id.party_size_edit_text)
        partyMaxEditText = findViewById(R.id.party_max_edit_text)
        largeImageKeyEditText = findViewById(R.id.large_image_key_edit_text)
        largeImageTextEditText = findViewById(R.id.large_image_text_edit_text)
        smallImageKeyEditText = findViewById(R.id.small_image_key_edit_text)
        smallImageTextEditText = findViewById(R.id.small_image_text_edit_text)

        val layLargeImageKey = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.lay_large_image_key)
        val laySmallImageKey = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.lay_small_image_key)
        layLargeImageKey.setEndIconOnClickListener { fetchAssets(largeImageKeyEditText) }
        laySmallImageKey.setEndIconOnClickListener { fetchAssets(smallImageKeyEditText) }

        btn1Text = findViewById(R.id.btn1_text)
        btn1Url = findViewById(R.id.btn1_url)
        btn2Text = findViewById(R.id.btn2_text)
        btn2Url = findViewById(R.id.btn2_url)

        timestampSpinner = findViewById(R.id.timestamp_spinner)
        customTimestampLayout = findViewById(R.id.custom_timestamp_layout)
        btnPickStartTime = findViewById(R.id.btn_pick_start_time)
        tvStartTimeVal = findViewById(R.id.tv_start_time_val)
        btnPickEndTime = findViewById(R.id.btn_pick_end_time)
        tvEndTimeVal = findViewById(R.id.tv_end_time_val)

        btnSaveApply = findViewById(R.id.btn_save_apply)
        btnCancelConfig = findViewById(R.id.btn_cancel_config)

        val btnBackConfig = findViewById<ImageView>(R.id.btn_back_config)
        btnBackConfig.setOnClickListener {
            loadSettings()
            showDashboard()
        }

        // Spinners
        val types = arrayOf("Playing", "Streaming", "Listening", "Watching", "Custom", "Competing")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        activityTypeSpinner.adapter = typeAdapter

        val statusOptions = arrayOf("Online", "Idle", "Do Not Disturb", "Invisible")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        statusSpinner.adapter = statusAdapter

        val tsTypes = arrayOf(getString(R.string.ts_none), "Elapsed Time", "Local Time", "Custom Range")
        val tsAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tsTypes)
        tsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timestampSpinner.adapter = tsAdapter

        timestampSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                customTimestampLayout.visibility = if (position == 3) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnPickStartTime.setOnClickListener { pickDateTime { ts -> customStartTime = ts; tvStartTimeVal.text = Date(ts).toString() } }
        btnPickEndTime.setOnClickListener { pickDateTime { ts -> customEndTime = ts; tvEndTimeVal.text = Date(ts).toString() } }

        btnSaveApply.setOnClickListener {
            saveSettings()
            // cache for presence info card
            lastPresenceName = appNameEditText.text.toString()
            lastPresenceDetails = detailsEditText.text.toString()
            if (isServiceConnected) {
                sendPresenceUpdate()
                updatePresenceInfoCard()
            }
            AppLogger.info("Settings saved (app: \"${appNameEditText.text}\")")
            showDashboard()
        }

        btnCancelConfig.setOnClickListener {
            loadSettings()
            showDashboard()
        }

        // Live preview watcher
        val previewWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { updateLivePreview() }
        }
        appNameEditText.addTextChangedListener(previewWatcher)
        detailsEditText.addTextChangedListener(previewWatcher)
        stateEditText.addTextChangedListener(previewWatcher)

        activityTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                streamUrlLayout.visibility = if (position == 1) View.VISIBLE else View.GONE
                updateLivePreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val btnSaveAsPreset = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save_as_preset)
        btnSaveAsPreset.setOnClickListener { saveCurrentAsRotationPreset() }

        btnResetSettings.setOnClickListener { confirmAndResetSettings() }
        btnExportSettings.setOnClickListener { exportSettingsToClipboard() }
        btnImportSettings.setOnClickListener { importSettingsFromClipboard() }
    }

    private fun updateLivePreview() {
        if (!::previewHeader.isInitialized) return
        val name = appNameEditText.text.toString().trim()
        val details = detailsEditText.text.toString().trim()
        val state = stateEditText.text.toString().trim()
        val pos = activityTypeSpinner.selectedItemPosition
        val headerRes = when (pos) {
            1 -> R.string.preview_header_streaming
            2 -> R.string.preview_header_listening
            3 -> R.string.preview_header_watching
            4 -> R.string.preview_header_custom
            5 -> R.string.preview_header_competing
            else -> R.string.preview_header_playing
        }
        previewHeader.setText(headerRes)
        previewName.text = name.ifBlank { getString(R.string.preview_default_name) }
        previewDetails.text = details.ifBlank { getString(R.string.preview_default_details) }
        previewState.text = state.ifBlank { getString(R.string.preview_default_state) }
    }

    private fun confirmAndResetSettings() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_reset_title))
            .setMessage(getString(R.string.dialog_reset_msg))
            .setPositiveButton(getString(R.string.btn_yes)) { _, _ ->
                clearAllPresenceFields()
                Toast.makeText(this, getString(R.string.msg_settings_reset), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.btn_no), null)
            .show()
    }

    private fun clearAllPresenceFields() {
        appIdEditText.setText("")
        appNameEditText.setText("")
        activityTypeSpinner.setSelection(0)
        statusSpinner.setSelection(0)
        streamUrlEditText.setText("")
        detailsEditText.setText("")
        stateEditText.setText("")
        partySizeEditText.setText("")
        partyMaxEditText.setText("")
        partyIdEditText.setText("")
        largeImageKeyEditText.setText("")
        largeImageKeyEditText.tag = ""
        largeImageTextEditText.setText("")
        smallImageKeyEditText.setText("")
        smallImageKeyEditText.tag = ""
        smallImageTextEditText.setText("")
        btn1Text.setText("")
        btn1Url.setText("")
        btn2Text.setText("")
        btn2Url.setText("")
        timestampSpinner.setSelection(0)
        customStartTime = null
        customEndTime = null
        tvStartTimeVal.text = getString(R.string.ts_none)
        tvEndTimeVal.text = getString(R.string.ts_none)
        saveSettings()
        updateLivePreview()
    }

    private fun exportSettingsToClipboard() {
        val json = org.json.JSONObject().apply {
            put("_format", "customrpc/v1")
            put("appId", appIdEditText.text.toString())
            put("appName", appNameEditText.text.toString())
            put("activityType", activityTypeSpinner.selectedItemPosition)
            put("streamUrl", streamUrlEditText.text.toString())
            put("userStatus", statusSpinner.selectedItemPosition)
            put("details", detailsEditText.text.toString())
            put("state", stateEditText.text.toString())
            put("partySize", partySizeEditText.text.toString())
            put("partyMax", partyMaxEditText.text.toString())
            put("partyId", partyIdEditText.text.toString())
            put("largeImageKey", (largeImageKeyEditText.tag as? String) ?: "")
            put("largeImageName", largeImageKeyEditText.text.toString())
            put("largeImageText", largeImageTextEditText.text.toString())
            put("smallImageKey", (smallImageKeyEditText.tag as? String) ?: "")
            put("smallImageName", smallImageKeyEditText.text.toString())
            put("smallImageText", smallImageTextEditText.text.toString())
            put("btn1Text", btn1Text.text.toString())
            put("btn1Url", btn1Url.text.toString())
            put("btn2Text", btn2Text.text.toString())
            put("btn2Url", btn2Url.text.toString())
            put("timestampMode", timestampSpinner.selectedItemPosition)
            put("customStartTime", customStartTime ?: 0L)
            put("customEndTime", customEndTime ?: 0L)
        }.toString(2)
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("CustomRPC settings", json))
        Toast.makeText(this, getString(R.string.msg_settings_exported), Toast.LENGTH_SHORT).show()
    }

    private fun importSettingsFromClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val raw = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
        if (raw.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.msg_clipboard_empty), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val j = org.json.JSONObject(raw)
            if (j.optString("_format") != "customrpc/v1") {
                Toast.makeText(this, getString(R.string.msg_invalid_json), Toast.LENGTH_SHORT).show()
                return
            }
            appIdEditText.setText(j.optString("appId"))
            appNameEditText.setText(j.optString("appName"))
            activityTypeSpinner.setSelection(j.optInt("activityType", 0))
            streamUrlEditText.setText(j.optString("streamUrl"))
            statusSpinner.setSelection(j.optInt("userStatus", 0))
            detailsEditText.setText(j.optString("details"))
            stateEditText.setText(j.optString("state"))
            partySizeEditText.setText(j.optString("partySize"))
            partyMaxEditText.setText(j.optString("partyMax"))
            partyIdEditText.setText(j.optString("partyId"))
            largeImageKeyEditText.setText(j.optString("largeImageName"))
            largeImageKeyEditText.tag = j.optString("largeImageKey")
            largeImageTextEditText.setText(j.optString("largeImageText"))
            smallImageKeyEditText.setText(j.optString("smallImageName"))
            smallImageKeyEditText.tag = j.optString("smallImageKey")
            smallImageTextEditText.setText(j.optString("smallImageText"))
            btn1Text.setText(j.optString("btn1Text"))
            btn1Url.setText(j.optString("btn1Url"))
            btn2Text.setText(j.optString("btn2Text"))
            btn2Url.setText(j.optString("btn2Url"))
            timestampSpinner.setSelection(j.optInt("timestampMode", 0))
            customStartTime = j.optLong("customStartTime", 0L).takeIf { it != 0L }
            customEndTime = j.optLong("customEndTime", 0L).takeIf { it != 0L }
            if (customStartTime != null) tvStartTimeVal.text = Date(customStartTime!!).toString()
            if (customEndTime != null) tvEndTimeVal.text = Date(customEndTime!!).toString()
            saveSettings()
            updateLivePreview()
            Toast.makeText(this, getString(R.string.msg_settings_imported), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.msg_invalid_json), Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendPresenceUpdate() {
        val typeInt = activityTypeSpinner.selectedItemPosition
        val userStatusStr = when (statusSpinner.selectedItemPosition) {
            0 -> "online"; 1 -> "idle"; 2 -> "dnd"; 3 -> "invisible"; else -> "online"
        }
        var start: Long? = null
        var end: Long? = null
        when (timestampSpinner.selectedItemPosition) {
            1 -> start = System.currentTimeMillis()
            2 -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                start = cal.timeInMillis
            }
            3 -> { start = customStartTime; end = customEndTime }
        }
        val presenceData = PresenceData(
            appId = appIdEditText.text.toString().trim(),
            name = appNameEditText.text.toString().trim(),
            details = detailsEditText.text.toString().trim(),
            state = stateEditText.text.toString().trim(),
            largeImageKey = (largeImageKeyEditText.tag as? String) ?: largeImageKeyEditText.text.toString().trim(),
            largeImageText = largeImageTextEditText.text.toString().trim(),
            smallImageKey = (smallImageKeyEditText.tag as? String) ?: smallImageKeyEditText.text.toString().trim(),
            smallImageText = smallImageTextEditText.text.toString().trim(),
            activityType = typeInt,
            streamUrl = streamUrlEditText.text.toString().trim(),
            partySize = partySizeEditText.text.toString().toIntOrNull(),
            partyMax = partyMaxEditText.text.toString().toIntOrNull(),
            partyId = partyIdEditText.text.toString().trim().ifEmpty { null },
            button1Label = btn1Text.text.toString().trim(),
            button1Url = btn1Url.text.toString().trim(),
            button2Label = btn2Text.text.toString().trim(),
            button2Url = btn2Url.text.toString().trim(),
            timestampStart = start,
            timestampEnd = end,
            userStatus = userStatusStr
        )
        val serviceIntent = Intent(this, RpcService::class.java).apply {
            action = RpcService.ACTION_UPDATE_PRESENCE
            putExtra("PRESENCE_DATA", presenceData)
        }
        startService(serviceIntent)
        Toast.makeText(this, getString(R.string.msg_rpc_updated), Toast.LENGTH_SHORT).show()
    }

    // ── ASSET FETCHING ────────────────────────────────────────────────────────

    private fun fetchAssets(targetInput: EditText) {
        val appId = appIdEditText.text.toString().trim()
        val token = loginTokenInput.text.toString().trim()
        if (appId.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_no_app_id), Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, getString(R.string.msg_fetching_assets), Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://discord.com/api/v9/oauth2/applications/$appId/assets")
                    .addHeader("Authorization", token)
                    .build()
                val response = client.newCall(request).execute()
                val json = response.body?.string()
                if (response.isSuccessful && json != null) {
                    val assets = org.json.JSONArray(json)
                    val assetMap = mutableMapOf<String, String>()
                    assetMap["(None)"] = ""
                    for (i in 0 until assets.length()) {
                        val obj = assets.getJSONObject(i)
                        assetMap[obj.getString("name")] = obj.getString("id")
                    }
                    runOnUiThread {
                        if (assetMap.size <= 1) {
                            Toast.makeText(this, getString(R.string.msg_no_assets_found), Toast.LENGTH_LONG).show()
                        } else {
                            showAssetSelector(assetMap, targetInput)
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun showAssetSelector(assetMap: Map<String, String>, targetInput: EditText) {
        val names = assetMap.keys.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_select_image))
            .setItems(names) { _, which ->
                val selectedName = names[which]
                targetInput.setText(selectedName)
                targetInput.tag = assetMap[selectedName]
            }
            .show()
    }

    // ── LOGS ──────────────────────────────────────────────────────────────────

    private fun bindLogsViews() {
        tvLogs = findViewById(R.id.tv_logs)
        scrollLogs = findViewById(R.id.scroll_logs)

        val btnBackLogs = findViewById<ImageView>(R.id.btn_back_logs)
        val btnClearLogs = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_clear_logs)

        btnBackLogs.setOnClickListener { showDashboard() }
        btnClearLogs.setOnClickListener {
            AppLogger.clear()
            refreshLogsView()
        }
    }

    private val logListener: (LogEntry) -> Unit = { _ ->
        runOnUiThread {
            if (viewLogs.visibility == View.VISIBLE) {
                refreshLogsView()
            }
        }
    }

    private fun refreshLogsView() {
        val entries = AppLogger.getAll()
        if (entries.isEmpty()) {
            tvLogs.text = getString(R.string.log_empty)
        } else {
            val sb = StringBuilder()
            entries.forEach { e ->
                val levelTag = when (e.level) {
                    "WARN" -> "⚠"
                    "ERROR" -> "✗"
                    else -> "✓"
                }
                sb.append("[${e.timestamp}] $levelTag ${e.message}\n")
            }
            tvLogs.text = sb.toString().trimEnd()
            // Scroll to bottom
            scrollLogs.post { scrollLogs.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    // ── ROTATION MODE ─────────────────────────────────────────────────────────

    private fun updateRotationCard() {
        if (!::tvRotationStatus.isInitialized) return
        val presets = loadRotationPresets()
        tvRotationStatus.text = if (rotationEnabled) {
            getString(R.string.rotation_status_on, presets.size)
        } else {
            getString(R.string.rotation_status_off)
        }
        layRotationControls.visibility = if (rotationEnabled) View.VISIBLE else View.GONE
    }

    private fun startRotationInService() {
        val presets = loadRotationPresets()
        if (presets.size < 2) {
            Toast.makeText(this, getString(R.string.msg_need_presets_first), Toast.LENGTH_SHORT).show()
            swRotation.isChecked = false
            rotationEnabled = false
            getSharedPreferences("RpcSettings", Context.MODE_PRIVATE).edit()
                .putBoolean("rotationEnabled", false).apply()
            updateRotationCard()
            return
        }
        val presetsJson = org.json.JSONArray().apply { presets.forEach { put(it.toJson()) } }.toString()
        val idx = spinnerRotationInterval.selectedItemPosition.coerceIn(0, rotationIntervalValues.size - 1)
        val intervalMs = rotationIntervalValues[idx]
        val serviceIntent = Intent(this, RpcService::class.java).apply {
            action = RpcService.ACTION_START_ROTATION
            putExtra("PRESETS_JSON", presetsJson)
            putExtra("INTERVAL_MS", intervalMs)
        }
        startService(serviceIntent)
        Toast.makeText(this, getString(R.string.msg_rotation_started), Toast.LENGTH_SHORT).show()
    }

    private fun stopRotationInService() {
        val serviceIntent = Intent(this, RpcService::class.java).apply {
            action = RpcService.ACTION_STOP_ROTATION
        }
        startService(serviceIntent)
    }

    private fun loadRotationPresets(): List<RotationPreset> {
        val json = getSharedPreferences("RpcSettings", Context.MODE_PRIVATE)
            .getString("rotationPresets", "[]") ?: "[]"
        return try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { RotationPreset.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    private fun saveRotationPresets(presets: List<RotationPreset>) {
        val arr = org.json.JSONArray().apply { presets.forEach { put(it.toJson()) } }
        getSharedPreferences("RpcSettings", Context.MODE_PRIVATE).edit()
            .putString("rotationPresets", arr.toString()).apply()
        updateRotationCard()
    }

    private fun showManagePresetsDialog() {
        val presets = loadRotationPresets().toMutableList()
        val items: Array<String> = if (presets.isEmpty()) {
            arrayOf(getString(R.string.label_no_presets))
        } else {
            presets.map { "▸ ${it.label}  —  ${it.name}" }.toTypedArray()
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_presets_title))
            .setItems(items) { _, which ->
                if (presets.isNotEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle(presets[which].label)
                        .setMessage(
                            "Activity: ${presets[which].name}\n" +
                            "Details: ${presets[which].details.ifBlank { "—" }}\n" +
                            "State: ${presets[which].state.ifBlank { "—" }}"
                        )
                        .setNegativeButton(getString(R.string.btn_close), null)
                        .setPositiveButton(getString(R.string.btn_preset_delete)) { _, _ ->
                            presets.removeAt(which)
                            saveRotationPresets(presets)
                            Toast.makeText(this, getString(R.string.msg_preset_deleted), Toast.LENGTH_SHORT).show()
                        }
                        .show()
                }
            }
            .setNeutralButton(getString(R.string.btn_preset_add_current)) { _, _ ->
                saveCurrentAsRotationPreset()
            }
            .setPositiveButton(getString(R.string.btn_close), null)
            .show()
    }

    private fun saveCurrentAsRotationPreset() {
        val name = appNameEditText.text.toString().trim()
        val appId = appIdEditText.text.toString().trim()
        if (appId.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_no_app_id), Toast.LENGTH_SHORT).show()
            return
        }
        val preset = RotationPreset(
            label = name,
            appId = appId,
            name = name,
            details = detailsEditText.text.toString().trim(),
            state = stateEditText.text.toString().trim(),
            activityType = activityTypeSpinner.selectedItemPosition,
            largeImageKey = (largeImageKeyEditText.tag as? String) ?: largeImageKeyEditText.text.toString().trim(),
            largeImageText = largeImageTextEditText.text.toString().trim(),
            smallImageKey = (smallImageKeyEditText.tag as? String) ?: smallImageKeyEditText.text.toString().trim(),
            userStatus = when (statusSpinner.selectedItemPosition) {
                0 -> "online"; 1 -> "idle"; 2 -> "dnd"; 3 -> "invisible"; else -> "online"
            }
        )
        val presets = loadRotationPresets().toMutableList()
        presets.add(preset)
        saveRotationPresets(presets)
        AppLogger.info("Rotation preset saved: \"${preset.label}\"")
        Toast.makeText(this, getString(R.string.msg_preset_saved), Toast.LENGTH_SHORT).show()
    }

    // ── DEVICE APP TRACKING ───────────────────────────────────────────────────

    private fun startForegroundPolling() {
        stopForegroundPolling()
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP_MR1) return
        foregroundPollRunnable = object : Runnable {
            override fun run() {
                updateDeviceAppDisplay()
                foregroundPollHandler.postDelayed(this, 10_000)
            }
        }
        foregroundPollHandler.post(foregroundPollRunnable!!)
    }

    private fun stopForegroundPolling() {
        foregroundPollRunnable?.let { foregroundPollHandler.removeCallbacks(it) }
        foregroundPollRunnable = null
    }

    private fun updateDeviceAppDisplay() {
        if (!::tvDeviceApp.isInitialized) return
        val appName = getForegroundApp()
        if (appName != null) {
            tvDeviceApp.text = "${getString(R.string.label_device_app)} $appName"
            tvDeviceApp.visibility = View.VISIBLE
            tvDeviceApp.setOnClickListener(null)
        } else if (!hasUsageStatsPermission()) {
            tvDeviceApp.text = getString(R.string.msg_usage_permission)
            tvDeviceApp.visibility = View.VISIBLE
            tvDeviceApp.setOnClickListener {
                startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        } else {
            tvDeviceApp.visibility = View.GONE
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP_MR1) return false
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY, now - 60_000, now
        )
        return !stats.isNullOrEmpty()
    }

    private fun getForegroundApp(): String? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP_MR1) return null
        if (!hasUsageStatsPermission()) return null
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY, now - 15_000, now
        )
        if (stats.isNullOrEmpty()) return null
        val topApp = stats.maxByOrNull { it.lastTimeUsed } ?: return null
        if (topApp.packageName == packageName) return null
        return try {
            val info = packageManager.getApplicationInfo(topApp.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) { null }
    }

    // ── NAVIGATION ────────────────────────────────────────────────────────────

    private fun showLogin() {
        viewLogin.visibility = View.VISIBLE
        viewDashboard.visibility = View.GONE
        viewSettings.visibility = View.GONE
        viewAbout.visibility = View.GONE
        viewLogs.visibility = View.GONE
    }

    private fun showDashboard() {
        viewLogin.visibility = View.GONE
        viewDashboard.visibility = View.VISIBLE
        viewSettings.visibility = View.GONE
        viewAbout.visibility = View.GONE
        viewLogs.visibility = View.GONE
    }

    private fun showSettings() {
        viewLogin.visibility = View.GONE
        viewDashboard.visibility = View.GONE
        viewSettings.visibility = View.VISIBLE
        viewAbout.visibility = View.GONE
        viewLogs.visibility = View.GONE
        updateLivePreview()
    }

    private fun showAbout() {
        viewLogin.visibility = View.GONE
        viewDashboard.visibility = View.GONE
        viewSettings.visibility = View.GONE
        viewAbout.visibility = View.VISIBLE
        viewLogs.visibility = View.GONE

        val btnBackAbout = findViewById<Button>(R.id.btn_back_about)
        val btnGithub = findViewById<Button>(R.id.btn_github)
        val btnEmail = findViewById<Button>(R.id.btn_email)
        val btnDonate = findViewById<Button>(R.id.btn_donate)

        btnBackAbout.setOnClickListener { showDashboard() }
        btnGithub.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/khoirulaksara"))) }
        btnEmail.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:me@serat.us")
                putExtra(Intent.EXTRA_SUBJECT, "CustomRPC Feedback")
            })
        }
        btnDonate.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/gonzsky"))) }
        findViewById<Button>(R.id.btn_nyxen_github).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/developer51709")))
        }
        findViewById<Button>(R.id.btn_nyxen_website).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://nyxen.is-a.dev/")))
        }
    }

    private fun showLogs() {
        viewLogin.visibility = View.GONE
        viewDashboard.visibility = View.GONE
        viewSettings.visibility = View.GONE
        viewAbout.visibility = View.GONE
        viewLogs.visibility = View.VISIBLE
        refreshLogsView()
    }

    // ── BROADCAST RECEIVER & STATUS ───────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(RpcService.ACTION_STATUS_UPDATE)
        ContextCompat.registerReceiver(this, statusReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        AppLogger.addListener(logListener)

        val probeIntent = Intent(this, RpcService::class.java).apply {
            action = RpcService.ACTION_PROBE
        }
        startService(probeIntent)
    }

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == RpcService.ACTION_STATUS_UPDATE) {
                val isConnected = intent.getBooleanExtra("IS_CONNECTED", false)
                val message = intent.getStringExtra("MESSAGE") ?: "Unknown"
                updateDashboardStatus(isConnected, message)
            }
        }
    }

    private fun updateDashboardStatus(isConnected: Boolean, message: String) {
        val particleView = findViewById<ParticleRingView>(R.id.particle_view)
        isServiceConnected = isConnected

        if (isConnected) {
            tvDashboardStatus.text = getString(R.string.status_online)
            tvDashboardStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            tvDashboardDesc.text = message
            btnToggleConnection.text = getString(R.string.btn_stop)
            btnToggleConnection.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
            particleView.setStatus(2)
            updatePresenceInfoCard()
            cardPresenceInfo.visibility = View.VISIBLE
            if (rotationEnabled) startRotationInService()
            startForegroundPolling()
        } else {
            val isConnecting = message.contains("Connecting", true)
            if (isConnecting) {
                tvDashboardStatus.text = getString(R.string.status_connecting)
                tvDashboardStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                particleView.setStatus(1)
            } else {
                tvDashboardStatus.text = getString(R.string.status_offline)
                tvDashboardStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                btnToggleConnection.text = getString(R.string.btn_start)
                btnToggleConnection.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9B5DE5"))
                particleView.setStatus(0)
                cardPresenceInfo.visibility = View.GONE
                stopRotationInService()
                stopForegroundPolling()
                if (::tvDeviceApp.isInitialized) tvDeviceApp.visibility = View.GONE
            }
            tvDashboardDesc.text = message
        }
    }

    private fun updatePresenceInfoCard() {
        val sharedPref = getSharedPreferences("RpcSettings", Context.MODE_PRIVATE)
        val name = sharedPref.getString("appName", "") ?: ""
        val details = sharedPref.getString("details", "") ?: ""
        tvPresenceName.text = name.ifBlank { "—" }
        tvPresenceDetails.text = details.ifBlank { "—" }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(statusReceiver)
        AppLogger.removeListener(logListener)
        stopForegroundPolling()
    }

    // ── PERSISTENCE ───────────────────────────────────────────────────────────

    private fun saveSettings() {
        val sharedPref = getSharedPreferences("RpcSettings", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("token", loginTokenInput.text.toString())
            putString("appId", appIdEditText.text.toString())
            putString("appName", appNameEditText.text.toString())
            putInt("activityType", activityTypeSpinner.selectedItemPosition)
            putString("streamUrl", streamUrlEditText.text.toString())
            putInt("userStatus", statusSpinner.selectedItemPosition)
            putString("details", detailsEditText.text.toString())
            putString("state", stateEditText.text.toString())
            putString("partySize", partySizeEditText.text.toString())
            putString("partyId", partyIdEditText.text.toString())
            putString("partyMax", partyMaxEditText.text.toString())
            putString("largeImageKey", (largeImageKeyEditText.tag as? String) ?: largeImageKeyEditText.text.toString())
            putString("largeImageName", largeImageKeyEditText.text.toString())
            putString("largeImageText", largeImageTextEditText.text.toString())
            putString("smallImageKey", (smallImageKeyEditText.tag as? String) ?: smallImageKeyEditText.text.toString())
            putString("smallImageName", smallImageKeyEditText.text.toString())
            putString("smallImageText", smallImageTextEditText.text.toString())
            putString("btn1Text", btn1Text.text.toString())
            putString("btn1Url", btn1Url.text.toString())
            putString("btn2Text", btn2Text.text.toString())
            putString("btn2Url", btn2Url.text.toString())
            putInt("timestampMode", timestampSpinner.selectedItemPosition)
            putLong("customStartTime", customStartTime ?: 0L)
            putLong("customEndTime", customEndTime ?: 0L)
            apply()
        }
    }

    private fun loadSettings() {
        val sharedPref = getSharedPreferences("RpcSettings", Context.MODE_PRIVATE)
        loginTokenInput.setText(sharedPref.getString("token", ""))
        appIdEditText.setText(sharedPref.getString("appId", ""))
        appNameEditText.setText(sharedPref.getString("appName", ""))
        activityTypeSpinner.setSelection(sharedPref.getInt("activityType", 0))
        streamUrlEditText.setText(sharedPref.getString("streamUrl", ""))
        streamUrlLayout.visibility = if (activityTypeSpinner.selectedItemPosition == 1) View.VISIBLE else View.GONE
        val statusSelection = try { sharedPref.getInt("userStatus", 0) } catch (e: ClassCastException) { 0 }
        statusSpinner.setSelection(statusSelection)
        detailsEditText.setText(sharedPref.getString("details", ""))
        stateEditText.setText(sharedPref.getString("state", ""))
        partySizeEditText.setText(sharedPref.getString("partySize", ""))
        partyIdEditText.setText(sharedPref.getString("partyId", ""))
        partyMaxEditText.setText(sharedPref.getString("partyMax", ""))
        largeImageKeyEditText.setText(sharedPref.getString("largeImageName", ""))
        largeImageKeyEditText.tag = sharedPref.getString("largeImageKey", "")
        largeImageTextEditText.setText(sharedPref.getString("largeImageText", ""))
        smallImageKeyEditText.setText(sharedPref.getString("smallImageName", ""))
        smallImageKeyEditText.tag = sharedPref.getString("smallImageKey", "")
        smallImageTextEditText.setText(sharedPref.getString("smallImageText", ""))
        btn1Text.setText(sharedPref.getString("btn1Text", ""))
        btn1Url.setText(sharedPref.getString("btn1Url", ""))
        btn2Text.setText(sharedPref.getString("btn2Text", ""))
        btn2Url.setText(sharedPref.getString("btn2Url", ""))
        timestampSpinner.setSelection(sharedPref.getInt("timestampMode", 2))
        customStartTime = sharedPref.getLong("customStartTime", 0L).takeIf { it != 0L }
        customEndTime = sharedPref.getLong("customEndTime", 0L).takeIf { it != 0L }
        if (customStartTime != null) tvStartTimeVal.text = Date(customStartTime!!).toString()
        if (customEndTime != null) tvEndTimeVal.text = Date(customEndTime!!).toString()
    }

    // ── UTILITIES ─────────────────────────────────────────────────────────────

    private fun pickDateTime(onPicked: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(year, month, day, hour, minute)
                onPicked(calendar.timeInMillis)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTokenGuideDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_guide_title))
            .setMessage(getString(R.string.dialog_guide_msg))
            .setPositiveButton(getString(R.string.btn_open_login)) { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com/login")))
            }
            .setNegativeButton(getString(R.string.btn_close), null)
            .show()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null && uri.scheme == "customrpc" && uri.host == "token") {
                val token = uri.getQueryParameter("value")
                if (!token.isNullOrEmpty()) {
                    loginTokenInput.setText(token)
                    saveSettings()
                    showDashboard()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
}
