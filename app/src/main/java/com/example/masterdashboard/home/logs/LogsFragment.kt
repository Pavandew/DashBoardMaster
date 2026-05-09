package com.example.masterdashboard.home.logs

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
import com.example.masterdashboard.home.logs.models.LogData

class LogsFragment : Fragment(), LogsAdapter.OnLogClickListener {

    private lateinit var toolbar: Toolbar
    private lateinit var toolbarTitle: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var logsAdapter: LogsAdapter

    private val logsList = ArrayList<LogData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_logs, container, false)

        toolbar(view)
        initRecyclerView(view)
        loadDummyLogs()

        return view
    }

    private fun toolbar(view: View) {
        toolbar = view.findViewById(R.id.logs_toolbar)
        toolbarTitle = view.findViewById(R.id.toolbar_tvTitle)
        toolbarTitle.text = getString(R.string.activity_logs)
    }

    private fun initRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.rvLogs)

        logsAdapter = LogsAdapter(logsList, this)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = logsAdapter
    }

    private fun loadDummyLogs() {

        logsList.add(
            LogData(
                1,
                "Admin Login",
                "Admin logged in successfully",
                "Admin",
                "System",
                "09 May 2026",
                "Active"
            )
        )

        logsList.add(
            LogData(
                2,
                "Restaurant Added",
                "Burger King added by admin",
                "Admin",
                "Burger King",
                "09 May 2026",
                "Active"
            )
        )

        logsList.add(
            LogData(
                3,
                "Restaurant Updated",
                "Pizza Hut details updated",
                "Admin",
                "Pizza Hut",
                "08 May 2026",
                "Active"
            )
        )

        logsList.add(
            LogData(
                4,
                "Restaurant Deleted",
                "Dominos removed",
                "Admin",
                "Dominos",
                "07 May 2026",
                "Inactive"
            )
        )

        logsAdapter.notifyDataSetChanged()
    }

    override fun onViewClick(log: LogData) {
        // View click
    }

    override fun onEditClick(log: LogData) {
        // Edit click
    }

    override fun onDeleteClick(log: LogData) {
        // Delete click
    }
}