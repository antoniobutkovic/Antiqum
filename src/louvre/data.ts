import type {
  LouvreClosureNotice,
  LouvreEdge,
  LouvreIndoorBootstrap,
  LouvreLevel,
  LouvreNode,
  LouvreSight,
} from "./types";

export const LOUVRE_MUSEUM_ID = "Q19675" as const;
export const LOUVRE_DATASET_VERSION = "2026.08.17-schematic-v1";
export const LOUVRE_CLOSURES_URL = "https://www.louvre.fr/en/visit/list-of-available-galleries";

export const levels: LouvreLevel[] = [
  { id: "-2", label: "Level -2", subtitle: "Reception area and visitor exit" },
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
): LouvreNode => ({ id, name, shortName, level, wing, kind, x, y, searchableAliases });

export const nodes: LouvreNode[] = [
  node("exit-pyramid", "Public visitor exit under the Pyramid", "Visitor exit", "-2", "Pyramid", "visitor_exit", 25, 50, ["exit", "metro", "car park"]),
  node("hall-pyramid", "Hall Napoléon under the Pyramid", "Under Pyramid", "-2", "Pyramid", "junction", 50, 50, ["hall napoleon", "reception", "information desk"]),
  node("entry-denon", "Denon entrance from the Pyramid", "Denon entrance", "-2", "Denon", "entrance", 48, 77),
  node("entry-richelieu", "Richelieu entrance from the Pyramid", "Richelieu entrance", "-2", "Richelieu", "entrance", 48, 22),
  node("entry-sully", "Sully entrance from the Pyramid", "Sully entrance", "-2", "Sully", "entrance", 76, 50),
  node("lift-central-m2", "Central accessible lift, Level -2", "Central lift", "-2", "Pyramid", "lift", 58, 48, ["elevator"]),

  node("central-m1", "Central junction, Level -1", "Central junction", "-1", "Pyramid", "junction", 51, 50),
  node("lift-central-m1", "Central accessible lift, Level -1", "Central lift", "-1", "Pyramid", "lift", 58, 48, ["elevator"]),
  node("denon-160", "Room 160 - Galerie Donatello", "Room 160", "-1", "Denon", "room", 29, 72, ["galerie donatello", "cycladic idol"]),
  node("denon-170", "Room 170 - Greek Antiquities", "Room 170", "-1", "Denon", "room", 45, 72, ["greek antiquities"]),
  node("richelieu-100", "Room 100 - Cour Marly south", "Room 100", "-1", "Richelieu", "room", 27, 28, ["cour marly"]),
  node("richelieu-102", "Room 102 - Cour Marly", "Room 102", "-1", "Richelieu", "room", 39, 28, ["marly horses"]),
  node("sully-130", "Room 130 - Rotonde Sully", "Room 130", "-1", "Sully", "room", 72, 56, ["medieval louvre"]),
  node("sully-133", "Room 133 - Medieval Louvre", "Room 133", "-1", "Sully", "room", 84, 58, ["fosses", "history of the louvre"]),

  node("central-0", "Central junction, Level 0", "Central junction", "0", "Pyramid", "junction", 51, 50),
  node("lift-central-0", "Central accessible lift, Level 0", "Central lift", "0", "Pyramid", "lift", 58, 48, ["elevator"]),
  node("richelieu-200", "Room 200 - French Sculptures", "Room 200", "0", "Richelieu", "room", 27, 27),
  node("richelieu-227", "Room 227 - Code of Hammurabi", "Room 227", "0", "Richelieu", "room", 43, 27, ["hammurabi", "mesopotamia"]),
  node("richelieu-230", "Room 230 - Mesopotamia", "Room 230", "0", "Richelieu", "room", 53, 27, ["near eastern antiquities"]),
  node("sully-300", "Room 300 - Near Eastern Antiquities", "Room 300", "0", "Sully", "room", 69, 35),
  node("sully-308", "Room 308 - Palace of Darius", "Room 308", "0", "Sully", "room", 82, 35, ["darius", "susa"]),
  node("sully-323", "Room 323 - Egyptian Antiquities", "Room 323", "0", "Sully", "room", 84, 50, ["tamutnefret", "osiris"]),
  node("sully-338", "Room 338 - Crypt of the Sphinx", "Room 338", "0", "Sully", "room", 73, 55, ["great sphinx", "sphinx"]),
  node("sully-345", "Room 345 - Venus de Milo", "Room 345", "0", "Sully", "room", 65, 64, ["venus", "aphrodite"]),
  node("denon-400", "Room 400 - European Sculptures", "Room 400", "0", "Denon", "room", 29, 68, ["sculptures"]),
  node("denon-403", "Room 403 - Galerie Michel-Ange", "Room 403", "0", "Denon", "room", 40, 68, ["michelangelo", "dying slave"]),
  node("denon-406", "Room 406 - Galerie Daru", "Room 406", "0", "Denon", "room", 51, 68, ["daru"]),
  node("denon-427", "Room 427 - Gallery of Five Continents", "Room 427", "0", "Denon", "room", 24, 81, ["africa", "asia", "oceania", "americas"]),

  node("central-1", "Central junction, Level 1", "Central junction", "1", "Pyramid", "junction", 51, 50),
  node("lift-central-1", "Central accessible lift, Level 1", "Central lift", "1", "Pyramid", "lift", 58, 48, ["elevator"]),
  node("denon-700", "Room 700 - French Romanticism", "Room 700", "1", "Denon", "room", 28, 69, ["liberty leading the people", "raft of the medusa"]),
  node("denon-703", "Room 703 - Winged Victory landing", "Room 703", "1", "Denon", "room", 43, 66, ["samothrace", "winged victory"]),
  node("denon-705", "Room 705 - Galerie d'Apollon", "Room 705", "1", "Denon", "room", 55, 66, ["apollo gallery", "crown jewels"]),
  node("denon-710", "Room 710 - Grande Galerie", "Room 710", "1", "Denon", "room", 45, 77, ["italian paintings"]),
  node("denon-711", "Room 711 - Salle des États", "Room 711", "1", "Denon", "room", 37, 77, ["mona lisa", "joconde", "wedding feast at cana", "salle des etats"]),
  node("denon-712", "Room 712 - Grande Galerie", "Room 712", "1", "Denon", "room", 28, 77, ["italian paintings"]),
  node("denon-718", "Room 718 - Spanish Paintings", "Room 718", "1", "Denon", "room", 17, 77, ["clubfoot"]),
  node("richelieu-500", "Room 500 - Decorative Arts", "Room 500", "1", "Richelieu", "room", 30, 28, ["decorative arts"]),
  node("richelieu-503", "Room 503 - Virgin of Jeanne d'Evreux", "Room 503", "1", "Richelieu", "room", 40, 28, ["jeanne d'evreux"]),
  node("richelieu-544", "Room 544 - Napoléon III Apartments", "Room 544", "1", "Richelieu", "room", 22, 20, ["napoleon apartments"]),
  node("sully-600", "Room 600 - Salle de la Chapelle", "Room 600", "1", "Sully", "room", 67, 35, ["chapelle"]),
  node("sully-635", "Room 635 - Seated Scribe", "Room 635", "1", "Sully", "room", 84, 40, ["seated scribe", "egyptian antiquities"]),
  node("sully-663", "Room 663 - Sarcophagus of the Spouses", "Room 663", "1", "Sully", "room", 70, 61, ["etruscan", "sarcophagus"]),

  node("central-2", "Central junction, Level 2", "Central junction", "2", "Pyramid", "junction", 51, 50),
  node("lift-central-2", "Central accessible lift, Level 2", "Central lift", "2", "Pyramid", "lift", 58, 48, ["elevator"]),
  node("richelieu-800", "Room 800 - Northern European Paintings", "Room 800", "2", "Richelieu", "room", 31, 30, ["northern europe"]),
  node("richelieu-814", "Room 814 - Money Changer and His Wife", "Room 814", "2", "Richelieu", "room", 43, 30, ["money changer"]),
  node("richelieu-822", "Room 822 - French Paintings", "Room 822", "2", "Richelieu", "room", 53, 30, ["francis i"]),
  node("richelieu-837", "Room 837 - The Lacemaker", "Room 837", "2", "Richelieu", "room", 22, 22, ["lacemaker", "vermeer"]),
  node("sully-900", "Room 900 - French Paintings", "Room 900", "2", "Sully", "room", 67, 36, ["french paintings"]),
  node("sully-912", "Room 912 - The Cheat", "Room 912", "2", "Sully", "room", 82, 36, ["the cheat"]),
  node("sully-940", "Room 940 - The Bather", "Room 940", "2", "Sully", "room", 76, 61, ["bather"]),
];

