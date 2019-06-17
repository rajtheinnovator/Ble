package me.abhishekraj.ble.client

interface GattClientActionListener {

    fun log(message: String)

    fun logError(message: String)

    fun setConnected(connected: Boolean)

    fun initializeTime()

    fun initializeEcho()

    fun disconnectGattServer()
}