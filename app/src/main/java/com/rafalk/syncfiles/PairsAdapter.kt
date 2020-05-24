package com.rafalk.syncfiles

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


import com.rafalk.syncfiles.PairsListFragment.OnListFragmentInteractionListener
import com.rafalk.syncfiles.database.DirsPair
import com.rafalk.syncfiles.dummy.DummyContent.DummyItem

import kotlinx.android.synthetic.main.fragment_pair.view.*

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class PairsAdapter(
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<PairsAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener
    private var mValues: List<DirsPair> = ArrayList()

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as DirsPair
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_pair, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.mRemoteDirView.text = item.remoteDir
        holder.mLocalDirView.text = item.localDir

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mRemoteDirView: TextView = mView.remote_dir
        val mLocalDirView: TextView = mView.local_dir

        override fun toString(): String {
            return super.toString() + " '" + mRemoteDirView.text + "'" + mLocalDirView.text
        }
    }

    fun setData(data: List<DirsPair>){
        mValues = data
        notifyDataSetChanged()
    }
}
