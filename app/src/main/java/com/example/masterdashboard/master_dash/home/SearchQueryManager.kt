package com.example.masterdashboard.master_dash.home

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class SearchQueryManager<T>(
    private val searchEditText: EditText,
    private val originalList: List<T>, // Passed reference list pointer
    private val onResultFiltered: (List<T>) -> Unit,
    private val filterRule: (item: T, query: String) -> Boolean
) {

    // Track the last processed query to prevent redundant layout updates on empty white-spaces
    private var lastQuery = ""

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
            val query = s?.toString() ?: ""
            if (query != lastQuery) {
                lastQuery = query
                filter(query)
            }
        }

        override fun afterTextChanged(p0: Editable?) {}
    }

    init {
        searchEditText.addTextChangedListener(textWatcher)
    }

    private fun filter(query: String) {
        val trimmedQuery = query.trim()

        // FIXED: Creating a clean .toList() snapshot copy isolates memory layers
        // from being modified concurrently by the Fragment lifecycle flow.
        val filteredList = if (trimmedQuery.isEmpty()) {
            originalList.toList()
        } else {
            originalList.filter { item ->
                filterRule(item, trimmedQuery)
            }
        }

        onResultFiltered(filteredList)
    }

    // Call this inside onDestroyView
    fun removeListener() {
        searchEditText.removeTextChangedListener(textWatcher)
    }
}