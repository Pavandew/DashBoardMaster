package com.example.masterdashboard.home.create_res

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.example.masterdashboard.R

class CreateNewResFragment : Fragment() {

    private lateinit var toolbar: Toolbar
    private lateinit var toolbarTitle: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_create_new_res, container, false)

        toolbar(view)

        return view
    }

    private fun toolbar(view: View) {

        toolbar = view.findViewById(R.id.new_res_toolbar)
        toolbarTitle = view.findViewById(R.id.toolbar_tvTitle)
        toolbarTitle.setText(R.string.create_new_res)

    }
}