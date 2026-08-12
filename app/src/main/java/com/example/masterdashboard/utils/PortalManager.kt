package com.example.masterdashboard.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.LinearGradient
import android.graphics.Shader
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemPortalCardBinding
import com.example.masterdashboard.login.models.PortalFeature
import com.example.masterdashboard.login.models.PortalItem

class PortalManager(private val context: Context) {

    /**
     * Applies a horizontal gradient to a TextView
     */
    fun applyTextGradient(textView: TextView) {
        textView.post {
            val paint = textView.paint
            val width = paint.measureText(textView.text.toString())
            val colorStart = ContextCompat.getColor(context, R.color.portal_title_start)
            val colorEnd = ContextCompat.getColor(context, R.color.portal_title_end)

            textView.paint.shader = LinearGradient(
                0f, 0f, width, 0f,
                intArrayOf(colorStart, colorEnd),
                null, Shader.TileMode.CLAMP
            )
            textView.invalidate()
        }
    }

    /**
     * Binds data to an included portal card layout
     */
    fun bindCard(binding: ItemPortalCardBinding, item: PortalItem) {
        val color = ContextCompat.getColor(context, item.themeColor)
        val colorState = ColorStateList.valueOf(color)

        binding.apply {
            portalTvTitleFirst.text = item.title
            portalTvTitleFirst.setTextColor(color)
            portalTvSubTitle.text = item.subTitle
            portalTvDescription.text = item.description
            portalIvMainIcon.setImageResource(item.mainIcon)

            // Apply the theme color to card elements
            portalCardMain.setCardBackgroundColor(color)
            portalCardIconBg.setCardBackgroundColor(color)
            portalIvArrow.backgroundTintList = colorState

            // Bind features dynamically
            setupFeature(ivFeature1Icon, tvFeature1Text, item.features.getOrNull(0))
            setupFeature(ivFeature2Icon, tvFeature2Text, item.features.getOrNull(1))
            setupFeature(ivFeature3Icon, tvFeature3Text, item.features.getOrNull(2))

            root.setOnClickListener { item.onClick() }
        }
    }

    private fun setupFeature(icon: ImageView, text: TextView, feature: PortalFeature?) {
        if (feature != null) {
            icon.setImageResource(feature.icon)
            icon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, feature.color))
            text.text = feature.text
        }
    }
}