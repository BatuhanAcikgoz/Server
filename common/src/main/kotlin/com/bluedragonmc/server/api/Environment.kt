package com.bluedragonmc.server.api

import com.bluedragonmc.server.VersionInfo

abstract class Environment {

    companion object {

        lateinit var current: Environment

        val queue get() = current.queue
        val mongoHostname get() = current.mongoHostname
        val dbName get() = current.dbName
        val puffinHostname get() = current.puffinHostname
        val defaultGameName get() = current.defaultGameName
        val gameClasses get() = current.gameClasses
        val versionInfo get() = current.versionInfo
        val isDev get() = current.isDev
        suspend fun getServerName() = current.getServerName()

        fun setEnvironment(env: Environment) {
            if (!::current.isInitialized) {
                current = env
            } else error("Tried to override current Environment")
        }
    }

    abstract val queue: Queue
    abstract val mongoHostname: String
    abstract val puffinHostname: String
    abstract val luckPermsHostname: String
    abstract val gameClasses: Collection<String>
    abstract val versionInfo: VersionInfo
    abstract val isDev: Boolean
    open val defaultGameName: String = "Lobby"
    open val dbName: String = "bluedragon"

    abstract suspend fun getServerName(): String
}