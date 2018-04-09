package com.otsuka.simplemio

import android.app.Fragment
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import com.otsuka.simplemio.Util.Companion.showAlertDialog
import com.otsuka.simplemio.fragments.AboutFragment
import com.otsuka.simplemio.fragments.ConfigFragment
import com.otsuka.simplemio.fragments.TestFragment
import com.otsuka.simplemio.mio.MioManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val mioManager: MioManager = MioManager(this, { startOAuth() })

    private val configFragment: ConfigFragment = ConfigFragment()
    private val testFragment: TestFragment = TestFragment()
    private val aboutFragment: AboutFragment = AboutFragment()

    private lateinit var navigationView: NavigationView

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

        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = testFragmentName

        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment, testFragment)
        fragmentTransaction.commit()

        // トークンが存在するか，有効かを確認
        val token = mioManager.loadToken()

        if (token == "") {
            showAlertDialog(this, "ログイン", "IIJmioでのログインが必要です\nブラウザを開いてログインページに移動してもよろしいですか？",
                    "はい", negativeButtonText = "いいえ",
                    positiveFunc = { Log.d("positive", "positive"); startOAuth() }, negativeFunc = { this.finish() })
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
        var itemIndex = 0

        when (item.itemId) {
            R.id.nav_test -> {
                fragment = testFragment
                fragmentName = testFragmentName
                itemIndex = 0
            }
            R.id.nav_config -> {
                fragment = configFragment
                fragmentName = configFragmentName
                itemIndex = 1
            }
            R.id.nav_about -> {
                fragment = aboutFragment
                fragmentName = aboutFragmentName
                itemIndex = 2
            }
        }

        val bundle = Bundle()
        bundle.putSerializable("MioManager", mioManager)
        fragment?.arguments = bundle

        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment, fragment)
        fragmentTransaction.commit()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = fragmentName

        navigationView.getMenu().getItem(itemIndex).isChecked = true

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun startOAuth() {

        val uri = "https://api.iijmio.jp/mobile/d/v1/authorization/?" +
                "response_type=token" +
                "&client_id=" + getString(R.string.developer_id) +
                "&redirect_uri=" + getString(R.string.simple_app_name) + "%3A%2F%2Fcallback" +
                "&state=" + "success"

        Log.d("request URI : ", uri)

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

                // 受け取るURIが
                // simplemio://callback#access_token=token&state=success&token_type=Bearer&expires_in=7776000
                // となっていて，正しくエンコードできないので # を ? に置き換える

                var uriStr = uri.toString()
                uriStr = uriStr.replace('#', '?')
                val validUri = Uri.parse(uriStr)

                val token = validUri.getQueryParameter("access_token")
                val state = validUri.getQueryParameter("state")

                if (state != "success") {
                    Toast.makeText(this, "正しく認証することができませんでした．", Toast.LENGTH_LONG).show()
                } else {
                    mioManager.saveToken(token)
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
