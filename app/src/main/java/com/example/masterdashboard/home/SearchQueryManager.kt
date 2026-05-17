package com.example.masterdashboard.home

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class SearchQueryManager <T> (
    private val searchEditText: EditText,
    private val originalList: List<T>,
    private val onResultFiltered: (List<T>) -> Unit,
    private val filterRule: (item: T, query: String) -> Boolean
) {

    private val textWatcher = object : TextWatcher{

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
            filter(s.toString())
        }

        override fun afterTextChanged(p0: Editable?) {
        }
    }

    init {
        searchEditText.addTextChangedListener(textWatcher)
    }

    private fun filter(query: String) {
        val filteredList = if(query.trim().isEmpty()) {
            originalList
        } else {
            originalList.filter { item ->
                filterRule(item, query.trim())
            }
        }

        onResultFiltered(filteredList)
    }

    // call this inside onDestroyView
    fun removeListener() {
        searchEditText.removeTextChangedListener(textWatcher)
    }
}