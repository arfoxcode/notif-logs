package com.navre.notiflogs.util

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object AppColors {
    var darkMode: Boolean = false

    fun apply(isDark: Boolean) {
        darkMode = isDark
    }

    val bg: Int get() = if (darkMode) Color.parseColor("#0F1115") else Color.parseColor("#F8F5EC")
    val surface: Int get() = if (darkMode) Color.parseColor("#171A21") else Color.WHITE
    val surfaceAlt: Int get() = if (darkMode) Color.parseColor("#1E232D") else Color.parseColor("#FFF8E1")
    val surfaceWarn: Int get() = if (darkMode) Color.parseColor("#2A2118") else Color.parseColor("#FFF4E5")
    val surfaceDanger: Int get() = if (darkMode) Color.parseColor("#2B1818") else Color.parseColor("#FDECEC")
    val surfaceSuccess: Int get() = if (darkMode) Color.parseColor("#16261B") else Color.parseColor("#E9F7EF")
    val textPrimary: Int get() = if (darkMode) Color.parseColor("#F5F7FA") else Color.parseColor("#1F2937")
    val textSecondary: Int get() = if (darkMode) Color.parseColor("#B8C0CC") else Color.parseColor("#6B7280")
    val textMuted: Int get() = if (darkMode) Color.parseColor("#8A94A6") else Color.parseColor("#9CA3AF")
    val border: Int get() = if (darkMode) Color.parseColor("#2E3440") else Color.parseColor("#E8DEC4")
    val brand: Int get() = Color.parseColor("#D4AF37")
    val brandDark: Int get() = if (darkMode) Color.parseColor("#E1BC47") else Color.parseColor("#9A6B00")
    val gold: Int get() = Color.parseColor("#D4AF37")
    val goldDark: Int get() = if (darkMode) Color.parseColor("#E1BC47") else Color.parseColor("#8A5A00")
    val goldSoft: Int get() = if (darkMode) Color.parseColor("#3A2C0D") else Color.parseColor("#F3E3A3")
    val goldText: Int get() = if (darkMode) Color.parseColor("#F0D57A") else Color.parseColor("#7A5200")
    val navBg: Int get() = if (darkMode) Color.parseColor("#171A21") else Color.parseColor("#FFFDF7")
    val navItemBg: Int get() = if (darkMode) Color.parseColor("#30240C") else Color.parseColor("#FFF4CC")
    val green: Int get() = if (darkMode) Color.parseColor("#4ADE80") else Color.parseColor("#15803D")
    val orange: Int get() = if (darkMode) Color.parseColor("#FB923C") else Color.parseColor("#EA580C")
    val red: Int get() = if (darkMode) Color.parseColor("#F87171") else Color.parseColor("#DC2626")
    val chipBlue: Int get() = if (darkMode) Color.parseColor("#1E3A5F") else Color.parseColor("#DBEAFE")
    val chipGreen: Int get() = if (darkMode) Color.parseColor("#183524") else Color.parseColor("#DCFCE7")
    val chipOrange: Int get() = if (darkMode) Color.parseColor("#3A2615") else Color.parseColor("#FFEDD5")
    val chipRed: Int get() = if (darkMode) Color.parseColor("#3A1E1E") else Color.parseColor("#FEE2E2")
    val chipGray: Int get() = if (darkMode) Color.parseColor("#2B313D") else Color.parseColor("#E5E7EB")
    val chipGold: Int get() = if (darkMode) Color.parseColor("#3A2C0D") else Color.parseColor("#FFF1B8")
}

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun roundedBg(color: Int, radius: Float, strokeColor: Int? = null, strokeWidth: Int = 1): GradientDrawable {
    return GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (strokeColor != null) setStroke(strokeWidth, strokeColor)
    }
}

fun TextView.bold() {
    typeface = Typeface.DEFAULT_BOLD
}

