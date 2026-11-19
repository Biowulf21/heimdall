package com.chooloo.www.koler.ui.main

import com.chooloo.www.chooloolib.domain.model.DialpadContact
import com.chooloo.www.chooloolib.domain.model.phoneKeyLetters
import java.text.Normalizer

internal data class DialpadMatch(
    val entry: DialpadContact,
    val numbers: List<String>,
    val nameMatch: List<IntRange>,
    val numberMatch: List<IntRange>,
)

internal fun dialpadQuery(value: String): String? =
    value.removePrefix("+").takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        ?.map { it.digitToInt().digitToChar() }?.joinToString("")

internal fun matchContact(entry: DialpadContact, query: String): DialpadMatch? {
    val nameMatch = nameMatches(entry.contact.name.orEmpty(), query)
    val matchingNumbers = entry.numbers.mapNotNull { number ->
        numberMatch(number, query)?.let { number to it }
    }
    if (nameMatch.isEmpty() && matchingNumbers.isEmpty()) return null

    return DialpadMatch(
        entry = entry,
        numbers = matchingNumbers.map { it.first }.ifEmpty { entry.numbers },
        nameMatch = nameMatch,
        numberMatch = matchingNumbers.firstOrNull()?.second.orEmpty(),
    )
}

private val words = Regex("[\\p{L}\\p{Nd}][\\p{L}\\p{M}\\p{Nd}]*")
private val combiningMarks = Regex("\\p{M}+")

private fun nameMatches(name: String, query: String): List<IntRange> {
    val tokens = words.findAll(name).toList()
    for (token in tokens) {
        val encoded = token.value.mapNotNull(::keyFor)
        if (encoded.joinToString("").startsWith(query)) {
            val lastIndex = token.value.indices.filter { keyFor(token.value[it]) != null }
            return listOf(token.range.first..(token.range.first + lastIndex[query.lastIndex]))
        }
    }
    val initials = tokens.map { keyFor(it.value.first()) ?: ' ' }.joinToString("")
    val start = initials.indexOf(query)
    if (start < 0) return emptyList()
    return tokens.subList(start, start + query.length).map { it.range.first..it.range.first }
}

private fun keyFor(letter: Char): Char? {
    letter.digitToIntOrNull()?.let { return it.digitToChar() }
    val base = Normalizer.normalize(letter.toString(), Normalizer.Form.NFD)
        .replace(combiningMarks, "").firstOrNull()?.uppercaseChar() ?: return null
    return phoneKeyLetters.entries.firstOrNull { base in it.value }?.key
}

private fun numberMatch(number: String, query: String): List<IntRange>? {
    val positions = number.indices.filter { number[it].isDigit() }
    val digits = positions.map { number[it].digitToInt().digitToChar() }.joinToString("")
    val start = digits.indexOf(query)
    if (start < 0) return null
    return positions.subList(start, start + query.length).map { it..it }
}
