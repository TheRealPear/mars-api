package network.warzone.api.database.model

import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import org.litote.kmongo.eq

@Serializable
data class Player(
    var _id: String,
    var name: String,
    var nameLower: String,
    var firstJoinedAt: Long,
    var lastJoinedAt: Long,
    var playtime: Long,
    var ips: List<String>
) {
    suspend fun getActiveSession(): Session? {
        return Database.sessions.findOne(Session::endedAt eq null, Session::playerId eq _id)
    }
}