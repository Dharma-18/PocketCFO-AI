package com.example.pocketcfo1

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * OnboardingActivity — shown ONCE on first app launch.
 * User picks their shop type → saved to SharedPreferences → MainActivity uses it for context.
 */
class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Skip onboarding if already done
        val prefs = getSharedPreferences("pocketcfo_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("onboarding_done", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_onboarding)

        fun launch(shopType: String, greeting: String) {
            prefs.edit()
                .putString("shop_type", shopType)
                .putBoolean("onboarding_done", true)
                .putString("greeting_name", greeting)
                .apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.shopGrocery).apply {
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                launch("grocery", "Grocery Store")
            }
        }

        findViewById<LinearLayout>(R.id.shopTeaStall).apply {
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                launch("tea_stall", "Tea Stall")
            }
        }

        findViewById<LinearLayout>(R.id.shopFreelancer).apply {
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                launch("freelancer", "Freelancer")
            }
        }
    }
}
