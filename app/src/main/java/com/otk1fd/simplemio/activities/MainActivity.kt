package com.otk1fd.simplemio.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.navigation.NavigationView
import com.otk1fd.simplemio.HttpErrorHandler
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.dialog.AlertDialogFragment
import com.otk1fd.simplemio.dialog.AlertDialogFragmentData
import com.otk1fd.simplemio.fragments.AboutFragment
import com.otk1fd.simplemio.fragments.ConfigFragment
import com.otk1fd.simplemio.fragments.CouponFragment
import com.otk1fd.simplemio.mio.MioUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*


/**
 * ナビゲーションドロワーを利用してFragmentを切り替えて表示する，アプリの基本となるActivity．
 */
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MioUtil.setUp(this)
        HttpErrorHandler.setUp(
            loginFunc = { startOAuthWithDialog() },
            showErrorMessageFunc = { errorMessage ->
                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            })

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close
        )
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

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

                val token: String = validUri.getQueryParameter("access_token") ?: ""
                val state: String = validUri.getQueryParameter("state") ?: ""

                if (state != "success") {
                    Toast.makeText(this, "正しく認証することができませんでした。", Toast.LENGTH_LONG).show()
                } else {
                    MioUtil.saveToken(this, token)
                }
            }

        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            // ナビゲーションドロワーが開いていれば閉じる
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.

        var fragment: Fragment? = null
        var fragmentName = ""
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        // タップしたメニューアイテムのIDによって，表示するFragmentとToolbarに表示するタイトル名を決定する
        when (item.itemId) {
            R.id.nav_coupon -> {
                fragmentName = getString(R.string.menu_coupon)
                fragment = supportFragmentManager.findFragmentByTag(fragmentName)
                if (fragment == null) {
                    fragment = CouponFragment()
                }
            }
            R.id.nav_config -> {
                fragmentName = getString(R.string.menu_config)
                fragment = supportFragmentManager.findFragmentByTag(fragmentName)
                if (fragment == null) {
                    fragment = ConfigFragment()
                }
            }
            R.id.nav_about -> {
                fragmentName = getString(R.string.menu_about)
                fragment = supportFragmentManager.findFragmentByTag(fragmentName)
                if (fragment == null) {
                    fragment = AboutFragment()
                }
            }
        }


        // Fragmentをセット
        fragment?.let {
            fragmentTransaction.replace(R.id.fragmentLayout, fragment, fragmentName)
            fragmentTransaction.commit()
        }

        // Toolbarにタイトルをセット
        supportActionBar?.title = fragmentName

        // ドロワーを閉じる
        drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    /**
     * ナビゲーションドロワーのヘッダの電話番号表示ステータスを更新する．
     * 電話番号を表示するかの設定や，端末の電話番号の有無によって，電話番号を表示するかどうかを決める．
     *
     * @param usePreference Preferenceに保存してある設定内容に従うかどうか
     * @param showPhoneNumberParameter 任意で電話番号を表示するか決める
     */
    fun updatePhoneNumberOnNavigationHeader(
        usePreference: Boolean = true,
        showPhoneNumberParameter: Boolean = false
    ) {
        // 電話番号を表示するかのフラグ
        val showPhoneNumber = if (usePreference) {
            getSharedPreferences(
                getString(R.string.preference_file_name),
                Context.MODE_PRIVATE
            ).getBoolean(getString(R.string.preference_key_show_phone_number), false)
        } else {
            showPhoneNumberParameter
        }

        // 電話番号表示用のTextViewを取得
        var phoneNumber = ""

        // 電話番号の取得がパーミッションで許可されているか
        val canShowPhoneNumberByPermiiison = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (showPhoneNumber && canShowPhoneNumberByPermiiison) {
            // 電話番号を取得
            val telephonyManager: TelephonyManager =
                getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            phoneNumber = telephonyManager.line1Number.orEmpty()
        } else {
            // パーミッションで許可されていないので，Preferenceを電話番号を表示しない設定に書き換える．
            getSharedPreferences(
                getString(R.string.preference_file_name),
                Context.MODE_PRIVATE
            ).edit { putBoolean(getString(R.string.preference_key_show_phone_number), false) }
        }

        val navigationHeader = navigationView.getHeaderView(0)
        val phoneNumberTextView: TextView = navigationHeader.findViewById(R.id.phoneNumberTextView)
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
        val alertDialogFragmentData = AlertDialogFragmentData(
            "ログイン",
            "IIJmioでのログインが必要です\nブラウザを開いてログインページに移動してもよろしいですか？",
            positiveButtonText = "はい",
            positiveButtonFunc = { fragmentActivity: FragmentActivity -> (fragmentActivity as MainActivity).startOAuth() },
            negativeButtonText = "いいえ",
            negativeButtonFunc = { fragmentActivity: FragmentActivity -> (fragmentActivity as MainActivity).finish() })

        AlertDialogFragment.show(this, alertDialogFragmentData)
    }

    /**
     * 外部ブラウザを起動してIIJmioにログインする．
     */
    private fun startOAuth() {

        val uri = "https://api.iijmio.jp/mobile/d/v1/authorization/?" +
                "response_type=token" +
                "&client_id=" + getString(R.string.developer_id) +
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
