package com.zaaam.zreming.data.nobar

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
        roomRef(roomId).child("meta").setValue(meta).await()
    }

    suspend fun getRoomMeta(roomId: String): NobarRoomMeta? {
        val snapshot = roomRef(roomId).child("meta").get().await()
        return snapshot.getValue(NobarRoomMeta::class.java)
    }

    suspend fun roomExists(roomId: String): Boolean {
        val snapshot = roomRef(roomId).child("meta").get().await()
        return snapshot.exists()
    }

    fun observePlayback(roomId: String): Flow<NobarPlaybackState> = callbackFlow {
        val ref = roomRef(roomId).child("playback")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.getValue(NobarPlaybackState::class.java)
                if (state != null) trySend(state)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun pushPlayback(roomId: String, state: NobarPlaybackState) {
        roomRef(roomId).child("playback").setValue(state).await()
    }

    fun markOnline(roomId: String, username: String) {
        val ref = roomRef(roomId).child("presence").child(username)
        ref.setValue(mapOf("username" to username, "online" to true))
        ref.onDisconnect().setValue(mapOf("username" to username, "online" to false))
    }

    fun observePresence(roomId: String): Flow<List<NobarParticipant>> = callbackFlow {
        val ref = roomRef(roomId).child("presence")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val username = child.child("username").getValue(String::class.java) ?: child.key ?: return@mapNotNull null
                    val online = child.child("online").getValue(Boolean::class.java) ?: false
                    NobarParticipant(username = username, online = online)
                }.filter { it.online }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeChat(roomId: String): Flow<List<NobarChatMessage>> = callbackFlow {
        val ref = roomRef(roomId).child("chat").limitToLast(200)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val msg = child.getValue(NobarChatMessage::class.java)
                    msg?.copy(id = child.key ?: "")
                }.sortedBy { it.at }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun sendChat(roomId: String, username: String, text: String) {
        val ref = roomRef(roomId).child("chat").push()
        ref.setValue(
            mapOf(
                "username" to username,
                "text" to text,
                "at" to System.currentTimeMillis(),
            )
        ).await()
    }
}