fun View.cardBackground(context: Context) {
    background = roundedBg(AppColors.surface, context.dp(18).toFloat(), AppColors.border, context.dp(1))
    elevation = context.dp(1).toFloat()
}

fun Context.sectionTitle(text: String): TextView = TextView(this).apply {
    this.text = text
    textSize = 18f
    setTextColor(AppColors.textPrimary)
    bold()
}

fun Context.bodyText(text: String, size: Float = 14f): TextView = TextView(this).apply {
    this.text = text
    textSize = size
    setTextColor(AppColors.textSecondary)
}

fun Context.chip(text: String, bgColor: Int, textColor: Int = AppColors.textPrimary): TextView = TextView(this).apply {
    this.text = text
    textSize = 12f
    setTextColor(textColor)
    setPadding(dp(10), dp(5), dp(10), dp(5))
    background = roundedBg(bgColor, dp(999).toFloat())
}

fun Context.statCard(value: String, label: String): LinearLayout {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = roundedBg(AppColors.surfaceAlt, dp(16).toFloat(), AppColors.border, dp(1))
        addView(TextView(context).apply {
            text = value
            textSize = 20f
            setTextColor(AppColors.textPrimary)
            bold()
        })
        addView(TextView(context).apply {
            text = label
            textSize = 12f
            setTextColor(AppColors.textSecondary)
            setPadding(0, dp(4), 0, 0)
        })
    }
}

fun Context.primaryButton(textValue: String, click: () -> Unit): Button = Button(this).apply {
    text = textValue
    isAllCaps = false
    textSize = 14f
    setTextColor(if (AppColors.darkMode) AppColors.textPrimary else Color.WHITE)
    background = roundedBg(AppColors.brandDark, dp(14).toFloat())
    setPadding(dp(12), dp(10), dp(12), dp(10))
    setOnClickListener { click() }
}

fun Context.secondaryButton(textValue: String, click: () -> Unit): Button = Button(this).apply {
    text = textValue
    isAllCaps = false
    textSize = 14f
    setTextColor(AppColors.textPrimary)
    background = roundedBg(AppColors.surface, dp(14).toFloat(), AppColors.border, dp(1))
    setPadding(dp(12), dp(10), dp(12), dp(10))
    setOnClickListener { click() }
}

fun Context.pillButton(textValue: String, fill: Int, textColor: Int = if (AppColors.darkMode) AppColors.textPrimary else Color.WHITE, click: () -> Unit): Button = Button(this).apply {
    text = textValue
    isAllCaps = false
    textSize = 13f
    setTextColor(textColor)
    background = roundedBg(fill, dp(12).toFloat())
    setPadding(dp(10), dp(8), dp(10), dp(8))
    setOnClickListener { click() }
}

fun Context.emptyState(text: String): TextView = TextView(this).apply {
    this.text = text
    gravity = Gravity.CENTER
    textSize = 15f
    setTextColor(AppColors.textSecondary)
    setPadding(dp(16), dp(32), dp(16), dp(32))
}

fun Context.showInfoDialog(title: String, message: String) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton("OK", null)
        .show()
}

fun Context.infoButton(title: String, message: String): TextView = TextView(this).apply {
    text = "i"
    gravity = Gravity.CENTER
    textSize = 12f
    bold()
    setTextColor(AppColors.goldText)
    background = roundedBg(AppColors.chipGold, dp(999).toFloat(), AppColors.goldSoft, dp(1))
    setOnClickListener { showInfoDialog(title, message) }
}

fun Context.titleRow(title: String, infoTitle: String? = null, infoMessage: String? = null, textSize: Float = 18f): LinearLayout {
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(TextView(context).apply {
            text = title
            this.textSize = textSize
            setTextColor(AppColors.textPrimary)
            bold()
        }, LinearLayout.LayoutParams(0, -2, 1f))
        if (infoTitle != null && infoMessage != null) {
            addView(infoButton(infoTitle, infoMessage), LinearLayout.LayoutParams(dp(24), dp(24)))
        }
    }
}
