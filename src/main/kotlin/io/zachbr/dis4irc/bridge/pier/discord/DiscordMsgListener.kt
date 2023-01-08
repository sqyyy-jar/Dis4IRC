/*
 * This file is part of Dis4IRC.
 *
 * Copyright (c) 2018-2023 Dis4IRC contributors
 *
 * MIT License
 */

package io.zachbr.dis4irc.bridge.pier.discord

import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

/**
 * Responsible for listening to incoming discord messages and filtering garbage
 */
class DiscordMsgListener(private val pier: DiscordPier) : ListenerAdapter() {
    private val logger = pier.logger

    override fun onMessageReceived(event: MessageReceivedEvent) {
        // don't bridge DMs on this handler (later?)
        if (event.message.channelType != ChannelType.TEXT) {
            return
        }

        // don't bridge itself
        val source = event.channel.asTextChannel().asBridgeSource() // if we bridge DMs in the future, this needs to change
        if (pier.isThisBot(source, event.author.idLong)) {
            return
        }

        // don't bridge empty messages (discord does this on join)
        if (event.message.contentDisplay.isEmpty() && event.message.attachments.isEmpty() && event.message.stickers.isEmpty()) {
            return
        }

        val receiveTimestamp = System.nanoTime()
        logger.debug("DISCORD MSG ${event.channel.name} ${event.author.name}: ${event.message.contentStripped}")

        val message = event.message.toBridgeMsg(logger, receiveTimestamp)
        pier.sendToBridge(message)
    }
}
