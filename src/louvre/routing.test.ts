import assert from "node:assert/strict";
import test from "node:test";

import { nodes, sights } from "./data";
import { calculateLouvreRoute, calculateLouvreTour, searchLouvreLocations } from "./routing";

test("publishes every reviewed Louvre room and numbered visitor sight", () => {
  const roomNodes = nodes.filter((item) => item.kind === "room");
  assert.ok(roomNodes.length >= 393);
  assert.equal(sights.length, 30);
  assert.deepEqual(sights.map((item) => item.mapNumber), [...Array(30)].map((_, index) => index + 1));
  assert.equal(sights.every((item) => nodes.some((node) => node.id === item.nodeId)), true);
});

test("search resolves artwork names and room numbers", () => {
  assert.equal(searchLouvreLocations("Mona Lisa").sights[0]?.id, "mona-lisa");
  assert.equal(searchLouvreLocations("room 345").nodes[0]?.id, "sully-345");
});

test("routes from the Mona Lisa to the verified visitor exit", () => {
  const route = calculateLouvreRoute({ fromNodeId: "denon-711", nearestVisitorExit: true });
  assert.equal(route.destinationNodeId, "exit-pyramid");
  assert.ok(route.nodeIds.includes("hall-pyramid"));
  assert.ok(route.distanceMeters > 0);
});

test("routes from a previously unmapped official room", () => {
  const route = calculateLouvreRoute({ fromNodeId: "richelieu-236", sightId: "mona-lisa" });
  assert.equal(route.destinationNodeId, "denon-711");
  assert.ok(route.nodeIds.includes("central-0"));
  assert.ok(route.nodeIds.includes("central-1"));
});

test("accessible routes use the lift instead of stairs", () => {
  const route = calculateLouvreRoute({ fromNodeId: "denon-711", toNodeId: "sully-345", accessible: true });
  assert.ok(route.steps.some((step) => step.kind === "lift"));
  assert.equal(route.steps.some((step) => step.kind === "stairs"), false);
});

test("tour groups sights in one room and optionally finishes at the exit", () => {
  const tour = calculateLouvreTour({
    fromNodeId: "hall-pyramid",
    sightIds: ["mona-lisa", "wedding-feast-at-cana", "venus-de-milo", "winged-victory"],
    finishAtVisitorExit: true,
  });
  assert.equal(tour.orderedSightIds.length, 4);
  assert.equal(tour.orderedNodeIds.filter((id) => id === "denon-711").length, 1);
  assert.equal(tour.route.destinationNodeId, "exit-pyramid");
});
