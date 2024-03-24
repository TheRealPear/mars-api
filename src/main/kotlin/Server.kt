/*
    Warzone Mars - Interface with PGM for the purposes of data persistence & enhancing gameplay with new features
    This is the backend API project which processes requests & handles data storage.

    Copyright (C) 2021 Warzone Contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package network.warzone.api

import com.korrit.kotlin.ktor.features.logging.Logging
import http.player.playerRoutes
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.websocket.*
import network.warzone.api.database.Database
import network.warzone.api.http.ApiException
import network.warzone.api.http.InternalServerErrorException
import network.warzone.api.http.achievements.achievementRoutes
import network.warzone.api.http.broadcast.broadcastRoutes
import network.warzone.api.http.leaderboard.leaderboardRoutes
import network.warzone.api.http.level.levelRoutes
import network.warzone.api.http.map.mapRoutes
import network.warzone.api.http.match.matchRoutes
import network.warzone.api.http.perks.perkRoutes
import network.warzone.api.http.punishment.punishmentRoutes
import network.warzone.api.http.rank.rankRoutes
import network.warzone.api.http.report.reportRoutes
import network.warzone.api.http.server.serverRoutes
import network.warzone.api.http.status.statusRoutes
import network.warzone.api.http.tag.tagRoutes
import network.warzone.api.socket.initSocketHandler
import java.util.*

fun main() {
    embeddedServer(Netty, host = Config.listenHost, port = Config.listenPort) {
        Server().apply { main() }
    }.start(wait = true)
}

class Server {
    fun Application.main() {
        install(CORS) {
            anyHost()
            method(HttpMethod.Options)
            method(HttpMethod.Put)
            method(HttpMethod.Patch)
            method(HttpMethod.Delete)
            header(HttpHeaders.ContentType)
            header(HttpHeaders.Authorization)
        }

        install(ContentNegotiation) {
            json()
        }

        install(StatusPages) {
            exception<ApiException> { ex ->
                call.respond(ex.statusCode, ex.response)
            }

            exception<Throwable> { cause ->
                call.respond(
                    HttpStatusCode.InternalServerError,
                    InternalServerErrorException().response
                )
                log.error(cause)
            }

        }

        install(CallId) {
            header(HttpHeaders.XRequestId)
            generate { UUID.randomUUID().toString() }
            verify { it.isNotBlank() }
        }

        install(DoubleReceive) {
            receiveEntireContent = true
        }

        install(Logging) {
            logRequests = true
            logResponses = true
            logBody = false
            logHeaders = false
        }

        // Connect to database
        Database.database



        install(WebSockets)

        initSocketHandler()

        achievementRoutes()
        statusRoutes()
        playerRoutes()
        matchRoutes()
        rankRoutes()
        tagRoutes()
        mapRoutes()
        punishmentRoutes()
        broadcastRoutes()
        levelRoutes()
        leaderboardRoutes()
        serverRoutes()
        reportRoutes()
        perkRoutes()
    }
}