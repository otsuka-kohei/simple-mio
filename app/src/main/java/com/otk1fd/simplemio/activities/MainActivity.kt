package com.otk1fd.simplemio.activities

import android.app.Fragment
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import com.otk1fd.simplemio.HttpErrorHandler
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.Util
import com.otk1fd.simplemio.Util.Companion.showAlertDialog
import com.otk1fd.simplemio.fragments.AboutFragment
import com.otk1fd.simplemio.fragments.ConfigFragment
import com.otk1fd.simplemio.fragments.CouponFragment
import com.otk1fd.simplemio.fragments.PacketLogFragment
import com.otk1fd.simplemio.mio.MioUtil
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val couponFragment: CouponFragment = CouponFragment()
    private val packetLogFragment: PacketLogFragment = PacketLogFragment()
    private val configFragment: ConfigFragment = ConfigFragment()
    private val aboutFragment: AboutFragment = AboutFragment()

    private lateinit var navigationView: NavigationView


    override fun onCreate(savedInstanceState: Bundle?) {
        MioUtil.setUp(this)
        HttpErrorHandler.setUp(loginFunc = { startOAuthWithDialog() }, showErrorMessageFunc = { errorMessage -> Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show() })

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.packetLogToolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val actionBarDrawerToggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close)
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.menu.getItem(0).isChecked = true

        couponFragment.startOAuthWithDialog = { startOAuthWithDialog() }
    }

    override fun onStart() {
        super.onStart()

        supportActionBar?.title = getString(R.string.menu_coupon)

        val defaultFragment = couponFragment
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment, defaultFragment)
        fragmentTransaction.commit()
    }

    public override fun onResume() {
        super.onResume()

        val intent = intent
        val action = intent.action

        if (action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            if (uri != null && uri.toString().contains("simplemio")) {

                // 受け取るURIが
                // simplemio://callback#access_token=token&state=success&token_type=Bearer&expires_in=7776000
                // となっていて，正しくエンコードできないので # を ? に置き換える

                var uriStr = uri.toString()
                uriStr = uriStr.replace('#', '?')
                val validUri = Uri.parse(uriStr)

                val token = validUri.getQueryParameter("access_token")
                val state = validUri.getQueryParameter("state")

                if (state != "success") {
                    Toast.makeText(this, "正しく認証することができませんでした。", Toast.LENGTH_LONG).show()
                } else {
                    MioUtil.saveToken(this, token)
                }
            }
        } else {
            // トークンが存在しない場合はログインする
            if (MioUtil.loadToken(this) == "") {
                Log.d("resume without login", "please login")
                startOAuthWithDialog()
            }
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
            R.id.nav_coupon -> {
                fragment = couponFragment
                fragmentName = getString(R.string.menu_coupon)
            }
            R.id.nav_history -> {
                fragment = packetLogFragment
                fragmentName = getString(R.string.menu_history)
            }
            R.id.nav_config -> {
                fragment = configFragment
                fragmentName = getString(R.string.menu_config)
            }
            R.id.nav_about -> {
                fragment = aboutFragment
                fragmentName = getString(R.string.menu_about)
            }
        }

        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment, fragment)
        fragmentTransaction.commit()

        val toolbar: Toolbar = findViewById(R.id.packetLogToolbar)
        toolbar.title = fragmentName

        navigationView.isEnabled = true

        item.isChecked = true

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun startOAuthWithDialog() {
        showAlertDialog(this, "ログイン", "IIJmioでのログインが必要です\nブラウザを開いてログインページに移動してもよろしいですか？",
                "はい", negativeButtonText = "いいえ",
                positiveFunc = { startOAuth() }, negativeFunc = { this.finish() })
    }

    private fun startOAuth() {

        val uri = "https://api.iijmio.jp/mobile/d/v1/authorization/?" +
                "response_type=token" +
                "&client_id=" + Util.getDeveloperId(this) +
                "&redirect_uri=" + getString(R.string.simple_app_name) + "%3A%2F%2Fcallback" +
                "&state=" + "success"

        Log.d("request URI : ", uri)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(intent)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}