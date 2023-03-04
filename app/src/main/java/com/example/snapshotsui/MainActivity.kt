package com.example.snapshotsui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.snapshotsui.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mActiveFragment: Fragment
    private lateinit var mFragmentManager: FragmentManager

    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener
    private var mFirebaseAuth: FirebaseAuth? = null

    private val authResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                Toast.makeText(this, "Bienvenido..", Toast.LENGTH_SHORT).show()
            } else {
                if (IdpResponse.fromResultIntent(it.data) == null)
                    finish()
            }
        }


override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mBinding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(mBinding.root)
    setupAuth()
    setupBottomnav()
}

private fun setupAuth() {
    mFirebaseAuth = FirebaseAuth.getInstance()
    mAuthListener = FirebaseAuth.AuthStateListener {

        if (it.currentUser == null) {
            authResult.launch(
                AuthUI.getInstance().createSignInIntentBuilder()
                    .setIsSmartLockEnabled(false)
                    .setAvailableProviders(
                        Arrays.asList(
                            AuthUI.IdpConfig.EmailBuilder().build(),
                            AuthUI.IdpConfig.GoogleBuilder().build()
                        ),

                        )
                    .build()
            )
        }
    }
}

private fun setupBottomnav() {
    mFragmentManager = supportFragmentManager

    val homeFragment = HomeFragment()
    val addFragment = AddFragment()
    val profileFragment = ProfileFragment()

    mActiveFragment = homeFragment

    mFragmentManager.beginTransaction()
        .add(R.id.hostFragment, profileFragment, profileFragment::class.java.name)
        .hide(profileFragment).commit()

    mFragmentManager.beginTransaction()
        .add(R.id.hostFragment, addFragment, addFragment::class.java.name)
        .hide(addFragment).commit()

    mFragmentManager.beginTransaction()
        .add(R.id.hostFragment, homeFragment, HomeFragment::class.java.name)
        .commit()



    mBinding.bottomNav.setOnItemSelectedListener {
        when (it.itemId) {
            R.id.action_home -> {
                mFragmentManager.beginTransaction().hide(mActiveFragment).show(homeFragment)
                    .commit()
                mActiveFragment = homeFragment
                true
            }
            R.id.action_add -> {
                mFragmentManager.beginTransaction().hide(mActiveFragment).show(addFragment).commit()
                mActiveFragment = addFragment
                true
            }
            R.id.action_profile -> {
                mFragmentManager.beginTransaction().hide(mActiveFragment).show(profileFragment)
                    .commit()
                mActiveFragment = profileFragment
                true
            }
            else -> false
        }

    }

    mBinding.bottomNav.setOnItemReselectedListener {
        when (it.itemId) {
            R.id.action_home -> (homeFragment as HomeAux).goToTop()
        }

    }
}

override fun onResume() {
    super.onResume()
    mFirebaseAuth?.addAuthStateListener(mAuthListener)
}

override fun onPause() {
    super.onPause()
    mFirebaseAuth?.removeAuthStateListener(mAuthListener)
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

}
}
