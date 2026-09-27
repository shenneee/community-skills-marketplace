package marketplace.ui

// ai-assisted: #2
// why: initial scaffold generated with an AI assistant; see ai/interaction_log.md entry 2

import scalafx.Includes.handle
import scalafx.beans.property.ObjectProperty
import scalafx.scene.layout.{VBox, HBox}
import scalafx.scene.control.{Label, Button, Alert, ButtonType, ScrollPane}
import scalafx.scene.control.Alert.AlertType
import scalafx.geometry.Insets
import marketplace.*
import marketplace.model.*

/** Screen 2: the listings owned by whoever is "logged in" this session. */
object MyListingsScreen:

  def build(
             appState: ObjectProperty[MarketplaceState],
             currentMemberId: ObjectProperty[String],
             updateState: (MarketplaceState => MarketplaceState) => Unit,
             navigateToNewListing: () => Unit
           ): VBox =

    val listArea = new VBox(12):
      padding = Insets(16)

    // Keeps a long listing history usable while preserving the existing refresh flow.
    val listingsScroll = new ScrollPane:
      content = listArea
      fitToWidth = true
      hbarPolicy = ScrollPane.ScrollBarPolicy.Never

    // ai-assisted: #11
    // why: derives request information from immutable transaction history instead of storing duplicate state.
    def listingRow(listing: SkillListing[SkillCategory]): HBox =
      val requests = appState.value.transactions.all.filter(_.listingId == listing.id)
      val requesterNames = requests.flatMap { request =>
        appState.value.members.find(_.id, request.requesterId).map(_.displayName)
      }.distinct
      val requestText =
        if requests.isEmpty then "No requests yet"
        else s"${requests.size} request(s) - from ${requesterNames.mkString(", ")}"

      val deleteButton = new Button("Delete"):
        styleClass = Seq("button-secondary")
        onAction = handle {
          val confirmation = new Alert(AlertType.Confirmation, "Delete this listing?")
          confirmation.showAndWait() match
            case Some(ButtonType.OK) =>
              MarketplaceService.removeListing(appState.value, listing.id, currentMemberId.value) match
                case Right(newState) => updateState(_ => newState)
                case Left(reason) => new Alert(AlertType.Error, reason).showAndWait()
            case _ => ()
        }

      new HBox(12,
        new VBox(4,
          new Label(s"${listing.title}  (${listing.category}, ${listing.creditCost} credit(s))") {
            styleClass = Seq("card-title")
          },
          new Label(s"Posted ${listing.postedAt.toLocalDate}") {
            styleClass = Seq("card-description")
          },
          new Label(requestText) {
            styleClass = Seq("card-description")
          }
        ),
        deleteButton
      ):
        padding = Insets(12)
        styleClass = Seq("card")

    val createFirstListingBtn = new Button("Create your first listing"):
      styleClass = Seq("button-primary")
      onAction = handle(navigateToNewListing())

    def refresh(): Unit =
      val mine = appState.value.listings.all.filter(_.ownerId == currentMemberId.value)
      listArea.children =
        if mine.isEmpty then
          Seq(
            new Label("You haven't listed any skills yet.") { styleClass = Seq("empty-state") },
            createFirstListingBtn
          )
        else mine.map(listingRow)

    refresh()
    appState.onChange { (_, _, _) => refresh() }
    currentMemberId.onChange { (_, _, _) => refresh() }

    val title = new Label("My Listings"):
      styleClass = Seq("screen-title")

    new VBox(12, title, listingsScroll):
      padding = Insets(16)