const edge = (
  from: string,
  to: string,
  distanceMeters: number,
  accessible = true,
  kind: LouvreEdge["kind"] = "walk",
  instruction?: string,
): LouvreEdge => ({ from, to, distanceMeters, accessible, kind, instruction });

export const edges: LouvreEdge[] = [
  edge("exit-pyramid", "hall-pyramid", 70), edge("hall-pyramid", "lift-central-m2", 22),
  edge("hall-pyramid", "entry-denon", 42), edge("hall-pyramid", "entry-richelieu", 42), edge("hall-pyramid", "entry-sully", 48),
  edge("entry-denon", "central-m1", 65, false, "escalator"), edge("entry-richelieu", "central-m1", 65, false, "escalator"), edge("entry-sully", "central-m1", 65, false, "escalator"),
  edge("lift-central-m2", "lift-central-m1", 18, true, "lift", "Take the central lift to Level -1."),
  edge("lift-central-m1", "lift-central-0", 18, true, "lift", "Take the central lift to Level 0."),
  edge("lift-central-0", "lift-central-1", 18, true, "lift", "Take the central lift to Level 1."),
  edge("lift-central-1", "lift-central-2", 18, true, "lift", "Take the central lift to Level 2."),
  edge("central-m1", "lift-central-m1", 14), edge("central-0", "lift-central-0", 14), edge("central-1", "lift-central-1", 14), edge("central-2", "lift-central-2", 14),
  edge("central-m1", "central-0", 16, false, "stairs", "Use the main stairs to Level 0."),
  edge("central-0", "central-1", 16, false, "stairs", "Use the main stairs to Level 1."),
  edge("central-1", "central-2", 16, false, "stairs", "Use the main stairs to Level 2."),
  edge("central-m1", "denon-170", 75), edge("denon-170", "denon-160", 45), edge("central-m1", "richelieu-102", 70), edge("richelieu-102", "richelieu-100", 35), edge("central-m1", "sully-130", 62), edge("sully-130", "sully-133", 35),
  edge("central-0", "richelieu-230", 58), edge("richelieu-230", "richelieu-227", 28), edge("richelieu-227", "richelieu-200", 48), edge("central-0", "sully-300", 52), edge("sully-300", "sully-308", 42), edge("sully-300", "sully-338", 45), edge("sully-338", "sully-323", 35), edge("sully-338", "sully-345", 34), edge("central-0", "denon-406", 55), edge("denon-406", "denon-403", 32), edge("denon-403", "denon-400", 32), edge("denon-400", "denon-427", 42),
  edge("central-1", "denon-705", 48), edge("denon-705", "denon-703", 32), edge("denon-703", "denon-700", 38), edge("denon-703", "denon-710", 38), edge("denon-710", "denon-711", 24), edge("denon-711", "denon-712", 28), edge("denon-712", "denon-718", 34), edge("central-1", "richelieu-500", 60), edge("richelieu-500", "richelieu-503", 28), edge("richelieu-500", "richelieu-544", 38), edge("central-1", "sully-600", 48), edge("sully-600", "sully-635", 48), edge("sully-600", "sully-663", 52),
  edge("central-2", "richelieu-822", 54), edge("richelieu-822", "richelieu-814", 30), edge("richelieu-814", "richelieu-800", 34), edge("richelieu-800", "richelieu-837", 32), edge("central-2", "sully-900", 48), edge("sully-900", "sully-912", 45), edge("sully-900", "sully-940", 50),
];

