package com.mapshoppinglist

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.mapshoppinglist.data.local.AppDatabase
import com.mapshoppinglist.data.repository.DefaultGeofenceRegistryRepository
import com.mapshoppinglist.data.repository.DefaultNotificationStateRepository
import com.mapshoppinglist.data.repository.DefaultPlacesRepository
import com.mapshoppinglist.data.repository.DefaultShoppingListRepository
import com.mapshoppinglist.geofence.GeofencePendingIntentProvider
import com.mapshoppinglist.geofence.GeofenceRegistrar
import com.mapshoppinglist.geofence.GeofenceSyncCoordinator
import com.mapshoppinglist.geofence.GeofenceSyncScheduler
import com.mapshoppinglist.notification.NotificationSender
import com.mapshoppinglist.domain.repository.GeofenceRegistryRepository
import com.mapshoppinglist.domain.repository.NotificationStateRepository
import com.mapshoppinglist.domain.repository.PlacesRepository
import com.mapshoppinglist.domain.repository.ShoppingListRepository
import com.mapshoppinglist.domain.usecase.AddShoppingItemUseCase
import com.mapshoppinglist.domain.usecase.BuildGeofenceSyncPlanUseCase
import com.mapshoppinglist.domain.usecase.BuildNotificationMessageUseCase
import com.mapshoppinglist.domain.usecase.CreatePlaceUseCase
import com.mapshoppinglist.domain.usecase.DeletePlaceUseCase
import com.mapshoppinglist.domain.usecase.DeleteShoppingItemUseCase
import com.mapshoppinglist.domain.usecase.LinkItemToPlaceUseCase
import com.mapshoppinglist.domain.usecase.MarkPlaceItemsPurchasedUseCase
import com.mapshoppinglist.domain.usecase.ObserveShoppingItemsUseCase
import com.mapshoppinglist.domain.usecase.ObserveItemDetailUseCase
import com.mapshoppinglist.domain.usecase.RecordPlaceNotificationUseCase
import com.mapshoppinglist.domain.usecase.ShouldSendNotificationUseCase
import com.mapshoppinglist.domain.usecase.SnoozePlaceNotificationsUseCase
import com.mapshoppinglist.domain.usecase.UnlinkItemFromPlaceUseCase
import com.mapshoppinglist.domain.usecase.UpdatePurchasedStateUseCase
import com.mapshoppinglist.domain.usecase.UpdateItemUseCase
import com.mapshoppinglist.domain.usecase.ValidatePlaceRegistrationUseCase
import com.mapshoppinglist.domain.usecase.GetRecentPlacesUseCase

/**
 * アプリケーション全体の初期化を担当するクラス。
 */
class MapShoppingListApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
    }
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
            itemsDao = database.itemsDao()
        )
    }

    val placesRepository: PlacesRepository by lazy {
        DefaultPlacesRepository(
            placesDao = database.placesDao(),
            itemPlaceDao = database.itemPlaceDao(),
            itemsDao = database.itemsDao()
        )
    }

    val geofenceRegistryRepository: GeofenceRegistryRepository by lazy {
        DefaultGeofenceRegistryRepository(
            geofenceRegistryDao = database.geofenceRegistryDao()
        )
    }

    val notificationStateRepository: NotificationStateRepository by lazy {
        DefaultNotificationStateRepository(
            notifyStateDao = database.notifyStateDao()
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

    val observeItemDetailUseCase: ObserveItemDetailUseCase by lazy {
        ObserveItemDetailUseCase(shoppingListRepository)
    }

    val updateItemUseCase: UpdateItemUseCase by lazy {
        UpdateItemUseCase(shoppingListRepository)
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

    val notificationSender: NotificationSender by lazy {
        NotificationSender(this)
    }

    val markPlaceItemsPurchasedUseCase: MarkPlaceItemsPurchasedUseCase by lazy {
        MarkPlaceItemsPurchasedUseCase(
            shoppingListRepository = shoppingListRepository,
            geofenceSyncScheduler = geofenceSyncScheduler
        )
    }

    val createPlaceUseCase: CreatePlaceUseCase by lazy {
        CreatePlaceUseCase(
            validatePlaceRegistrationUseCase = validatePlaceRegistrationUseCase,
            placesRepository = placesRepository,
            geofenceSyncScheduler = geofenceSyncScheduler
        )
    }

    val deletePlaceUseCase: DeletePlaceUseCase by lazy {
        DeletePlaceUseCase(
            placesRepository = placesRepository,
            geofenceSyncScheduler = geofenceSyncScheduler
        )
    }

    val linkItemToPlaceUseCase: LinkItemToPlaceUseCase by lazy {
        LinkItemToPlaceUseCase(
            placesRepository = placesRepository,
            geofenceSyncScheduler = geofenceSyncScheduler
        )
    }

    val unlinkItemFromPlaceUseCase: UnlinkItemFromPlaceUseCase by lazy {
        UnlinkItemFromPlaceUseCase(
            placesRepository = placesRepository,
            geofenceSyncScheduler = geofenceSyncScheduler
        )
    }

    val getRecentPlacesUseCase: GetRecentPlacesUseCase by lazy {
        GetRecentPlacesUseCase(placesRepository)
    }

    val snoozePlaceNotificationsUseCase: SnoozePlaceNotificationsUseCase by lazy {
        SnoozePlaceNotificationsUseCase(notificationStateRepository)
    }

    val shouldSendNotificationUseCase: ShouldSendNotificationUseCase by lazy {
        ShouldSendNotificationUseCase(notificationStateRepository)
    }

    val recordPlaceNotificationUseCase: RecordPlaceNotificationUseCase by lazy {
        RecordPlaceNotificationUseCase(notificationStateRepository)
    }
}
