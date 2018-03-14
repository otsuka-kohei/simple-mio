package com.otsuka.simplemio

import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.widget.Toast
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

        // トークンが存在するか，有効かを確認
        val token = MioUtil.loadToken(this)

        if (token == "") {
            startOAuth()
        }

        if (!MioUtil.checkTokenAvailable(token)) {

        }
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

    private fun startOAuth() {

        val uri = "https://api.iijmio.jp/mobile/d/v1/authorization/?" +
                "client_id=" + getString(R.string.developerID) +
                "&redirect_uri=" + getString(R.string.app_name) + "://callback" +
                "&state=" + "success"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(intent)
    }

    public override fun onResume() {
        super.onResume()

        val intent = intent
        val action = intent.action

        if (action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            if (uri != null) {

                val token = uri.getQueryParameter("access_token")
                val state = uri.getQueryParameter("state")

                if (state != "success") {
                    Toast.makeText(this, "正しく認証することができませんでした．", Toast.LENGTH_LONG).show()
                } else {
                    MioUtil.saveToken(this, token)
                    Toast.makeText(this, "トークン:" + token, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
