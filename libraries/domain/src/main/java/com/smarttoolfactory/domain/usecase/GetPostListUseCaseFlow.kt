package com.smarttoolfactory.domain.usecase

import com.smarttoolfactory.data.model.PostEntity
import com.smarttoolfactory.data.repository.PostRepositoryCoroutines
import com.smarttoolfactory.domain.dispatcher.UseCaseDispatchers
import com.smarttoolfactory.domain.error.EmptyDataException
import com.smarttoolfactory.domain.mapper.EntityToPostMapper
import com.smarttoolfactory.domain.model.Post
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * UseCase for getting Post list with offline first or offline last approach.
 *
 * * *Offline-first* first source to look for data is local data source, or database,
 * if database is empty or if caching is used it's expiry date is over, looks for remote source
 * for data. If both of the sources are empty, either return empty list or error to notify UI.
 *
 * This approach is good for when user is offline or have no internet connection, additional logic
 * can be added to check if user is offline to not deleted cached data.
 *
 * * *Offline-last* always checks remote data source for data and applies to database or offline
 * source as last resort. Offline-last is used especially when user refreshes data with a UI
 * element to get the latest data or new data is always first preference.
 */

class GetPostListUseCaseFlow @Inject constructor(
    private val repository: PostRepositoryCoroutines,
    private val entityToPostMapper: EntityToPostMapper,
    private val dispatcherProvider: UseCaseDispatchers
) {

    /**
     * Function to retrieve data from repository with offline-last which checks
     * REMOTE data source first.
     *
     * * Check out Remote Source first
     * * If empty data or null returned throw empty set exception
     * * If error occurred while fetching data from remote: Try to fetch data from db
     * * If data is fetched from remote source: delete old data, save new data and return new data
     * * If both network and db don't have any data throw empty set exception
     *
     */
    fun getPostFlowOfflineLast(): Flow<List<Post>> {
        return flow { emit(repository.fetchEntitiesFromRemote()) }
            .map {
                if (it.isNullOrEmpty()) {
                    throw EmptyDataException("No Data is available in Remote source!")
                } else {
                    repository.run {
                        deletePostEntities()
                        savePostEntities(it)
                        getPostEntitiesFromLocal()
                    }
                }
            }
            .flowOn(dispatcherProvider.ioDispatcher)
            // This is where remote exception or least likely db exceptions are caught
            .catch { throwable ->
                emitAll(flowOf(repository.getPostEntitiesFromLocal()))
            }
            .map {
                toPostListOrError(it)
            }
    }

    /**
     * Function to retrieve data from repository with offline-first which checks
     * LOCAL data source first.
     *
     * * Check out Local Source first
     * * If empty data or null returned throw empty set exception
     * * If error occurred while fetching data from remote: Try to fetch data from db
     * * If data is fetched from remote source: delete old data, save new data and return new data
     * * If both network and db don't have any data throw empty set exception
     *
     */
    fun getPostFlowOfflineFirst(): Flow<List<Post>> {
        return flow { emit(repository.getPostEntitiesFromLocal()) }
            .catch { throwable ->
                emitAll(flowOf(listOf()))
            }
            .map {
                if (it.isEmpty()) {
                    repository.run {
                        val data = fetchEntitiesFromRemote()
                        deletePostEntities()
                        savePostEntities(data)
                        data
                    }
                } else {
                    it
                }
            }
            .flowOn(dispatcherProvider.ioDispatcher)
            .catch { throwable ->
                emitAll(flowOf(listOf()))
            }
            .map {
                toPostListOrError(it)
            }
    }

    fun getPostWithStatusFlowOfflineFirst(): Flow<List<Post>> {

        return flow { emit(repository.getPostEntitiesFromLocal()) }
            .catch { throwable ->
                emitAll(flowOf(listOf()))
            }
            .map {
                if (it.isEmpty()) {
                    repository.run {
                        val data = fetchEntitiesFromRemote()
                        deletePostEntities()
                        savePostEntities(data)
                        data
                    }
                } else {
                    it
                }
            }
            .flowOn(dispatcherProvider.ioDispatcher)
            .catch { throwable ->
                emitAll(flowOf(listOf()))
            }
            .map {
                toPostListOrError(it)
            }
    }

    private fun toPostListOrError(postEntityList: List<PostEntity>): List<Post> {
        return if (!postEntityList.isNullOrEmpty()) {
            entityToPostMapper.map(postEntityList)
        } else {
            throw EmptyDataException("Empty data mapping error!")
        }
    }
}
