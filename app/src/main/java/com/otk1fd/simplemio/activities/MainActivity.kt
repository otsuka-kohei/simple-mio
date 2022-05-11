package com.otk1fd.simplemio.activities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.otk1fd.simplemio.HttpErrorHandler
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.databinding.ActivityMainBinding
import com.otk1fd.simplemio.databinding.NavHeaderMainBinding
import com.otk1fd.simplemio.fragments.AboutFragment
import com.otk1fd.simplemio.fragments.ConfigFragment
import com.otk1fd.simplemio.fragments.CouponFragment
import com.otk1fd.simplemio.mio.Mio
import kotlinx.coroutines.launch


/**
 * ナビゲーションドロワーを利用してFragmentを切り替えて表示する，アプリの基本となるActivity．
 */
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    lateinit var mio: Mio
    lateinit var httpErrorHandler: HttpErrorHandler

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

        httpErrorHandler = HttpErrorHandler(this, mio)

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

        // 最新の通信量情報を先に取得しておく
        lifecycleScope.launch {
            val packetLogInfoResponseWithHttpResponseCode = mio.getUsageInfo()
            packetLogInfoResponseWithHttpResponseCode.packetLogInfoResponse?.let {
                mio.cacheJsonString(
                    Mio.parsePacketLogToJson(it),
                    getString(R.string.preference_key_cache_packet_log)
                )
            }
        }
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
        val phoneNumberTextView: TextView =
            NavHeaderMainBinding.bind(navigationHeader).phoneNumberTextView
        phoneNumberTextView.text = phoneNumber

        // 電話番号が空文字列（表示しないことも含めて）なら電話番号表示用TextViewを非表示にする．
        if (phoneNumber.isNotBlank()) {
            phoneNumberTextView.visibility = View.VISIBLE
        } else {
            phoneNumberTextView.visibility = View.GONE
        }
    }
}
