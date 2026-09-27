package marketplace.ui

// ai-assisted: #2
// why: initial scaffold generated with an AI assistant; see ai/interaction_log.md entry 2

import scalafx.Includes.handle
import scalafx.beans.property.ObjectProperty
import scalafx.scene.layout.{VBox, HBox, Priority}
import scalafx.scene.control.{Label, Button, Alert, ButtonType, ScrollPane, TextField, ComboBox, Tooltip}
import scalafx.scene.control.Alert.AlertType
import scalafx.geometry.Insets
import scalafx.collections.ObservableBuffer
import marketplace.*
import marketplace.model.*


/** Screen 1: browse every listing in the marketplace and request an
 * exchange. This is where MarketplaceService.requestExchange's
 * Either result gets turned into either an updated state or an alert
 * — the only place a Left ever becomes user-visible text.
 */
object BrowseScreen:

  def build(
             appState: ObjectProperty[MarketplaceState],
             currentMemberId: ObjectProperty[String],
             updateState: (MarketplaceState => MarketplaceState) => Unit
           ): VBox =

    def listingCard(listing: SkillListing[SkillCategory]): HBox =
      val colour = SkillCategory.colourFor(listing.category)
      val icon = SkillCategory.iconFor(listing.category)
      val tag = new Label(s"$icon ${listing.category.toString}"):
        style = s"-fx-background-color: $colour; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 8;"

      // Coordinators moderate rather than trade, so the Request flow is
      // disabled for them — their action lives on the Moderate screen.
      val isCoordinator = appState.value.coordinators.find(_.id, currentMemberId.value).isDefined
      val requestBtn = new Button("Request"):
        styleClass = Seq("button-primary")
        disable = isCoordinator
        if isCoordinator then
          tooltip = new Tooltip("Coordinators moderate listings; they don't request exchanges")
        onAction = handle(
          if currentMemberId.value.isEmpty then
            new Alert(AlertType.Warning, "Please log in first to request an exchange.").showAndWait()
          else
            val confirmation = new Alert(
              AlertType.Confirmation,
              s"Spend ${listing.creditCost} credit(s) on '${listing.title}'?"
            )
            confirmation.showAndWait() match
              case Some(ButtonType.OK) =>
                MarketplaceService.requestExchange(appState.value, currentMemberId.value, listing.id) match
                  case Right(newState) =>
                    updateState(_ => newState)
                    new Alert(AlertType.Information, s"Exchange completed for '${listing.title}'").showAndWait()
                  case Left(reason) =>
                    new Alert(AlertType.Error, reason).showAndWait()
              case _ => ()
        )

      val cardDetails = new VBox(12,
        new Label(listing.title) {
          styleClass = Seq("card-title")
        },
        new Label(listing.description) {
          styleClass = Seq("card-description")
          wrapText = true
          maxWidth = 400
        },
        // ai-assisted: #9
        // why: surfaces the listing's creation date carried by the postedAt persistence feature.
        new Label(s"Posted ${listing.postedAt.toLocalDate}") {
          styleClass = Seq("card-description")
        },
        new Label(s"${listing.creditCost} credit(s)") {
          styleClass = Seq("card-cost")
        }
      ):
        padding = Insets(8)

      HBox.setHgrow(cardDetails, Priority.Always)

      new HBox(12, tag, cardDetails, requestBtn):
        padding = Insets(10)
        styleClass = Seq("card")

    val listArea = new VBox(12):
      padding = Insets(16)

    // ai-assisted: #10
    // why: lets members narrow a large listing catalogue without duplicating card rendering.
    val searchField = new TextField:
      promptText = "Search listings by title"

    val categoryFilter = new ComboBox[String]():
      items = ObservableBuffer.from("All" :: SkillCategory.values.toList.map(_.toString))
      value = "All"

    // Keeps an expanding marketplace accessible without changing its card refresh logic.
    val listingsScroll = new ScrollPane:
      content = listArea
      fitToWidth = true
      hbarPolicy = ScrollPane.ScrollBarPolicy.Never

    def refresh(): Unit =
      val searchTerm = searchField.text.value.trim.toLowerCase
      val selectedCategory = categoryFilter.value.value
      val filtered = appState.value.listings.all.filter { listing =>
        listing.title.toLowerCase.contains(searchTerm) &&
          (selectedCategory == "All" || listing.category.toString == selectedCategory)
      }
      listArea.children =
        if filtered.isEmpty then
          Seq(new Label("No listings match your search yet.") { styleClass = Seq("empty-state") })
        else filtered.map(listingCard)

    refresh()
    appState.onChange { (_, _, _) => refresh() }
    currentMemberId.onChange { (_, _, _) => refresh() }
    searchField.text.onChange { (_, _, _) => refresh() }
    categoryFilter.onAction = handle(refresh())

    val title = new Label("Browse Skills"):
      styleClass = Seq("screen-title")

    new VBox(12, title, searchField, categoryFilter, listingsScroll):
      padding = Insets(16)