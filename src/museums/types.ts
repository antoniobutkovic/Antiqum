export const museumCategories = [
  "art",
  "history",
  "archaeology",
  "science",
  "natural_history",
  "technology",
  "military",
  "ethnography",
  "maritime",
  "other",
] as const;

export type MuseumCategory = (typeof museumCategories)[number];
export type MuseumSort = "distance" | "alphabetical";

export interface MuseumRecord {
  wikidataId: string;
  name: string;
  description: string;
  category: MuseumCategory;
  city: string;
  country: string;
  latitude: number;
  longitude: number;
  imageUrl: string | null;
  website: string | null;
  address: string | null;
  foundedYear: string | null;
  sourceModifiedAt: string | null;
  contentHash: string;
}

export interface MuseumApiItem {
  id: string;
  name: string;
  description: string;
  category: string;
  city: string;
  country: string;
  latitude: number;
  longitude: number;
  distanceKm: number;
  imageUrl: string | null;
  website: string | null;
  address: string | null;
  foundedYear: string | null;
}

export interface MuseumPageQuery {
  cursor: string | null;
  limit: number;
  latitude: number;
  longitude: number;
  radiusKm: number | null;
  sort: MuseumSort;
  category: MuseumCategory | null;
  search: string | null;
}

export interface MuseumPageResult {
  museums: MuseumApiItem[];
  nextCursor: string | null;
  hasMore: boolean;
}
