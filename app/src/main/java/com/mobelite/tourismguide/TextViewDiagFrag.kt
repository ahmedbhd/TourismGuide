package com.mobelite.tourismguide

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class TextViewDiagFrag : DialogFragment() {
    fun newInstance(title: String): TextViewDiagFrag {
        val frag = TextViewDiagFrag()
        val args = Bundle()
        args.putString("title", title)

        frag.arguments = args
        return frag
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = activity!!.layoutInflater.inflate(R.layout.desc_diag, container)

        val text = root.findViewById<TextView>(R.id.textdisdiag)


        if (arguments!=null) {
            text.text = arguments!!.getString("title")

        }
        return root

    }
}