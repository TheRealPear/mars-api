package network.warzone.api.socket2.event

import network.warzone.api.socket2.listeners.server.LiveGameServer
import kotlin.reflect.KClass
import kotlin.reflect.KSuspendFunction1

open class Event {
    var cancelled: Boolean = false
}

open class ServerEvent(val server: LiveGameServer) : Event()

abstract class Listener {
    abstract val handlers: Map<out KSuspendFunction1<Nothing, Unit>, KClass<out ServerEvent>>
}