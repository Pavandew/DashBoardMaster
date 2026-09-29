package com.restroone.app.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.LinearGradient
import android.graphics.Shader
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.restroone.app.R
import com.restroone.app.databinding.ItemPortalCardBinding
import com.restroone.app.login.models.PortalFeature
import com.restroone.app.login.models.PortalItem

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
        val themeColorVal = ContextCompat.getColor(context, item.themeColor)
        val bgLightColorVal = ContextCompat.getColor(context, item.bgLightColor)
        val themeColorState = ColorStateList.valueOf(themeColorVal)

        binding.apply {
            // Main card background is always pure white
            portalCardMain.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))

            // Title and Descriptions
            portalTvTitleFirst.text = item.title
            portalTvTitleFirst.setTextColor(themeColorVal)
            portalTvSubTitle.text = item.subTitle
            portalTvDescription.text = item.description

            // Main Icon container (soft pastel background, icon tinted with primary theme color)
            portalCardIconBg.setCardBackgroundColor(bgLightColorVal)
            portalIvMainIcon.setImageResource(item.mainIcon)
            portalIvMainIcon.imageTintList = themeColorState

            // Arrow button (circle background filled with primary theme color, white arrow)
            portalIvArrow.backgroundTintList = themeColorState
            portalIvArrow.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))

            // Bind bottom features dynamically
            setupFeature(ivFeature1Icon, tvFeature1Text, item.features.getOrNull(0))
            setupFeature(ivFeature2Icon, tvFeature2Text, item.features.getOrNull(1))
            setupFeature(ivFeature3Icon, tvFeature3Text, item.features.getOrNull(2))

            root.setOnClickListener { item.onClick() }
        }
    }

    private fun setupFeature(icon: ImageView, text: TextView, feature: PortalFeature?) {
        if (feature != null) {
            val featureColor = ContextCompat.getColor(context, feature.color)
            val featureColorState = ColorStateList.valueOf(featureColor)
            icon.setImageResource(feature.icon)
            icon.imageTintList = featureColorState
            text.text = feature.text
            text.setTextColor(featureColor)
        }
    }
}