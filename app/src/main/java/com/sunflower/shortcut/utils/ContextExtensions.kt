package com.sunflower.shortcut.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * The Activity behind this Context, or null for an Application/Service
 * context. Compose's LocalContext is often a ContextWrapper around the
 * Activity (themed contexts, dialogs), so a plain `as? Activity` isn't enough.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
