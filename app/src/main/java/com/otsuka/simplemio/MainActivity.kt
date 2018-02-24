package com.otsuka.simplemio

import android.app.Fragment
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.otsuka.simplemio.fragments.AboutFragment
import com.otsuka.simplemio.fragments.ConfigFragment
import com.otsuka.simplemio.fragments.TestFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val configFragment: ConfigFragment = ConfigFragment()
    private val testFragment: TestFragment = TestFragment()
    private val aboutFragment: AboutFragment = AboutFragment()

    private val testFragmentName = "テスト"
    private val configFragmentName = "設定"
    private val aboutFragmentName = "このアプリについて"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = testFragmentName

        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment, testFragment)
        fragmentTransaction.commit()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.

        var fragment: Fragment? = null
        var fragmentName = ""

        when (item.itemId) {
            R.id.nav_test -> {
                fragment = testFragment
                fragmentName = testFragmentName
            }
            R.id.nav_config -> {
                fragment = configFragment
                fragmentName = configFragmentName
            }
            R.id.nav_about -> {
                fragment = aboutFragment
                fragmentName = aboutFragmentName
            }
        }

        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment, fragment)
        fragmentTransaction.commit()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = fragmentName

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
