import { officialRoomSeeds } from "./officialRooms";
import type {
  LouvreClosureNotice,
  LouvreEdge,
  LouvreIndoorBootstrap,
  LouvreLevel,
  LouvreNode,
  LouvreSight,
} from "./types";

export const LOUVRE_MUSEUM_ID = "Q19675" as const;
export const LOUVRE_DATASET_VERSION = "2026.08.18-official-rooms-v2";
export const LOUVRE_CLOSURES_URL = "https://www.louvre.fr/en/visit/list-of-available-galleries";
export const LOUVRE_PLAN_URL = "https://collections.louvre.fr/en/plan-accessible";

export const levels: LouvreLevel[] = [
  { id: "-2", label: "Level -2", subtitle: "Reception area, entrances and visitor exit" },
  { id: "-1", label: "Level -1", subtitle: "Museum and exhibitions" },
  { id: "0", label: "Level 0", subtitle: "Museum" },
  { id: "1", label: "Level 1", subtitle: "Museum and exhibitions" },
  { id: "2", label: "Level 2", subtitle: "Museum" },
];

const node = (
  id: string,
  name: string,
  shortName: string,
  level: LouvreNode["level"],
  wing: LouvreNode["wing"],
  kind: LouvreNode["kind"],
  x: number,
  y: number,
  searchableAliases: string[] = [],
  details: Pick<LouvreNode, "roomNumber" | "officialLocationId" | "sourceUrl"> = {},
): LouvreNode => ({ id, name, shortName, level, wing, kind, x, y, searchableAliases, ...details });

const infrastructureNodes: LouvreNode[] = [
  node("exit-pyramid", "Public visitor exit under the Pyramid", "Visitor exit", "-2", "Pyramid", "visitor_exit", 35, 51, ["exit", "metro", "car park", "pyramid exit"]),
  node("hall-pyramid", "Hall Napoléon under the Pyramid", "Under Pyramid", "-2", "Pyramid", "junction", 50, 51, ["hall napoleon", "reception", "information desk", "tickets"]),
  node("entry-denon", "Denon entrance from the Pyramid", "Denon entrance", "-2", "Denon", "entrance", 55, 76, ["denon entrance"]),
  node("entry-richelieu", "Richelieu entrance from the Pyramid", "Richelieu entrance", "-2", "Richelieu", "entrance", 48, 26, ["richelieu entrance"]),
  node("entry-sully", "Sully entrance from the Pyramid", "Sully entrance", "-2", "Sully", "entrance", 75, 50, ["sully entrance"]),
  node("lift-central-m2", "Central accessible lift, Level -2", "Central lift", "-2", "Pyramid", "lift", 57, 49, ["elevator", "accessible lift"]),

  node("central-m1", "Central visitor junction, Level -1", "Central junction", "-1", "Pyramid", "junction", 52, 51, ["pyramid", "central junction"]),
  node("lift-central-m1", "Central accessible lift, Level -1", "Central lift", "-1", "Pyramid", "lift", 57, 49, ["elevator", "accessible lift"]),
  node("central-0", "Central visitor junction, Level 0", "Central junction", "0", "Pyramid", "junction", 52, 51, ["pyramid", "central junction"]),
  node("lift-central-0", "Central accessible lift, Level 0", "Central lift", "0", "Pyramid", "lift", 57, 49, ["elevator", "accessible lift"]),
  node("central-1", "Central visitor junction, Level 1", "Central junction", "1", "Pyramid", "junction", 52, 51, ["pyramid", "central junction"]),
  node("lift-central-1", "Central accessible lift, Level 1", "Central lift", "1", "Pyramid", "lift", 57, 49, ["elevator", "accessible lift"]),
  node("central-2", "Central visitor junction, Level 2", "Central junction", "2", "Pyramid", "junction", 52, 51, ["pyramid", "central junction"]),
  node("lift-central-2", "Central accessible lift, Level 2", "Central lift", "2", "Pyramid", "lift", 57, 49, ["elevator", "accessible lift"]),
];

