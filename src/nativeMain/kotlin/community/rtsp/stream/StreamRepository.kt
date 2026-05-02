package community.rtsp.stream

import community.rtsp.db.Database

class StreamRepository(
    private val database: Database,
) {

    private val streamQueries = database.streamQueries
    private val streamShareQueries = database.streamShareQueries
    private val streamFavoriteQueries = database.streamFavoriteQueries

    // Stream access methods
    fun getAllStreams() = streamQueries.getAllStreams().executeAsList()

    fun getStreamsForUser(userId: Long) = streamQueries.getStreamsForUser(userId)

    fun addStream(ownerId: Long, alias: String, url: String, dir: String) {
        streamQueries.insertStream(ownerId, alias, url, dir)
    }

    fun shareStream(streamId: Long, targetUserId: Long) {
        streamShareQueries.shareStream(streamId, targetUserId)
    }

    fun unshareStream(streamId: Long, userId: Long) {
        streamShareQueries.unshareStream(streamId, userId)
    }

    fun deleteAllShares(streamId: Long) {
        streamShareQueries.deleteAllShares(streamId)
    }

    fun inactivateStream(streamId: Long) {
        streamQueries.inactivateStream(streamId)
    }

    fun getStreamById(id: Long, userId: Long) =
        streamQueries.getStreamById(id, userId).executeAsOneOrNull()

    fun getStreamByAlias(alias: String, userId: Long) =
        streamQueries.getStreamByAlias(alias, userId).executeAsOneOrNull()

    fun addFavorite(userId: Long, streamId: Long) {
        streamFavoriteQueries.addFavorite(userId, streamId)
    }

    fun deleteFavorite(userId: Long, streamId: Long) {
        streamFavoriteQueries.deleteFavorite(userId, streamId)
    }
}

