@file:Suppress("unused")

package `in`.fivedegree.volineui.dropdown

/**
 * Interface for custom filtering logic in searchable dropdowns.
 */
fun interface DropdownFilter {
    /**
     * Filters the list of options based on the search query.
     * 
     * @param query The search query string
     * @param options The complete list of options to filter
     * @return A filtered list of options matching the query
     */
    fun filter(query: String, options: List<DropdownOption>): List<DropdownOption>
}

/**
 * Default filter implementation that performs case-insensitive text matching.
 */
class DefaultDropdownFilter : DropdownFilter {
    override fun filter(query: String, options: List<DropdownOption>): List<DropdownOption> {
        if (query.isBlank()) return options
        
        val lowerQuery = query.lowercase()
        return options.filter { option ->
            // Skip headers and dividers from filtering
            if (option.isHeader || option.isDivider) return@filter true
            
            // Match against text, description, and value
            option.text.lowercase().contains(lowerQuery) ||
            option.description?.lowercase()?.contains(lowerQuery) == true ||
            option.value?.toString()?.lowercase()?.contains(lowerQuery) == true
        }
    }
}

/**
 * Fuzzy filter that matches options even with typos or partial matches.
 * Uses a simple character-by-character matching algorithm.
 */
class FuzzyDropdownFilter : DropdownFilter {
    override fun filter(query: String, options: List<DropdownOption>): List<DropdownOption> {
        if (query.isBlank()) return options
        
        val lowerQuery = query.lowercase()
        return options.filter { option ->
            if (option.isHeader || option.isDivider) return@filter true
            
            fuzzyMatch(lowerQuery, option.text.lowercase()) ||
            fuzzyMatch(lowerQuery, option.description?.lowercase() ?: "")
        }
    }
    
    private fun fuzzyMatch(query: String, text: String): Boolean {
        var queryIndex = 0
        for (char in text) {
            if (queryIndex < query.length && char == query[queryIndex]) {
                queryIndex++
            }
        }
        return queryIndex == query.length
    }
}

/**
 * Filter that only matches options starting with the query (prefix matching).
 */
class PrefixDropdownFilter : DropdownFilter {
    override fun filter(query: String, options: List<DropdownOption>): List<DropdownOption> {
        if (query.isBlank()) return options
        
        val lowerQuery = query.lowercase()
        return options.filter { option ->
            if (option.isHeader || option.isDivider) return@filter true
            
            option.text.lowercase().startsWith(lowerQuery)
        }
    }
}
