package com.example.bjj.ui;

import com.example.bjj.model.*;
import com.example.bjj.service.TechniqueService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.*;
import java.util.stream.Collectors;

@Route("")
@PageTitle("BJJ Techniques • Positions → Submissions / Escapes / Sweeps / Passes")
public class MainView extends VerticalLayout {

  private final TechniqueService service;

  // Filters / state
  private final Select<Position> position = new Select<>();
  private final MultiSelectComboBox<Category> categories = new MultiSelectComboBox<>();
  private final Select<Belt> belt = new Select<>();
  private final RadioButtonGroup<String> context = new RadioButtonGroup<>();
  private final ComboBoxLike sort = new ComboBoxLike(); // lightweight non-editable select for sort
  private final TextField search = new TextField();

  // DOM containers
  private final Div cards = new Div();
  private final Div count = new Div();
  private final Div chipsWrap = new Div();
  private final Div container = new Div();

  // Learned + consent (kept from previous version)
  private final Set<Long> learned = new HashSet<>();
  private static final String LEARNED_COOKIE = "bjjLearned";
  private static final String CONSENT_COOKIE = "bjjConsent";
  private boolean consentGiven = false;
  private final Div cookieBar = new Div();

  public MainView(TechniqueService service){
    this.service = service;

    setPadding(false);
    setSpacing(false);
    setSizeFull();
    addClassName("bjj-root");

    /* ---------- Header ---------- */
    var header = new Div();
    header.addClassName("bjj-header");

    var left = new HorizontalLayout();
    left.addClassName("bjj-brand");
    left.setPadding(false);
    left.setSpacing(true);
    var cube = new Div(); cube.addClassName("bjj-cube");
    var title = new H1("BJJ Tech Map"); title.addClassName("bjj-title");
    left.add(cube, title);

    var toolbar = new HorizontalLayout();
    toolbar.addClassName("bjj-toolbar");
    toolbar.setPadding(false);
    toolbar.setSpacing(true);

    // Search
    var searchWrap = new Div(); searchWrap.addClassName("bjj-search");
    var searchIcon = VaadinIcon.SEARCH.create(); searchIcon.addClassName("icon");
    search.setPlaceholder("Search technique, grip, tag…");
    search.getElement().setAttribute("aria-label","Search");
    searchWrap.add(searchIcon, search);

    // Sort (simple select-like)
    sort.setItems("relevance","alpha","belt");
    sort.setValue("relevance");
    sort.setWidth("180px");

    // Compact toggle
    var compactBtn = new Button("Compact", e -> {
      if (container.getClassNames().contains("compact")) container.removeClassName("compact");
      else container.addClassName("compact");
    });
    compactBtn.addClassName("bjj-compact-btn");

    toolbar.add(searchWrap, sort, compactBtn);
    header.add(left, toolbar);
    add(header);

    UI.getCurrent().getPage().executeJs(
      "window.addEventListener('scroll',()=>{document.body.classList.toggle('scrolled', window.scrollY>0);});"
    );

    /* ---------- Main grid ---------- */
    container.addClassName("bjj-container");
    var grid = new Div(); grid.addClassName("bjj-grid");
    container.add(grid);
    add(container);
    expand(container);

    /* ---------- Sidebar (filters) ---------- */
    var sidebar = new Div();
    sidebar.addClassName("bjj-panel");
    sidebar.addClassName("bjj-filters");

    // Position (Select = non-editable)
    sidebar.add(h2("Position"));
    position.setWidthFull();
    position.setItems(Position.values());
    position.setPlaceholder("All positions");
    position.setEmptySelectionAllowed(true);
    sidebar.add(position);

    // Category chips
    sidebar.add(h2("Category"));
    chipsWrap.addClassName("bjj-chips");
    for (var c : Category.values()){
      var chip = new Button(formatEnum(c.name()));
      chip.addClassName("bjj-chip");
      chip.addClickListener(e -> {
        var sel = new HashSet<>(categories.getSelectedItems());
        if (sel.contains(c)) { sel.remove(c); chip.removeClassName("active"); }
        else { sel.add(c); chip.addClassName("active"); }
        categories.setValue(sel);
        refresh();
      });
      chipsWrap.add(chip);
    }
    var resetBtn = new Button("Reset", e -> {
      categories.clear();
      chipsWrap.getChildren().forEach(c -> c.getElement().getClassList().remove("active"));
      refresh();
    });
    resetBtn.addClassName("bjj-reset-btn");
    chipsWrap.add(resetBtn);
    sidebar.add(chipsWrap);

    // Context → Radio: Any / Gi / No-Gi / Both
    sidebar.add(h2("Context"));
    context.setItems("Gi", "No-Gi", "Both");
    //context.setValue("Any");
    context.addClassName("bjj-context-group");
    sidebar.add(context);

    // Belt (Select with colored items)
    sidebar.add(h2("Belt level"));
    belt.setWidthFull();
    belt.setItems(Belt.values());
    belt.setPlaceholder("All");
    belt.setEmptySelectionAllowed(true);
    belt.setRenderer(new ComponentRenderer<>(b -> {
      var row = new Div();
      row.getStyle().set("display","flex").set("alignItems","center").set("gap","8px");
      var dot = new Div();
      dot.getStyle().set("width","10px").set("height","10px").set("borderRadius","50%")
          .set("background", beltHex(b));
      var label = new Div(new Text(capitalize(formatEnum(b.name()))));
      row.add(dot, label);
      return row;
    }));
    sidebar.add(belt);

    /* ---------- Results panel ---------- */
    var results = new Div(); results.addClassName("bjj-panel");
    var headerRow = new HorizontalLayout(); headerRow.addClassName("bjj-results-header");
    count.addClassName("bjj-count");
    var clearBtn = new Button("Clear filters", e -> clearFilters()); clearBtn.addClassName("bjj-clear-btn");
    headerRow.setWidthFull(); headerRow.add(count); headerRow.add(clearBtn); headerRow.expand(count);
    cards.addClassName("bjj-cards");
    results.add(headerRow, cards);

    grid.add(sidebar, results);

    // Hidden state holder
    categories.setItems(Category.values());

    /* ---------- Events ---------- */
    position.addValueChangeListener(e -> refresh());
    belt.addValueChangeListener(e -> refresh());
    context.addValueChangeListener(e -> refresh());
    sort.addValueChangeListener(e -> refresh());
    search.addValueChangeListener(e -> refresh());

    /* ---------- Cookie bar (from previous step) ---------- */
    buildCookieBar();

    refresh();
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    injectMinimalCss();
    checkConsentAndInit();
  }

