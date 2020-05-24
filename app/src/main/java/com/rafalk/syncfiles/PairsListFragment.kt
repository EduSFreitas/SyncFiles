package com.rafalk.syncfiles

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import com.rafalk.syncfiles.database.DirsPair

import com.rafalk.syncfiles.dummy.DummyContent
import com.rafalk.syncfiles.dummy.DummyContent.DummyItem
import timber.log.Timber

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [PairsListFragment.OnListFragmentInteractionListener] interface.
 */
class PairsListFragment : Fragment() {

    private var listener: OnListFragmentInteractionListener? = null
    private var adapter: PairsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProviders.of(this).get(MainViewModel::class.java)
        model.allPairs.value?.let { adapter?.setData(it) }
        model.allPairs.observe(this, Observer { pairs ->
            pairs?.let {
                adapter?.setData(pairs)
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pair_list, container, false)
        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter = getFragmentAdapter()
                addItemDecoration(
                    DividerItemDecoration(view.getContext(),
                        DividerItemDecoration.VERTICAL
                    )
                )
            }
        }
        return view
    }

    private fun getFragmentAdapter(): PairsAdapter? {
        return adapter
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
            adapter = PairsAdapter(listener)
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        adapter = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: DirsPair?)
    }
}
