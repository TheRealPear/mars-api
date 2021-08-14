package network.warzone.api.socket.listeners

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import network.warzone.api.database.models.*
import network.warzone.api.database.realtime.LiveMatch
import network.warzone.api.database.realtime.LiveMatchPlayer
import network.warzone.api.database.realtime.LiveMinecraftServer
import network.warzone.api.database.realtime.LiveParty
import network.warzone.api.socket.SocketEvent
import network.warzone.api.socket.SocketListener
import java.util.*

object MatchListener : SocketListener() {
    override suspend fun handle(server: LiveMinecraftServer, event: SocketEvent, json: JsonObject) {
        when (event) {
            SocketEvent.MATCH_LOAD -> onLoad(server, Json.decodeFromJsonElement(json))
            SocketEvent.MATCH_START -> onStart(server, Json.decodeFromJsonElement(json))
            SocketEvent.MATCH_END -> onEnd(server)
            SocketEvent.PARTY_JOIN -> onPartyJoin(server, Json.decodeFromJsonElement(json))
            SocketEvent.PARTY_LEAVE -> onPartyLeave(server, Json.decodeFromJsonElement(json))
            else -> Unit
        }
    }

    private fun onLoad(server: LiveMinecraftServer, data: MatchLoadData) {
        val now = System.currentTimeMillis()
        val match = LiveMatch(
            _id = UUID.randomUUID().toString(),
            loadedAt = now,
            startedAt = null,
            endedAt = null,
            mapId = data.mapId,
            events = mutableListOf(MatchLoadEvent(data)),
            parties = emptyList(),
            participants = hashMapOf(),
            serverId = server.id,
        )

        match.parties = data.parties.map {
            LiveParty(
                it.name,
                it.alias,
                it.colour,
                it.min,
                it.max
            )
        }

        match.save()
        server.currentMatchId = match._id
    }

    private fun onStart(server: LiveMinecraftServer, data: MatchStartData) {
        val current = server.currentMatch ?: throw RuntimeException("Starting non-existent match? ${server.id}")
        if (current.startedAt !== null) throw RuntimeException("Starting match that already started?")
        current.startedAt = System.currentTimeMillis()
        data.participants.forEachIndexed { index, player ->
            current.participants[player.id] = data.participants.elementAt(index)
        }
        current.events.add(MatchStartEvent(data))
        current.save()
    }

    // todo: process "match metadata" — known participants, team mappings,
    // todo: save to DB and all --- this is unfinished
    private fun onEnd(server: LiveMinecraftServer) {
        val current = server.currentMatch ?: throw RuntimeException("Ending non-existent match? ${server.id}")
        if (current.startedAt == null) return println("Trying to end match before it's started")
        if (current.endedAt != null) return println("Trying to end match that's already ended somehow")
        current.endedAt = System.currentTimeMillis()
        current.save()
    }

    private fun onPartyJoin(server: LiveMinecraftServer, data: PartyJoinData) {
        val current =
            server.currentMatch ?: throw RuntimeException("Joining party in non-existent match? ${data.playerName}")
        val party = current.parties.find { it.name == data.partyName }
            ?: throw RuntimeException("Party not found ${data.partyName}")
        val player = current.participants[data.playerId]
        if (player == null) { // Player is new to the match
            current.participants[data.playerId] = LiveMatchPlayer(data.playerName, data.playerId, party.name)
        } else {
            player.partyName = party.name
            current.participants[player.id] = player
        }
        current.events.add(PartyJoinEvent(data))
        current.save()
    }

    private fun onPartyLeave(server: LiveMinecraftServer, data: PartyLeaveData) {
        val current =
            server.currentMatch ?: throw RuntimeException("Leaving party in non-existent match? ${data.playerName}")
        val player = current.participants[data.playerId]
            ?: throw RuntimeException("Player leaving party when they were never in one: ${data.playerName}")
        player.partyName = null
        current.participants[data.playerId] = player
        current.events.add(PartyLeaveEvent(data))
        current.save()
    }
}

@Serializable
data class MatchLoadData(val mapId: String, val parties: List<PartyData>)

@Serializable
data class MatchStartData(val participants: Set<LiveMatchPlayer>)

@Serializable
data class PartyData(val name: String, val alias: String, val colour: String, val min: Int, val max: Int)

@Serializable
data class PartyJoinData(val playerId: String, val playerName: String, val partyName: String)

@Serializable
data class PartyLeaveData(val playerId: String, val playerName: String)