package com.cliambrown.easynoise.helpers

private const val PATH = "com.cliambrown.easynoise.action."

const val PLAY = PATH + "PLAY"
const val PAUSE = PATH + "PAUSE"
const val TOGGLE_PLAY = PATH + "TOGGLE_PLAY"
const val DISMISS = PATH + "DISMISS"
const val OUTSIDE_PLAY = PATH + "OUTSIDE_PLAY"
const val OUTSIDE_PAUSE = PATH + "OUTSIDE_PAUSE"

const val VOLUME_UP = PATH + "VOLUME_UP"
const val VOLUME_DOWN = PATH + "VOLUME_DOWN"
const val VOLUME_CHANGED = PATH + "VOLUME_CHANGED"

const val SET_PLAYING = PATH + "SET_PLAYING"
const val SET_PAUSED = PATH + "SET_PAUSED"

const val CHANNEL_ID = PATH + "channel"

const val PHONE_STATE = "android.intent.action.PHONE_STATE"
const val HEADSET_PLUG = "android.intent.action.HEADSET_PLUG"
const val HEADSET_STATE_CHANGED = "android.bluetooth.headset.action.STATE_CHANGED"
const val CONNECTION_STATE_CHANGED = "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED"