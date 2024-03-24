package network.warzone.api.database

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.result.DeleteResult
import kotlinx.coroutines.runBlocking
import network.warzone.api.Config
import network.warzone.api.database.models.*
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.json
import org.litote.kmongo.reactivestreams.KMongo

object Database {
    val database: CoroutineDatabase
    val players: CoroutineCollection<Player>
    val sessions: CoroutineCollection<Session>
    val ranks: CoroutineCollection<Rank>
    val tags: CoroutineCollection<Tag>
    val levels: CoroutineCollection<Level>
    val matches: CoroutineCollection<Match>
    val deaths: CoroutineCollection<Death>
    val punishments: CoroutineCollection<Punishment>
    val achievements: CoroutineCollection<Achievement>

    init {
        val client = KMongo.createClient(Config.mongoUrl).coroutine
        database = client.getDatabase("mars-api")
        players = database.getCollection()
        sessions = database.getCollection()
        ranks = database.getCollection()
        tags = database.getCollection()
        levels = database.getCollection()
        matches = database.getCollection()
        deaths = database.getCollection()
        punishments = database.getCollection()
        achievements = database.getCollection()

        runBlocking {
            // remove if indexes are problematic later...
            players.createIndex(Indexes.text("name"), IndexOptions().name("name"))
            ranks.createIndex(Indexes.text("name"), IndexOptions().name("name"))
            tags.createIndex(Indexes.text("name"), IndexOptions().name("name"))
            levels.createIndex(Indexes.text("name"), IndexOptions().name("name"))

            players.createIndex(Indexes.descending("nameLower"), IndexOptions().name("lowercase name (not text)"))
            sessions.createIndex(Indexes.descending("player.id"), IndexOptions().name("player ID"))
            matches.createIndex(Indexes.descending("serverId"), IndexOptions().name("server ID"))
            deaths.createIndex(
                Indexes.descending("victim.id", "attacker.id"),
                IndexOptions().name("attacker ID & victim ID")
            )
            punishments.createIndex(
                Indexes.descending("target.id", "issuedAt"),
                IndexOptions().name("target ID & issued time")
            )
        }
    }
}

suspend inline fun <reified T : Any> CoroutineCollection<T>.findById(id: String): T? {
    return Database.database.getCollection<T>().findOne("{ _id: ${id.json} }")
}

suspend inline fun <reified T : Any> CoroutineCollection<T>.findByName(name: String): T? {
    return Database.database.getCollection<T>().findOne("{ nameLower: ${name.lowercase().json} }")
}

suspend inline fun <reified T : Any> CoroutineCollection<T>.findByIdOrName(target: String): T? {
    return Database.database.getCollection<T>()
        .findOne("{ \$or: [ { nameLower: ${target.lowercase().json} }, { _id: ${target.json} } ] }")
}

suspend inline fun <reified T : Any> CoroutineCollection<T>.deleteById(id: String): DeleteResult {
    return Database.database.getCollection<T>().deleteOne("{ _id: ${id.json} }")
}
