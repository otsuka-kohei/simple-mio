package com.otk1fd.simplemio.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.Switch
import android.widget.TextView
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.ui.listview_item.CouponListItemChild
import com.otk1fd.simplemio.ui.listview_item.CouponListItemParent


class CouponExpandableListAdapter(val context: Context, val parents: List<CouponListItemParent>, val children: List<List<CouponListItemChild>>, val setCouponStatus: (serviceCode: String, status: Boolean) -> Unit, val getCouponStatus: (serviceCode: String) -> Boolean) : BaseExpandableListAdapter() {

    private fun getBasicChildView(): View {
        return LayoutInflater.from(context).inflate(R.layout.item_child_coupon, null)
    }

    private fun getBasicParentView(): View {
        return LayoutInflater.from(context).inflate(R.layout.item_parent_coupon, null)
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean {
        return true
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(p0: Int, p1: Boolean, p2: View?, p3: ViewGroup?): View {
        val parentView: View = getBasicParentView()

        val volumeTextView: TextView = parentView.findViewById(R.id.couponVolumeTextView)
        val hddServiceCodeTextView: TextView = parentView.findViewById(R.id.historyHddServiceCodeTextView)
        val planTextView: TextView = parentView.findViewById(R.id.historyPlanTextView)

        val couponListItemParent: CouponListItemParent = parents[p0]

        hddServiceCodeTextView.text = couponListItemParent.hddServiceCode
        planTextView.text = couponListItemParent.plan
        volumeTextView.text = couponListItemParent.volume

        return parentView
    }

    override fun getChildrenCount(p0: Int): Int {
        return children[p0].size
    }

    override fun getChild(p0: Int, p1: Int): Any {
        return children[p0][p1]
    }

    override fun getGroupId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getChildView(p0: Int, p1: Int, p2: Boolean, p3: View?, p4: ViewGroup?): View {
        val childView: View = getBasicChildView()

        val phoneNumberTextView: TextView = childView.findViewById(R.id.couponPhoneNumberTextView)
        val serviceCodeTextView: TextView = childView.findViewById(R.id.couponServiceCodeTextView)
        val typeTextView: TextView = childView.findViewById(R.id.couponTypeTextView)
        val couponSwitch: Switch = childView.findViewById(R.id.couponSwitch)

        val couponListItemChild: CouponListItemChild = children[p0][p1]

        couponSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            setCouponStatus(couponListItemChild.serviceCode, isChecked)
        }


        phoneNumberTextView.text = couponListItemChild.phoneNumber
        serviceCodeTextView.text = couponListItemChild.serviceCode
        typeTextView.text = couponListItemChild.type
        couponSwitch.isChecked = getCouponStatus(couponListItemChild.serviceCode)

        return childView
    }

    override fun getChildId(p0: Int, p1: Int): Long {
        return p1.toLong()
    }

    override fun getGroupCount(): Int {
        return parents.size
    }

    override fun getGroup(p0: Int): Any {
        return parents[p0]
    }
}