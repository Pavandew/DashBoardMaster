package com.restroone.app.manager_single_res_dash.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.restroone.app.R
import com.restroone.app.databinding.FragmentHelpSupportBinding
import com.restroone.app.manager_single_res_dash.settings.adapter.TermsClauseAdapter
import com.restroone.app.manager_single_res_dash.settings.adapter.TermsClauseModel

class HelpSupportFragment : Fragment() {

    private var _binding: FragmentHelpSupportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHelpSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupAppVersion()
        setupActionButtons()
        setupTermsRecyclerView()
        setupTabSwitching()
    }

    private fun setupAppVersion() {
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionName = pInfo.versionName ?: "1.0"
            binding.tvAppVersion.text = "Version $versionName"
        } catch (e: Exception) {
            binding.tvAppVersion.text = "Version 1.0"
        }
    }

    private fun setupToolbar() {
        binding.helpToolbar.tvToolbarTitle.text = getString(R.string.help_terms_title)
        binding.helpToolbar.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupActionButtons() {
        binding.btnCallSupport.setOnClickListener {
            val phone = binding.tvSupportPhone.text.toString().trim()
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.err_dialer), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEmailSupport.setOnClickListener {
            val email = binding.tvSupportEmail.text.toString().trim()
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$email")
                    putExtra(Intent.EXTRA_SUBJECT, "${getString(R.string.app_title_pos)} Support Query")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.err_email_client), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupTermsRecyclerView() {
        binding.rvTermsConditions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTermsConditions.adapter = TermsClauseAdapter(getTermsClauses())
    }

    private fun getTermsClauses(): List<TermsClauseModel> {
        return listOf(
            TermsClauseModel(
                id = 1,
                title = getString(R.string.terms_clause_1_title),
                content = getString(R.string.terms_clause_1_desc)
            ),
            TermsClauseModel(
                id = 2,
                title = getString(R.string.terms_clause_2_title),
                content = getString(R.string.terms_clause_2_desc)
            ),
            TermsClauseModel(
                id = 3,
                title = getString(R.string.terms_clause_3_title),
                content = getString(R.string.terms_clause_3_desc)
            ),
            TermsClauseModel(
                id = 4,
                title = getString(R.string.terms_clause_4_title),
                content = getString(R.string.terms_clause_4_desc)
            ),
            TermsClauseModel(
                id = 5,
                title = getString(R.string.terms_clause_5_title),
                content = getString(R.string.terms_clause_5_desc)
            ),
            TermsClauseModel(
                id = 6,
                title = getString(R.string.terms_clause_6_title),
                content = getString(R.string.terms_clause_6_desc)
            ),
            TermsClauseModel(
                id = 7,
                title = getString(R.string.terms_clause_7_title),
                content = getString(R.string.terms_clause_7_desc)
            )
        )
    }

    private fun setupTabSwitching() {
        val activeBg = ContextCompat.getColor(requireContext(), R.color.mid_app_color)
        val transparentBg = ContextCompat.getColor(requireContext(), android.R.color.transparent)
        val activeTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        val inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        fun selectTab(selectedTab: Int) {
            // Tab 1: Terms, Tab 2: Support, Tab 3: Privacy
            binding.btnTabTerms.setBackgroundColor(if (selectedTab == 1) activeBg else transparentBg)
            binding.btnTabTerms.setTextColor(if (selectedTab == 1) activeTextColor else inactiveTextColor)

            binding.btnTabSupport.setBackgroundColor(if (selectedTab == 2) activeBg else transparentBg)
            binding.btnTabSupport.setTextColor(if (selectedTab == 2) activeTextColor else inactiveTextColor)

            binding.btnTabPrivacy.setBackgroundColor(if (selectedTab == 3) activeBg else transparentBg)
            binding.btnTabPrivacy.setTextColor(if (selectedTab == 3) activeTextColor else inactiveTextColor)

            binding.layoutTermsConditions.visibility = if (selectedTab == 1) View.VISIBLE else View.GONE
            binding.layoutSupportInfo.visibility = if (selectedTab == 2) View.VISIBLE else View.GONE
            binding.layoutPrivacyPolicy.visibility = if (selectedTab == 3) View.VISIBLE else View.GONE
        }

        binding.btnTabTerms.setOnClickListener { selectTab(1) }
        binding.btnTabSupport.setOnClickListener { selectTab(2) }
        binding.btnTabPrivacy.setOnClickListener { selectTab(3) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