  /* ---------------- Cookie consent helpers (unchanged logic) ---------------- */

  private void buildCookieBar(){
    cookieBar.addClassName("bjj-cookiebar");
    cookieBar.getStyle().set("display", "none");

    var text = new Paragraph(
      "We use a small cookie to remember techniques you mark as “learned”. No trackers."
    );
    text.getStyle().set("margin","0");

    var learnMore = new Anchor("/cookies", "Learn more");
    learnMore.getElement().setAttribute("target","_self");
    learnMore.getStyle().set("marginRight","12px");

    var accept = new Button("Accept", e -> {
      consentGiven = true;
      setCookie(CONSENT_COOKIE, "1", 60*60*24*365*2);
      cookieBar.getStyle().set("display","none");
      saveLearnedCookie();
      toast("Thanks! Your progress will be kept on this device.", true);
    });
    var decline = new Button("Decline", e -> {
      consentGiven = false;
      setCookie(CONSENT_COOKIE, "0", 60*60*24*365*2);
      cookieBar.getStyle().set("display","none");
      setCookie(LEARNED_COOKIE, "", 0);
      toast("Okay — progress kept only for this session.", false);
    });

    var right = new HorizontalLayout(learnMore, accept, decline);
    right.setPadding(false); right.setSpacing(true);
    right.addClassName("buttons");  
    right.getStyle().set("marginLeft","auto");

    cookieBar.add(text, right);
    add(cookieBar);
  }

  private void checkConsentAndInit(){
    UI.getCurrent().getPage().executeJs(
      "const m=document.cookie.match(/(?:^|; )"+CONSENT_COOKIE+"=([^;]*)/); return m?decodeURIComponent(m[1]):'';")
      .then(String.class, val -> {
        consentGiven = "1".equals(val);
        if (val == null || val.isBlank()){
          cookieBar.getStyle().set("display","flex");
        }
        if (consentGiven) loadLearnedFromCookie();
      });
  }