const officialRoomNodes: LouvreNode[] = officialRoomSeeds.map((room) =>
  node(
    room.id,
    room.name,
    room.shortName,
    room.level,
    room.wing,
    "room",
    room.x,
    room.y,
    [room.roomNumber, room.roomNumber ? `room ${room.roomNumber}` : "", room.name, room.officialLocationId].filter(Boolean),
    {
      roomNumber: room.roomNumber || undefined,
      officialLocationId: room.officialLocationId,
      sourceUrl: room.sourceUrl,
    },
  ),
);

const supplementalRoomNodes: LouvreNode[] = [
  node(
    "denon-427",
    "Room 427 - Gallery of Five Continents",
    "Room 427",
    "0",
    "Denon",
    "room",
    24,
    81,
    ["room 427", "five continents", "africa", "asia", "oceania", "americas"],
    { roomNumber: "427", sourceUrl: LOUVRE_PLAN_URL },
  ),
];

export const nodes: LouvreNode[] = [...infrastructureNodes, ...officialRoomNodes, ...supplementalRoomNodes];

const edge = (
  from: string,
  to: string,
  distanceMeters: number,
  accessible = true,
  kind: LouvreEdge["kind"] = "walk",
  instruction?: string,
): LouvreEdge => ({ from, to, distanceMeters, accessible, kind, instruction });

const infrastructureEdges: LouvreEdge[] = [
  edge("exit-pyramid", "hall-pyramid", 70),
  edge("hall-pyramid", "lift-central-m2", 22),
  edge("hall-pyramid", "entry-denon", 42),
  edge("hall-pyramid", "entry-richelieu", 42),
  edge("hall-pyramid", "entry-sully", 48),
  edge("entry-denon", "central-m1", 65, false, "escalator", "Use the Denon escalator to Level -1."),
  edge("entry-richelieu", "central-m1", 65, false, "escalator", "Use the Richelieu escalator to Level -1."),
  edge("entry-sully", "central-m1", 65, false, "escalator", "Use the Sully escalator to Level -1."),
  edge("lift-central-m2", "lift-central-m1", 18, true, "lift", "Take the central lift to Level -1."),
  edge("lift-central-m1", "lift-central-0", 18, true, "lift", "Take the central lift to Level 0."),
  edge("lift-central-0", "lift-central-1", 18, true, "lift", "Take the central lift to Level 1."),
  edge("lift-central-1", "lift-central-2", 18, true, "lift", "Take the central lift to Level 2."),
  edge("central-m1", "lift-central-m1", 14),
  edge("central-0", "lift-central-0", 14),
  edge("central-1", "lift-central-1", 14),
  edge("central-2", "lift-central-2", 14),
  edge("central-m1", "central-0", 16, false, "stairs", "Use the main stairs to Level 0."),
  edge("central-0", "central-1", 16, false, "stairs", "Use the main stairs to Level 1."),
  edge("central-1", "central-2", 16, false, "stairs", "Use the main stairs to Level 2."),
];

function mapDistance(left: LouvreNode, right: LouvreNode): number {
  return Math.hypot(left.x - right.x, left.y - right.y);
}
function walkingDistance(left: LouvreNode, right: LouvreNode): number {
  return Math.max(8, Math.round(mapDistance(left, right) * 4.5));
}

