package com.otk1fd.simplemio.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.Util
import com.otk1fd.simplemio.ui.listview_item.PacketLogListItemChild
import com.otk1fd.simplemio.ui.listview_item.PacketLogListItemParent


class PacketLogExpandableListAdapter(val activity: Activity, val parents: List<PacketLogListItemParent>, val children: List<List<PacketLogListItemChild>>) : BaseExpandableListAdapter() {

    private fun getBasicChildView(): View {
        return LayoutInflater.from(activity).inflate(R.layout.item_child_packet_log, null)
    }

    private fun getBasicParentView(): View {
        return LayoutInflater.from(activity).inflate(R.layout.item_parent_packet_log, null)
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean {
        return true
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(p0: Int, p1: Boolean, p2: View?, p3: ViewGroup?): View {
        val parentView: View = getBasicParentView()

        val hddServiceCodeTextView: TextView = parentView.findViewById(R.id.hddServiceCodeTextView)
        val planTextView: TextView = parentView.findViewById(R.id.planTextView)

        val packetLogListItemParent: PacketLogListItemParent = parents[p0]

        hddServiceCodeTextView.text = packetLogListItemParent.hddServiceCode
        planTextView.text = packetLogListItemParent.plan

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

        val phoneNumberTextView: TextView = childView.findViewById(R.id.phoneNumberTextView)
        val serviceCodeTextView: TextView = childView.findViewById(R.id.serviceCodeTextView)
        val typeTextView: TextView = childView.findViewById(R.id.typeTextView)
        val simNameTestView: TextView = childView.findViewById(R.id.simNameTextView)

        val packetLogListItemChild: PacketLogListItemChild = children[p0][p1]


        phoneNumberTextView.text = packetLogListItemChild.phoneNumber
        serviceCodeTextView.text = packetLogListItemChild.serviceCode
        typeTextView.text = packetLogListItemChild.type

        val simmName = Util.loadSimName(activity, packetLogListItemChild.serviceCode)
        if (simmName != "") {
            simNameTestView.visibility = View.VISIBLE
            simNameTestView.text = simmName
        } else {
            simNameTestView.visibility = View.GONE
        }

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