  private void setCookie(String key, String value, int maxAgeSeconds){
    UI.getCurrent().getPage().executeJs(
      "document.cookie = $0 + '=' + encodeURIComponent($1) + '; path=/; max-age=' + $2",
      key, value, maxAgeSeconds
    );
  }

  private void toast(String msg, boolean success){
    var n = Notification.show(msg, 2500, Notification.Position.BOTTOM_CENTER);
    if (success) n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    else n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
  }

  /* ---------------- Rendering ---------------- */

  private void refresh(){
    Position p = position.getValue();
    Set<Category> cats = categories.getSelectedItems();
    Belt max = belt.getValue();

    boolean giOnly = false, nogiOnly = false, bothOnly = false;
    String ctx = context.getValue();
    if ("Gi".equals(ctx)) giOnly = true;
    else if ("No-Gi".equals(ctx)) nogiOnly = true;
    else if ("Both".equals(ctx)) bothOnly = true;

    String q = Optional.ofNullable(search.getValue()).orElse("");
    String s = sort.getValue();

    if (cats == null || cats.isEmpty()) {
      count.setText("0 techniques");
      cards.removeAll();
      var empty = new Div(); empty.addClassName("bjj-card");
      var body = new Div(); body.addClassName("bjj-body");
      body.add(new H3("No results"));
      var p1 = new Paragraph("Select at least one category to see techniques.");
      p1.addClassName("bjj-desc");
      body.add(p1);
      empty.add(body);
      cards.add(empty);
      return;
    }

    var list = service.find(p, cats, max, giOnly, nogiOnly, q, s);

    // If “Both” selected, keep only BOTH ruleset
    if (bothOnly) {
      list = list.stream()
        .filter(t -> t.getRuleset() == Ruleset.BOTH)
        .collect(Collectors.toList());
    }

    count.setText(list.size() + " techniques");

    cards.removeAll();
    if (list.isEmpty()){
      var empty = new Div(); empty.addClassName("bjj-card");
      var body = new Div(); body.addClassName("bjj-body");
      body.add(new H3("No results"));
      var p1 = new Paragraph("Try changing position/category or clearing filters.");
      p1.addClassName("bjj-desc");
      body.add(p1);
      empty.add(body);
      cards.add(empty);
      return;
    }

    list.forEach(this::addCard);
  }

  private void addCard(Technique t){
  var card = new Div(); 
  card.addClassName("bjj-card");

  // Guard against null Technique or null id
  if (t == null) return;

  var learnedBtn = new Button();
  learnedBtn.addClassName("bjj-learned-btn");
  setLearnedIcon(learnedBtn, t.getId()!=null && learned.contains(t.getId()));
  learnedBtn.addClickListener(e -> {
    if (t.getId() != null) {
      toggleLearned(t.getId());
      setLearnedIcon(learnedBtn, learned.contains(t.getId()));
      if (learned.contains(t.getId())) card.addClassName("is-learned"); 
      else card.removeClassName("is-learned");
      if (!consentGiven) toast("Progress kept for this session only (declined cookies).", false);
    }
  });

  var thumb = new Div(); 
  thumb.addClassName("bjj-thumb");
  if (t.getThumbnailUrl()!=null && !t.getThumbnailUrl().isBlank()){
    thumb.getStyle().set("backgroundImage","url(" + t.getThumbnailUrl() + ")");
    thumb.getStyle().set("backgroundSize","cover");
    thumb.getStyle().set("backgroundPosition","center");
  } else {
    thumb.setText("No thumbnail");
  }

  var body = new Div(); 
  body.addClassName("bjj-body");

  var titleText = (t.getName()==null || t.getName().isBlank()) ? "Untitled technique" : t.getName();
  var title = new H3(titleText); 
  title.addClassName("bjj-title2");
  body.add(title);

  var meta = new Div(); 
  meta.addClassName("bjj-meta");

  if (t.getPosition() != null) {
    meta.add(badge(formatEnum(t.getPosition().name()), true));
  }
  if (t.getCategory() != null) {
    meta.add(badge(formatEnum(t.getCategory().name()), true));
  }
  if (t.getBelt() != null) {
    meta.add(beltBadge(t.getBelt()));
  }
  if (t.getRuleset() != null) {
    String rs = switch (t.getRuleset()) {
      case BOTH -> "Both";
      case GI -> "Gi";
      case NOGI -> "No-Gi";
    };
    meta.add(badge(rs, true));
  }
  body.add(meta);

  var descText = (t.getDescription()==null || t.getDescription().isBlank()) ? "—" : t.getDescription();
  var desc = new Paragraph(descText); 
  desc.addClassName("bjj-desc");
  body.add(desc);

  var actions = new Div(); 
  actions.addClassName("bjj-actions");
  actions.add(learnedBtn);
  if (t.getVideoUrl()!=null && !t.getVideoUrl().isBlank()){
    var a = new Anchor(t.getVideoUrl(), "Watch video");
    a.setTarget("_blank"); 
    a.getElement().setAttribute("rel","noopener noreferrer");
    a.addClassName("bjj-btn"); 
    actions.add(a);
  }
  body.add(actions);

  if (t.getId()!=null && learned.contains(t.getId())) {
    card.addClassName("is-learned");
  }

  card.add(thumb, body);
  cards.add(card);
}


