package com.otk1fd.simplemio.fragments


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.activities.OpenSourceActivity
import com.otk1fd.simplemio.databinding.FragmentAboutBinding


/**
 * Created by otk1fd on 2018/02/24.
 */
class AboutFragment : Fragment() {

    private lateinit var binding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.aboutTextView.text = requireActivity().getString(R.string.about)

        binding.openSourceTitleTextView.setOnClickListener {
            val intent = Intent(requireActivity(), OpenSourceActivity::class.java)
            requireActivity().startActivity(intent)
        }

    }
}