package net.multun.gamecounter.store

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import com.google.protobuf.InvalidProtocolBufferException
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import net.multun.gamecounter.di.AppDispatchers
import net.multun.gamecounter.di.ApplicationScope
import net.multun.gamecounter.di.Dispatcher
import net.multun.gamecounter.proto.ProtoGame
import net.multun.gamecounter.proto.ProtoNewGame
import net.multun.gamecounter.proto.counter
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Singleton


typealias NewGameStore = DataStore<ProtoNewGame.NewGame>

@Module
@InstallIn(SingletonComponent::class)
object NewGameStoreProvider {
    @Provides
    @Singleton
    internal fun providesGameStateStore(
        @ApplicationContext context: Context,
        @Dispatcher(AppDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): NewGameStore =
        DataStoreFactory.create(
            serializer = NewGameSerializer,
            scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
            migrations = listOf(),
        ) {
            context.dataStoreFile("new_game.pb")
        }
}

fun makeDefaultCounter(): ProtoGame.Counter {
    return counter {
        this.id = 0
        this.name = "hp"
        this.defaultValue = 100
    }
}

object NewGameSerializer : Serializer<ProtoNewGame.NewGame> {
    override val defaultValue: ProtoNewGame.NewGame = ProtoNewGame.NewGame.newBuilder()
        .addCounter(makeDefaultCounter()).build()

    override suspend fun readFrom(input: InputStream): ProtoNewGame.NewGame {
        try {
            return ProtoNewGame.NewGame.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: ProtoNewGame.NewGame, output: OutputStream) = t.writeTo(output)
}
