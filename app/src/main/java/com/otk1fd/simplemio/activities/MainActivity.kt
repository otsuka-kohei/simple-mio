package com.otk1fd.simplemio.activities

import android.Manifest
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.telephony.TelephonyManager
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import com.otk1fd.simplemio.HttpErrorHandler
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.Util
import com.otk1fd.simplemio.Util.showAlertDialog
import com.otk1fd.simplemio.fragments.AboutFragment
import com.otk1fd.simplemio.fragments.ConfigFragment
import com.otk1fd.simplemio.fragments.CouponFragment
import com.otk1fd.simplemio.fragments.PacketLogFragment
import com.otk1fd.simplemio.mio.MioUtil
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MioUtil.setUp(this)
        HttpErrorHandler.setUp(loginFunc = { startOAuthWithDialog() }, showErrorMessageFunc = { errorMessage -> Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show() })

        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val actionBarDrawerToggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close)
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // デフォルトのFragmentを設定
        val item = navigationView.menu.findItem(R.id.nav_coupon)
        onNavigationItemSelected(item)
        item.isChecked = true
    }

    override fun onStart() {
        super.onStart()

        updatePhoneNumberOnNavigationHeader()
    }

    override fun onResume() {
        super.onResume()

        val intent = intent
        val action = intent.action

        if (action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data

            // 外部ブラウザでのIIJmioログインから戻ってきたとき
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
                startOAuthWithDialog()
            }
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            // ナビゲーションドロワーが開いていれば閉じる
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.

        var fragment: Fragment? = null
        var fragmentName = ""
        val fragmentTransaction = fragmentManager.beginTransaction()

        // タップしたメニューアイテムのIDによって，表示するFragmentとToolbarに表示するタイトル名を決定する
        when (item.itemId) {
            R.id.nav_coupon -> {
                fragmentName = getString(R.string.menu_coupon)
                fragment = fragmentManager.findFragmentByTag(fragmentName)
                if (fragment == null) {
                    fragment = CouponFragment()
                }
            }
            R.id.nav_history -> {
                fragmentName = getString(R.string.menu_packet_log)
                fragment = fragmentManager.findFragmentByTag(fragmentName)
                if (fragment == null) {
                    fragment = PacketLogFragment()
                }
            }
            R.id.nav_config -> {
                fragmentName = getString(R.string.menu_config)
                fragment = fragmentManager.findFragmentByTag(fragmentName)
                if (fragment == null) {
                    fragment = ConfigFragment()
                }
            }
            R.id.nav_about -> {
                fragmentName = getString(R.string.menu_about)
                fragment = fragmentManager.findFragmentByTag(fragmentName)
                if (fragment == null) {
                    fragment = AboutFragment()
                }
            }
        }


        // Fragmentをセット
        fragmentTransaction.replace(R.id.fragmentLayout, fragment, fragmentName)
        fragmentTransaction.commit()

        // Toolbarにタイトルをセット
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = fragmentName

        // ドロワーを閉じる
        drawer_layout.closeDrawer(GravityCompat.START)

        return true
    }

    /**
     * ナビゲーションドロワーのヘッダの電話番号表示ステータスを更新する．
     * 電話番号を表示するかの設定や，端末の電話番号の有無によって，電話番号を表示するかどうかを決める．
     *
     * @param usePreference Preferenceに保存してある設定内容に従うかどうか
     * @param showPhoneNumberParameter 任意で電話番号を表示するか決める
     */
    fun updatePhoneNumberOnNavigationHeader(usePreference: Boolean = true, showPhoneNumberParameter: Boolean = false) {
        // 電話番号を表示するかのフラグ
        val showPhoneNumber = if (usePreference) {
            getSharedPreferences(getString(R.string.preference_file_name), Context.MODE_PRIVATE).getBoolean(getString(R.string.preference_key_show_phone_number), false)
        } else {
            showPhoneNumberParameter
        }

        // 電話番号表示用のTextViewを取得
        val navigationHeader = navigationView.getHeaderView(0)
        val phoneNumberTextView: TextView = navigationHeader.findViewById(R.id.phoneNumberTextView)
        var phoneNumber = ""

        // 電話番号の取得がパーミッションで許可されているか
        val canShowPhoneNumberByPermiiison = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

        if (showPhoneNumber && canShowPhoneNumberByPermiiison) {
            // 電話番号を取得
            val telephonyManager: TelephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            phoneNumber = telephonyManager.line1Number.orEmpty()
        } else {
            // パーミッションで許可されていないので，Preferenceを電話番号を表示しない設定に書き換える．
            getSharedPreferences(getString(R.string.preference_file_name), Context.MODE_PRIVATE).edit { putBoolean(getString(R.string.preference_key_show_phone_number), false) }
        }

        phoneNumberTextView.text = phoneNumber

        // 電話番号が空文字列（表示しないことも含めて）なら電話番号表示用TextViewを非表示にする．
        if (phoneNumber.isNotBlank()) {
            phoneNumberTextView.visibility = View.VISIBLE
        } else {
            phoneNumberTextView.visibility = View.GONE
        }
    }

    /**
     * 確認ダイアログを表示して，IIJmioにログインする．
     */
    private fun startOAuthWithDialog() {
        showAlertDialog(this, "ログイン", "IIJmioでのログインが必要です\nブラウザを開いてログインページに移動してもよろしいですか？",
                "はい", negativeButtonText = "いいえ",
                positiveFunc = { startOAuth() }, negativeFunc = { this.finish() })
    }

    /**
     * 外部ブラウザを起動してIIJmioにログインする．
     */
    private fun startOAuth() {

        val uri = "https://api.iijmio.jp/mobile/d/v1/authorization/?" +
                "response_type=token" +
                "&client_id=" + Util.getDeveloperId(this) +
                "&redirect_uri=" + getString(R.string.simple_app_name) + "%3A%2F%2Fcallback" +
                "&state=" + "success"

        // リクエスト用URIを渡して，外部ブラウザのActivityを起動する．
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(intent)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