  private void setLearnedIcon(Button btn, boolean isLearned){
    btn.setIcon(isLearned ? VaadinIcon.CHECK_SQUARE.create() : VaadinIcon.SQUARE_SHADOW.create());
    btn.getElement().setProperty("title", isLearned ? "Marked learned (click to unmark)" : "Mark as learned");
    btn.getStyle().set("border","1px solid #2b3b56");
    btn.getStyle().set("background","#0b1220");
    btn.getStyle().set("color", isLearned ? "#6fe28a" : "#b2c4da");
    btn.getStyle().set("padding","4px 8px");
    btn.getStyle().set("borderRadius","8px");
  }

  private void toggleLearned(Long id){
    if (id == null) return;
    if (learned.contains(id)) learned.remove(id); else learned.add(id);
    if (consentGiven) saveLearnedCookie();
  }

  private void saveLearnedCookie(){
    String value = learned.stream().map(String::valueOf).collect(Collectors.joining(","));
    setCookie(LEARNED_COOKIE, value, 60*60*24*365*5);
  }

  private void loadLearnedFromCookie(){
    UI.getCurrent().getPage().executeJs(
      "const m=document.cookie.match(/(?:^|; )"+LEARNED_COOKIE+"=([^;]*)/); return m?decodeURIComponent(m[1]):'';")
      .then(String.class, cookieVal -> {
        learned.clear();
        if (cookieVal != null && !cookieVal.isBlank()){
          for (String s : cookieVal.split(",")) {
            try { learned.add(Long.parseLong(s.trim())); } catch (Exception ignored) {}
          }
        }
        refresh();
      });
  }

