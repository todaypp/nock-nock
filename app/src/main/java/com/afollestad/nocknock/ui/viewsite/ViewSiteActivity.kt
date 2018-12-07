/**
 * Designed and developed by Aidan Follestad (@afollestad)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.afollestad.nocknock.ui.viewsite

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.afollestad.nocknock.R
import com.afollestad.nocknock.broadcasts.StatusUpdateIntentReceiver
import com.afollestad.nocknock.data.model.Site
import com.afollestad.nocknock.data.model.ValidationMode
import com.afollestad.nocknock.viewcomponents.ext.attachLiveData
import com.afollestad.nocknock.viewcomponents.ext.dimenFloat
import com.afollestad.nocknock.viewcomponents.ext.onScroll
import com.afollestad.nocknock.viewcomponents.ext.toViewError
import com.afollestad.nocknock.viewcomponents.ext.toViewText
import com.afollestad.nocknock.viewcomponents.ext.toViewVisibility
import kotlinx.android.synthetic.main.activity_viewsite.checkIntervalLayout
import kotlinx.android.synthetic.main.activity_viewsite.disableChecksButton
import kotlinx.android.synthetic.main.activity_viewsite.doneBtn
import kotlinx.android.synthetic.main.activity_viewsite.iconStatus
import kotlinx.android.synthetic.main.activity_viewsite.inputName
import kotlinx.android.synthetic.main.activity_viewsite.inputUrl
import kotlinx.android.synthetic.main.activity_viewsite.loadingProgress
import kotlinx.android.synthetic.main.activity_viewsite.responseTimeoutInput
import kotlinx.android.synthetic.main.activity_viewsite.responseValidationMode
import kotlinx.android.synthetic.main.activity_viewsite.responseValidationSearchTerm
import kotlinx.android.synthetic.main.activity_viewsite.scriptInputLayout
import kotlinx.android.synthetic.main.activity_viewsite.scrollView
import kotlinx.android.synthetic.main.activity_viewsite.textLastCheckResult
import kotlinx.android.synthetic.main.activity_viewsite.textNextCheck
import kotlinx.android.synthetic.main.activity_viewsite.textUrlWarning
import kotlinx.android.synthetic.main.activity_viewsite.toolbar
import kotlinx.android.synthetic.main.activity_viewsite.validationModeDescription
import org.koin.androidx.viewmodel.ext.android.viewModel

/** @author Aidan Follestad (@afollestad) */
class ViewSiteActivity : AppCompatActivity() {

  internal val viewModel by viewModel<ViewSiteViewModel>()

  private val statusUpdateReceiver =
    StatusUpdateIntentReceiver(application) {
      viewModel.setModel(it)
    }

  @SuppressLint("SetTextI18n")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_viewsite)
    setupUi()

    lifecycle.run {
      addObserver(viewModel)
      addObserver(statusUpdateReceiver)
    }

    // Loading
    loadingProgress.observe(this, viewModel.onIsLoading())

    // Status
    viewModel.status.observe(this, Observer {
      iconStatus.setStatus(it)
      invalidateMenuForStatus(it)
    })

    // Name
    inputName.attachLiveData(this, viewModel.name)
    viewModel.onNameError()
        .toViewError(this, inputName)

    // Url
    inputUrl.attachLiveData(this, viewModel.url)
    viewModel.onUrlError()
        .toViewError(this, inputUrl)
    viewModel.onUrlWarningVisibility()
        .toViewVisibility(this, textUrlWarning)

    // Timeout
    responseTimeoutInput.attachLiveData(this, viewModel.timeout)
    viewModel.onTimeoutError()
        .toViewError(this, responseTimeoutInput)

    // Validation mode
    responseValidationMode.attachLiveData(this, viewModel.validationMode,
        { ValidationMode.fromIndex(it) },
        { it.toIndex() })
    viewModel.onValidationSearchTermError()
        .toViewError(this, responseValidationSearchTerm)
    viewModel.onValidationModeDescription()
        .toViewText(this, validationModeDescription)

    // Validation search term
    responseValidationSearchTerm.attachLiveData(this, viewModel.validationSearchTerm)
    viewModel.onValidationSearchTermVisibility()
        .toViewVisibility(this, responseValidationSearchTerm)

    // Validation script
    scriptInputLayout.attach(
        codeData = viewModel.validationScript,
        errorData = viewModel.onValidationScriptError(),
        visibility = viewModel.onValidationScriptVisibility()
    )

    // Check interval
    checkIntervalLayout.attach(
        valueData = viewModel.checkIntervalValue,
        multiplierData = viewModel.checkIntervalUnit,
        errorData = viewModel.onCheckIntervalError()
    )

    // Last/next check
    viewModel.onLastCheckResultText()
        .toViewText(this, textLastCheckResult)
    viewModel.onNextCheckText()
        .toViewText(this, textNextCheck)

    // Disabled button
    viewModel.onDisableChecksVisibility()
        .toViewVisibility(this, disableChecksButton)
    disableChecksButton.setOnClickListener { maybeDisableChecks() }

    // Done button
    viewModel.onDoneButtonText()
        .toViewText(this, doneBtn)
    doneBtn.setOnClickListener {
      viewModel.commit { finish() }
    }

    // Populate view model with initial data
    val model = intent.getSerializableExtra(KEY_SITE) as Site
    viewModel.setModel(model)
  }

  private fun setupUi() {
    toolbar.run {
      setNavigationOnClickListener { finish() }
      inflateMenu(R.menu.menu_viewsite)
      menu.findItem(R.id.refresh)
          .setActionView(R.layout.menu_item_refresh_icon)
          .apply {
            actionView.setOnClickListener { viewModel.checkNow() }
          }
      setOnMenuItemClickListener {
        maybeRemoveSite()
        true
      }
    }

    scrollView.onScroll {
      toolbar.elevation = if (it > toolbar.height / 4) {
        toolbar.dimenFloat(R.dimen.default_elevation)
      } else {
        0f
      }
    }

    val validationOptionsAdapter = ArrayAdapter(
        this,
        R.layout.list_item_spinner,
        resources.getStringArray(R.array.response_validation_options)
    )
    validationOptionsAdapter.setDropDownViewResource(R.layout.list_item_spinner_dropdown)
    responseValidationMode.adapter = validationOptionsAdapter
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    if (intent != null && intent.hasExtra(KEY_SITE)) {
      val newModel = intent.getSerializableExtra(KEY_SITE) as Site
      viewModel.setModel(newModel)
    }
  }
}