const sight = (
  id: string,
  title: string,
  subtitle: string,
  nodeId: string,
  room: string,
  wing: string,
  level: LouvreSight["level"],
  category: string,
  sourceUrl: string,
  featured = true,
): LouvreSight => ({ id, title, subtitle, nodeId, room, wing, level, category, sourceUrl, featured });

export const sights: LouvreSight[] = [
  sight("mona-lisa", "Mona Lisa", "Leonardo da Vinci", "denon-711", "711", "Denon", "1", "Painting", "https://www.louvre.fr/en/explore/the-palace/from-the-mona-lisa-to-the-wedding-feast-at-cana"),
  sight("wedding-feast-at-cana", "The Wedding Feast at Cana", "Paolo Veronese", "denon-711", "711", "Denon", "1", "Painting", "https://collections.louvre.fr/en/plan"),
  sight("winged-victory", "Winged Victory of Samothrace", "Hellenistic sculpture", "denon-703", "703", "Denon", "1", "Sculpture", "https://collections.louvre.fr/en/plan"),
  sight("liberty-leading", "Liberty Leading the People", "Eugène Delacroix", "denon-700", "700", "Denon", "1", "Painting", "https://collections.louvre.fr/en/plan"),
  sight("raft-medusa", "The Raft of the Medusa", "Théodore Géricault", "denon-700", "700", "Denon", "1", "Painting", "https://collections.louvre.fr/en/plan"),
  sight("galerie-apollon", "Galerie d'Apollon", "French Crown Jewels", "denon-705", "705", "Denon", "1", "Palace", "https://collections.louvre.fr/en/plan"),
  sight("venus-de-milo", "Venus de Milo", "Ancient Greek sculpture", "sully-345", "345", "Sully", "0", "Sculpture", "https://collections.louvre.fr/ark:/53355/cl010277627"),
  sight("code-hammurabi", "Code of Hammurabi", "Babylonian law stele", "richelieu-227", "227", "Richelieu", "0", "Antiquity", "https://collections.louvre.fr/en/plan"),
  sight("great-sphinx", "Great Sphinx of Tanis", "Egyptian antiquity", "sully-338", "338", "Sully", "0", "Antiquity", "https://collections.louvre.fr/en/plan"),
  sight("palace-darius", "Palace of Darius", "Achaemenid Persia", "sully-308", "308", "Sully", "0", "Antiquity", "https://collections.louvre.fr/en/plan"),
  sight("tamutnefret", "Tamutnefret's Coffin", "Egyptian antiquity", "sully-323", "323", "Sully", "0", "Antiquity", "https://collections.louvre.fr/en/plan"),
  sight("dying-slave", "The Dying Slave", "Michelangelo", "denon-403", "403", "Denon", "0", "Sculpture", "https://collections.louvre.fr/en/plan"),
  sight("five-continents", "Gallery of Five Continents", "Africa, Asia, Oceania and the Americas", "denon-427", "427", "Denon", "0", "Gallery", "https://collections.louvre.fr/en/plan"),
  sight("seated-scribe", "The Seated Scribe", "Egyptian antiquity", "sully-635", "635", "Sully", "1", "Antiquity", "https://collections.louvre.fr/en/plan"),
  sight("sarcophagus-spouses", "Sarcophagus of the Spouses", "Etruscan antiquity", "sully-663", "663", "Sully", "1", "Antiquity", "https://collections.louvre.fr/en/plan"),
  sight("marly-horses", "The Marly Horses", "Guillaume Coustou", "richelieu-102", "102", "Richelieu", "-1", "Sculpture", "https://collections.louvre.fr/en/plan"),
  sight("cycladic-idol", "Cycladic Idol", "Ancient Greek antiquity", "denon-170", "170", "Denon", "-1", "Antiquity", "https://collections.louvre.fr/en/plan"),
  sight("napoleon-apartments", "Napoléon III Apartments", "Second Empire interiors", "richelieu-544", "544", "Richelieu", "1", "Palace", "https://collections.louvre.fr/en/plan"),
  sight("virgin-jeanne", "Virgin of Jeanne d'Evreux", "Gilded silver statuette", "richelieu-503", "503", "Richelieu", "1", "Decorative Art", "https://collections.louvre.fr/en/plan"),
  sight("lacemaker", "The Lacemaker", "Johannes Vermeer", "richelieu-837", "837", "Richelieu", "2", "Painting", "https://collections.louvre.fr/en/plan"),
  sight("money-changer", "The Money Changer and His Wife", "Quentin Matsys", "richelieu-814", "814", "Richelieu", "2", "Painting", "https://collections.louvre.fr/en/plan"),
  sight("francis-i", "Portrait of Francis I", "Jean Clouet", "richelieu-822", "822", "Richelieu", "2", "Painting", "https://collections.louvre.fr/en/plan"),
  sight("the-cheat", "The Cheat", "Georges de La Tour", "sully-912", "912", "Sully", "2", "Painting", "https://collections.louvre.fr/en/plan"),
  sight("the-bather", "The Bather", "Jean-Auguste-Dominique Ingres", "sully-940", "940", "Sully", "2", "Painting", "https://collections.louvre.fr/en/plan"),
];

