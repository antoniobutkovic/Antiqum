import { edges, nodes, sights } from "./data";
import type {
  LouvreEdge,
  LouvreNode,
  LouvreRouteRequest,
  LouvreRouteResult,
  LouvreRouteSegment,
  LouvreRouteStep,
  LouvreTourRequest,
  LouvreTourResult,
} from "./types";

const nodeById = new Map(nodes.map((item) => [item.id, item]));
const sightById = new Map(sights.map((item) => [item.id, item]));

interface PathResult {
  nodeIds: string[];
  traversedEdges: LouvreEdge[];
  distanceMeters: number;
}

export class LouvreRoutingError extends Error {
  constructor(message: string, readonly status = 400) {
    super(message);
    this.name = "LouvreRoutingError";
  }
}

export function searchLouvreLocations(query: string) {
  const normalized = normalize(query);
  if (!normalized) return { nodes: nodes.filter((item) => item.kind === "room").slice(0, 12), sights: sights.slice(0, 12) };
  const matchingSights = sights
    .map((sight) => ({ sight, score: matchScore(normalized, [sight.title, sight.subtitle, sight.room, sight.wing, sight.category]) }))
    .filter((result) => result.score > 0)
    .sort((a, b) => b.score - a.score || a.sight.title.localeCompare(b.sight.title))
    .slice(0, 20)
    .map((result) => result.sight);
  const matchingNodes = nodes
    .map((item) => ({ item, score: matchScore(normalized, [item.name, item.shortName, item.wing, item.level, ...item.searchableAliases]) }))
    .filter((result) => result.score > 0)
    .sort((a, b) => b.score - a.score || a.item.name.localeCompare(b.item.name))
    .slice(0, 20)
    .map((result) => result.item);
  return { nodes: matchingNodes, sights: matchingSights };
}

export function calculateLouvreRoute(request: LouvreRouteRequest): LouvreRouteResult {
  requireNode(request.fromNodeId);
  let destinationNodeId = request.toNodeId;
  if (request.sightId) destinationNodeId = requireSight(request.sightId).nodeId;
  if (request.nearestVisitorExit) {
    const candidates = nodes.filter((item) => item.kind === "visitor_exit");
    const paths = candidates.map((candidate) => shortestPath(request.fromNodeId, candidate.id, Boolean(request.accessible)));
    destinationNodeId = paths.sort((a, b) => a.distanceMeters - b.distanceMeters)[0]?.nodeIds.at(-1);
  }
  if (!destinationNodeId) throw new LouvreRoutingError("Choose a destination sight, location, or visitor exit");
  const path = shortestPath(request.fromNodeId, destinationNodeId, Boolean(request.accessible));
  return routeResult(path, Boolean(request.accessible));
}

export function calculateLouvreTour(request: LouvreTourRequest): LouvreTourResult {
  requireNode(request.fromNodeId);
  const uniqueSights = [...new Set(request.sightIds)].map(requireSight);
  if (uniqueSights.length === 0) throw new LouvreRoutingError("Choose at least one favorite sight");

  const nodeToSights = new Map<string, string[]>();
  for (const sight of uniqueSights) nodeToSights.set(sight.nodeId, [...(nodeToSights.get(sight.nodeId) ?? []), sight.id]);
  const targetNodeIds = [...nodeToSights.keys()];
  const accessible = Boolean(request.accessible);
  const orderedTargets = targetNodeIds.length <= 11
    ? exactOpenTour(request.fromNodeId, targetNodeIds, accessible)
    : greedyTour(request.fromNodeId, targetNodeIds, accessible);

  const routeTargets = [...orderedTargets];
  if (request.finishAtVisitorExit) {
    const finalStart = routeTargets.at(-1) ?? request.fromNodeId;
    const exit = nodes
      .filter((item) => item.kind === "visitor_exit")
      .map((item) => ({ id: item.id, path: shortestPath(finalStart, item.id, accessible) }))
      .sort((a, b) => a.path.distanceMeters - b.path.distanceMeters)[0];
    if (exit) routeTargets.push(exit.id);
  }

  const combined = combinePaths(request.fromNodeId, routeTargets, accessible);
  return {
    orderedSightIds: orderedTargets.flatMap((nodeId) => nodeToSights.get(nodeId) ?? []),
    orderedNodeIds: orderedTargets,
    route: routeResult(combined, accessible),
    skippedSightIds: [],
  };
}

