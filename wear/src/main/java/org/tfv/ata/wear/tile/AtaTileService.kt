package org.tfv.ata.wear.tile

import androidx.concurrent.futures.ResolvableFuture
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material.Chip
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.MultiSlotLayout
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import org.tfv.ata.core.AtaAction
import org.tfv.ata.wear.R

private const val RESOURCES_VERSION = "1"

class AtaTileService : TileService() {

  override fun onTileRequest(
    requestParams: RequestBuilders.TileRequest,
  ): ListenableFuture<TileBuilders.Tile> {
    val future = ResolvableFuture.create<TileBuilders.Tile>()
    val layout = layout(requestParams.deviceConfiguration)
    val tile = TileBuilders.Tile.Builder()
      .setResourcesVersion(RESOURCES_VERSION)
      .setTileTimeline(Timeline.fromLayoutElement(layout))
      .build()
    future.set(tile)
    return future
  }

  override fun onTileResourcesRequest(
    requestParams: RequestBuilders.ResourcesRequest,
  ): ListenableFuture<Resources> {
    val future = ResolvableFuture.create<Resources>()
    future.set(Resources.Builder().setVersion(RESOURCES_VERSION).build())
    return future
  }

  private fun launch(action: AtaAction): Clickable =
    Clickable.Builder()
      .setId(action.id)
      .setOnClick(
        ActionBuilders.LaunchAction.Builder()
          .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
              .setPackageName(packageName)
              .setClassName("org.tfv.ata.wear.WearMainActivity")
              .build(),
          )
          .build(),
      )
      .build()

  private fun chip(action: AtaAction, device: DeviceParameters): Chip =
    Chip.Builder(this, launch(action), device)
      .setPrimaryLabelContent(getString(R.string.a_open_tesla_app))
      .setChipColors(ChipColors.primaryChipColors(Colors.DEFAULT))
      .build()

  private fun layout(device: DeviceParameters): LayoutElementBuilders.LayoutElement =
    PrimaryLayout.Builder(device)
      .setPrimaryLabelTextContent(
        Text.Builder(this, getString(R.string.app_name))
          .setTypography(Typography.TYPOGRAPHY_TITLE3)
          .setColor(androidx.wear.protolayout.ColorBuilders.argb(0xFFFFFFFF.toInt()))
          .build(),
      )
      .setContent(
        MultiSlotLayout.Builder()
          .addSlotContent(chip(AtaAction.OPEN_TESLA_APP, device))
          .build(),
      )
      .build()
}
