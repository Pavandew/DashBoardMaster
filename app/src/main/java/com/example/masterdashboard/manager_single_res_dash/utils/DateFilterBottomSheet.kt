package com.example.masterdashboard.manager_single_res_dash.utils

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.masterdashboard.databinding.BottomSheetDateFilterBinding
import com.example.masterdashboard.manager_single_res_dash.repo.ReportsRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DateFilterBottomSheet(
    private val activeFilter: ReportsRepository.TimeFilter,
    private val onFilterSelected: (filter: ReportsRepository.TimeFilter, isCustom: Boolean) -> Unit
) : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "DateFilterBottomSheet"
    }

    private var _binding: BottomSheetDateFilterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDateFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "DateFilterBottomSheet opened with activeFilter: $activeFilter")

        highlightActiveFilter()

        binding.optionToday.setOnClickListener {
            Log.d(TAG, "Option Selected: Today")
            onFilterSelected(ReportsRepository.TimeFilter.TODAY, false)
            dismiss()
        }

        binding.optionWeek.setOnClickListener {
            Log.d(TAG, "Option Selected: This Week")
            onFilterSelected(ReportsRepository.TimeFilter.WEEK, false)
            dismiss()
        }

        binding.optionMonth.setOnClickListener {
            Log.d(TAG, "Option Selected: This Month")
            onFilterSelected(ReportsRepository.TimeFilter.MONTH, false)
            dismiss()
        }

        binding.optionCustom.setOnClickListener {
            Log.d(TAG, "Option Selected: Custom Date Range")
            onFilterSelected(ReportsRepository.TimeFilter.TODAY, true)
            dismiss()
        }
    }

    private fun highlightActiveFilter() {
        binding.checkToday.visibility = View.GONE
        binding.checkWeek.visibility = View.GONE
        binding.checkMonth.visibility = View.GONE
        binding.checkCustom.visibility = View.GONE

        when (activeFilter) {
            ReportsRepository.TimeFilter.TODAY -> binding.checkToday.visibility = View.VISIBLE
            ReportsRepository.TimeFilter.WEEK -> binding.checkWeek.visibility = View.VISIBLE
            ReportsRepository.TimeFilter.MONTH -> binding.checkMonth.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