  private void injectMinimalCss(){
  UI.getCurrent().getPage().executeJs("""
    (function(){
      const css = `
        :root { --pad: 16px; }

        /* Cards */
        .bjj-card.is-learned { outline: 2px solid #2e824a; box-shadow: 0 0 0 2px #2e824a22 inset; }
        .bjj-learned-btn { margin-right: 8px; }

        /* Cookie bar */
        .bjj-cookiebar {
          position: fixed; left: var(--pad); right: var(--pad);
          bottom: calc(12px + env(safe-area-inset-bottom, 0));
          z-index: 2000; display: flex; align-items: center; gap: 12px;
          padding: 12px 16px; background: #0b1220; border: 1px solid #2b3b56; border-radius: 12px;
          box-shadow: 0 8px 30px #0006;
        }
        .bjj-cookiebar a { color: #7fb4ff; text-decoration: none; }
        .bjj-cookiebar vaadin-button { --lumo-border-radius-m: 10px; }

        /* Filter chips */
        .bjj-chips { display: flex; flex-wrap: wrap; gap: 8px; }
        .bjj-chip { border-radius: 999px; }
        .bjj-chip.active { outline: 2px solid #37527a; background: #0c1422; }

        /* Results header tweaks */
        .bjj-results-header { align-items: center; gap: 8px; }
        .bjj-clear-btn { margin-left: auto; }

        /* RADIO: better label color */
        .bjj-context-group vaadin-radio-button::part(label) { color: #c0d0e6; }

        /* --------- MOBILE --------- */
        @media (max-width: 520px){
          .bjj-header { position: sticky; top: 0; z-index: 1000;
                        background: #0a1020; border-bottom: 1px solid #14223a; }
          .bjj-toolbar { flex-wrap: wrap; gap: 8px; }
          .bjj-search { flex: 1 1 100%; }
          .bjj-search .icon { margin-right: 6px; }

          /* Panels stack and reduce inner paddings */
          .bjj-panel { padding: 12px !important; border-radius: 12px; }
          .bjj-container { padding: 12px !important; }

          /* Filters: keep controls full width */
          .bjj-filters vaadin-select,
          .bjj-filters vaadin-multi-select-combo-box,
          .bjj-filters vaadin-text-field { width: 100% !important; }

          /* Chips: avoid overflowing the viewport */
          .bjj-chips { gap: 6px; }
          .bjj-chips { margin-right: -8px; padding-right: 8px; overflow-x: auto; }
          .bjj-chip { white-space: nowrap; }

          /* Radio group wraps nicely */
          .bjj-context-group { display: flex; flex-wrap: wrap; gap: 10px; }

          /* Cards tighten a bit */
          .bjj-cards .bjj-card { border-radius: 12px; }
          .bjj-body .bjj-meta { row-gap: 6px; }

          /* Cookie bar becomes column, buttons fill width */
          .bjj-cookiebar { flex-direction: column; align-items: stretch; gap: 10px; }
          .bjj-cookiebar p { margin: 0; font-size: 14px; line-height: 1.3; }
          .bjj-cookiebar .buttons { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
          .bjj-cookiebar .buttons vaadin-button { width: 100%; }
        }
      `;
      const s=document.createElement('style'); s.textContent=css; document.head.appendChild(s);
    })();
  """);
}


  private Div beltBadge(Belt b){
    var s = badge(capitalize(formatEnum(b.name())) + " belt", true);
    s.getStyle().set("borderColor", beltHex(b));
    s.getStyle().set("color", beltHex(b));
    s.getStyle().set("background", "#0c1422");
    return s;
  }

  private static String beltHex(Belt b){
    if (b == null) return "#b2c4da";
    switch (b){
      case WHITE:  return "#e5e7eb";
      case BLUE:   return "#60a5fa";
      case PURPLE: return "#a78bfa";
      case BROWN:  return "#c08457";
      case BLACK:  return "#111827";
      default:     return "#b2c4da";
    }
  }

  private Div badge(String text, boolean small){
    var s = new Div();
    s.setText(capitalize(text));
    s.getElement().getThemeList().add("badge");
    s.getStyle().set("font-size","12px");
    s.getStyle().set("color","#b2c4da");
    s.getStyle().set("background","#0c1422");
    s.getStyle().set("border","1px solid #23324a");
    s.getStyle().set("borderRadius","8px");
    s.getStyle().set("padding","4px 8px");
    return s;
  }

  private static String capitalize(String s){
    if (s==null || s.isBlank()) return s;
    return s.substring(0,1).toUpperCase() + s.substring(1);
  }

  private static String formatEnum(String name){
    return name.toLowerCase().replace('_',' ');
  }

  private H2 h2(String text){
    var h = new H2(text);
    h.getElement().getThemeList().add("small");
    h.addClassName("bjj-h2");
    return h;
  }

  private void clearFilters(){
    position.clear();
    categories.clear();
    belt.clear();
    context.setValue("Both");
    sort.setValue("relevance");
    search.clear();
    chipsWrap.getChildren().forEach(c -> c.getElement().getClassList().remove("active"));
    refresh();
  }

  /* ---------- tiny non-editable select utility for sort ---------- */
  private static class ComboBoxLike extends Select<String> {
    ComboBoxLike(){
      setEmptySelectionAllowed(false);
      setRenderer(new ComponentRenderer<>(txt -> {
        var d = new Div(); d.setText(txt); return d;
      }));
    }
  }
}