function shortestPath(startId: string, endId: string, accessible: boolean): PathResult {
  requireNode(startId);
  requireNode(endId);
  if (startId === endId) return { nodeIds: [startId], traversedEdges: [], distanceMeters: 0 };
  const adjacency = new Map<string, LouvreEdge[]>();
  for (const item of edges) {
    if (accessible && !item.accessible) continue;
    adjacency.set(item.from, [...(adjacency.get(item.from) ?? []), item]);
    adjacency.set(item.to, [...(adjacency.get(item.to) ?? []), { ...item, from: item.to, to: item.from }]);
  }
  const distances = new Map<string, number>([[startId, 0]]);
  const previous = new Map<string, { nodeId: string; edge: LouvreEdge }>();
  const pending = new Set(nodes.map((item) => item.id));
  while (pending.size > 0) {
    let current: string | null = null;
    let currentDistance = Number.POSITIVE_INFINITY;
    for (const candidate of pending) {
      const distance = distances.get(candidate) ?? Number.POSITIVE_INFINITY;
      if (distance < currentDistance) {
        current = candidate;
        currentDistance = distance;
      }
    }
    if (current === null || !Number.isFinite(currentDistance)) break;
    pending.delete(current);
    if (current === endId) break;
    for (const item of adjacency.get(current) ?? []) {
      if (!pending.has(item.to)) continue;
      const proposed = currentDistance + item.distanceMeters;
      if (proposed < (distances.get(item.to) ?? Number.POSITIVE_INFINITY)) {
        distances.set(item.to, proposed);
        previous.set(item.to, { nodeId: current, edge: item });
      }
    }
  }
  if (!previous.has(endId)) throw new LouvreRoutingError("No suitable route is available for these options", 422);
  const reversedNodes = [endId];
  const reversedEdges: LouvreEdge[] = [];
  let cursor = endId;
  while (cursor !== startId) {
    const item = previous.get(cursor);
    if (!item) throw new LouvreRoutingError("Route data is incomplete", 503);
    reversedEdges.push(item.edge);
    cursor = item.nodeId;
    reversedNodes.push(cursor);
  }
  return {
    nodeIds: reversedNodes.reverse(),
    traversedEdges: reversedEdges.reverse(),
    distanceMeters: distances.get(endId) ?? 0,
  };
}

function routeResult(path: PathResult, accessible: boolean): LouvreRouteResult {
  const destination = requireNode(path.nodeIds.at(-1)!);
  return {
    fromNodeId: path.nodeIds[0]!,
    destinationNodeId: destination.id,
    destinationName: destination.name,
    accessible,
    distanceMeters: Math.round(path.distanceMeters),
    estimatedMinutes: Math.max(1, Math.ceil(path.distanceMeters / (accessible ? 55 : 70))),
    nodeIds: path.nodeIds,
    steps: buildSteps(path),
    segments: buildSegments(path.nodeIds),
    warning: "Visitor guidance only. Check current room closures and follow Louvre signs and staff instructions.",
  };
}

function buildSteps(path: PathResult): LouvreRouteStep[] {
  const result: LouvreRouteStep[] = path.traversedEdges.map((edge, index) => {
    const destination = requireNode(edge.to);
    const title = edge.instruction ?? instructionFor(edge, destination);
    return {
      index: index + 1,
      title,
      detail: `${Math.round(edge.distanceMeters)} m · ${destination.wing} · Level ${destination.level}`,
      level: destination.level,
      nodeId: destination.id,
      kind: edge.kind,
      distanceMeters: edge.distanceMeters,
    };
  });
  const destination = requireNode(path.nodeIds.at(-1)!);
  result.push({
    index: result.length + 1,
    title: `Arrive at ${destination.shortName}`,
    detail: destination.name,
    level: destination.level,
    nodeId: destination.id,
    kind: "arrive",
    distanceMeters: 0,
  });
  return result;
}

function instructionFor(edge: LouvreEdge, destination: LouvreNode): string {
  if (edge.kind === "lift") return `Take the lift to ${destination.shortName}`;
  if (edge.kind === "stairs") return `Use the stairs to ${destination.shortName}`;
  if (edge.kind === "escalator") return `Use the escalator toward ${destination.shortName}`;
  return `Continue to ${destination.shortName}`;
}

