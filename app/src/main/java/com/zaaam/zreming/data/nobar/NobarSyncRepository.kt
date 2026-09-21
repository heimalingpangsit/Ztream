package com.zaaam.zreming.data.nobar

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NobarSyncRepository @Inject constructor() {

    private val db: FirebaseDatabase = Firebase.database

    private fun roomRef(roomId: String) = db.getReference("nobar_rooms").child(roomId)

    suspend fun createRoomMeta(roomId: String, meta: NobarRoomMeta) {
        try {
            Log.d(TAG, "createRoomMeta: $roomId")
            roomRef(roomId).child("meta").setValue(meta).await()
            Log.d(TAG, "createRoomMeta: OK")
        } catch (e: Exception) {
            Log.e(TAG, "createRoomMeta: ${e.message}")
            throw e
        }
    }

    suspend fun markOnline(roomId: String, username: String) {
        try {
            Log.d(TAG, "markOnline: $roomId $username")
            val ref = roomRef(roomId).child("presence").child(username)
            ref.setValue(mapOf("username" to username, "online" to true)).await()
            ref.onDisconnect().setValue(mapOf("username" to username, "online" to false))
            Log.d(TAG, "markOnline: OK")
        } catch (e: Exception) {
            Log.e(TAG, "markOnline: ${e.message}")
            throw e
        }
    }

    fun observePlayback(roomId: String): Flow<NobarPlaybackState> = callbackFlow {
        Log.d(TAG, "observePlayback: listener for $roomId")
        val ref = roomRef(roomId).child("playback")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val state = snapshot.getValue(NobarPlaybackState::class.java)
                    if (state != null) {
                        trySend(state)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "observePlayback onDataChange: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observePlayback cancelled: ${error.message}")
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    suspend fun pushPlayback(roomId: String, state: NobarPlaybackState) {
        try {
            Log.d(TAG, "pushPlayback: $roomId")
            roomRef(roomId).child("playback").setValue(state).await()
            Log.d(TAG, "pushPlayback: OK")
        } catch (e: Exception) {
            Log.e(TAG, "pushPlayback: ${e.message}")
            throw e
        }
    }

    fun observePresence(roomId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        Log.d(TAG, "observePresence: listener for $roomId")
        val ref = roomRef(roomId).child("presence")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val list = snapshot.children.mapNotNull { child ->
                        val data = child.value as? Map<String, Any>
                        data?.takeIf { it["online"] as? Boolean == true }
                    }
                    Log.d(TAG, "observePresence: ${list.size} online")
                    trySend(list)
                } catch (e: Exception) {
                    Log.e(TAG, "observePresence onDataChange: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observePresence cancelled: ${error.message}")
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    fun observeChat(roomId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        Log.d(TAG, "observeChat: listener for $roomId")
        val ref = roomRef(roomId).child("chat").limitToLast(200)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val list = snapshot.children.mapNotNull { child ->
                        (child.value as? Map<String, Any>)?.let { msg ->
                            msg + ("id" to (child.key ?: ""))
                        }
                    }.sortedBy { it["at"] as? Long ?: 0L }
                    Log.d(TAG, "observeChat: ${list.size} messages")
                    trySend(list)
                } catch (e: Exception) {
                    Log.e(TAG, "observeChat onDataChange: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeChat cancelled: ${error.message}")
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    suspend fun sendChat(roomId: String, username: String, text: String) {
        try {
            Log.d(TAG, "sendChat: $roomId")
            val ref = roomRef(roomId).child("chat").push()
            ref.setValue(mapOf(
                "username" to username,
                "text" to text,
                "at" to System.currentTimeMillis(),
            )).await()
            Log.d(TAG, "sendChat: OK")
        } catch (e: Exception) {
            Log.e(TAG, "sendChat: ${e.message}")
            throw e
        }
    }

    companion object {
        private const val TAG = "NOBAR_SYNC"
    }
}

data class NobarRoomMeta(
    val hostUsername: String = "",
    val contentId: String = "",
    val contentTitle: String = "",
    val isTv: Boolean = false,
    val season: Int = 1,
    val episode: Int = 1,
    val posterUrl: String = "",
    val createdAt: Long = 0L,
)

data class NobarPlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val updatedAt: Long = 0L,
    val updatedBy: String = "",
)
