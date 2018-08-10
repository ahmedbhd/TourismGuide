package com.mobelite.tourismguide


import android.content.ContextWrapper
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.daimajia.swipe.util.Attributes
import com.mobelite.tourismguide.adapters.ListCommentAdapter
import com.mobelite.tourismguide.adapters.ListViewAdapter
import com.mobelite.tourismguide.data.roomservice.database.RestaurantRepository
import com.mobelite.tourismguide.data.roomservice.local.RestaurantDataBase
import com.mobelite.tourismguide.data.roomservice.local.RestaurantDataSource
import com.mobelite.tourismguide.data.roomservice.model.Restaurant
import com.mobelite.tourismguide.data.webservice.Model
import com.mobelite.tourismguide.data.webservice.RestaurantServices
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


class CommentFragment : DialogFragment() {

    companion object {
        fun newInstance(id: String): CommentFragment {
            val frag = CommentFragment()
            val args = Bundle()
            args.putString("id", id)

            frag.arguments = args
            return frag
        }
    }
    private var mAdapter: ListCommentAdapter? = null
    var list: ListView? = null
    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_comment, container, false)

        list = root.findViewById(R.id.cmntlist)

        getComments()


        return root
    }


    private fun getComments() {


        disposable =
                restaurantServices.allCmnts(arguments!!.getString("id"))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    mAdapter = ListCommentAdapter( result as MutableList<Model.Review>, context!!)

                                    list!!.adapter = mAdapter
                                    mAdapter!!.notifyDataSetChanged()


                                },
                                { error -> println(error.message) }
                        )
    }

}
