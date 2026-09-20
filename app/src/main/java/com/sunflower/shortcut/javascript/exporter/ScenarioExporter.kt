package com.sunflower.shortcut.javascript.exporter

/**
 * Turns a stored automation back into the "обычный .js файл" spec §33
 * insists on — normalized line endings plus a small identifying header
 * comment, never a proprietary wrapper format. The header is a comment, so
 * it never changes what the script does.
 */
object ScenarioExporter {

    fun export(js: String, title: String? = null): String {
        val normalized = js.replace("\r\n", "\n").replace("\r", "\n").trim('\n') + "\n"
        val header = "// ${title?.takeIf { it.isNotBlank() } ?: "Сценарий Shortcut"}\n" +
            "// Экспортировано из Shortcut — bina.co\n\n"
        return header + normalized
    }

    /** Cyrillic-safe: transliterates before slugifying, so a scenario named
     *  "Фото при зарядке" doesn't export as "automation.js" by accident. */
    fun suggestedFileName(title: String): String {
        val slug = title.lowercase()
            .map(::transliterate)
            .joinToString("")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        return "${slug.ifBlank { "automation" }}.js"
    }

    private fun transliterate(c: Char): String = when (c) {
        'а' -> "a"; 'б' -> "b"; 'в' -> "v"; 'г' -> "g"; 'д' -> "d"; 'е' -> "e"; 'ё' -> "e"
        'ж' -> "zh"; 'з' -> "z"; 'и' -> "i"; 'й' -> "y"; 'к' -> "k"; 'л' -> "l"; 'м' -> "m"
        'н' -> "n"; 'о' -> "o"; 'п' -> "p"; 'р' -> "r"; 'с' -> "s"; 'т' -> "t"; 'у' -> "u"
        'ф' -> "f"; 'х' -> "h"; 'ц' -> "ts"; 'ч' -> "ch"; 'ш' -> "sh"; 'щ' -> "sch"
        'ъ' -> ""; 'ы' -> "y"; 'ь' -> ""; 'э' -> "e"; 'ю' -> "yu"; 'я' -> "ya"
        else -> c.toString()
    }
}
