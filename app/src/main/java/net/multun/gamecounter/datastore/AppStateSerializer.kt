package net.multun.gamecounter.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import net.multun.gamecounter.DEFAULT_PALETTE
import java.io.InputStream
import java.io.OutputStream

object AppStateSerializer : Serializer<AppState> {
    override val defaultValue: AppState = AppState.newBuilder()
        .addCounter(counter {
            this.id = 0
            this.name = "hp"
            this.defaultValue = 50
        })
        .addPlayer(player {
            this.id = 0
            this.selectedCounter = 0
            this.counters.put(0, 50)
            this.color = DEFAULT_PALETTE[0].encode()
        })
        .build()

    override suspend fun readFrom(input: InputStream): AppState {
        try {
            return AppState.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: AppState, output: OutputStream) = t.writeTo(output)
}