function buildRoomEdges(): LouvreEdge[] {
  const result: LouvreEdge[] = [];
  const keys = new Set<string>();
  const add = (left: LouvreNode, right: LouvreNode, instruction?: string) => {
    const key = [left.id, right.id].sort().join("|");
    if (left.id === right.id || keys.has(key)) return;
    keys.add(key);
    result.push(edge(left.id, right.id, walkingDistance(left, right), true, "walk", instruction));
  };

  for (const level of ["-1", "0", "1", "2"] as const) {
    const central = nodes.find((item) => item.id === (level === "-1" ? "central-m1" : `central-${level}`))!;
    for (const wing of ["Denon", "Richelieu", "Sully"] as const) {
      const rooms = officialRoomNodes.filter((item) => item.level === level && item.wing === wing);
      if (rooms.length === 0) continue;

      // A spatial minimum-spanning tree keeps every published room reachable
      // without copying the Louvre's protected floor-plan geometry.
      const connected = new Set<string>([rooms[0]!.id]);
      while (connected.size < rooms.length) {
        let best: { left: LouvreNode; right: LouvreNode; distance: number } | undefined;
        for (const left of rooms) {
          if (!connected.has(left.id)) continue;
          for (const right of rooms) {
            if (connected.has(right.id)) continue;
            const distance = mapDistance(left, right);
            if (!best || distance < best.distance) best = { left, right, distance };
          }
        }
        if (!best) break;
        add(best.left, best.right);
        connected.add(best.right.id);
      }

      // Nearby cross-links make routes less brittle and more closely follow the
      // visible run of adjacent rooms in the original Antiqum schematic.
      for (const room of rooms) {
        rooms
          .filter((candidate) => candidate.id !== room.id)
          .map((candidate) => ({ candidate, distance: mapDistance(room, candidate) }))
          .filter((item) => item.distance <= 7.5)
          .sort((a, b) => a.distance - b.distance)
          .slice(0, 2)
          .forEach(({ candidate }) => add(room, candidate));
      }

      rooms
        .map((room) => ({ room, distance: mapDistance(central, room) }))
        .sort((a, b) => a.distance - b.distance)
        .slice(0, 2)
        .forEach(({ room }) => add(central, room, `Enter the ${wing} wing toward ${room.shortName}.`));
    }
  }

  const gallery427 = supplementalRoomNodes[0]!;
  const nearestDenon = officialRoomNodes
    .filter((item) => item.level === "0" && item.wing === "Denon")
    .sort((a, b) => mapDistance(gallery427, a) - mapDistance(gallery427, b))[0];
  if (nearestDenon) add(gallery427, nearestDenon);
  return result;
}

export const edges: LouvreEdge[] = [...infrastructureEdges, ...buildRoomEdges()];

const roomById = new Map(nodes.map((item) => [item.id, item]));

const sight = (
  mapNumber: number,
  id: string,
  title: string,
  subtitle: string,
  nodeId: string,
  category: string,
  sourceUrl?: string,
): LouvreSight => {
  const room = roomById.get(nodeId);
  if (!room) throw new Error(`Unknown Louvre room for sight ${id}: ${nodeId}`);
  return {
    id,
    mapNumber,
    title,
    subtitle,
    nodeId,
    room: room.roomNumber ?? room.shortName.replace(/^Room\s+/i, ""),
    wing: room.wing,
    level: room.level,
    category,
    sourceUrl: sourceUrl ?? room.sourceUrl ?? LOUVRE_PLAN_URL,
    featured: true,
  };
};

/**
 * Numbered visitor highlights shown on the May 2026 Louvre visitor map, plus
 * four major collection landmarks present on the official interactive plan.
 */
