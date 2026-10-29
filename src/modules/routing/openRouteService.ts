import AsyncStorage from '@react-native-async-storage/async-storage';
import { GeoPoint, ManeuverInstruction, NavigationSession } from '../../types/navigation';

const STORAGE_KEY = '@voice_nav_ors_api_key';
const GEOCODE_URL = 'https://api.openrouteservice.org/geocode/search';
const DIRECTIONS_URL = 'https://api.openrouteservice.org/v2/directions/driving-car/geojson';

export class OpenRouteService {
  async getStoredApiKey(): Promise<string> {
    try {
      const key = await AsyncStorage.getItem(STORAGE_KEY);
      return key?.trim() || '';
    } catch {
      return '';
    }
  }

  async saveApiKey(key: string): Promise<void> {
    try {
      await AsyncStorage.setItem(STORAGE_KEY, key.trim());
    } catch (err) {
      console.warn('Failed to save API key to AsyncStorage', err);
    }
  }

  async geocode(destinationQuery: string, proximity?: GeoPoint | null): Promise<GeoPoint> {
    const apiKey = await this.getStoredApiKey();
    if (!apiKey) {
      throw new Error('OpenRouteService API key not set. Tap settings icon to configure.');
    }

    let url = `${GEOCODE_URL}?api_key=${encodeURIComponent(apiKey)}&text=${encodeURIComponent(
      destinationQuery
    )}&boundary.country=IND&size=1`;

    if (proximity) {
      url += `&focus.point.lat=${proximity.latitude}&focus.point.lon=${proximity.longitude}`;
    }

    const res = await fetch(url, { headers: { Accept: 'application/json' } });
    if (!res.ok) {
      throw new Error(`Geocoding HTTP error: ${res.status}`);
    }

    const data = await res.json();
    const features = data.features;
    if (!features || features.length === 0) {
      throw new Error(`Location '${destinationQuery}' not found in India.`);
    }

    const [lon, lat] = features[0].geometry.coordinates;
    const label = features[0].properties?.label || destinationQuery;
    return { latitude: lat, longitude: lon, label };
  }

  async getRoute(start: GeoPoint, destination: GeoPoint): Promise<NavigationSession> {
    const apiKey = await this.getStoredApiKey();
    if (!apiKey) {
      throw new Error('OpenRouteService API key not set. Tap settings icon to configure.');
    }

    const payload = {
      coordinates: [
        [start.longitude, start.latitude],
        [destination.longitude, destination.latitude],
      ],
      instructions: true,
    };

    const res = await fetch(DIRECTIONS_URL, {
      method: 'POST',
      headers: {
        Authorization: apiKey,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      throw new Error(`Directions HTTP error: ${res.status}`);
    }

    const data = await res.json();
    const segment = data.features?.[0]?.properties?.segments?.[0];
    if (!segment) {
      throw new Error('No driving route found between points.');
    }

    const maneuvers: ManeuverInstruction[] = (segment.steps || []).map((step: any) => ({
      instruction: step.instruction,
      distanceMeters: Math.round(step.distance),
      streetName: step.name || '',
      turnType: String(step.type),
    }));

    return {
      isActive: true,
      destinationName: destination.label || 'Destination',
      destinationCoords: destination,
      maneuvers,
      currentStepIndex: 0,
      totalDistanceMeters: Math.round(segment.distance),
      totalDurationSeconds: Math.round(segment.duration),
    };
  }
}
