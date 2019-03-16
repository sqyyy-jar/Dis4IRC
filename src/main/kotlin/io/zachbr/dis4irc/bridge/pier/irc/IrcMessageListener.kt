/*
 * This file is part of Dis4IRC.
 *
 * Copyright (c) 2018-2019 Dis4IRC contributors
 *
 * MIT License
 */

package io.zachbr.dis4irc.bridge.pier.irc

import io.zachbr.dis4irc.bridge.message.Message
import net.engio.mbassy.listener.Handler
import org.kitteh.irc.client.library.event.channel.ChannelCtcpEvent
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import org.kitteh.irc.client.library.event.client.ClientConnectionClosedEvent
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent
import org.kitteh.irc.client.library.event.user.PrivateNoticeEvent
import org.kitteh.irc.client.library.util.Format

private const val CTCP_ACTION = "ACTION"

/**
 * Responsible for listening to incoming IRC messages and filtering garbage
 */
class IrcMessageListener(private val pier: IrcPier) {
    private val logger = pier.logger

    @Handler
    fun onMessage(event: ChannelMessageEvent) {
        // ignore messages sent by this bot
        if (event.actor.nick == pier.getBotNick()) {
            return
        }

        val receiveTimestamp = System.nanoTime()
        logger.debug("IRC MSG ${event.channel.name} ${event.actor.nick}: ${event.message}")

        val sender = event.actor.asBridgeSender()
        val source = event.channel.asBridgeSource()
        val message = Message(event.message, sender, source, receiveTimestamp)
        pier.sendToBridge(message)
    }

    @Handler
    fun onChannelCtcp(event: ChannelCtcpEvent) {
        // ignore messages sent by this bot
        if (event.actor.nick == pier.getBotNick()) {
            return
        }

        val receiveTimestamp = System.nanoTime()
        logger.debug("IRC CTCP ${event.channel.name} ${event.actor.nick}: ${event.message}")

        // if it's not an action we probably don't care
        if (!event.message.startsWith(CTCP_ACTION)) {
            return
        }

        // add italic code and strip off the ctcp type info
        val messageText = "${Format.ITALIC}${event.message.substring(CTCP_ACTION.length + 1)}"

        val sender = event.actor.asBridgeSender()
        val source = event.channel.asBridgeSource()
        val message = Message(messageText, sender, source, receiveTimestamp)
        pier.sendToBridge(message)
    }

    @Handler
    fun onPrivateMessage(event: PrivateMessageEvent) {
        logger.debug("IRC PRIVMSG ${event.actor.nick}: ${event.message}")
    }

    @Handler
    fun onPrivateNotice(event: PrivateNoticeEvent) {
        logger.debug("IRC NOTICE ${event.actor.nick}: ${event.message}")
    }

    @Handler
    fun onConnectionClosed(event: ClientConnectionClosedEvent) {
        event.setAttemptReconnect(true)
        event.reconnectionDelay = 3000

        logger.warn("IRC connection closed: ${event.cause.toNullable()?.localizedMessage ?: "null reason"}")
        logger.info("Will attempt to reconnect")
    }
}
