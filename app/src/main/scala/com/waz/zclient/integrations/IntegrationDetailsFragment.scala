/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.integrations

import android.content.Context
import android.os.Bundle
import android.support.annotation.Nullable
import android.support.v7.widget.Toolbar
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{ImageView, TextView}
import com.waz.model.{AssetId, IntegrationData, IntegrationId, ProviderId}
import com.waz.utils.events.Signal
import com.waz.zclient.common.views.ImageAssetDrawable
import com.waz.zclient.common.views.ImageAssetDrawable.{RequestBuilder, ScaleType}
import com.waz.zclient.common.views.ImageController.{ImageSource, WireImage}
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils.ContextUtils.isInLandscape
import com.waz.zclient.utils.{LayoutSpec, ViewUtils}
import com.waz.zclient.{FragmentHelper, R}
import com.waz.ZLog._
import com.waz.ZLog.ImplicitTag._
import com.waz.utils.returning
import com.waz.zclient.common.controllers.IntegrationsController

class IntegrationDetailsFragment extends BaseFragment[IntegrationDetailsFragment.Container] with FragmentHelper {

  import com.waz.threading.Threading.Implicits.Ui

  private var nameView: TypefaceTextView = _
  private var pictureView: ImageView = _
  private var shortDescrView: TypefaceTextView = _
  private var descriptionView: TypefaceTextView = _
  private var addButton: TextView = _

  private var toolbar: Toolbar = _
  private var toolbarTitle: TextView = _

  private lazy val controller = inject[IntegrationsController]

  private lazy val providerId = ProviderId(getArguments.getString(IntegrationDetailsFragment.ProviderId))
  private lazy val integrationId = IntegrationId(getArguments.getString(IntegrationDetailsFragment.IntegrationId))

  override def onCreateView(inflater: LayoutInflater, viewContainer: ViewGroup, savedInstanceState: Bundle): View = {
    verbose(s"IN onCreateView")
    inflater.inflate(R.layout.fragment_integration_details, viewContainer, false)
  }

  override def onViewCreated(view: View, @Nullable savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)

    verbose(s"IN onViewCreated")

    implicit val ctx: Context = getActivity

    nameView = findById[TypefaceTextView](R.id.integration_name)
    pictureView = findById[ImageView](R.id.integration_picture)
    shortDescrView = findById[TypefaceTextView](R.id.integration_short_descr)
    descriptionView = findById[TypefaceTextView](R.id.integration_description)
    addButton = findById[TextView](R.id.integration_add_button)

    pictureView.setImageDrawable(drawable)
    addButton.setText(getString(R.string.integrations_picker__add))

    toolbar = findById(R.id.t_integration_toolbar)

    if (LayoutSpec.isTablet(getContext) && isInLandscape(getContext)) toolbar.setNavigationIcon(null)

    toolbarTitle = ViewUtils.getView(toolbar, R.id.tv__integration_toolbar__title).asInstanceOf[TextView]

    controller.getIntegration(providerId, integrationId).map { setIntegration }
  }

  def setIntegration(data: IntegrationData): Unit = {
    verbose(s"IN setIntegration(${data.name})")
    nameView.setText(data.name)
    descriptionView.setText(data.description)
    data.assets.headOption.foreach { pictureAssetId ! _.id } // TODO: check the asset type for the profile pic
    toolbarTitle.setText(data.name)

    addButton.setOnClickListener(new OnClickListener {
      override def onClick(v: View): Unit = {
        verbose(s"adding a service ${data.name}")
      }
    })
  }

  override def onStart(): Unit = {
    super.onStart()

    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      override def onClick(v: View): Unit = {
        getActivity.onBackPressed()
      }
    })
  }

  private val pictureAssetId = Signal[AssetId]()
  private val picture: Signal[ImageSource] = pictureAssetId.collect{ case pic => WireImage(pic) }
  private lazy val drawable = new ImageAssetDrawable(picture, scaleType = ScaleType.CenterInside, request = RequestBuilder.Regular)
}

object IntegrationDetailsFragment {
  trait Container {
  }

  val Tag = classOf[IntegrationDetailsFragment].getName
  val IntegrationId = "ARG_INTEGRATION_ID"
  val ProviderId = "ARG_PROVIDER_ID"

  def newInstance(providerId: ProviderId, integrationId: IntegrationId) = returning(new IntegrationDetailsFragment) {
    _.setArguments(returning(new Bundle){ b =>
      b.putString(ProviderId, providerId.str)
      b.putString(IntegrationId, integrationId.str)
    })
  }

}
