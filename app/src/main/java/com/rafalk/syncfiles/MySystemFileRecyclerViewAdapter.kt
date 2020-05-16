package com.rafalk.syncfiles


import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rafalk.syncfiles.MySystemFileRecyclerViewAdapter.FileItem
import com.rafalk.syncfiles.SystemFilesListFragment.OnListFragmentInteractionListener
import kotlinx.android.synthetic.main.fragment_system_file.view.*
import timber.log.Timber
import java.io.File

/**
 * [RecyclerView.Adapter] that can display a [FileItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class MySystemFileRecyclerViewAdapter(
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<MySystemFileRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener
    private var mValues: MutableList<FileItem> = ArrayList()

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as FileItem
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }

        val directory = File(Environment.getExternalStorageDirectory().toString())
        Timber.d("Path: %s", directory.toString())
        val files = directory.listFiles()
        Timber.d("Size: ${files.size}")
        for (i in files.indices) {
            var name = files[i].name
            if (files[i]?.isDirectory!!){
                name+='/'
            }
            mValues.add(i, FileItem(name, files[i]))
            Timber.d("FileName: ${files[i].name}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_system_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.mContentView.text = item.content

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mContentView: TextView = mView.content

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }

    data class FileItem(val content: String, val file: File) {
        override fun toString(): String = content
    }
}
