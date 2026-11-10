package com.controlparental.kioscosuave.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Only preschool / first grade uses English throughout the child interface. */
internal val LocalStarterEnglish = staticCompositionLocalOf { false }

@Composable
internal fun childText(spanish: String, english: String): String =
    if (LocalStarterEnglish.current) english else spanish
