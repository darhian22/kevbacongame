const svgNamespace = "http://www.w3.org/2000/svg";

const elements = {
  routeForm: document.querySelector("#route-form"),
  startActor: document.querySelector("#start-actor"),
  endActor: document.querySelector("#end-actor"),
  actorOptions: document.querySelector("#actor-options"),
  routeSummary: document.querySelector("#route-summary"),
  routeSteps: document.querySelector("#route-steps"),
  filmCards: document.querySelector("#film-cards"),
  mapCount: document.querySelector("#map-count"),
  map: document.querySelector("#network-map"),
  mapDescription: document.querySelector("#map-description"),
  edgeLayer: document.querySelector("#edge-layer"),
  nodeLayer: document.querySelector("#node-layer"),
};

const data = {
  actorsById: new Map(),
  actorIdsByName: new Map(),
  moviesById: new Map(),
  moviesByActor: new Map(),
  actorsByMovie: new Map(),
};

function svgElement(tag, attributes = {}) {
  const element = document.createElementNS(svgNamespace, tag);
  for (const [name, value] of Object.entries(attributes)) {
    element.setAttribute(name, String(value));
  }
  return element;
}

function parsePipeFile(contents, label, fieldCount) {
  return contents
    .split(/\r?\n/)
    .filter((line) => line.trim().length > 0)
    .map((line, index) => {
      const fields = line.split("|");
      if (fields.length < fieldCount || fields.slice(0, fieldCount).some((field) => field.length === 0)) {
        throw new Error(`${label} has an invalid record at line ${index + 1}.`);
      }
      return fields;
    });
}

function addToIndex(index, key, value) {
  const values = index.get(key) ?? [];
  values.push(value);
  index.set(key, values);
}

function repairActorName(name) {
  const codePoints = Array.from(name, (character) => character.codePointAt(0));
  if (!codePoints.some((codePoint) => codePoint >= 0x80 && codePoint <= 0x9f) ||
      codePoints.some((codePoint) => codePoint > 0xff)) {
    return name;
  }

  try {
    const bytes = new Uint8Array(codePoints);
    const repaired = new TextDecoder("utf-8", { fatal: true }).decode(bytes);
    return repaired
      .replaceAll("Á", "ç")
      .replaceAll("¡", "Á")
      .replaceAll("È", "é")
      .replaceAll("Ë", "è")
      .replaceAll("Ì", "í")
      .replaceAll("Û", "ó")
      .replaceAll("Ù", "ú")
      .replaceAll("·", "á")
      .replaceAll("˙", "ú")
      .replaceAll("ˆ", "ö")
      .replaceAll("Ò", "ñ")
      .normalize("NFC");
  } catch {
    return name;
  }
}

function normalizeActorName(name) {
  return repairActorName(name)
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/gi, " ")
    .trim()
    .toLocaleLowerCase();
}

