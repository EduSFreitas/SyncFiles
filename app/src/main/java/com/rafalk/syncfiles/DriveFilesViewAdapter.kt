package com.rafalk.syncfiles


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList
import com.rafalk.syncfiles.DriveFilesViewAdapter.FileItem
import kotlinx.android.synthetic.main.fragment_system_file.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * [RecyclerView.Adapter] that can display a [FileItem] and makes a call to the
 * specified [OnDriveListFragmentInteractionListener].
 */
class DriveFilesViewAdapter(
    private val mListener: SystemFilesListFragment.OnDriveListFragmentInteractionListener?,
    private val googleDriveService: Drive
) : RecyclerView.Adapter<DriveFilesViewAdapter.ViewHolder>(), CoroutineScope by MainScope() {

    private val mOnClickListener: View.OnClickListener
    private var mValues: MutableList<FileItem> = ArrayList()

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as FileItem
            if(isDirectory(item.file)){
                addFilesFromDirectory(item.file.id)
            }
        }

        addFilesFromDirectory("root")
    }

    private fun addFilesFromDirectory(id: String) {
        mValues.clear()
        launch(Dispatchers.Default) {
            val result = googleDriveService
                .files().list()
                .setSpaces("drive")
                .setQ("'${id}' in parents")
                .setFields("nextPageToken, files(id, name, mimeType)")
                .setPageToken(null)
                .execute()
            Timber.d("Result received $result")

            launch (Dispatchers.Main ){
                addFilesToList(result)
            }
        }
    }


    private fun isDirectory(file: com.google.api.services.drive.model.File): Boolean {
        return file.mimeType == "application/vnd.google-apps.folder"
    }

    private fun addFilesToList(result: FileList) {
        for (i in result.files.indices) {
            val file = result.files[i]
            var name = file.name
            if(isDirectory(file)){
                name+='/'
            }
            Timber.d("i=${i} name=${name}")
            mValues.add(i, FileItem(name, file))
        }
        notifyDataSetChanged()
    }

//    private fun addFiles(files: Array<File>, current: File) {
//        var parentFile = current.parentFile
//        if (current.toString() == Environment.getExternalStorageDirectory().toString()) {
//            parentFile = current
//        }
//        currentFile = current
//        mListener?.onDriveListFragmentInteraction(FileItem(".", currentFile))
//        mValues.add(0, FileItem("..", parentFile))
//        for (i in files.indices) {
//            var name = files[i].name
//            if (files[i].isDirectory) {
//                name += '/'
//            }
//            mValues.add(i + 1, FileItem(name, files[i]))
//            Timber.d("FileName: ${files[i].name}")
//        }
//    }

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

    data class FileItem(val content: String, val file: com.google.api.services.drive.model.File) {
        override fun toString(): String = content
    }
}
