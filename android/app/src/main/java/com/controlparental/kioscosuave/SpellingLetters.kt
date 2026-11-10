package com.controlparental.kioscosuave

/** Each tile has an identity, so repeated letters can be used independently. */
object SpellingLetters {
    fun bank(word: String): List<Char> {
        val extras = ('a'..'z').filter { it !in word.lowercase() }.shuffled().take(3)
        return (word.toList() + extras).shuffled()
    }

    fun answer(letters: List<Char>): String = letters.joinToString("-") { it.uppercase() }

    /** Spelling first, then the whole word: "see, ay, tee, cat." */
    fun reviewSpeech(word: String): String =
        "${word.map(::letterName).joinToString(", ")}, $word."

    // English letter names spelled out, because the TTS voice may read a lone "A" as "uh".
    fun letterName(letter: Char): String = when (letter.lowercaseChar()) {
        'a' -> "ay"; 'b' -> "bee"; 'c' -> "see"; 'd' -> "dee"
        'e' -> "ee"; 'f' -> "eff"; 'g' -> "jee"; 'h' -> "aitch"
        'i' -> "eye"; 'j' -> "jay"; 'k' -> "kay"; 'l' -> "el"
        'm' -> "em"; 'n' -> "en"; 'o' -> "oh"; 'p' -> "pee"
        'q' -> "cue"; 'r' -> "ar"; 's' -> "ess"; 't' -> "tee"
        'u' -> "you"; 'v' -> "vee"; 'w' -> "double you"; 'x' -> "ex"
        'y' -> "why"; 'z' -> "zee"
        else -> letter.toString()
    }

    // Short phonics approximations for Android's English voice, not letter names.
    fun sound(letter: Char): String = when (letter.lowercaseChar()) {
        'a' -> "ah"; 'b' -> "buh"; 'c', 'k' -> "kuh"; 'd' -> "duh"
        'e' -> "eh"; 'f' -> "fff"; 'g' -> "guh"; 'h' -> "huh"
        'i' -> "ih"; 'j' -> "juh"; 'l' -> "lll"; 'm' -> "mmm"
        'n' -> "nnn"; 'o' -> "ah"; 'p' -> "puh"; 'q' -> "kwuh"
        'r' -> "rrr"; 's' -> "sss"; 't' -> "tuh"; 'u' -> "uh"
        'v' -> "vvv"; 'w' -> "wuh"; 'x' -> "ks"; 'y' -> "yuh"; 'z' -> "zzz"
        else -> letter.toString()
    }
}
