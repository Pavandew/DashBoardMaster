package com.example.masterdashboard.home.res_lists.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.home.logs.adapter.LogsAdapter
import com.example.masterdashboard.home.res_lists.models.RestaurantData

class EditResFragment : Fragment() {

    private lateinit var toolbar: Toolbar
    private lateinit var toolbarTitle: TextView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_res, container, false)

        toolbar(view)

        return view
    }

    private fun toolbar(view: View) {
        toolbar = view.findViewById(R.id.edit_res_toolbar)
        toolbarTitle = view.findViewById(R.id.toolbar_tvTitle)
        toolbarTitle.text = getString(R.string.edit_restaurant)
    }
}