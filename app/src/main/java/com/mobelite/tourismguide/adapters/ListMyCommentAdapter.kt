package com.mobelite.tourismguide.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.mobelite.tourismguide.R
import com.mobelite.tourismguide.data.webservice.Model
import com.mobelite.tourismguide.data.webservice.RestaurantServices
import com.mobelite.tourismguide.tools.PhoneGrantings
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.text.DateFormat

class ListMyCommentAdapter(var data: MutableList<Model.Review>, var context: Context) : BaseAdapter() {

    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null
    var cmntext: EditText? = null

    private class ViewHolder(row: View?) {
        var txtdate: TextView? = null
        var txtComment: EditText? = null
        var save: Button? = null
        var delete: Button? = null

        init {
            this.txtdate = row?.findViewById(R.id.datemycmnt)
            this.txtComment = row?.findViewById(R.id.mycmntcontent)
            this.save = row?.findViewById(R.id.savemycmnt)
            this.delete = row?.findViewById(R.id.deletemycmnt)
        }
    }

    @SuppressLint("InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View?
        val viewHolder: ViewHolder
        if (convertView==null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.listmycmnt_item, null)
            viewHolder = ViewHolder(view)
            view?.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }
        cmntext = view!!.findViewById(R.id.mycmntcontent)
        val userDto = data[position]
        viewHolder.txtdate?.text = DateFormat.getDateTimeInstance().format(userDto.date)

        viewHolder.txtComment?.setText(userDto.comment)

        // adding new comment is only available online
        viewHolder.save!!.setOnClickListener {
            when {
                (!PhoneGrantings.isNetworkAvailable(context))
                -> Toast.makeText(context, "Internet is required for this feature", Toast.LENGTH_SHORT).show()
                (viewHolder.txtComment!!.text.isEmpty())
                -> Toast.makeText(context, "Empty comment!", Toast.LENGTH_SHORT).show()
                else
                -> {
                    println(viewHolder.txtComment!!.text.toString())
                    updateCmnt(Model.Review(userDto.id, viewHolder.txtComment!!.text.toString(), PhoneGrantings.getSharedId(context), userDto.idres, userDto.date), position)
                }
            }
        }

        // deleting comment is only available online
        viewHolder.delete!!.setOnClickListener {
            if (!PhoneGrantings.isNetworkAvailable(context))
                Toast.makeText(context, "Internet is required for this feature", Toast.LENGTH_SHORT).show()
            else
                dleteCmnt(userDto.id.toString(), position)
        }

        return view
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

    //=============================== update the comment in data base ===============================
    private fun updateCmnt(r: Model.Review, pos: Int) {


        disposable =
                restaurantServices.updateCmnt(Model.Review(r.id, r.comment, r.iduser, r.idres, r.date))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    if (result=="ok") {
                                        Toast.makeText(context, "Update succeeded", Toast.LENGTH_SHORT).show()
                                        data[pos].comment = r.comment
                                        notifyDataSetChanged()
                                    }
                                },
                                { error -> println(error.message) }
                        )
    }

    //=============================== delete the comment from data base ===============================
    private fun dleteCmnt(id: String, pos: Int) {


        disposable =
                restaurantServices.deleteCmnt(id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    if (result=="ok") {
                                        Toast.makeText(context, "Delete succeeded", Toast.LENGTH_SHORT).show()
                                        data.removeAt(pos)
                                        notifyDataSetChanged()
                                    }
                                },
                                { error -> println(error.message) }
                        )
    }
}