export const notices: LouvreClosureNotice[] = [
  {
    id: "verify-closures",
    title: "Check today's gallery access",
    detail: "Rooms and lifts may close without notice. Antiqum routes use a reviewed schematic and must be confirmed with museum signs and staff.",
    status: "warning",
    sourceUrl: LOUVRE_CLOSURES_URL,
    checkedAt: "2026-08-17T00:00:00Z",
  },
  {
    id: "exit-final",
    title: "Leaving is final",
    detail: "The Louvre states that re-entry is not permitted after leaving the museum.",
    status: "information",
    sourceUrl: "https://www.louvre.fr/en/visit/hours-admission",
    checkedAt: "2026-08-17T00:00:00Z",
  },
];

export function indoorBootstrap(): LouvreIndoorBootstrap {
  return {
    museumId: LOUVRE_MUSEUM_ID,
    datasetVersion: LOUVRE_DATASET_VERSION,
    generatedAt: "2026-08-17T00:00:00Z",
    levels,
    nodes,
    edges,
    sights,
    notices,
    defaultStartNodeId: "hall-pyramid",
    officialClosuresUrl: LOUVRE_CLOSURES_URL,
    emergencyNotice: "For emergencies, follow illuminated exit signs and Louvre staff instructions. Do not rely on this visitor route.",
    attribution: "Schematic navigation data by Antiqum. Artwork and room information: Musée du Louvre, accessed 17 August 2026. Not an official Louvre application.",
  };
}
