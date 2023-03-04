package com.example.snapshotsui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.snapshotsui.databinding.FragmentHomeBinding
import com.example.snapshotsui.databinding.ItemSnapshotsBinding
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class HomeFragment : Fragment(),HomeAux {
    private lateinit var mBinding: FragmentHomeBinding
    private lateinit var mFirebaseAdapter: FirebaseRecyclerAdapter<Snapshot, SnapshotsHolder>
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private val PATH_SNAPSHOT = "snapshots"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mBinding = FragmentHomeBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val query = FirebaseDatabase.getInstance().reference.child("snapshots")

        val options =
            FirebaseRecyclerOptions.Builder<Snapshot>().setQuery(query, {
                val snapshot = it.getValue(Snapshot::class.java)
                snapshot!!.id = it.key!!
                snapshot
            }).build()


        mFirebaseAdapter = object : FirebaseRecyclerAdapter<Snapshot, SnapshotsHolder>(options) {
            private lateinit var mContext: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnapshotsHolder {
                mContext = parent.context

                val view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_snapshots, parent, false)
                return SnapshotsHolder(view)
            }

            override fun onBindViewHolder(holder: SnapshotsHolder, position: Int, model: Snapshot) {
                val snapshot = getItem(position)

                with(holder) {
                    setListener(snapshot)



                    binding.tvTitle.text = snapshot.title
                    binding.cbLike.text = snapshot.likeList.size.toString()
                    FirebaseAuth.getInstance().currentUser?.let {
                    binding.cbLike.isChecked = snapshot.likeList
                        .containsKey(it.uid)
                    }
                    Glide.with(mContext)
                        .load(snapshot.photoUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(binding.ivPhoto)
                }

            }

            @SuppressLint("NotifyDataSetChanged") // ERROR INTERNO FIREBASE UI 8.0.0
            override fun onDataChanged() {
                super.onDataChanged()
                mBinding.pbItems.visibility = View.GONE
                notifyDataSetChanged()
            }

            override fun onError(error: DatabaseError) {
                super.onError(error)
                Toast.makeText(mContext, error.message, Toast.LENGTH_SHORT).show()

            }

        }

        mLayoutManager = LinearLayoutManager(context)
        mBinding.rvItems.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutManager
            adapter = mFirebaseAdapter
        }
    }

    override fun onStart() {
        super.onStart()
        mFirebaseAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        mFirebaseAdapter.stopListening()
    }

    override fun goToTop() {
        mBinding.rvItems.smoothScrollToPosition(0)
    }

    private fun deleteSnapshot(snapshot: Snapshot) {
        val databaseReference = FirebaseDatabase.getInstance().reference.child("snapshots/")
        confirmDelete(snapshot)
        val storageSnapshotref= FirebaseStorage.getInstance().reference.child(PATH_SNAPSHOT)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child(snapshot.id)
        storageSnapshotref.delete().addOnCompleteListener {
            if (it.isSuccessful){
                databaseReference.child(snapshot.id).removeValue()
            }else{
                Snackbar.make(mBinding.root,getString(R.string.home_delete_error)
                ,Snackbar.LENGTH_LONG).show()
            }
        }

    }

    private fun setLike(snapshot: Snapshot, checked: Boolean) {
        val databaseReference = FirebaseDatabase.getInstance().reference.child("snapshots")
        if (checked) {
            databaseReference.child(snapshot.id)
                .child("likeList").child(FirebaseAuth.getInstance().currentUser!!.uid)
                .setValue(checked)
        } else {
            databaseReference.child(snapshot.id)
                .child("likeList").child(FirebaseAuth.getInstance().currentUser!!.uid)
                .setValue(null)
        }

    }
    private fun confirmDelete(snapshot: Snapshot){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_dialog))
            .setPositiveButton(getString(R.string.confirm_dialog)){_,_->
                deleteSnapshot(snapshot)
            }
            .setNegativeButton(getString(R.string.denied_dialog),null)
            .show()
    }



    inner class SnapshotsHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemSnapshotsBinding.bind(view)

        fun setListener(snapshot: Snapshot) {
            binding.btnDelete.setOnClickListener { deleteSnapshot(snapshot) }
            binding.cbLike.setOnCheckedChangeListener { compoundButton, checked ->
                setLike(snapshot, checked)
            }
        }
    }

}