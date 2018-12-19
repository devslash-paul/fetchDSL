package net.devslash.err

import kotlinx.coroutines.channels.Channel
import net.devslash.ChannelReceiving
import net.devslash.Envelope
import net.devslash.HttpRequest
import net.devslash.RequestData
import java.io.File

class DeadLetterQueue(filename: String,
                      private val split: String = " ") : ChannelReceiving<Pair<HttpRequest, RequestData>> {
  private val file = File(filename)

  init {
    file.createNewFile()
  }

  override suspend fun accept(channel: Channel<Envelope<Pair<HttpRequest, RequestData>>>,
                              envelope: Envelope<Pair<HttpRequest, RequestData>>,
                              e: Exception) {
    val data = envelope.get().second.getReplacements()
    var builder = ""
    for (i in 1..data.size) {
      val value = data["!$i!"]
      builder += value + split
    }

    synchronized(this) {
      file.writeText(builder + "\n")
    }
  }
}
