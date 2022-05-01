package com.otk1fd.simplemio.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.Util
import com.otk1fd.simplemio.activities.PacketLogActivity
import com.otk1fd.simplemio.dialog.EditTextDialogFragment
import com.otk1fd.simplemio.dialog.EditTextDialogFragmentData
import com.otk1fd.simplemio.fragments.CouponFragment
import com.otk1fd.simplemio.ui.listview_item.CouponListItemChild
import com.otk1fd.simplemio.ui.listview_item.CouponListItemParent
import kotlinx.coroutines.launch


class CouponExpandableListAdapter(
    private val fragment: Fragment,
    private val parents: List<CouponListItemParent>,
    private val children: List<List<CouponListItemChild>>,
    val setCouponStatusFunc: (serviceCode: String, status: Boolean) -> Unit,
    val getCouponStatusFunc: (serviceCode: String) -> Boolean
) : BaseExpandableListAdapter() {

    private fun getBasicChildView(): View {
        return LayoutInflater.from(fragment.requireActivity())
            .inflate(R.layout.item_child_coupon, null)
    }

    private fun getBasicParentView(): View {
        return LayoutInflater.from(fragment.requireActivity())
            .inflate(R.layout.item_parent_coupon, null)
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val parentView: View = getBasicParentView()

        val volumeTextView: TextView = parentView.findViewById(R.id.volumeTextView)
        val hddServiceCodeTextView: TextView = parentView.findViewById(R.id.hddServiceCodeTextView)
        val planTextView: TextView = parentView.findViewById(R.id.planTextView)

        val couponListItemParent: CouponListItemParent = parents[groupPosition]

        hddServiceCodeTextView.text = couponListItemParent.hddServiceCode
        planTextView.text = couponListItemParent.plan
        volumeTextView.text = couponListItemParent.volume

        return parentView
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return children[groupPosition].size
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return children[groupPosition][childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val childView: View = getBasicChildView()

        val phoneNumberTextView: TextView = childView.findViewById(R.id.phoneNumberTextView)
        val serviceCodeTextView: TextView = childView.findViewById(R.id.serviceCodeTextView)
        val typeTextView: TextView = childView.findViewById(R.id.typeTextView)
        val couponSwitch: SwitchCompat = childView.findViewById(R.id.couponSwitch)
        val simNameTestView: TextView = childView.findViewById(R.id.simNameTextView)
        val packetLogButton: Button = childView.findViewById(R.id.packetLogButton)
        val editSimNameButton: Button = childView.findViewById(R.id.editSimNameButton)

        val couponListItemChild: CouponListItemChild = children[groupPosition][childPosition]

        couponSwitch.setOnCheckedChangeListener { _, isChecked ->
            setCouponStatusFunc(couponListItemChild.serviceCode, isChecked)
        }

        packetLogButton.setOnClickListener {
            val couponListItemParent: CouponListItemParent = parents[groupPosition]
            val hddServiceCode = couponListItemParent.hddServiceCode
            val serviceCode = couponListItemChild.serviceCode

            val intent = Intent(fragment.requireActivity(), PacketLogActivity::class.java)
            intent.putExtra("hddServiceCode", hddServiceCode)
            intent.putExtra("serviceCode", serviceCode)
            fragment.requireActivity().startActivity(intent)
        }

        editSimNameButton.setOnClickListener {
            val serviceCode = couponListItemChild.serviceCode
            val editTextDialogFragmentData = EditTextDialogFragmentData(
                title = "SIMの名前を入力してください",
                message = "",
                positiveButtonText = "完了",
                positiveButtonFunc = { fragmentActivity, text ->
                    val simnName = text.replace("\n", " ")
                    Util.saveSimName(fragmentActivity, serviceCode, simnName)
                    fragmentActivity.lifecycleScope.launch {
                        (fragment as CouponFragment).refreshCouponInfo()
                    }
                },
                negativeButtonText = "キャンセル",
                defaultText = Util.loadSimName(fragment.requireActivity(), serviceCode),
                hint = "SIMの名前"
            )

            EditTextDialogFragment.show(fragment.requireActivity(), editTextDialogFragmentData)
        }

        phoneNumberTextView.text = couponListItemChild.phoneNumber
        serviceCodeTextView.text = couponListItemChild.serviceCode
        typeTextView.text = couponListItemChild.type
        couponSwitch.isChecked = getCouponStatusFunc(couponListItemChild.serviceCode)

        val simName = Util.loadSimName(fragment.requireActivity(), couponListItemChild.serviceCode)
        if (simName != "") {
            simNameTestView.visibility = View.VISIBLE
            simNameTestView.text = simName
        } else {
            simNameTestView.visibility = View.GONE
        }

        return childView
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getGroupCount(): Int {
        return parents.size
    }

    override fun getGroup(groupPosition: Int): Any {
        return parents[groupPosition]
    }
}