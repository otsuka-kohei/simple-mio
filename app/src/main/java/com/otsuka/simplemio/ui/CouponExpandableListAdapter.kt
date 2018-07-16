package com.otsuka.simplemio.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.Switch
import android.widget.TextView
import com.otsuka.simplemio.R
import com.otsuka.simplemio.ui.listview_item.CouponListItemChild
import com.otsuka.simplemio.ui.listview_item.CouponListItemParent


class CouponExpandableListAdapter(val context: Context) : BaseExpandableListAdapter() {

    private lateinit var children: List<List<CouponListItemChild>>
    private lateinit var parents: List<CouponListItemParent>

    fun addParents(parent: List<CouponListItemParent>): Unit {
        this.parents = parents
    }

    fun addChildren(children: List<List<CouponListItemChild>>): Unit {
        this.children = children
    }

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
        val hddServiceCodeTextView: TextView = parentView.findViewById(R.id.couponHddServiceCodeTextView)
        val planTextView: TextView = parentView.findViewById(R.id.couponPlanTextView)

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
        return p0 as Long
    }

    override fun getChildView(p0: Int, p1: Int, p2: Boolean, p3: View?, p4: ViewGroup?): View {
        val childView: View = getBasicChildView()

        val phoneNumberTextView: TextView = childView.findViewById(R.id.couponPhoneNumberTextView)
        val serviceCodeTextView: TextView = childView.findViewById(R.id.couponServiceCodeTextView)
        val typeTextView: TextView = childView.findViewById(R.id.couponTypeTextView)
        val couponSwitch: Switch = childView.findViewById(R.id.couponSwitch)

        val couponListItemChild: CouponListItemChild = children[p0][p1]

        phoneNumberTextView.text = couponListItemChild.phoneNumber
        serviceCodeTextView.text = couponListItemChild.serviceCode
        typeTextView.text = couponListItemChild.type
        couponSwitch.isChecked = couponListItemChild.couponUse

        return childView
    }

    override fun getChildId(p0: Int, p1: Int): Long {
        return p1 as Long
    }

    override fun getGroupCount(): Int {
        return parents.size
    }

    override fun getGroup(p0: Int): Any {
        return parents[p0]
    }
}