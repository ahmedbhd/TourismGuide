package com.mobelite.tourismguide.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.mobelite.tourismguide.R
import com.mobelite.tourismguide.data.webservice.Model

class ListCommentAdapter(var data: MutableList<Model.Review>, var context: Context) : BaseAdapter() {


    private class ViewHolder(row: View?) {
        var txtdate: TextView? = null
        var txtComment: TextView? = null

        init {
            this.txtdate = row?.findViewById(R.id.datecmnt)
            this.txtComment = row?.findViewById(R.id.cmntcontent)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View?
        val viewHolder: ViewHolder
        if (convertView==null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.listcmnt_item, null)
            viewHolder = ViewHolder(view)
            view?.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val userDto = data[position]
        viewHolder.txtdate?.text = userDto.date.toString()
        viewHolder.txtComment?.text = userDto.comment

        return view as View
    }

    override fun getItem(i: Int): Model.Review {
        return data[i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getCount(): Int {
        return data.size
    }
}