function buildSegments(nodeIds: string[]): LouvreRouteSegment[] {
  const segments: LouvreRouteSegment[] = [];
  for (const id of nodeIds) {
    const level = requireNode(id).level;
    const current = segments.at(-1);
    if (!current || current.level !== level) segments.push({ level, nodeIds: [id] });
    else current.nodeIds.push(id);
  }
  return segments;
}

function combinePaths(startId: string, targets: string[], accessible: boolean): PathResult {
  const nodeIds = [startId];
  const traversedEdges: LouvreEdge[] = [];
  let distanceMeters = 0;
  let cursor = startId;
  for (const target of targets) {
    const path = shortestPath(cursor, target, accessible);
    nodeIds.push(...path.nodeIds.slice(1));
    traversedEdges.push(...path.traversedEdges);
    distanceMeters += path.distanceMeters;
    cursor = target;
  }
  return { nodeIds, traversedEdges, distanceMeters };
}

function exactOpenTour(startId: string, targets: string[], accessible: boolean): string[] {
  const count = targets.length;
  const distances = Array.from({ length: count + 1 }, () => Array<number>(count).fill(0));
  for (let index = 0; index < count; index += 1) distances[0]![index] = shortestPath(startId, targets[index]!, accessible).distanceMeters;
  for (let from = 0; from < count; from += 1) {
    for (let to = 0; to < count; to += 1) distances[from + 1]![to] = from === to ? 0 : shortestPath(targets[from]!, targets[to]!, accessible).distanceMeters;
  }
  const dp = new Map<string, { cost: number; previous: number | null }>();
  for (let index = 0; index < count; index += 1) dp.set(`${1 << index}:${index}`, { cost: distances[0]![index]!, previous: null });
  for (let mask = 1; mask < 1 << count; mask += 1) {
    for (let last = 0; last < count; last += 1) {
      const current = dp.get(`${mask}:${last}`);
      if (!current) continue;
      for (let next = 0; next < count; next += 1) {
        if (mask & (1 << next)) continue;
        const nextMask = mask | (1 << next);
        const key = `${nextMask}:${next}`;
        const cost = current.cost + distances[last + 1]![next]!;
        if (cost < (dp.get(key)?.cost ?? Number.POSITIVE_INFINITY)) dp.set(key, { cost, previous: last });
      }
    }
  }
  const fullMask = (1 << count) - 1;
  let last = [...Array(count).keys()].sort((a, b) => (dp.get(`${fullMask}:${a}`)?.cost ?? Infinity) - (dp.get(`${fullMask}:${b}`)?.cost ?? Infinity))[0];
  const order: number[] = [];
  let mask = fullMask;
  while (last !== undefined) {
    order.push(last);
    const previous = dp.get(`${mask}:${last}`)?.previous;
    mask ^= 1 << last;
    if (previous === null || previous === undefined) break;
    last = previous;
  }
  return order.reverse().map((index) => targets[index]!);
}

function greedyTour(startId: string, targets: string[], accessible: boolean): string[] {
  const remaining = new Set(targets);
  const order: string[] = [];
  let cursor = startId;
  while (remaining.size > 0) {
    const next = [...remaining].sort((a, b) => shortestPath(cursor, a, accessible).distanceMeters - shortestPath(cursor, b, accessible).distanceMeters)[0]!;
    order.push(next);
    remaining.delete(next);
    cursor = next;
  }
  return order;
}

function requireNode(id: string): LouvreNode {
  const item = nodeById.get(id);
  if (!item) throw new LouvreRoutingError(`Unknown Louvre location: ${id}`);
  return item;
}

function requireSight(id: string) {
  const item = sightById.get(id);
  if (!item) throw new LouvreRoutingError(`Unknown Louvre sight: ${id}`);
  return item;
}

function normalize(value: string): string {
  return value.trim().toLocaleLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
}

function matchScore(query: string, values: string[]): number {
  const normalizedValues = values.map(normalize);
  if (normalizedValues.some((value) => value === query)) return 100;
  if (normalizedValues.some((value) => value.startsWith(query))) return 70;
  if (normalizedValues.some((value) => value.includes(query))) return 40;
  const tokens = query.split(/\s+/).filter(Boolean);
  return normalizedValues.some((value) => tokens.every((token) => value.includes(token))) ? 20 : 0;
}
