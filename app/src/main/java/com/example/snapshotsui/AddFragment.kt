package com.example.snapshotsui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import com.example.snapshotsui.databinding.FragmentAddBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


class AddFragment : Fragment() {
    private val PATH_SNAPSHOT = "snapshots"


    private lateinit var mBinding: FragmentAddBinding
    private lateinit var mStorageReference: StorageReference
    private lateinit var mDataBaseReference: DatabaseReference


    private var mPhotoSelectedUri: Uri? = null


    private var galleryResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                mPhotoSelectedUri = it.data?.data
                with(mBinding) {
                    ivPhoto.setImageURI(mPhotoSelectedUri)
                    tilTitle.visibility = View.VISIBLE
                    tvMessage.text = getString(R.string.post_message_valid)
                }
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentAddBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.btnPost.setOnClickListener { postSnapshot() }
        mBinding.ibSelect.setOnClickListener { openGallery() }
        setupFireBase()

    }

    private fun setupFireBase() {
        mStorageReference = FirebaseStorage.getInstance().reference.child(PATH_SNAPSHOT)
        mDataBaseReference = FirebaseDatabase.getInstance().reference.child(PATH_SNAPSHOT)

    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryResult.launch(intent)
    }

    private fun postSnapshot() {
        mBinding.progressBar.visibility = View.VISIBLE
        val key = mDataBaseReference.push().key!!
        val storageReference = mStorageReference.child(PATH_SNAPSHOT)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child(key)
        if (mPhotoSelectedUri != null) {
            storageReference.putFile(mPhotoSelectedUri!!)
                .addOnProgressListener {
                    val progress = (100 * it.bytesTransferred / it.totalByteCount).toDouble()
                    mBinding.progressBar.progress = progress.toInt()
                    mBinding.tvMessage.text = "$progress%"
                }
                .addOnCompleteListener {
                    mBinding.progressBar.visibility = View.INVISIBLE
                }
                .addOnSuccessListener {
                    Snackbar.make(
                        mBinding.root, R.string.instantanea_succefull,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    it.storage.downloadUrl.addOnSuccessListener {
                        saveSnapshot(key, it.toString(), mBinding.etTitle.text.toString().trim())
                        mBinding.tilTitle.visibility = View.GONE
                        mBinding.tvMessage.text = getString(R.string.post_message_title)
                    }
                }
                .addOnFailureListener {
                    Snackbar.make(
                        mBinding.root, R.string.instantanea_denied,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
        }

    }

    private fun saveSnapshot(key: String, url: String, title: String) {
        val snapshot = Snapshot(title = title, photoUrl = url)
        mDataBaseReference.child(key).setValue(snapshot)


    }


}
