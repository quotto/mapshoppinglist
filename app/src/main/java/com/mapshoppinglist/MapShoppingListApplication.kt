package com.mapshoppinglist

import android.app.Application
import com.mapshoppinglist.data.local.AppDatabase
import com.mapshoppinglist.data.repository.DefaultGeofenceRegistryRepository
import com.mapshoppinglist.data.repository.DefaultPlacesRepository
import com.mapshoppinglist.data.repository.DefaultShoppingListRepository
import com.mapshoppinglist.geofence.GeofencePendingIntentProvider
import com.mapshoppinglist.geofence.GeofenceRegistrar
import com.mapshoppinglist.geofence.GeofenceSyncCoordinator
import com.mapshoppinglist.geofence.GeofenceSyncScheduler
import com.mapshoppinglist.domain.repository.GeofenceRegistryRepository
import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.domain.repository.ShoppingListRepository
import com.mapshoppinglist.domain.usecase.AddShoppingItemUseCase
import com.mapshoppinglist.domain.usecase.DeleteShoppingItemUseCase
import com.mapshoppinglist.domain.usecase.BuildGeofenceSyncPlanUseCase
import com.mapshoppinglist.domain.usecase.ValidatePlaceRegistrationUseCase
import com.mapshoppinglist.domain.usecase.ObserveShoppingItemsUseCase
import com.mapshoppinglist.domain.usecase.UpdatePurchasedStateUseCase
import com.mapshoppinglist.domain.usecase.BuildNotificationMessageUseCase

/**
 * アプリケーション全体の初期化を担当するクラス。
 */
class MapShoppingListApplication : Application() {
    /**
     * Roomデータベースをアプリ共通の依存として遅延初期化する。
     */
    val database: AppDatabase by lazy {
        AppDatabase.build(this)
    }

    /**
     * 買い物リストのリポジトリ依存。
     */
    val shoppingListRepository: ShoppingListRepository by lazy {
        DefaultShoppingListRepository(
            itemsDao = database.itemsDao(),
            geofenceSyncScheduler = geofenceSyncScheduler
        )
    }

    val placesRepository: PlacesRepository by lazy {
        DefaultPlacesRepository(
            placesDao = database.placesDao()
        )
    }

    val geofenceRegistryRepository: GeofenceRegistryRepository by lazy {
        DefaultGeofenceRegistryRepository(
            geofenceRegistryDao = database.geofenceRegistryDao()
        )
    }

    /**
     * ユースケースはここでまとめて初期化しておき、ViewModelのファクトリから取得する。
     */
    val observeShoppingItemsUseCase: ObserveShoppingItemsUseCase by lazy {
        ObserveShoppingItemsUseCase(shoppingListRepository)
    }

    val addShoppingItemUseCase: AddShoppingItemUseCase by lazy {
        AddShoppingItemUseCase(shoppingListRepository)
    }

    val deleteShoppingItemUseCase: DeleteShoppingItemUseCase by lazy {
        DeleteShoppingItemUseCase(shoppingListRepository)
    }

    val updatePurchasedStateUseCase: UpdatePurchasedStateUseCase by lazy {
        UpdatePurchasedStateUseCase(shoppingListRepository)
    }

    val validatePlaceRegistrationUseCase: ValidatePlaceRegistrationUseCase by lazy {
        ValidatePlaceRegistrationUseCase(placesRepository)
    }

    val buildNotificationMessageUseCase: BuildNotificationMessageUseCase by lazy {
        BuildNotificationMessageUseCase()
    }

    val buildGeofenceSyncPlanUseCase: BuildGeofenceSyncPlanUseCase by lazy {
        BuildGeofenceSyncPlanUseCase(
            placesRepository = placesRepository,
            geofenceRegistryRepository = geofenceRegistryRepository
        )
    }

    val geofencePendingIntentProvider: GeofencePendingIntentProvider by lazy {
        GeofencePendingIntentProvider(this)
    }

    val geofenceRegistrar: GeofenceRegistrar by lazy {
        GeofenceRegistrar(
            context = this,
            pendingIntentProvider = geofencePendingIntentProvider
        )
    }

    val geofenceSyncScheduler: GeofenceSyncScheduler by lazy {
        GeofenceSyncScheduler(this)
    }

    val geofenceSyncCoordinator: GeofenceSyncCoordinator by lazy {
        GeofenceSyncCoordinator(
            buildGeofenceSyncPlanUseCase = buildGeofenceSyncPlanUseCase,
            geofenceRegistrar = geofenceRegistrar,
            geofenceRegistryRepository = geofenceRegistryRepository
        )
    }
}
