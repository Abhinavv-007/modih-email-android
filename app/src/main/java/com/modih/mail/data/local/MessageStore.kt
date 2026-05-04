package com.modih.mail.data.local

import com.modih.mail.data.model.MailMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory cache of the messages currently displayed in the Inbox view.
 *
 * The Cloudflare backend exposes a single `GET /api/messages?inbox_id=...`
 * endpoint that already returns every message body inline — there is no
 * per-message endpoint. To open a single message in MessageDetailScreen
 * we just keep the most recent list here and look it up by id.
 *
 * Process-scoped singleton: this is intentionally lightweight (a list +
 * a `StateFlow`) since the messages are already cached on the inbox
 * record itself by Cloudflare and a fresh fetch is only ever 5 s away.
 */
object MessageStore {
    private val state = MutableStateFlow<List<MailMessage>>(emptyList())

    val messages: StateFlow<List<MailMessage>> = state

    fun publish(list: List<MailMessage>) {
        state.value = list
    }

    fun get(messageId: String): MailMessage? = state.value.firstOrNull { it.id == messageId }

    fun remove(messageId: String) {
        state.value = state.value.filterNot { it.id == messageId }
    }

    fun clear() {
        state.value = emptyList()
    }
}
