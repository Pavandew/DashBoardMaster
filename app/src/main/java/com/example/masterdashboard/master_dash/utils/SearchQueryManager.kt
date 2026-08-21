package com.example.masterdashboard.master_dash.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class SearchQueryManager<T>(
    private val searchEditText: EditText,
    private var originalList: List<T>,
    private val onResultFiltered: (List<T>) -> Unit,
    private val filterRule: (item: T, query: String) -> Boolean
) {

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
        val filteredList = if (trimmedQuery.isEmpty()) {
            originalList.toList()
        } else {
            originalList.filter { item ->
                filterRule(item, trimmedQuery)
            }
        }
        onResultFiltered(filteredList)
    }

    fun updateDataList(newList: List<T>) {
        this.originalList = newList
        refreshSearch()
    }

    fun removeListener() {
        searchEditText.removeTextChangedListener(textWatcher)
    }

    fun refreshSearch() {
        filter(searchEditText.text.toString())
    }
}