export const sights: LouvreSight[] = [
  sight(1, "mona-lisa", "Mona Lisa", "Leonardo da Vinci", "denon-711", "Painting", "https://www.louvre.fr/en/explore/the-palace/from-the-mona-lisa-to-the-wedding-feast-at-cana"),
  sight(2, "venus-de-milo", "Venus de Milo", "Ancient Greek sculpture", "sully-345", "Sculpture", "https://collections.louvre.fr/ark:/53355/cl010277627"),
  sight(3, "winged-victory", "Winged Victory of Samothrace", "Hellenistic sculpture", "denon-703", "Sculpture"),
  sight(4, "code-hammurabi", "Code of Hammurabi", "Babylonian law stele", "richelieu-227", "Antiquity"),
  sight(5, "great-sphinx", "Great Sphinx of Tanis", "Egyptian antiquity", "sully-338", "Antiquity"),
  sight(6, "liberty-leading", "Liberty Leading the People", "Eugène Delacroix", "denon-700", "Painting"),
  sight(7, "raft-medusa", "The Raft of the Medusa", "Théodore Géricault", "denon-700", "Painting"),
  sight(8, "wedding-feast-at-cana", "The Wedding Feast at Cana", "Paolo Veronese", "denon-711", "Painting"),
  sight(9, "galerie-apollon", "Galerie d'Apollon", "French Crown Jewels", "denon-705", "Palace"),
  sight(10, "dying-slave", "The Dying Slave", "Michelangelo", "denon-403", "Sculpture"),
  sight(11, "five-continents", "Gallery of Five Continents", "Africa, Asia, Oceania and the Americas", "denon-427", "Gallery"),
  sight(12, "palace-darius", "Palace of Darius", "Achaemenid Persia", "sully-308", "Antiquity"),
  sight(13, "tamutnefret", "Tamutnefret's Coffin", "Egyptian antiquity", "sully-321", "Antiquity"),
  sight(14, "seated-scribe", "The Seated Scribe", "Egyptian antiquity", "sully-635", "Antiquity"),
  sight(15, "sarcophagus-spouses", "Sarcophagus of the Spouses", "Etruscan antiquity", "sully-663", "Antiquity"),
  sight(16, "marly-horses", "The Marly Horses", "Guillaume Coustou", "richelieu-102", "Sculpture"),
  sight(17, "cycladic-idol", "Cycladic Idol", "Ancient Greek antiquity", "denon-170", "Antiquity"),
  sight(18, "napoleon-apartments", "Napoléon III Apartments", "Second Empire interiors", "richelieu-544", "Palace"),
  sight(19, "virgin-jeanne", "Virgin of Jeanne d'Evreux", "Gilded silver statuette", "richelieu-503", "Decorative Art"),
  sight(20, "lacemaker", "The Lacemaker", "Johannes Vermeer", "richelieu-837", "Painting"),
  sight(21, "money-changer", "The Money Changer and His Wife", "Quentin Matsys", "richelieu-814", "Painting"),
  sight(22, "francis-i", "Portrait of Francis I", "Jean Clouet", "richelieu-822", "Painting"),
  sight(23, "the-cheat", "The Cheat", "Georges de La Tour", "sully-912", "Painting"),
  sight(24, "the-bather", "The Bather", "Jean-Auguste-Dominique Ingres", "sully-940", "Painting"),
  sight(25, "clubfoot", "The Clubfoot", "Jusepe de Ribera", "denon-718", "Painting"),
  sight(26, "conversation-park", "Conversation in a Park", "Thomas Gainsborough", "denon-713", "Painting"),
  sight(27, "christ-detached-cross", "Christ Detached from the Cross", "French medieval sculpture", "richelieu-201", "Sculpture"),
  sight(28, "statue-ebih-il", "Statue of Ebih-Il", "Mesopotamian antiquity", "richelieu-234", "Antiquity"),
  sight(29, "medieval-louvre", "The Medieval Louvre", "History of the Louvre", "sully-133", "Palace"),
  sight(30, "saint-mary-magdalene", "Saint Mary Magdalene", "European sculpture", "denon-169", "Sculpture"),
];

export const notices: LouvreClosureNotice[] = [
  {
    id: "verify-closures",
    title: "Check today's gallery access",
    detail: "The Louvre warns that some interactive-map information may be out of date. Rooms and lifts can close; confirm this route with current gallery access, museum signs and staff.",
    status: "warning",
    sourceUrl: LOUVRE_CLOSURES_URL,
    checkedAt: "2026-08-18T00:00:00Z",
  },
  {
    id: "exit-final",
    title: "Leaving is final",
    detail: "The Louvre states that re-entry is not permitted after leaving the museum.",
    status: "information",
    sourceUrl: "https://www.louvre.fr/en/visit/hours-admission",
    checkedAt: "2026-08-18T00:00:00Z",
  },
];

export function indoorBootstrap(): LouvreIndoorBootstrap {
  return {
    museumId: LOUVRE_MUSEUM_ID,
    datasetVersion: LOUVRE_DATASET_VERSION,
    generatedAt: "2026-08-18T00:00:00Z",
    levels,
    nodes,
    edges,
    sights,
    notices,
    defaultStartNodeId: "hall-pyramid",
    officialClosuresUrl: LOUVRE_CLOSURES_URL,
    emergencyNotice: "For emergencies, follow illuminated exit signs and Louvre staff instructions. Do not rely on this visitor route.",
    attribution: "Original Antiqum schematic and routing graph. Room names, levels, wings and reviewed map anchors: Musée du Louvre accessible and interactive plans, accessed 18 August 2026. Textual data reused under the Etalab Open Licence. Not an official Louvre application.",
  };
}
