export type LouvreLevelId = "-2" | "-1" | "0" | "1" | "2";

export type LouvreNodeKind =
  | "room"
  | "junction"
  | "stairs"
  | "lift"
  | "entrance"
  | "visitor_exit"
  | "service";

export interface LouvreLevel {
  id: LouvreLevelId;
  label: string;
  subtitle: string;
}

export interface LouvreNode {
  id: string;
  name: string;
  shortName: string;
  level: LouvreLevelId;
  wing: "Denon" | "Richelieu" | "Sully" | "Pyramid";
  kind: LouvreNodeKind;
  x: number;
  y: number;
  searchableAliases: string[];
}

export interface LouvreEdge {
  from: string;
  to: string;
  distanceMeters: number;
  accessible: boolean;
  kind: "walk" | "stairs" | "lift" | "escalator";
  instruction?: string;
}

export interface LouvreSight {
  id: string;
  title: string;
  subtitle: string;
  nodeId: string;
  room: string;
  wing: string;
  level: LouvreLevelId;
  category: string;
  sourceUrl: string;
  imageUrl?: string;
  imageCredit?: string;
  featured: boolean;
}

export interface LouvreClosureNotice {
  id: string;
  title: string;
  detail: string;
  status: "information" | "warning";
  sourceUrl: string;
  checkedAt: string;
}

export interface LouvreIndoorBootstrap {
  museumId: "Q19675";
  datasetVersion: string;
  generatedAt: string;
  levels: LouvreLevel[];
  nodes: LouvreNode[];
  edges: LouvreEdge[];
  sights: LouvreSight[];
  notices: LouvreClosureNotice[];
  defaultStartNodeId: string;
  officialClosuresUrl: string;
  emergencyNotice: string;
  attribution: string;
}

export interface LouvreRouteRequest {
  fromNodeId: string;
  toNodeId?: string;
  sightId?: string;
  nearestVisitorExit?: boolean;
  accessible?: boolean;
}

export interface LouvreTourRequest {
  fromNodeId: string;
  sightIds: string[];
  accessible?: boolean;
  finishAtVisitorExit?: boolean;
}

export interface LouvreRouteStep {
  index: number;
  title: string;
  detail: string;
  level: LouvreLevelId;
  nodeId: string;
  kind: LouvreEdge["kind"] | "arrive";
  distanceMeters: number;
}

export interface LouvreRouteSegment {
  level: LouvreLevelId;
  nodeIds: string[];
}

export interface LouvreRouteResult {
  fromNodeId: string;
  destinationNodeId: string;
  destinationName: string;
  accessible: boolean;
  distanceMeters: number;
  estimatedMinutes: number;
  nodeIds: string[];
  steps: LouvreRouteStep[];
  segments: LouvreRouteSegment[];
  warning: string;
}

export interface LouvreTourResult {
  orderedSightIds: string[];
  orderedNodeIds: string[];
  route: LouvreRouteResult;
  skippedSightIds: string[];
}
