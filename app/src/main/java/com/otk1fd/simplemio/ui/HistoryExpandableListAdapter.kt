package com.otk1fd.simplemio.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.ui.listview_item.HistoryListItemChild
import com.otk1fd.simplemio.ui.listview_item.HistoryListItemParent


class HistoryExpandableListAdapter(val context: Context, val parents: List<HistoryListItemParent>, val children: List<List<HistoryListItemChild>>) : BaseExpandableListAdapter() {

    private fun getBasicChildView(): View {
        return LayoutInflater.from(context).inflate(R.layout.item_child_history, null)
    }

    private fun getBasicParentView(): View {
        return LayoutInflater.from(context).inflate(R.layout.item_parent_history, null)
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean {
        return true
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(p0: Int, p1: Boolean, p2: View?, p3: ViewGroup?): View {
        val parentView: View = getBasicParentView()

        val hddServiceCodeTextView: TextView = parentView.findViewById(R.id.historyHddServiceCodeTextView)
        val planTextView: TextView = parentView.findViewById(R.id.historyPlanTextView)

        val historyListItemParent: HistoryListItemParent = parents[p0]

        hddServiceCodeTextView.text = historyListItemParent.hddServiceCode
        planTextView.text = historyListItemParent.plan

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

        val historyListItemChild: HistoryListItemChild = children[p0][p1]


        phoneNumberTextView.text = historyListItemChild.phoneNumber
        serviceCodeTextView.text = historyListItemChild.serviceCode
        typeTextView.text = historyListItemChild.type

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