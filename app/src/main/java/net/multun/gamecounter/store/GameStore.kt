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
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Singleton

typealias GameStore = DataStore<ProtoGame.Game>

@Module
@InstallIn(SingletonComponent::class)
object GameStoreProvider {
    @Provides
    @Singleton
    internal fun providesGameStateStore(
        @ApplicationContext context: Context,
        @Dispatcher(AppDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): GameStore =
        DataStoreFactory.create(
            serializer = GameSerializer,
            scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
            migrations = listOf(),
        ) {
            context.dataStoreFile("app_state.pb")
        }
}

object GameSerializer : Serializer<ProtoGame.Game> {
    override val defaultValue: ProtoGame.Game = ProtoGame.Game.newBuilder().build()

    override suspend fun readFrom(input: InputStream): ProtoGame.Game {
        try {
            return ProtoGame.Game.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: ProtoGame.Game, output: OutputStream) = t.writeTo(output)
}
