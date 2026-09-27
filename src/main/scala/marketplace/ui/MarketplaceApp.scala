package marketplace.ui

// ai-assisted: #2
// why: initial scaffold generated with an AI assistant; see ai/interaction_log.md entry 2
import scalafx.Includes.handle
import scalafx.Includes.jfxNode2sfx
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, VBox, Region, Priority}
import scalafx.scene.control.{Button, Label, ComboBox}
import scalafx.beans.property.{ObjectProperty, BooleanProperty}
import scalafx.geometry.{Insets, Pos}
import scalafx.collections.ObservableBuffer
import marketplace.*
import marketplace.model.*
import marketplace.persistence.CsvStore
import scalafx.scene.control.{MenuBar, Menu, MenuItem, Alert}
import scalafx.scene.control.Alert.AlertType
import scalafx.application.Platform


/** Entry point. Holds the single source of truth — `appState` — as an
 * ObjectProperty[MarketplaceState]. This is the one deliberate mutable
 * reference in the whole codebase, and it's a ScalaFX binding, which
 * is the explicitly-permitted exception to the "no var" rule (S1-11).
 * Every screen reads from it and calls `updateState` to replace it;
 * nothing mutates a domain object in place.
 */
object MarketplaceApp extends JFXApp3:

  private val listingsPath = "src/main/resources/data/listings.csv"
  private val txnsPath = "src/main/resources/data/transactions.csv"
  private val membersPath = "src/main/resources/data/members.csv"
  private val appVersion = "0.1.0"

  def start(): Unit =
    val loadedListings = CsvStore.loadListings(listingsPath).getOrElse(Nil)
    val loadedTxns      = CsvStore.loadTransactions(txnsPath).getOrElse(Nil)
    val loadedMembers   = CsvStore.loadMembers(membersPath).getOrElse(Nil)

    // Seed initial members if members.csv is empty/missing.
    val finalMembers = if loadedMembers.isEmpty then
      val seeded = List(
        Member(Person.newId(), "Aisyah"),
        Member(Person.newId(), "Ben")
      )
      CsvStore.saveMembers(membersPath, seeded)
      seeded
    else
      loadedMembers

    val demoMembers = Repository(finalMembers)

    // ai-assisted: #12
    // why: makes the moderator role reachable from the UI even when members.csv is populated.
    // Seed one Coordinator so the moderator role is reachable from the
    // UI. Unlike the member seeding above this is unconditional: with a
    // populated members.csv the fallback branch never runs, and the
    // coordinator id stays stable so "Switch user" can offer it.
    val demoCoordinators = Repository(
      List(Coordinator("C001", "Maya", SkillCategory.Repair))
    )

    val appState = ObjectProperty(
      MarketplaceState(
        members = demoMembers,
        listings = Repository(loadedListings),
        transactions = Repository(loadedTxns),
        coordinators = demoCoordinators
      )
    )

    // Whoever is "logged in" for this session — kept separate from
    // appState because it's session/UI concern, not domain data.
    val currentMemberId = ObjectProperty(demoMembers.all.headOption.map(_.id).getOrElse(""))

    def persist(state: MarketplaceState): Unit =
      CsvStore.saveListings(listingsPath, state.listings.all).recover { case e =>
        println(s"Warning: could not save listings — ${e.getMessage}")
      }
      CsvStore.saveTransactions(txnsPath, state.transactions.all).recover { case e =>
        println(s"Warning: could not save transactions — ${e.getMessage}")
      }
      CsvStore.saveMembers(membersPath, state.members.all).recover { case e =>
        println(s"Warning: could not save members — ${e.getMessage}")
      }

    def updateState(f: MarketplaceState => MarketplaceState): Unit =
      val next = f(appState.value)
      appState.value = next
      persist(next)

    // Build each screen once. Reusing these nodes avoids adding duplicate
    // state listeners every time the user navigates through the sidebar.
    // newListingScreen and content are built before myListingsScreen so
    // MyListingsScreen's empty-state button can jump straight to New Listing.
    val browseScreen = BrowseScreen.build(appState, currentMemberId, updateState).delegate
    val newListingScreen = NewListingScreen.build(appState, currentMemberId, updateState).delegate

    val content = ObjectProperty[javafx.scene.Node](browseScreen)

    val myListingsScreen = MyListingsScreen.build(
      appState, currentMemberId, updateState,
      () => content.value = newListingScreen
    ).delegate
    val moderateScreen = ModerateScreen.build(appState, currentMemberId, updateState).delegate
    val walletScreen = WalletScreen.build(appState, currentMemberId).delegate

    def navButton(label: String, screen: javafx.scene.Node): Button =
      new Button(label):
        prefWidth = 160
        styleClass = Seq("nav-button")
        onAction = handle { content.value = screen }

    val spacer = new Region():
      vgrow = Priority.Always

    val loginCombo = new ComboBox[String]():
      prefWidth = 160

    // ai-assisted: #12
    // why: lets the Switch-user dropdown offer Coordinators as first-class sessions.
    def sessionOptions(state: MarketplaceState): List[String] =
      "Logged Out" ::
        state.members.all.map(m => s"${m.displayName} (${m.id})") :::
        state.coordinators.all.map(c => s"${c.displayName} (${c.id})")

    // ai-assisted: #8
    // why: part of the visual redesign — surfaces the active session at the top of the sidebar.
    // Identity card: name + balance pill for whoever is logged in this
    // session. This is the single most-referenced piece of state in the
    // app, so it now lives at the top of the sidebar instead of being
    // buried below the nav buttons.
    val identityName = new Label("Logged Out"):
      styleClass = Seq("identity-name")

    val identityBalance = new Label("—"):
      styleClass = Seq("balance-pill")

    val identityCard = new VBox(6, identityName, identityBalance):
      padding = Insets(12)
      styleClass = Seq("identity-card")
    val syncingSession = BooleanProperty(false)

    def refreshSession(): Unit =
      syncingSession.value = true
      try
        val options = sessionOptions(appState.value)
        loginCombo.items = ObservableBuffer.from(options)

        // A session is either a trading Member or a Coordinator.
        val currentPerson =
          appState.value.members.find(_.id, currentMemberId.value)
            .orElse(appState.value.coordinators.find(_.id, currentMemberId.value))
        val expectedSelection = currentPerson match
          case Some(p) => s"${p.displayName} (${p.id})"
          case None => "Logged Out"

        if options.contains(expectedSelection) then
          loginCombo.value = expectedSelection
        else
          loginCombo.value = "Logged Out"

        currentPerson match
          case Some(m: Member) =>
            identityName.text = m.displayName
            identityBalance.text = s"${m.creditBalance} credit(s)"
          case Some(c: Coordinator) =>
            identityName.text = c.displayName
            identityBalance.text = s"Coordinator · ${c.moderatedCategory}"
          case _ =>
            identityName.text = "Logged Out"
            identityBalance.text = "—"
      finally
        syncingSession.value = false

    refreshSession()
    appState.onChange { (_, _, _) => refreshSession() }
    currentMemberId.onChange { (_, _, _) => refreshSession() }

    loginCombo.onAction = handle {
      if !syncingSession.value then
        val nextId = Option(loginCombo.value.value)
          .filterNot(_ == "Logged Out")
          .map { selected =>
            val idStart = selected.lastIndexOf('(')
            val idEnd = selected.lastIndexOf(')')
            if idStart != -1 && idEnd != -1 then selected.substring(idStart + 1, idEnd) else ""
          }
          .getOrElse("")

        if currentMemberId.value != nextId then
          currentMemberId.value = nextId
    }

    val sidebarTitle = new Label("Skills Marketplace"):
      styleClass = Seq("sidebar-title")

    val switchUserLabel = new Label("Switch user"):
      styleClass = Seq("form-label")

    val switchUserSection = new VBox(6, switchUserLabel, loginCombo):
      padding = Insets(8, 0, 0, 0)
      styleClass = Seq("session-section")

    val sidebar = new VBox(12,
      sidebarTitle,
      identityCard,
      navButton("Browse", browseScreen),
      navButton("My Listings", myListingsScreen),
      navButton("New Listing", newListingScreen),
      navButton("Moderate", moderateScreen),
      navButton("Wallet & History", walletScreen),
      spacer,
      switchUserSection
    ):

      padding = Insets(16)
      alignment = Pos.TopCenter
      styleClass = Seq("sidebar")

    def showAboutDialog(): Unit =
      new Alert(AlertType.Information):
        title = "About"
        headerText = "Community Skills Marketplace"
        contentText =
          s"""Version: $appVersion
             |Author: Sin Shen Nee (23050024)
             |License: Academic project — submitted for PRG2104, Sunway University
             |© 2026 Sin Shen Nee. All rights reserved.""".stripMargin
      .showAndWait()

    def showHelpDialog(): Unit =
      new Alert(AlertType.Information):
        title = "Help"
        headerText = "How to use this app"
        contentText =
          "Use the sidebar to navigate between screens: Browse, My Listings, " +
            "New Listing, Moderate, and Wallet & History. Switch users from the " +
            "dropdown at the bottom of the sidebar."
      .showAndWait()

    val menuBar = new MenuBar:
      styleClass = Seq("app-menu-bar")
      menus = List(
        new Menu("File"):
          items = List(
            new MenuItem("Exit"):
              onAction = handle {
                Platform.exit()
              }
          )
        ,
        new Menu("About"):
          items = List(
            new MenuItem("About Community Skills Marketplace"):
              onAction = handle {
                showAboutDialog()
              }
          )
        ,
        new Menu("Help"):
          items = List(
            new MenuItem("How to use"):
              onAction = handle {
                showHelpDialog()
              }
          )
      )

    val root = new BorderPane:
      left = sidebar
      center = content.value

    val outerRoot = new BorderPane:
      top = menuBar
      center = root



    content.onChange { (_, _, newNode) => root.center = newNode }

    stage = new JFXApp3.PrimaryStage:
      title = "Community Skills Marketplace"
      scene = new Scene(outerRoot, 960, 600):
        stylesheets = List(getClass.getResource("/style.css").toExternalForm)