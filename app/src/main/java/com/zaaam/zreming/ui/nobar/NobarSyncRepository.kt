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

data class NobarChatMessage(
    val id: String = "",
    val username: String = "",
    val text: String = "",
    val at: Long = 0L,
)

data class NobarParticipant(
    val username: String = "",
    val online: Boolean = false,
)

@Singleton
class NobarSyncRepository @Inject constructor() {

    private val db: FirebaseDatabase = Firebase.database

    private fun roomRef(roomId: String) = db.getReference("nobar_rooms").child(roomId)

    suspend fun createRoomMeta(roomId: String, meta: NobarRoomMeta) {
        try {
            Log.d(TAG, "createRoomMeta: roomId=$roomId")
            roomRef(roomId).child("meta").setValue(meta).await()
            Log.d(TAG, "createRoomMeta: SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "createRoomMeta failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun getRoomMeta(roomId: String): NobarRoomMeta? {
        return try {
            Log.d(TAG, "getRoomMeta: roomId=$roomId")
            val snapshot = roomRef(roomId).child("meta").get().await()
            snapshot.getValue(NobarRoomMeta::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "getRoomMeta failed: ${e.message}", e)
            null
        }
    }

    suspend fun roomExists(roomId: String): Boolean {
        return try {
            Log.d(TAG, "roomExists: roomId=$roomId")
            val snapshot = roomRef(roomId).child("meta").get().await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "roomExists failed: ${e.message}", e)
            false
        }
    }

    fun observePlayback(roomId: String): Flow<NobarPlaybackState> = callbackFlow {
        try {
            Log.d(TAG, "observePlayback: setting up listener for $roomId")
            val ref = roomRef(roomId).child("playback")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val state = snapshot.getValue(NobarPlaybackState::class.java)
                        if (state != null) {
                            Log.d(TAG, "observePlayback: onDataChange received")
                            trySend(state)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "observePlayback: onDataChange error: ${e.message}", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "observePlayback: onCancelled: ${error.message}")
                    close(error.toException())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose {
                Log.d(TAG, "observePlayback: removing listener")
                ref.removeEventListener(listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "observePlayback: setup error: ${e.message}", e)
            close(e)
        }
    }

    suspend fun pushPlayback(roomId: String, state: NobarPlaybackState) {
        try {
            Log.d(TAG, "pushPlayback: roomId=$roomId, playing=${state.isPlaying}")
            roomRef(roomId).child("playback").setValue(state).await()
            Log.d(TAG, "pushPlayback: SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "pushPlayback failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun markOnline(roomId: String, username: String) {
        try {
            Log.d(TAG, "markOnline: roomId=$roomId, username=$username")
            val ref = roomRef(roomId).child("presence").child(username)
            ref.setValue(mapOf("username" to username, "online" to true)).await()
            ref.onDisconnect().setValue(mapOf("username" to username, "online" to false))
            Log.d(TAG, "markOnline: SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "markOnline failed: ${e.message}", e)
            throw e
        }
    }

    fun observePresence(roomId: String): Flow<List<NobarParticipant>> = callbackFlow {
        try {
            Log.d(TAG, "observePresence: setting up listener for $roomId")
            val ref = roomRef(roomId).child("presence")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val list = snapshot.children.mapNotNull { child ->
                            val username = child.child("username").getValue(String::class.java)
                                ?: child.key
                                ?: return@mapNotNull null
                            val online = child.child("online").getValue(Boolean::class.java) ?: false
                            NobarParticipant(username = username, online = online)
                        }.filter { it.online }
                        Log.d(TAG, "observePresence: ${list.size} participants online")
                        trySend(list)
                    } catch (e: Exception) {
                        Log.e(TAG, "observePresence: onDataChange error: ${e.message}", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "observePresence: onCancelled: ${error.message}")
                    close(error.toException())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose {
                Log.d(TAG, "observePresence: removing listener")
                ref.removeEventListener(listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "observePresence: setup error: ${e.message}", e)
            close(e)
        }
    }

    fun observeChat(roomId: String): Flow<List<NobarChatMessage>> = callbackFlow {
        try {
            Log.d(TAG, "observeChat: setting up listener for $roomId")
            val ref = roomRef(roomId).child("chat").limitToLast(200)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val list = snapshot.children.mapNotNull { child ->
                            val msg = child.getValue(NobarChatMessage::class.java)
                            msg?.copy(id = child.key ?: "")
                        }.sortedBy { it.at }
                        Log.d(TAG, "observeChat: ${list.size} messages")
                        trySend(list)
                    } catch (e: Exception) {
                        Log.e(TAG, "observeChat: onDataChange error: ${e.message}", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "observeChat: onCancelled: ${error.message}")
                    close(error.toException())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose {
                Log.d(TAG, "observeChat: removing listener")
                ref.removeEventListener(listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "observeChat: setup error: ${e.message}", e)
            close(e)
        }
    }

    suspend fun sendChat(roomId: String, username: String, text: String) {
        try {
            Log.d(TAG, "sendChat: roomId=$roomId, username=$username")
            val ref = roomRef(roomId).child("chat").push()
            ref.setValue(
                mapOf(
                    "username" to username,
                    "text" to text,
                    "at" to System.currentTimeMillis(),
                )
            ).await()
            Log.d(TAG, "sendChat: SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "sendChat failed: ${e.message}", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "NOBAR_SYNC"
    }
}