async function fetchDataFile(name) {
  const url = new URL(`../${name}`, window.location.href);
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Could not load ${name} (${response.status}). Run a local web server from the project directory.`);
  }
  return response.text();
}

async function loadDataset() {
  const [actorText, movieText, castText] = await Promise.all([
    fetchDataFile("actors.txt"),
    fetchDataFile("movies.txt"),
    fetchDataFile("movie-actors.txt"),
  ]);

  for (const [actorId, rawActorName] of parsePipeFile(actorText, "actors.txt", 2)) {
    if (data.actorsById.has(actorId)) throw new Error(`actors.txt contains duplicate actor ID ${actorId}.`);
    const actorName = repairActorName(rawActorName);
    data.actorsById.set(actorId, actorName);
    addToIndex(data.actorIdsByName, normalizeActorName(actorName), actorId);
    data.moviesByActor.set(actorId, []);
  }

  for (const [movieId, movieName] of parsePipeFile(movieText, "movies.txt", 2)) {
    if (data.moviesById.has(movieId)) throw new Error(`movies.txt contains duplicate movie ID ${movieId}.`);
    data.moviesById.set(movieId, movieName);
    data.actorsByMovie.set(movieId, []);
  }

  for (const [movieId, actorId] of parsePipeFile(castText, "movie-actors.txt", 2)) {
    const actorName = data.actorsById.get(actorId);
    const movieName = data.moviesById.get(movieId);
    if (!actorName || !movieName) {
      throw new Error(`movie-actors.txt refers to an unknown actor or movie (IDs ${actorId}, ${movieId}).`);
    }
    data.actorsByMovie.get(movieId).push(actorId);
    data.moviesByActor.get(actorId).push(movieId);
  }

  const actorNames = [...data.actorsById.values()].sort((a, b) => a.localeCompare(b));
  const fragment = document.createDocumentFragment();
  for (const name of actorNames) {
    const option = document.createElement("option");
    option.value = name;
    fragment.append(option);
  }
  elements.actorOptions.append(fragment);
  const startId = resolveActor(elements.startActor.value);
  const endId = resolveActor(elements.endActor.value);
  if (startId && endId) showRoute(startId, endId);
}

function resolveActor(name) {
  const actorIds = data.actorIdsByName.get(normalizeActorName(name));
  if (!actorIds?.length) return null;
  return actorIds[0];
}

function shortestPath(startId, endId) {
  if (startId === endId) return { actorIds: [startId], movieIds: [] };

  const queue = [startId];
  const visitedActors = new Set([startId]);
  const parentByActor = new Map();

  for (let cursor = 0; cursor < queue.length; cursor += 1) {
    const actorId = queue[cursor];
    for (const movieId of data.moviesByActor.get(actorId) ?? []) {
      for (const coStarId of data.actorsByMovie.get(movieId) ?? []) {
        if (visitedActors.has(coStarId)) continue;
        visitedActors.add(coStarId);
        parentByActor.set(coStarId, { actorId, movieId });

        if (coStarId === endId) {
          const actorIds = [endId];
          const movieIds = [];
          let currentId = endId;
          while (currentId !== startId) {
            const parent = parentByActor.get(currentId);
            movieIds.push(parent.movieId);
            currentId = parent.actorId;
            actorIds.push(currentId);
          }
          return {
            actorIds: actorIds.reverse(),
            movieIds: movieIds.reverse(),
          };
        }

        queue.push(coStarId);
      }
    }
  }

  return { actorIds: [], movieIds: [] };
}

function setRouteMessage(summary, steps = []) {
  elements.routeSummary.textContent = summary;
  elements.routeSteps.replaceChildren();
  for (const text of steps) {
    const item = document.createElement("li");
    item.textContent = text;
    elements.routeSteps.append(item);
  }
}

function renderFilmCards(route) {
  elements.filmCards.replaceChildren();
  if (!route.movieIds.length) {
    const note = document.createElement("p");
    note.className = "map-hint";
    note.textContent = "Film illustrations appear here when the route includes a shared credit.";
    elements.filmCards.append(note);
    return;
  }

  const palettes = [
    ["#184f51", "#e6be83", "#e47b57"],
    ["#3e315f", "#edbc68", "#d87861"],
    ["#254e6a", "#e7dfbe", "#d16d58"],
    ["#45624c", "#e5c58c", "#ba7158"],
  ];

  route.movieIds.forEach((movieId, index) => {
    const movieName = data.moviesById.get(movieId);
    let colorSeed = 0;
    for (const character of movieName) {
      colorSeed = (colorSeed * 31 + character.charCodeAt(0)) >>> 0;
    }
    const [background, accent, highlight] = palettes[colorSeed % palettes.length];
    const artwork = svgElement("svg", {
      viewBox: "0 0 300 140",
      class: "film-art",
      role: "img",
      "aria-label": `Original abstract title-card illustration for ${movieName}, not official film artwork`,
    });
    artwork.append(
      svgElement("rect", { width: 300, height: 140, fill: background }),
      svgElement("circle", { cx: 223, cy: 39, r: 62, fill: accent, opacity: 0.92 }),
      svgElement("circle", { cx: 249, cy: 36, r: 28, fill: highlight, opacity: 0.9 }),
      svgElement("path", {
        d: "M0 112 C50 76 86 150 146 105 S230 73 300 112 V140 H0 Z",
        fill: highlight,
        opacity: 0.92,
      }),
      svgElement("path", {
        d: "M0 123 C63 96 101 151 168 118 S249 97 300 122",
        fill: "none",
        stroke: accent,
        "stroke-width": 3,
      }),
    );
    const label = svgElement("text", {
      x: 16,
      y: 25,
      fill: "#fffaf0",
      "font-family": "DM Sans, sans-serif",
      "font-size": 10,
      "font-weight": 700,
      "letter-spacing": 2,
    });
    label.textContent = `FILM ${String(index + 1).padStart(2, "0")}`;
    artwork.append(label);

    const card = document.createElement("article");
    card.className = "film-card";
    const copy = document.createElement("div");
    copy.className = "film-card-copy";
    const indexLabel = document.createElement("p");
    indexLabel.className = "film-index";
    indexLabel.textContent = `Shared credit ${index + 1}`;
    const title = document.createElement("p");
    title.className = "film-title";
    title.textContent = movieName;
    const pair = document.createElement("p");
    pair.className = "film-pair";
    pair.textContent = `${data.actorsById.get(route.actorIds[index])} · ${data.actorsById.get(route.actorIds[index + 1])}`;
    copy.append(indexLabel, title, pair);
    card.append(artwork, copy);
    elements.filmCards.append(card);
  });
}

function renderMap(route, startId, endId) {
  elements.edgeLayer.replaceChildren();
  elements.nodeLayer.replaceChildren();

  const { actorIds, movieIds } = route;
  elements.mapCount.textContent = `${data.actorsById.size.toLocaleString()} actors · ${data.moviesById.size.toLocaleString()} films`;

  if (!actorIds.length) {
    elements.map.setAttribute("viewBox", "0 0 900 360");
    elements.map.style.width = "";
    elements.mapDescription.textContent = "No route is available between the selected actors.";
    renderFilmCards(route);
    return;
  }

  const width = Math.max(900, 180 * (actorIds.length - 1) + 200);
  elements.map.setAttribute("viewBox", `0 0 ${width} 360`);
  elements.map.style.width = `${width}px`;
  elements.mapDescription.textContent = `${actorIds.map((id) => data.actorsById.get(id)).join(" connected to ")} by shared films.`;

  const positions = actorIds.map((actorId, index) => ({
    actorId,
    x: 100 + index * 180,
    y: index % 2 === 0 ? 165 : 215,
  }));

  for (let index = 0; index < movieIds.length; index += 1) {
    const from = positions[index];
    const to = positions[index + 1];
    const group = svgElement("g", { "aria-hidden": "true" });
    const line = svgElement("line", {
      x1: from.x,
      y1: from.y,
      x2: to.x,
      y2: to.y,
      class: "edge-line is-path",
    });
    const label = svgElement("text", {
      x: (from.x + to.x) / 2,
      y: (from.y + to.y) / 2 - 14,
      class: "edge-label is-path",
    });
    label.textContent = data.moviesById.get(movieIds[index]);
    group.append(line, label);
    elements.edgeLayer.append(group);
  }

  for (const position of positions) {
    const { actorId, x, y } = position;
    const actorName = data.actorsById.get(actorId);
    const isEndpoint = actorId === startId || actorId === endId;
    const group = svgElement("g", {
      class: "map-node",
      transform: `translate(${x} ${y})`,
      role: "img",
      "aria-label": `${actorName}${isEndpoint ? ", route endpoint" : ""}`,
    });
    const title = svgElement("title");
    title.textContent = actorName;
    const halo = svgElement("circle", { r: 24, class: "node-halo" });
    const dot = svgElement("circle", {
      r: 10,
      class: `node-dot is-path${isEndpoint ? " is-endpoint" : ""}`,
    });
    const name = svgElement("text", { y: 34, class: "node-label" });
    name.textContent = actorName;
    group.append(title, halo, dot, name);
    elements.nodeLayer.append(group);
  }
  renderFilmCards(route);
}

function showRoute(startId, endId) {
  const route = shortestPath(startId, endId);
  const startName = data.actorsById.get(startId);
  const endName = data.actorsById.get(endId);
  if (!route.actorIds.length) {
    setRouteMessage(`No co-star path connects ${startName} and ${endName}.`);
    renderMap(route, startId, endId);
    return;
  }

  if (route.actorIds.length === 1) {
    setRouteMessage(`${startName} is the same actor as the selected destination.`);
  } else {
    const steps = route.movieIds.map((movieId, index) => {
      const first = data.actorsById.get(route.actorIds[index]);
      const second = data.actorsById.get(route.actorIds[index + 1]);
      return `${first} appeared with ${second} in ${data.moviesById.get(movieId)}.`;
    });
    setRouteMessage(`${endName} is ${route.movieIds.length} ${route.movieIds.length === 1 ? "step" : "steps"} from ${startName}.`, steps);
  }
  renderMap(route, startId, endId);
}

elements.routeForm.addEventListener("submit", (event) => {
  event.preventDefault();
  const startName = elements.startActor.value.trim();
  const endName = elements.endActor.value.trim();
  const startId = resolveActor(startName);
  const endId = resolveActor(endName);

  if (!startId || !endId) {
    const unknownName = !startId ? startName : endName;
    setRouteMessage(`No exact actor match for “${unknownName}”. Choose a name from the suggestions.`);
    elements.edgeLayer.replaceChildren();
    elements.nodeLayer.replaceChildren();
    elements.filmCards.replaceChildren();
    return;
  }

  showRoute(startId, endId);
});

loadDataset().catch((error) => {
  elements.routeSummary.textContent = `Dataset error: ${error.message}`;
  elements.mapCount.textContent = "Dataset could not be loaded";
});
