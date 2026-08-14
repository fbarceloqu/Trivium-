package com.controlparental.kioscosuave.ui

import com.controlparental.kioscosuave.GradeLevel

/**
 * LENGUAJE VISUAL SEGÚN LA EDAD DEL ALUMNO
 * ========================================
 *
 * Kotlin puro, sin Compose ni Android: se puede verificar fuera del emulador.
 *
 * Hasta ahora la interfaz tenía UN solo lenguaje visual con un interruptor de
 * tamaño (`big: Boolean`). Por eso secundaria se sentía infantil: compartía el
 * lenguaje de preescolar, solo con la letra más chica. Y primaria no explotaba
 * lo visual: agrandaba el texto, pero el ejercicio seguía siendo texto.
 *
 * [Level] es un eje NUEVO, perpendicular al del espacio disponible:
 *
 *     ESPACIO   ancho × alto → escala, apilado o dos paneles
 *        ×
 *     NIVEL     PRIMARY | SECONDARY → qué se dibuja y con qué densidad
 *
 * No es un renombrado de `big`. `big` decía *de qué tamaño*; [Level] dice
 * *qué lenguaje*.
 */
enum class Level {
    /**
     * Preescolar y primaria. "Estoy jugando y aprendiendo."
     * Los dibujos son protagonistas, hay pocos elementos y son grandes,
     * el texto es amplio y los controles se ven sin buscarlos.
     */
    PRIMARY,

    /**
     * Secundaria. "Es una herramienta moderna para estudiar."
     * El enunciado manda, la densidad es mayor (cabe más información a la vez),
     * los controles son discretos y no hay decoración infantil.
     */
    SECONDARY;

    val isPrimary: Boolean get() = this == PRIMARY

    companion object {
        /**
         * Preescolar y primaria comparten lenguaje; secundaria tiene el suyo.
         *
         * Se deriva del perfil que ya existe: no hace falta tocar
         * [GradeLevel] ni nada del motor.
         */
        fun forGrade(grade: GradeLevel): Level = when (grade) {
            GradeLevel.PREESCOLAR, GradeLevel.PRIMARIA -> PRIMARY
            GradeLevel.SECUNDARIA -> SECONDARY
        }
    }
}
