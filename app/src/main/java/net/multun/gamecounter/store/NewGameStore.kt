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


import androidx.datastore.core.DataMigration
import net.multun.gamecounter.proto.copy
import net.multun.gamecounter.proto.counter

typealias NewGameStore = DataStore<ProtoNewGame.NewGame>

private object NewGameMigration : DataMigration<ProtoNewGame.NewGame> {
    override suspend fun shouldMigrate(currentData: ProtoNewGame.NewGame): Boolean =
        currentData.counterList.any { it.step == 0 || it.largeStep == 0 }

    override suspend fun migrate(currentData: ProtoNewGame.NewGame): ProtoNewGame.NewGame =
        currentData.copy {
            val updatedCounters = counter.map { c ->
                c.copy {
                    if (step == 0) step = 1
                    if (largeStep == 0) largeStep = 10
                }
            }
            counter.clear()
            counter.addAll(updatedCounters)
        }

    override suspend fun cleanUp() {}
}

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
            migrations = listOf(NewGameMigration),
        ) {
            context.dataStoreFile("new_game.pb")
        }
}

fun makeDefaultCounter(): ProtoGame.Counter {
    return counter {
        this.id = 0
        this.name = "hp"
        this.defaultValue = 100
        this.step = 1
        this.largeStep = 10
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
