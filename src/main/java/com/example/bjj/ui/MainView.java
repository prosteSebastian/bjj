package com.example.bjj.ui;

import com.example.bjj.model.*;
import com.example.bjj.service.TechniqueService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Route("")
@PageTitle("BJJ Techniques • Positions → Submissions / Escapes / Sweeps / Passes")
public class MainView extends VerticalLayout {

  private final TechniqueService service;

  // Filters / state
  private final ComboBox<Position> position = new ComboBox<>();
  private final MultiSelectComboBox<Category> categories = new MultiSelectComboBox<>();
  private final ComboBox<Belt> belt = new ComboBox<>();
  private final Checkbox giOnly = new Checkbox("Gi only");
  private final Checkbox nogiOnly = new Checkbox("No-Gi only");
  private final ComboBox<String> sort = new ComboBox<>();
  private final TextField search = new TextField();

  // DOM containers
  private final Div cards = new Div();
  private final Div count = new Div();
  private final Div chipsWrap = new Div();
  private final Div container = new Div();

  @Autowired
  public MainView(TechniqueService service){
    this.service = service;

    setPadding(false);
    setSpacing(false);
    setSizeFull();
    addClassName("bjj-root");

    // Header (sticky)
    var header = new Div();
    header.addClassName("bjj-header");

    var left = new HorizontalLayout();
    left.addClassName("bjj-brand");
    left.setPadding(false);
    left.setSpacing(true);
    var cube = new Div();
    cube.addClassName("bjj-cube");
    var title = new H1("BJJ Tech Map");
    title.addClassName("bjj-title");
    left.add(cube, title);

    var toolbar = new HorizontalLayout();
    toolbar.addClassName("bjj-toolbar");
    toolbar.setPadding(false);
    toolbar.setSpacing(true);

    // Search
    var searchWrap = new Div();
    searchWrap.addClassName("bjj-search");
    var searchIcon = new Icon(VaadinIcon.SEARCH);
    searchIcon.addClassName("icon");
    search.setPlaceholder("Search technique, grip, tag…");
    search.getElement().setAttribute("aria-label","Search");
    searchWrap.add(searchIcon, search);

    // Sort
    sort.setItems("relevance","alpha","belt");
    sort.setValue("relevance");
    sort.setWidth("180px");

    // Compact toggle (adds class to container)
    var compactBtn = new Button("Compact", e -> {
      if (container.getClassNames().contains("compact")) {
        container.removeClassName("compact");
      } else {
        container.addClassName("compact");
      }
    });
    compactBtn.addClassName("bjj-compact-btn");

    toolbar.add(searchWrap, sort, compactBtn);

    header.add(left, toolbar);
    add(header);

    // Sticky header shadow on scroll (adds 'scrolled' to body)
    UI.getCurrent().getPage().executeJs(
      "window.addEventListener('scroll',()=>{" +
        "document.body.classList.toggle('scrolled', window.scrollY>0);" +
      "});"
    );

    // Main layout (sidebar + results)
    container.addClassName("bjj-container");
    var grid = new Div();
    grid.addClassName("bjj-grid");
    container.add(grid);
    add(container);
    expand(container);

    /* ---------- Sidebar (filters) ---------- */

    var sidebar = new Div();
    sidebar.addClassName("bjj-panel");
    sidebar.addClassName("bjj-filters");

    // Position
    sidebar.add(h2("Position"));
    position.setPlaceholder("All positions");
    position.setItems(Position.values());
    position.setWidthFull();
    sidebar.add(position);

    // Category chips (+ Reset that looks different)
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

    // Context
    sidebar.add(h2("Context"));
    sidebar.add(nogiOnly, giOnly);

    // Belt
    sidebar.add(h2("Belt level"));
    belt.setPlaceholder("All");
    belt.setItems(Belt.values());
    belt.setWidthFull();
    sidebar.add(belt);

    /* ---------- Results panel ---------- */

    var results = new Div();
    results.addClassName("bjj-panel");

    var headerRow = new HorizontalLayout();
    headerRow.addClassName("bjj-results-header");
    count.addClassName("bjj-count");
    var clearBtn = new Button("Clear filters", e -> clearFilters());
    clearBtn.addClassName("bjj-clear-btn");
    headerRow.setWidthFull();
    headerRow.add(count);
    headerRow.add(clearBtn);
    headerRow.expand(count);

    cards.addClassName("bjj-cards");
    results.add(headerRow, cards);

    // Append to grid
    grid.add(sidebar, results);

    // Hidden holder for categories (state only)
    categories.setItems(Category.values());

    /* ---------- Events ---------- */
    position.addValueChangeListener(e -> refresh());
    belt.addValueChangeListener(e -> refresh());
    giOnly.addValueChangeListener(e -> { if (giOnly.getValue()) nogiOnly.setValue(false); refresh(); });
    nogiOnly.addValueChangeListener(e -> { if (nogiOnly.getValue()) giOnly.setValue(false); refresh(); });
    sort.addValueChangeListener(e -> refresh());
    search.addValueChangeListener(e -> refresh());

    /* ---------- Initial load ---------- */
    refresh();
  }

