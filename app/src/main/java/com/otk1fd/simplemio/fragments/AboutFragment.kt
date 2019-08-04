package com.otk1fd.simplemio.fragments


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.activities.OpenSourceActivity


/**
 * Created by otk1fd on 2018/02/24.
 */
class AboutFragment : Fragment() {

    private lateinit var logoutButton: Button
    private lateinit var aboutTextView: TextView
    private lateinit var openSourceTextView: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        Log.d("About", "before return")
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        aboutTextView = activity!!.findViewById(R.id.aboutTextView)
        aboutTextView.text = activity!!.getString(R.string.about)

        openSourceTextView = activity!!.findViewById(R.id.openSourceTitleTextView)
        openSourceTextView.setOnClickListener {
            val intent = Intent(activity!!, OpenSourceActivity::class.java)
            activity!!.startActivity(intent)
        }

    }
}