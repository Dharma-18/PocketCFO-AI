package com.example.pocketcfo1

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * MainActivity — thin navigation shell.
 * All content lives in Fragments. This only handles:
 *   1. BottomNav tab switching
 *   2. SoundBox broadcast receiver
 *   3. Permissions
 */
class MainActivity : AppCompatActivity() {

    private var soundBoxEnabled = false

    private lateinit var bottomNav: BottomNavigationView

    // Fragments (lazy to avoid re-creation)
    private val homeFragment        by lazy { HomeFragment() }
    private val transactionsFragment by lazy { TransactionsFragment() }
    private val insightsFragment    by lazy { InsightsFragment() }
    private val assistantFragment   by lazy { AssistantFragment() }
    private val profileFragment     by lazy { ProfileFragment() }

    // SoundBox auto-detection receiver
    private val soundboxReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val amount = intent?.getIntExtra(SoundBoxListenerService.EXTRA_AMOUNT, 0) ?: 0
            val text   = intent?.getStringExtra(SoundBoxListenerService.EXTRA_TEXT) ?: return
            if (amount > 0) {
                // Show badge on Capture tab
                bottomNav.getOrCreateBadge(R.id.nav_capture).number =
                    (bottomNav.getBadge(R.id.nav_capture)?.number ?: 0) + 1
                // If HomeFragment is visible, add card directly
                val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (current is HomeFragment) {
                    current.addBubble("🎤 SoundBox detected: ₹$amount auto-logged!", false)
                    current.setNotificationActive(true)
                }
                Toast.makeText(this@MainActivity, "📡 SoundBox: ₹$amount logged!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)

        // Load Home tab on first start
        if (savedInstanceState == null) {
            switchFragment(homeFragment)
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home    -> { switchFragment(homeFragment); true }
                R.id.nav_history -> { switchFragment(transactionsFragment); true }
                R.id.nav_capture -> {
                    toggleSoundBox()
                    bottomNav.removeBadge(R.id.nav_capture)
                    true
                }
                R.id.nav_search  -> { switchFragment(insightsFragment); true }
                R.id.nav_profile -> { switchFragment(profileFragment); true }
                else -> false
            }
        }

        requestAllPermissions()
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // ── SoundBox Toggle ─────────────────────────────────────────────────────
    private fun toggleSoundBox() {
        soundBoxEnabled = !soundBoxEnabled
        if (soundBoxEnabled) {
            val perm = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            if (perm == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, SoundBoxListenerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
                else startService(intent)
                Toast.makeText(this, "🎙 SoundBox ON — Listening for Paytm", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "⚠️ Grant microphone permission first", Toast.LENGTH_SHORT).show()
                soundBoxEnabled = false
            }
        } else {
            stopService(Intent(this, SoundBoxListenerService::class.java))
            Toast.makeText(this, "🔕 SoundBox OFF", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Lifecycle: register/unregister broadcast ────────────────────────────
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(SoundBoxListenerService.ACTION_SOUNDBOX_LOG)
        ContextCompat.registerReceiver(this, soundboxReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        super.onStop()
        try { unregisterReceiver(soundboxReceiver) } catch (_: Exception) {}
    }

    // ── Permissions ─────────────────────────────────────────────────────────
    private fun requestAllPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECEIVE_SMS)
            needed.add(Manifest.permission.READ_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
    }
}