  /* ---------- Rendering ---------- */

  private void refresh(){
    Position p = position.getValue();
    Set<Category> cats = categories.getSelectedItems();
    Belt max = belt.getValue();
    Boolean gi = giOnly.getValue();
    Boolean nogi = nogiOnly.getValue();
    String q = Optional.ofNullable(search.getValue()).orElse("");
    String s = sort.getValue();

    // If no categories are selected, show nothing
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

    var list = service.find(p, cats, max, gi, nogi, q, s);
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
    var card = new Div(); card.addClassName("bjj-card");

    var thumb = new Div(); thumb.addClassName("bjj-thumb");
    if (t.getThumbnailUrl() != null && !t.getThumbnailUrl().isBlank()){
      thumb.getStyle().set("backgroundImage","url(" + t.getThumbnailUrl() + ")");
      thumb.getStyle().set("backgroundSize","cover");
      thumb.getStyle().set("backgroundPosition","center");
    } else {
      thumb.setText("No thumbnail");
    }

    var body = new Div(); body.addClassName("bjj-body");
    var title = new H3(Optional.ofNullable(t.getName()).orElse("Untitled")); title.addClassName("bjj-title2");
    body.add(title);

    var meta = new Div(); meta.addClassName("bjj-meta");
    if (t.getPosition()!=null) meta.add(badge(formatEnum(t.getPosition().name()), true));
    if (t.getCategory()!=null) meta.add(badge(formatEnum(t.getCategory().name()), true));
    if (t.getBelt()!=null)     meta.add(badge(formatEnum(t.getBelt().name()) + " belt", true));
    if (t.getRuleset()!=null)  meta.add(badge(
      t.getRuleset()==Ruleset.BOTH ? "Both" :
      (t.getRuleset()==Ruleset.GI ? "Gi" :
      (t.getRuleset()==Ruleset.NOGI ? "No-Gi" : formatEnum(t.getRuleset().name()))), true));
    body.add(meta);

    var desc = new Paragraph(Optional.ofNullable(t.getDescription()).orElse(""));
    desc.addClassName("bjj-desc");
    body.add(desc);

    var actions = new Div();
    actions.addClassName("bjj-actions");
    if (t.getVideoUrl()!=null && !t.getVideoUrl().isBlank()){
      var a = new Anchor(t.getVideoUrl(), "Watch video");
      a.setTarget("_blank");
      a.getElement().setAttribute("rel","noopener noreferrer");
      a.addClassName("bjj-btn");
      actions.add(a);
    }
    body.add(actions);

    card.add(thumb, body);
    cards.add(card);
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
    giOnly.setValue(false);
    nogiOnly.setValue(false);
    sort.setValue("relevance");
    search.clear();
    // unselect visual chips
    chipsWrap.getChildren().forEach(c -> c.getElement().getClassList().remove("active"));
    refresh();
  }
}
