package com.mobelite.tourismguide


import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.mobelite.tourismguide.adapters.ListCommentAdapter
import com.mobelite.tourismguide.adapters.ListMyCommentAdapter
import com.mobelite.tourismguide.data.webservice.Model
import com.mobelite.tourismguide.data.webservice.RestaurantServices
import com.mobelite.tourismguide.tools.PhoneGrantings
import info.hoang8f.android.segmented.SegmentedGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*


class CommentDiagFragment : DialogFragment() {

    companion object {
        fun newInstance(id: String): CommentDiagFragment {
            val frag = CommentDiagFragment()
            val args = Bundle()
            args.putString("id", id)

            frag.arguments = args
            return frag
        }
    }

    var cmnt: EditText? = null
    private var mAdapter: ListCommentAdapter? = null
    var list: ListView? = null
    var segment: SegmentedGroup? = null
    var allcomments: RadioButton? = null

    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_comment, container, false)

        list = root.findViewById(R.id.cmntlist)

        val save = root.findViewById<Button>(R.id.savecmnt)

        cmnt = root.findViewById(R.id.cmnttext)

        save.setOnClickListener {
            when {
                (!PhoneGrantings.isNetworkAvailable(context!!))
                -> Toast.makeText(context, "Internet is required for this feature", Toast.LENGTH_SHORT).show()
                (cmnt!!.text.isEmpty())
                -> Toast.makeText(activity!!, "Empty Comment", Toast.LENGTH_SHORT).show()
                else
                -> insertCmnt()
            }
        }

        allcomments = root.findViewById(R.id.allcmnts)
        segment = root.findViewById(R.id.segmentedcmnt)
        //segment!!.setTintColor(Color.DKGRAY)
        segment!!.setOnCheckedChangeListener { g, i ->
            when (i) {
                R.id.allcmnts -> getAllComments()
                R.id.minecmnt -> getMyComments()
            }// Nothing to do
        }
        getAllComments()



        return root
    }


    //====================================== load all comments of this restaurant from data base ======================================
    private fun getAllComments() {


        disposable =
                restaurantServices.allCmnts(arguments!!.getString("id"))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    mAdapter = ListCommentAdapter(result as MutableList<Model.Review>, context!!)

                                    list!!.adapter = mAdapter
                                    mAdapter!!.notifyDataSetChanged()


                                },
                                { error -> println(error.message) }
                        )
    }

    //====================================== load all my comments of this restaurant ======================================
    private fun getMyComments() {

        println(PhoneGrantings.getSharedId(context!!))
        print(arguments!!.getString("id"))
        disposable =
                restaurantServices.myCmnts(PhoneGrantings.getSharedId(context!!), arguments!!.getString("id"))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    val myAdapter = ListMyCommentAdapter(result as MutableList<Model.Review>, context!!)
                                    list!!.adapter = myAdapter
                                    myAdapter.notifyDataSetChanged()


                                },
                                { error -> println(error.message) }
                        )
    }

    //====================================== add comment to data base ======================================
    private fun insertCmnt() {


        disposable =
                restaurantServices.insertCmnt(Model.Review(0, cmnt!!.text.toString(), PhoneGrantings.getSharedId(activity!!), arguments!!.getString("id").toInt(), Date()))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    if (result=="ok") {
                                        cmnt!!.text.clear()
                                        allcomments!!.isChecked = true
                                        getAllComments()
                                    }
                                },
                                { error -> println(error.message) }
                        )
    }

}
