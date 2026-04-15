package com.example.cancri.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.example.cancri.MainActivity
import com.example.cancri.R
import com.example.cancri.SettingsActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NavbarFragment : Fragment(R.layout.navbar_fragment) {

    interface Listener {
        fun onBottomNavFabClicked()
    }

    enum class Tab {
        DASHBOARD,
        SETTINGS
    }

    private val selectedTab: Tab
        get() {
            val raw = arguments?.getString(ARG_SELECTED_TAB)
            val fromArgs = Tab.entries.firstOrNull { it.name == raw }
            if (fromArgs != null) return fromArgs
            return if (activity is SettingsActivity) Tab.SETTINGS else Tab.DASHBOARD
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dashboard = view.findViewById<View>(R.id.navDashboard)
        val settings = view.findViewById<View>(R.id.navSettings)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddTransaction)

        fab.visibility = if (selectedTab == Tab.DASHBOARD) View.VISIBLE else View.GONE
        applySelectedState(view, selectedTab)

        dashboard.setOnClickListener {
            if (selectedTab != Tab.DASHBOARD) {
                navigateTo(MainActivity::class.java)
            }
        }

        settings.setOnClickListener {
            if (selectedTab != Tab.SETTINGS) {
                navigateTo(SettingsActivity::class.java)
            }
        }

        fab.setOnClickListener {
            val listener = activity as? Listener
            if (listener != null) {
                listener.onBottomNavFabClicked()
            } else if (selectedTab != Tab.DASHBOARD) {
                navigateTo(MainActivity::class.java)
            }
        }
    }

    private fun navigateTo(target: Class<*>) {
        startActivity(Intent(requireContext(), target))
        activity?.finish()
    }

    private fun applySelectedState(root: View, tab: Tab) {
        val selectedColor = requireContext().getColor(R.color.green_primary)
        val unselectedColor = requireContext().getColor(R.color.text_tertiary)

        val dashboardIcon = root.findViewById<ImageView>(R.id.navDashboardIcon)
        val dashboardLabel = root.findViewById<TextView>(R.id.navDashboardLabel)
        val settingsIcon = root.findViewById<ImageView>(R.id.navSettingsIcon)
        val settingsLabel = root.findViewById<TextView>(R.id.navSettingsLabel)

        if (tab == Tab.DASHBOARD) {
            dashboardIcon.setColorFilter(selectedColor)
            dashboardLabel.setTextColor(selectedColor)
            dashboardLabel.setTypeface(null, android.graphics.Typeface.BOLD)
            settingsIcon.setColorFilter(unselectedColor)
            settingsLabel.setTextColor(unselectedColor)
            settingsLabel.setTypeface(null, android.graphics.Typeface.NORMAL)
        } else {
            settingsIcon.setColorFilter(selectedColor)
            settingsLabel.setTextColor(selectedColor)
            settingsLabel.setTypeface(null, android.graphics.Typeface.BOLD)
            dashboardIcon.setColorFilter(unselectedColor)
            dashboardLabel.setTextColor(unselectedColor)
            dashboardLabel.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    companion object {
        private const val ARG_SELECTED_TAB = "selected_tab"

        fun newInstance(selectedTab: Tab): NavbarFragment {
            return NavbarFragment().apply {
                arguments = bundleOf(ARG_SELECTED_TAB to selectedTab.name)
            }
        }
    }
}
