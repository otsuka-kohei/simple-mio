package com.otk1fd.simplemio.activities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.otk1fd.simplemio.HttpErrorHandler
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.databinding.ActivityMainBinding
import com.otk1fd.simplemio.dialog.AlertDialogFragment
import com.otk1fd.simplemio.dialog.AlertDialogFragmentData
import com.otk1fd.simplemio.fragments.AboutFragment
import com.otk1fd.simplemio.fragments.ConfigFragment
import com.otk1fd.simplemio.fragments.CouponFragment
import com.otk1fd.simplemio.mio.Mio
import kotlinx.coroutines.launch


/**
 * ナビゲーションドロワーを利用してFragmentを切り替えて表示する，アプリの基本となるActivity．
 */
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var mio: Mio

    private lateinit var binding: ActivityMainBinding

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView
        toolbar = binding.appBarMain.toolbar

        mio = Mio(this)

        HttpErrorHandler.setUp(
            loginFunc = { loginWithDialog() },
            showErrorMessageFunc = { errorMessage ->
                val alertDialogFragmentData = AlertDialogFragmentData(
                    message = errorMessage,
                    positiveButtonText = "OK"
                )
                AlertDialogFragment.show(
                    this@MainActivity,
                    alertDialogFragmentData
                )
            })

        setSupportActionBar(toolbar)

        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
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
    private fun loginWithDialog() {
        Log.d("hoge", "start login")
        val alertDialogFragmentDataForLogin = AlertDialogFragmentData(
            title = "ログイン",
            message = "IIJmioでのログインが必要です。\nログイン画面に移動してもよろしいですか？",
            positiveButtonText = "はい",
            positiveButtonFunc = { fragmentActivity: FragmentActivity ->
                fragmentActivity.lifecycleScope.launch {
                    val result: Boolean = (fragmentActivity as MainActivity).mio.login()
                    if (!result) {
                        val alertDialogFragmentDataForLoginErrorMessage = AlertDialogFragmentData(
                            message = "ログインに失敗しました",
                            positiveButtonText = "OK",
                            positiveButtonFunc = {
                                (it as MainActivity).loginWithDialog()
                            }
                        )
                        AlertDialogFragment.show(
                            this@MainActivity,
                            alertDialogFragmentDataForLoginErrorMessage
                        )
                    }
                }
            },
            negativeButtonText = "いいえ",
            negativeButtonFunc = { fragmentActivity: FragmentActivity -> (fragmentActivity as MainActivity).finish() })

        AlertDialogFragment.show(this, alertDialogFragmentDataForLogin)
    }
}
