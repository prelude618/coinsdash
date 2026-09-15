package com.holyware.coinsdash

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.holyware.coinsdash.data.AuthenticationRequiredException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthManager(private val context: Context) {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    val currentEmail: String? get() = firebaseAuth.currentUser?.email

    suspend fun signIn(): String {
        val googleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(true)
            .build()
        val result = credentialManager.getCredential(
            context,
            GetCredentialRequest.Builder().addCredentialOption(googleOption).build(),
        )
        val credential = result.credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google 계정 인증 결과가 올바르지 않습니다."
        }
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        return firebaseAuth.signInWithCredential(firebaseCredential).await().user?.email
            ?: error("Google 계정 이메일을 확인할 수 없습니다.")
    }

    suspend fun idToken(forceRefresh: Boolean = false): String {
        val user = firebaseAuth.currentUser
            ?: throw AuthenticationRequiredException("Google 로그인이 필요합니다.")
        return try {
            user.getIdToken(forceRefresh).await().token
                ?: throw AuthenticationRequiredException("Google 인증이 만료되었습니다. 다시 로그인하세요.")
        } catch (error: FirebaseAuthInvalidUserException) {
            throw AuthenticationRequiredException("Google 인증이 만료되었습니다. 다시 로그인하세요.", error)
        }
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
